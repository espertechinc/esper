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
package com.espertech.esper.epl.variable;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.VariableValueException;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.StatementExtensionSvcContext;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.schedule.TimeProvider;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * A thread processing an event into the engine via sendEvent() calls the "setLocalVersion" method once
 * before processing a statement that has variables.
 * This places into a threadlocal variable the current version number, say version 570.
 * <p>
 * A statement that reads a variable has an {@link com.espertech.esper.epl.expression.core.ExprVariableNode} that has a {@link com.espertech.esper.epl.variable.VariableReader} handle
 * obtained during validation (example).
 * <p>
 * The {@link com.espertech.esper.epl.variable.VariableReader} takes the version from the threadlocal (570) and compares the version number with the
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
public class VariableServiceImpl implements VariableService {
    private final static Logger log = LoggerFactory.getLogger(VariableServiceImpl.class);

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
    private final Map<String, VariableMetaData> variables;

    // Write lock taken on write of any variable; and on read of older versions
    private final ReadWriteLock readWriteLock;

    // Thread-local for the visible version per thread
    private VariableVersionThreadLocal versionThreadLocal = new VariableVersionThreadLocal();

    // Number of milliseconds that old versions of a variable are allowed to live
    private final long millisecondLifetimeOldVersions;
    private final TimeProvider timeProvider;
    private final EventAdapterService eventAdapterService;
    private final VariableStateHandler optionalStateHandler;

    private volatile int currentVersionNumber;
    private int currentVariableNumber;

    /**
     * Ctor.
     *
     * @param millisecondLifetimeOldVersions number of milliseconds a version may hang around before expiry
     * @param timeProvider                   provides the current time
     * @param optionalStateHandler           a optional plug-in that may store variable state and retrieve state upon creation
     * @param eventAdapterService            event adapters
     */
    public VariableServiceImpl(long millisecondLifetimeOldVersions, TimeProvider timeProvider, EventAdapterService eventAdapterService, VariableStateHandler optionalStateHandler) {
        this(0, millisecondLifetimeOldVersions, timeProvider, eventAdapterService, optionalStateHandler);
    }

    /**
     * Ctor.
     *
     * @param startVersion                   the first version number to start from
     * @param millisecondLifetimeOldVersions number of milliseconds a version may hang around before expiry
     * @param timeProvider                   provides the current time
     * @param optionalStateHandler           a optional plug-in that may store variable state and retrieve state upon creation
     * @param eventAdapterService            for finding event types
     */
    protected VariableServiceImpl(int startVersion, long millisecondLifetimeOldVersions, TimeProvider timeProvider, EventAdapterService eventAdapterService, VariableStateHandler optionalStateHandler) {
        this.millisecondLifetimeOldVersions = millisecondLifetimeOldVersions;
        this.timeProvider = timeProvider;
        this.eventAdapterService = eventAdapterService;
        this.optionalStateHandler = optionalStateHandler;
        this.variables = new HashMap<String, VariableMetaData>();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.variableVersionsPerCP = new ArrayList<ConcurrentHashMap<Integer, VariableReader>>();
        this.changeCallbacksPerCP = new ArrayList<Map<Integer, Set<VariableChangeCallback>>>();
        currentVersionNumber = startVersion;
    }

    public void destroy() {
        versionThreadLocal = new VariableVersionThreadLocal();
    }

    public synchronized void removeVariableIfFound(String name) {
        VariableMetaData metaData = variables.get(name);
        if (metaData == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Removing variable '" + name + "'");
        }
        variables.remove(name);

        if (optionalStateHandler != null) {
            ConcurrentHashMap<Integer, VariableReader> readers = variableVersionsPerCP.get(metaData.getVariableNumber());
            Set<Integer> cps = Collections.emptySet();
            if (readers != null) {
                cps = readers.keySet();
            }
            optionalStateHandler.removeVariable(name, cps);
        }

        int number = metaData.getVariableNumber();
        variableVersionsPerCP.set(number, null);
        changeCallbacksPerCP.set(number, null);
    }

    public void setLocalVersion() {
        versionThreadLocal.getCurrentThread().setVersion(currentVersionNumber);
    }

    public void registerCallback(String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            return;
        }

        Map<Integer, Set<VariableChangeCallback>> cps = changeCallbacksPerCP.get(metaData.getVariableNumber());
        if (cps == null) {
            cps = new HashMap<Integer, Set<VariableChangeCallback>>();
            changeCallbacksPerCP.set(metaData.getVariableNumber(), cps);
        }

        if (metaData.getContextPartitionName() == null) {
            agentInstanceId = EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID;
        }

        Set<VariableChangeCallback> callbacks = cps.get(agentInstanceId);
        if (callbacks == null) {
            callbacks = new CopyOnWriteArraySet<VariableChangeCallback>();
            cps.put(agentInstanceId, callbacks);
        }
        callbacks.add(variableChangeCallback);
    }

    public void unregisterCallback(String variableName, int agentInstanceId, VariableChangeCallback variableChangeCallback) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            return;
        }

        Map<Integer, Set<VariableChangeCallback>> cps = changeCallbacksPerCP.get(metaData.getVariableNumber());
        if (cps == null) {
            return;
        }

        if (metaData.getContextPartitionName() == null) {
            agentInstanceId = 0;
        }

        Set<VariableChangeCallback> callbacks = cps.get(agentInstanceId);
        if (callbacks != null) {
            callbacks.remove(variableChangeCallback);
        }
    }

    public void createNewVariable(String optionalContextName, String variableName, String variableType, boolean constant, boolean array, boolean arrayOfPrimitive, Object value, EngineImportService engineImportService) throws VariableExistsException, VariableTypeException {
        // Determime the variable type
        Class primitiveType = JavaClassHelper.getPrimitiveClassForName(variableType);
        Class type = JavaClassHelper.getClassForSimpleName(variableType, engineImportService.getClassForNameProvider());
        Class arrayType = null;
        EventType eventType = null;
        if (type == null) {
            if (variableType.toLowerCase(Locale.ENGLISH).equals("object")) {
                type = Object.class;
            }
            if (type == null) {
                eventType = eventAdapterService.getExistsTypeByName(variableType);
                if (eventType != null) {
                    type = eventType.getUnderlyingType();
                }
            }
            if (type == null) {
                try {
                    type = engineImportService.resolveClass(variableType, false);
                    if (array) {
                        arrayType = JavaClassHelper.getArrayType(type);
                    }
                } catch (EngineImportException e) {
                    log.debug("Not found '" + type + "': " + e.getMessage(), e);
                    // expected
                }
            }
            if (type == null) {
                throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                        variableType + "' is not a recognized type");
            }
            if (array && eventType != null) {
                throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                        variableType + "' cannot be declared as an array type");
            }
        } else {
            if (array) {
                if (arrayOfPrimitive) {
                    if (primitiveType == null) {
                        throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                                variableType + "' is not a primitive type");
                    }
                    arrayType = JavaClassHelper.getArrayType(primitiveType);
                } else {
                    arrayType = JavaClassHelper.getArrayType(type);
                }
            }
        }

        if ((eventType == null) && (!JavaClassHelper.isJavaBuiltinDataType(type)) && (type != Object.class) && !type.isArray() && !type.isEnum()) {
            if (array) {
                throw new VariableTypeException("Cannot create variable '" + variableName + "', type '" +
                        variableType + "' cannot be declared as an array, only scalar types can be array");
            }
            eventType = eventAdapterService.addBeanType(type.getName(), type, false, false, false);
        }

        if (arrayType != null) {
            type = arrayType;
        }

        createNewVariable(variableName, optionalContextName, type, eventType, constant, value);
    }

    private synchronized void createNewVariable(String variableName, String optionalContextName, Class type, EventType eventType, boolean constant, Object value)
            throws VariableExistsException, VariableTypeException {
        // check type
        Class variableType = JavaClassHelper.getBoxedType(type);

        // check if it exists
        VariableMetaData metaData = variables.get(variableName);
        if (metaData != null) {
            throw new VariableExistsException(VariableServiceUtil.getAlreadyDeclaredEx(variableName, false));
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
            variableVersionsPerCP.set(emptySpot, new ConcurrentHashMap<Integer, VariableReader>());
            changeCallbacksPerCP.set(emptySpot, null);
        } else {
            variableNumber = currentVariableNumber;
            variableVersionsPerCP.add(new ConcurrentHashMap<Integer, VariableReader>());
            changeCallbacksPerCP.add(null);
            currentVariableNumber++;
        }

        // check coercion
        Object coercedValue = value;
        if (eventType != null) {
            if ((value != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(value.getClass(), eventType.getUnderlyingType()))) {
                throw new VariableTypeException("Variable '" + variableName
                        + "' of declared event type '" + eventType.getName() + "' underlying type '" + eventType.getUnderlyingType().getName() +
                        "' cannot be assigned a value of type '" + value.getClass().getName() + "'");
            }
            coercedValue = eventAdapterService.adapterForType(value, eventType);
        } else if (variableType == java.lang.Object.class) {
            // no validation
        } else {
            // allow string assignments to non-string variables
            if ((coercedValue != null) && (coercedValue instanceof String)) {
                try {
                    coercedValue = JavaClassHelper.parse(variableType, (String) coercedValue);
                } catch (Exception ex) {
                    throw new VariableTypeException("Variable '" + variableName
                            + "' of declared type " + JavaClassHelper.getClassNameFullyQualPretty(variableType) +
                            " cannot be initialized by value '" + coercedValue + "': " + ex.toString());
                }
            }

            if ((coercedValue != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(coercedValue.getClass(), variableType))) {
                // if the declared type is not numeric or the init value is not numeric, fail
                if ((!JavaClassHelper.isNumeric(variableType)) || (!(coercedValue instanceof Number))) {
                    throw getVariableTypeException(variableName, variableType, coercedValue.getClass());
                }
                if (!(JavaClassHelper.canCoerce(coercedValue.getClass(), variableType))) {
                    throw getVariableTypeException(variableName, variableType, coercedValue.getClass());
                }
                // coerce
                coercedValue = JavaClassHelper.coerceBoxed((Number) coercedValue, variableType);
            }
        }

        final Object initialState = coercedValue;
        VariableStateFactory stateFactory = new VariableStateFactoryConst(initialState);

        metaData = new VariableMetaData(variableName, optionalContextName, variableNumber, variableType, eventType, constant, stateFactory);
        variables.put(variableName, metaData);
    }

    public void allocateVariableState(String variableName, int agentInstanceId, StatementExtensionSvcContext extensionServicesContext, boolean isRecoveringResilient) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            throw new IllegalArgumentException("Failed to find variable '" + variableName + "'");
        }

        // Check current state - see if the variable exists in the state handler
        Object initialState = metaData.getVariableStateFactory().getInitialState();
        if (optionalStateHandler != null) {
            Pair<Boolean, Object> priorValue = optionalStateHandler.getHasState(variableName, metaData.getVariableNumber(), agentInstanceId, metaData.getType(), metaData.getEventType(), extensionServicesContext, metaData.isConstant());
            if (isRecoveringResilient) {
                if (priorValue.getFirst()) {
                    initialState = priorValue.getSecond();
                }
            } else {
                optionalStateHandler.setState(variableName, metaData.getVariableNumber(), agentInstanceId, initialState);
            }
        }

        // create new holder for versions
        long timestamp = timeProvider.getTime();
        VersionedValueList<Object> valuePerVersion = new VersionedValueList<Object>(variableName, currentVersionNumber, initialState, timestamp, millisecondLifetimeOldVersions, readWriteLock.readLock(), HIGH_WATERMARK_VERSIONS, false);
        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(metaData.getVariableNumber());
        VariableReader reader = new VariableReader(metaData, versionThreadLocal, valuePerVersion);
        cps.put(agentInstanceId, reader);
    }

    public void deallocateVariableState(String variableName, int agentInstanceId) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            throw new IllegalArgumentException("Failed to find variable '" + variableName + "'");
        }

        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(metaData.getVariableNumber());
        cps.remove(agentInstanceId);

        if (optionalStateHandler != null) {
            optionalStateHandler.removeState(variableName, metaData.getVariableNumber(), agentInstanceId);
        }
    }

    public VariableMetaData getVariableMetaData(String variableName) {
        return variables.get(variableName);
    }

    public VariableReader getReader(String variableName, int agentInstanceIdAccessor) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            return null;
        }
        Map<Integer, VariableReader> cps = variableVersionsPerCP.get(metaData.getVariableNumber());
        if (metaData.getContextPartitionName() == null) {
            return cps.get(EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
        }
        return cps.get(agentInstanceIdAccessor);
    }

    public String isContextVariable(String variableName) {
        VariableMetaData metaData = variables.get(variableName);
        if (metaData == null) {
            return null;
        }
        return metaData.getContextPartitionName();
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
                String name = versions.getName();
                int agentInstanceId = reader.getVariableMetaData().getContextPartitionName() == null ? EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID : uncommittedEntry.getValue().getFirst();
                optionalStateHandler.setState(name, uncommittedEntry.getKey(), agentInstanceId, newValue);
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
                String name = entry.getValue().getVariableMetaData().getVariableName();
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

    public void checkAndWrite(String variableName, int agentInstanceId, Object newValue) throws VariableValueException {
        VariableMetaData metaData = variables.get(variableName);
        int variableNumber = metaData.getVariableNumber();

        if (newValue == null) {
            write(variableNumber, agentInstanceId, null);
            return;
        }

        Class valueType = newValue.getClass();

        if (metaData.getEventType() != null) {
            if (!JavaClassHelper.isSubclassOrImplementsInterface(newValue.getClass(), metaData.getEventType().getUnderlyingType())) {
                throw new VariableValueException("Variable '" + variableName
                        + "' of declared event type '" + metaData.getEventType().getName() + "' underlying type '" + metaData.getEventType().getUnderlyingType().getName() +
                        "' cannot be assigned a value of type '" + valueType.getName() + "'");
            }
            EventBean eventBean = eventAdapterService.adapterForType(newValue, metaData.getEventType());
            write(variableNumber, agentInstanceId, eventBean);
            return;
        }

        Class variableType = metaData.getType();
        if ((valueType.equals(variableType)) || (variableType == Object.class)) {
            write(variableNumber, agentInstanceId, newValue);
            return;
        }

        if ((!JavaClassHelper.isNumeric(variableType)) ||
                (!JavaClassHelper.isNumeric(valueType))) {
            throw new VariableValueException(VariableServiceUtil.getAssigmentExMessage(variableName, variableType, valueType));
        }

        // determine if the expression type can be assigned
        if (!(JavaClassHelper.canCoerce(valueType, variableType))) {
            throw new VariableValueException(VariableServiceUtil.getAssigmentExMessage(variableName, variableType, valueType));
        }

        Object valueCoerced = JavaClassHelper.coerceBoxed((Number) newValue, variableType);
        write(variableNumber, agentInstanceId, valueCoerced);
    }

    public String toString() {
        StringWriter writer = new StringWriter();
        for (Map.Entry<String, VariableMetaData> entryMeta : variables.entrySet()) {
            int variableNum = entryMeta.getValue().getVariableNumber();
            for (Map.Entry<Integer, VariableReader> entry : variableVersionsPerCP.get(variableNum).entrySet()) {
                VersionedValueList<Object> list = entry.getValue().getVersionsLow();
                writer.write("Variable '" + entry.getKey() + "' : " + list.toString() + "\n");
            }
        }
        return writer.toString();
    }

    public Map<String, VariableReader> getVariableReadersNonCP() {
        Map<String, VariableReader> result = new HashMap<String, VariableReader>();
        for (Map.Entry<String, VariableMetaData> entryMeta : variables.entrySet()) {
            int variableNum = entryMeta.getValue().getVariableNumber();
            if (entryMeta.getValue().getContextPartitionName() == null) {
                for (Map.Entry<Integer, VariableReader> entry : variableVersionsPerCP.get(variableNum).entrySet()) {
                    result.put(entryMeta.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    public ConcurrentHashMap<Integer, VariableReader> getReadersPerCP(String variableName) {
        VariableMetaData metaData = variables.get(variableName);
        return variableVersionsPerCP.get(metaData.getVariableNumber());
    }

    private static VariableTypeException getVariableTypeException(String variableName, Class variableType, Class initValueClass) {
        return new VariableTypeException("Variable '" + variableName
                + "' of declared type " + JavaClassHelper.getClassNameFullyQualPretty(variableType) +
                " cannot be initialized by a value of type " + JavaClassHelper.getClassNameFullyQualPretty(initValueClass));
    }
}
