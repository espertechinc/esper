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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.EventBeanCopyMethod;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.espertech.esper.common.internal.context.util.StatementCPCacheService.DEFAULT_AGENT_INSTANCE_ID;

/**
 * A convenience class for dealing with reading and updating multiple variable values.
 */
public class VariableReadWritePackage {

    private Map<EventTypeSPI, EventBeanCopyMethod> copyMethods;
    private VariableTriggerSetDesc[] assignments;
    private VariableTriggerWriteDesc[] writers;
    private Variable[] variables;
    private boolean[] mustCoerce;
    private VariableReader[] readersForGlobalVars;

    public void setCopyMethods(Map<EventTypeSPI, EventBeanCopyMethod> copyMethods) {
        this.copyMethods = copyMethods;
    }

    public void setAssignments(VariableTriggerSetDesc[] assignments) {
        this.assignments = assignments;
    }

    public void setWriters(VariableTriggerWriteDesc[] writers) {
        this.writers = writers;
    }

    public void setVariables(Variable[] variables) {
        this.variables = variables;
    }

    public void setMustCoerce(boolean[] mustCoerce) {
        this.mustCoerce = mustCoerce;
    }

    public void setReadersForGlobalVars(VariableReader[] readersForGlobalVars) {
        this.readersForGlobalVars = readersForGlobalVars;
    }

    /**
     * Write new variable values and commit, evaluating assignment expressions using the given
     * events per stream.
     * <p>
     * Populates an optional map of new values if a non-null map is passed.
     *
     * @param eventsPerStream      events per stream
     * @param valuesWritten        null or an empty map to populate with written values
     * @param agentInstanceContext expression evaluation context
     */
    public void writeVariables(EventBean[] eventsPerStream,
                               Map<String, Object> valuesWritten,
                               AgentInstanceContext agentInstanceContext) {
        Set<String> variablesBeansCopied = null;
        VariableManagementService variableService = agentInstanceContext.getVariableManagementService();
        if (!copyMethods.isEmpty()) {
            variablesBeansCopied = new HashSet<String>();
        }

        // We obtain a write lock global to the variable space
        // Since expressions can contain variables themselves, these need to be unchangeable for the duration
        // as there could be multiple statements that do "var1 = var1 + 1".
        variableService.getReadWriteLock().writeLock().lock();
        try {
            variableService.setLocalVersion();

            int count = 0;
            for (VariableTriggerSetDesc assignment : assignments) {
                Variable variable = variables[count];
                VariableMetaData variableMetaData = variable.getMetaData();
                int agentInstanceId = variableMetaData.getOptionalContextName() == null ? DEFAULT_AGENT_INSTANCE_ID : agentInstanceContext.getAgentInstanceId();
                Object value = assignment.getEvaluator().evaluate(eventsPerStream, true, agentInstanceContext);
                int variableNumber = variable.getVariableNumber();

                if (writers[count] != null) {
                    VariableReader reader = variableService.getReader(variables[count].getDeploymentId(), variableMetaData.getVariableName(), agentInstanceId);
                    EventBean current = (EventBean) reader.getValue();
                    if (current == null) {
                        value = null;
                    } else {
                        VariableTriggerWriteDesc writeDesc = writers[count];
                        boolean copy = variablesBeansCopied.add(writeDesc.getVariableName());
                        if (copy) {
                            current = copyMethods.get(writeDesc.getType()).copy(current);
                        }
                        variableService.write(variableNumber, agentInstanceId, current);
                        writeDesc.getWriter().write(value, current);
                    }
                } else if (variableMetaData.getEventType() != null) {
                    EventBean eventBean = agentInstanceContext.getEventBeanTypedEventFactory().adapterForTypedBean(value, variableMetaData.getEventType());
                    variableService.write(variableNumber, agentInstanceId, eventBean);
                } else {
                    if ((value != null) && (mustCoerce[count])) {
                        value = JavaClassHelper.coerceBoxed((Number) value, variableMetaData.getType());
                    }
                    variableService.write(variableNumber, agentInstanceId, value);
                }

                count++;

                if (valuesWritten != null) {
                    valuesWritten.put(assignment.getVariableName(), value);
                }
            }

            variableService.commit();
        } catch (RuntimeException ex) {
            variableService.rollback();
            throw new EPException("Failed variable write: " + ex.getMessage(), ex);
        } finally {
            variableService.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Iterate returning all values.
     *
     * @param variableManagementService variable management
     * @param agentInstanceId           context partition id
     * @return map of values
     */
    public Map<String, Object> iterate(VariableManagementService variableManagementService, int agentInstanceId) {
        Map<String, Object> values = new HashMap<String, Object>();

        int count = 0;
        for (VariableTriggerSetDesc assignment : assignments) {
            Object value;
            if (readersForGlobalVars[count] == null) {
                VariableReader reader = variableManagementService.getReader(variables[count].getDeploymentId(), assignment.getVariableName(), agentInstanceId);
                if (reader == null) {
                    continue;
                }
                value = reader.getValue();
            } else {
                value = readersForGlobalVars[count].getValue();
            }

            if (value == null) {
                values.put(assignment.getVariableName(), null);
            } else if (writers[count] != null) {
                EventBean current = (EventBean) value;
                values.put(assignment.getVariableName(), writers[count].getGetter().get(current));
            } else if (value instanceof EventBean) {
                values.put(assignment.getVariableName(), ((EventBean) value).getUnderlying());
            } else {
                values.put(assignment.getVariableName(), value);
            }
            count++;
        }
        return values;
    }
}
