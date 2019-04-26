/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.variable.VariableValueException;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.schedule.TimeProvider;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.NullableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

/**
 * Variables service for reading and writing variables, and for setting a version number for the current thread to
 * consider variables for.
 * <p>
 * Consider a statement as follows: select * from MyEvent as A where A.val &gt; var1 and A.val2 &gt; var1 and A.val3 &gt; var2
 * <p>
 * Upon statement execution we need to guarantee that the same atomic value for all variables is applied for all
 * variable reads (by expressions typically) within the statement.
 * <p>
 * Designed to support:
 * <ol>
 * <li>lock-less read of the current and prior version, locked reads for older versions
 * <li>atomicity by keeping multiple versions for each variable and a threadlocal that receives the current version each call
 * <li>one write lock for all variables (required to coordinate with single global version number),
 * however writes are very fast (entry to collection plus increment an int) and therefore blocking should not be an issue
 * </ol>
 * <p>
 * As an alternative to a version-based design, a read-lock for the variable space could also be used, with the following
 * disadvantages: The write lock may just not be granted unless fair locks are used which are more expensive; And
 * a read-lock is more expensive to acquire for multiple CPUs; A thread-local is still need to deal with
 * "set var1=3, var2=var1+1" assignments where the new uncommitted value must be visible in the local evaluation.
 * <p>
 * Every new write to a variable creates a new version. Thus when reading variables, readers can ignore newer versions
 * and a read lock is not required in most circumstances.
 * <p>
 * This algorithm works as follows:
 * <p>
 * A thread processing an event into the runtimevia sendEvent() calls the "setLocalVersion" method once
 * before processing a statement that has variables.
 * This places into a threadlocal variable the current version number, say version 570.
 * <p>
 * A statement that reads a variable has an variable node that has a VariableReader handle
 * obtained during validation (example).
 * <p>
 * The VariableReader takes the version from the threadlocal (570) and compares the version number with the
 * version numbers held for the variable.
 * If the current version is same or lower (520, as old or older) then the threadlocal version,
 * then use the current value.
 * If the current version is higher (571, newer) then the threadlocal version, then go to the prior value.
 * Use the prior value until a version is found that as old or older then the threadlocal version.
 * <p>
 * If no version can be found that is old enough, output a warning and return the newest version.
 * This should not happen, unless a thread is executing for very long within a single statement such that
 * lifetime-old-version time speriod passed before the thread asks for variable values.
 * <p>
 * As version numbers are counted up they may reach a boundary. Any write transaction after the boundary
 * is reached performs a roll-over. In a roll-over, all variables version lists are
 * newly created and any existing threads that read versions go against a (old) high-collection,
 * while new threads reading the reset version go against a new low-collection.
 * <p>
 * The class also allows an optional state handler to be plugged in to handle persistence for variable state.
 * The state handler gets invoked when a variable changes value, and when a variable gets created
 * to obtain the current value from persistence, if any.
 */
public class VariableManagementServiceImpl implements VariableManagementService {
    private final static Logger log = LoggerFactory.getLogger(VariableManagementServiceImpl.class);

    /**
     * Sets the boundary above which a reader considers the high-version list of variable values.
     * For use in roll-over when the current version number overflows the ROLLOVER_WRITER_BOUNDARY.
     */
    protected final static int ROLLOVER_READER_BOUNDARY = Integer.MAX_VALUE - 100000;

    /**
     * Applicable for each variable if more then the number of versions accumulated, check
     * timestamps to determine if a version can be expired.
     */
    protected final static int HIGH_WATERMARK_VERSIONS = 50;

    // Each variable has an index number, a context-partition id, a current version and a list of values
    private final ArrayList<ConcurrentHashMap<Integer, VariableReader>> variableVersionsPerCP;

    // Each variable and a context-partition id may have a set of callbacks to invoke when the variable changes
    private final ArrayList<Map<Integer, Set<VariableChangeCallback>>> changeCallbacksPerCP;

    // Keep the variable list
    private final Map<String, VariableDeployment> deploymentsWithVariables;

    // Write lock taken on write of any variable; and on read of older versions
    private final ReadWriteLock readWriteLock;

    // Thread-local for the visible version per thread
    private VariableVersionThreadLocal versionThreadLocal = new VariableVersionThreadLocal();

    // Number of milliseconds that old versions of a variable are allowed to live
    private final long millisecondLifetimeOldVersions;
    private final TimeProvider timeProvider;
    private final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    private final VariableStateNonConstHandler optionalStateHandler;

    private volatile int currentVersionNumber;
    private int currentVariableNumber;

    /**
     * Ctor.
     *
     * @param millisecondLifetimeOldVersions number of milliseconds a version may hang around before expiry
     * @param timeProvider                   provides the current time
     * @param optionalStateHandler           a optional plug-in that may store variable state and retrieve state upon creation
     * @param eventBeanTypedEventFactory     event adapters
     */
    public VariableManagementServiceImpl(long millisecondLifetimeOldVersions, TimeProvider timeProvider, EventBeanTypedEventFactory eventBeanTypedEventFactory, VariableStateNonConstHandler optionalStateHandler) {
        this(0, millisecondLifetimeOldVersions, timeProvider, eventBeanTypedEventFactory, optionalStateHandler);
    }

    /**
     * Ctor.
     *
     * @param startVersion                   the first version number to start from
     * @param millisecondLifetimeOldVersions number of milliseconds a version may hang around before expiry
     * @param timeProvider                   provides the current time
     * @param optionalStateHandler           a optional plug-in that may store variable state and retrieve state upon creation
     * @param eventBeanTypedEventFactory     for finding event types
     */
    protected VariableManagementServiceImpl(int startVersion, long millisecondLifetimeOldVersions, TimeProvider timeProvider, EventBeanTypedEventFactory eventBeanTypedEventFactory, VariableStateNonConstHandler optionalStateHandler) {
        this.millisecondLifetimeOldVersions = millisecondLifetimeOldVersions;
        this.timeProvider = timeProvider;
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
        this.optionalStateHandler = optionalStateHandler;
        this.deploymentsWithVariables = new HashMap<>();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.variableVersionsPerCP = new ArrayList<>();
        this.changeCallbacksPerCP = new ArrayList<>();
        currentVersionNumber = startVersion;
    }

    public void destroy() {
        versionThreadLocal = new VariableVersionThreadLocal();
    }

    public synchronized void removeVariableIfFound(String deploymentId, String variableName) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            return;
        }

        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Removing variable '" + variableName + "'");
        }
        entry.remove(variableName);

        if (optionalStateHandler != null && !variable.getMetaData().isConstant()) {
            ConcurrentHashMap<Integer, VariableReader> readers = variableVersionsPerCP.get(variable.getVariableNumber());
            Set<Integer> cps = Collections.emptySet();
            if (readers != null) {
                cps = readers.keySet();
            }
            optionalStateHandler.removeVariable(variable, deploymentId, cps);
        }

        int number = variable.getVariableNumber();
        variableVersionsPerCP.set(number, null);
        changeCallbacksPerCP.set(number, null);
    }

    public void setLocalVersion() {
        versionThreadLocal.getCurrentThread().setVersion(currentVersionNumber);
    }

    public void registerCallback(String deploymentId, String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            return;
        }

        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            return;
        }

        Map<Integer, Set<VariableChangeCallback>> cps = changeCallbacksPerCP.get(variable.getVariableNumber());
        if (cps == null) {
            cps = new HashMap<Integer, Set<VariableChangeCallback>>();
            changeCallbacksPerCP.set(variable.getVariableNumber(), cps);
        }

        if (variable.getMetaData().getOptionalContextName() == null) {
            agentInstanceId = DEFAULT_AGENT_INSTANCE_ID;
        }

        Set<VariableChangeCallback> callbacks = cps.get(agentInstanceId);
        if (callbacks == null) {
            callbacks = new CopyOnWriteArraySet<VariableChangeCallback>();
            cps.put(agentInstanceId, callbacks);
        }
        callbacks.add(variableChangeCallback);
    }

    public void unregisterCallback(String deploymentId, String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            return;
        }

        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            return;
        }

        Map<Integer, Set<VariableChangeCallback>> cps = changeCallbacksPerCP.get(variable.getVariableNumber());
        if (cps == null) {
            return;
        }

        if (variable.getMetaData().getOptionalContextName() == null) {
            agentInstanceId = 0;
        }

        Set<VariableChangeCallback> callbacks = cps.get(agentInstanceId);
        if (callbacks != null) {
            callbacks.remove(variableChangeCallback);
        }
    }

    public synchronized void addVariable(String deploymentId, VariableMetaData metaData, String optionalDeploymentIdContext, DataInputOutputSerde<Object> optionalSerde) {

        // check if already exists
        VariableDeployment deploymentEntry = deploymentsWithVariables.get(deploymentId);
        if (deploymentEntry != null) {
            Variable variable = deploymentEntry.getVariable(metaData.getVariableName());
            if (variable != null) {
                throw new IllegalArgumentException("Variable already exists by name '" + metaData.getVariableName() + "' and deployment '" + deploymentId + "'");
            }
        } else {
            deploymentEntry = new VariableDeployment();
            deploymentsWithVariables.put(deploymentId, deploymentEntry);
        }

        // find empty spot
        int emptySpot = -1;
        int count = 0;
        for (Map<Integer, VariableReader> entry : variableVersionsPerCP) {
            if (entry == null) {
                emptySpot = count;
                break;
            }
            count++;
        }

        int variableNumber;
        if (emptySpot != -1) {
            variableNumber = emptySpot;
            variableVersionsPerCP.set(emptySpot, new ConcurrentHashMap<>());
            changeCallbacksPerCP.set(emptySpot, null);
        } else {
            variableNumber = currentVariableNumber;
            variableVersionsPerCP.add(new ConcurrentHashMap<>());
            changeCallbacksPerCP.add(null);
            currentVariableNumber++;
        }

        Variable variable = new Variable(variableNumber, deploymentId, metaData, optionalDeploymentIdContext);
        deploymentEntry.addVariable(metaData.getVariableName(), variable);

        if (optionalStateHandler != null && !metaData.isConstant()) {
            optionalStateHandler.addVariable(deploymentId, metaData.getVariableName(), variable, optionalSerde);
        }
    }

    public void allocateVariableState(String deploymentId, String variableName, int agentInstanceId, boolean recovery, NullableObject<Object> initialValue, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            throw new IllegalArgumentException("Failed to find variable deployment id '" + deploymentId + "'");
        }

        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            throw new IllegalArgumentException("Failed to find variable '" + variableName + "'");
        }

        // Check current state - see if the variable exists in the state handler
        Object initialState;
        if (initialValue != null) {
            initialState = initialValue.getObject();
        } else {
            initialState = variable.getMetaData().getValueWhenAvailable();
        }
        if (variable.getMetaData().getEventType() != null && initialState != null && !(initialState instanceof EventBean)) {
            initialState = eventBeanTypedEventFactory.adapterForTypedBean(initialState, variable.getMetaData().getEventType());
        }
        if (optionalStateHandler != null && !variable.getMetaData().isConstant()) {
            NullableObject<Object> priorValue = optionalStateHandler.getHasState(variable, agentInstanceId);
            if (recovery) {
                if (priorValue != null) {
                    initialState = priorValue.getObject();
                }
            } else {
                if (priorValue == null) { // we do not already have a value
                    optionalStateHandler.setState(variable, agentInstanceId, initialState);
                } else {
                    initialState = priorValue.getObject();
                }
            }
        }

        // create new holder for versions
        long timestamp = timeProvider.getTime();
        VersionedValueList<Object> valuePerVersion = new VersionedValueList<>(variableName, currentVersionNumber, initialState, timestamp, millisecondLifetimeOldVersions, readWriteLock.readLock(), HIGH_WATERMARK_VERSIONS, false);
        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(variable.getVariableNumber());
        VariableReader reader = new VariableReader(variable, versionThreadLocal, valuePerVersion);
        cps.put(agentInstanceId, reader);
    }

    public void deallocateVariableState(String deploymentId, String variableName, int agentInstanceId) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            throw new IllegalArgumentException("Failed to find variable deployment id '" + deploymentId + "'");
        }

        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            throw new IllegalArgumentException("Failed to find variable '" + variableName + "'");
        }

        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(variable.getVariableNumber());
        cps.remove(agentInstanceId);

        if (optionalStateHandler != null && !variable.getMetaData().isConstant()) {
            optionalStateHandler.removeState(variable, agentInstanceId);
        }
    }

    public Variable getVariableMetaData(String deploymentId, String variableName) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            return null;
        }
        return entry.getVariable(variableName);
    }

    public VariableReader getReader(String deploymentId, String variableName, int agentInstanceIdAccessor) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            return null;
        }
        Variable variable = entry.getVariable(variableName);
        if (variable == null) {
            return null;
        }
        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(variable.getVariableNumber());
        if (variable.getMetaData().getOptionalContextName() == null) {
            return cps.get(DEFAULT_AGENT_INSTANCE_ID);
        }
        return cps.get(agentInstanceIdAccessor);
    }

    public void write(int variableNumber, int agentInstanceId, Object newValue) {
        VariableVersionThreadEntry entry = versionThreadLocal.getCurrentThread();
        if (entry.getUncommitted() == null) {
            entry.setUncommitted(new HashMap<Integer, Pair<Integer, Object>>());
        }
        entry.getUncommitted().put(variableNumber, new Pair<Integer, Object>(agentInstanceId, newValue));
    }

    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    public void commit() {
        VariableVersionThreadEntry entry = versionThreadLocal.getCurrentThread();
        if (entry.getUncommitted() == null) {
            return;
        }

        // get new version for adding the new values (1 or many new values)
        int newVersion = currentVersionNumber + 1;

        if (currentVersionNumber == ROLLOVER_READER_BOUNDARY) {
            // Roll over to new collections;
            // This honors existing threads that will now use the "high" collection in the reader for high version requests
            // and low collection (new and updated) for low version requests
            rollOver();
            newVersion = 2;
        }
        long timestamp = timeProvider.getTime();

        // apply all uncommitted changes
        for (Map.Entry<Integer, Pair<Integer, Object>> uncommittedEntry : entry.getUncommitted().entrySet()) {
            Map<Integer, VariableReader> cps = variableVersionsPerCP.get(uncommittedEntry.getKey());
            VariableReader reader = cps.get(uncommittedEntry.getValue().getFirst());
            VersionedValueList<Object> versions = reader.getVersionsLow();

            // add new value as a new version
            Object newValue = uncommittedEntry.getValue().getSecond();
            Object oldValue = versions.addValue(newVersion, newValue, timestamp);

            // make a callback that the value changed
            Map<Integer, Set<VariableChangeCallback>> cpsCallback = changeCallbacksPerCP.get(uncommittedEntry.getKey());
            if (cpsCallback != null) {
                Set<VariableChangeCallback> callbacks = cpsCallback.get(uncommittedEntry.getValue().getFirst());
                if (callbacks != null) {
                    for (VariableChangeCallback callback : callbacks) {
                        callback.update(newValue, oldValue);
                    }
                }
            }

            // Check current state - see if the variable exists in the state handler
            if (optionalStateHandler != null) {
                VariableMetaData metaData = reader.getMetaData();
                if (!metaData.isConstant()) {
                    int agentInstanceId = metaData.getOptionalContextName() == null ? DEFAULT_AGENT_INSTANCE_ID : uncommittedEntry.getValue().getFirst();
                    optionalStateHandler.setState(reader.getVariable(), agentInstanceId, newValue);
                }
            }
        }

        // this makes the new values visible to other threads (not this thread unless set-version called again)
        currentVersionNumber = newVersion;
        entry.setUncommitted(null);    // clean out uncommitted variables
    }

    public void rollback() {
        VariableVersionThreadEntry entry = versionThreadLocal.getCurrentThread();
        entry.setUncommitted(null);
    }

    /**
     * Rollover includes creating a new
     */
    private void rollOver() {
        for (Map<Integer, VariableReader> entryCP : variableVersionsPerCP) {
            for (Map.Entry<Integer, VariableReader> entry : entryCP.entrySet()) {
                String name = entry.getValue().getMetaData().getVariableName();
                long timestamp = timeProvider.getTime();

                // Construct a new collection, forgetting the history
                VersionedValueList<Object> versionsOld = entry.getValue().getVersionsLow();
                Object currentValue = versionsOld.getCurrentAndPriorValue().getCurrentVersion().getValue();
                VersionedValueList<Object> versionsNew = new VersionedValueList<Object>(name, 1, currentValue, timestamp, millisecondLifetimeOldVersions, readWriteLock.readLock(), HIGH_WATERMARK_VERSIONS, false);

                // Tell the reader to use the high collection for old requests
                entry.getValue().setVersionsHigh(versionsOld);
                entry.getValue().setVersionsLow(versionsNew);
            }
        }
    }

    public void checkAndWrite(String deploymentId, String variableName, int agentInstanceId, Object newValue) throws VariableValueException {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            throw new IllegalArgumentException("Failed to find variable deployment id '" + deploymentId + "'");
        }
        Variable variable = entry.getVariable(variableName);
        int variableNumber = variable.getVariableNumber();

        if (newValue == null) {
            write(variableNumber, agentInstanceId, null);
            return;
        }

        Class valueType = newValue.getClass();

        if (variable.getMetaData().getEventType() != null) {
            if (!JavaClassHelper.isSubclassOrImplementsInterface(newValue.getClass(), variable.getMetaData().getEventType().getUnderlyingType())) {
                throw new VariableValueException("Variable '" + variableName
                    + "' of declared event type '" + variable.getMetaData().getEventType().getName() + "' underlying type '" + variable.getMetaData().getEventType().getUnderlyingType().getName() +
                    "' cannot be assigned a value of type '" + valueType.getName() + "'");
            }
            EventBean eventBean = eventBeanTypedEventFactory.adapterForTypedBean(newValue, variable.getMetaData().getEventType());
            write(variableNumber, agentInstanceId, eventBean);
            return;
        }

        Class variableType = variable.getMetaData().getType();
        if ((valueType.equals(variableType)) || (variableType == Object.class)) {
            write(variableNumber, agentInstanceId, newValue);
            return;
        }

        if (JavaClassHelper.isSubclassOrImplementsInterface(valueType, variableType)) {
            write(variableNumber, agentInstanceId, newValue);
            return;
        }

        if ((!JavaClassHelper.isNumeric(variableType)) ||
            (!JavaClassHelper.isNumeric(valueType))) {
            throw new VariableValueException(VariableUtil.getAssigmentExMessage(variableName, variableType, valueType));
        }

        // determine if the expression type can be assigned
        if (!(JavaClassHelper.canCoerce(valueType, variableType))) {
            throw new VariableValueException(VariableUtil.getAssigmentExMessage(variableName, variableType, valueType));
        }

        Object valueCoerced = JavaClassHelper.coerceBoxed((Number) newValue, variableType);
        write(variableNumber, agentInstanceId, valueCoerced);
    }

    public ConcurrentHashMap<Integer, VariableReader> getReadersPerCP(String deploymentId, String variableName) {
        VariableDeployment entry = deploymentsWithVariables.get(deploymentId);
        if (entry == null) {
            throw new IllegalArgumentException("Failed to find variable deployment id '" + deploymentId + "'");
        }
        Variable variable = entry.getVariable(variableName);
        return variableVersionsPerCP.get(variable.getVariableNumber());
    }

    public Map<DeploymentIdNamePair, VariableReader> getVariableReadersNonCP() {
        Map<DeploymentIdNamePair, VariableReader> result = new HashMap<>();
        for (Map.Entry<String, VariableDeployment> deployment : deploymentsWithVariables.entrySet()) {
            for (Map.Entry<String, Variable> variable : deployment.getValue().getVariables().entrySet()) {
                int variableNum = variable.getValue().getVariableNumber();
                if (variable.getValue().getMetaData().getOptionalContextName() == null) {
                    for (Map.Entry<Integer, VariableReader> entry : variableVersionsPerCP.get(variableNum).entrySet()) {
                        result.put(new DeploymentIdNamePair(deployment.getKey(), variable.getKey()), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    public VariableStateNonConstHandler getOptionalStateHandler() {
        return optionalStateHandler;
    }

    public Map<String, VariableDeployment> getDeploymentsWithVariables() {
        return deploymentsWithVariables;
    }

    public void traverseVariables(BiConsumer<String, Variable> consumer) {
        for (Map.Entry<String, VariableDeployment> entry : deploymentsWithVariables.entrySet()) {
            for (Map.Entry<String, Variable> variable : entry.getValue().getVariables().entrySet()) {
                consumer.accept(entry.getKey(), variable.getValue());
            }
        }
    }
}
