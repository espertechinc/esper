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

import com.espertech.esper.client.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.start.EPStatementStartMethod;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventBeanCopyMethod;
import com.espertech.esper.event.EventPropertyWriter;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A convenience class for dealing with reading and updating multiple variable values.
 */
public class VariableReadWritePackage {
    private static final Logger log = LoggerFactory.getLogger(VariableReadWritePackage.class);

    private final VariableTriggerSetDesc[] assignments;
    private final VariableMetaData[] metaData;
    private final VariableReader[] readersForGlobalVars;
    private final boolean[] mustCoerce;
    private final WriteDesc[] writers;
    private final Map<EventTypeSPI, EventBeanCopyMethod> copyMethods;

    private final EventAdapterService eventAdapterService;
    private final Map<String, Object> variableTypes;
    private final VariableService variableService;

    /**
     * Ctor.
     *
     * @param assignments         the list of variable assignments
     * @param variableService     variable service
     * @param eventAdapterService event adapters
     * @param statementName statement name
     * @throws com.espertech.esper.epl.expression.core.ExprValidationException when variables cannot be found
     */
    public VariableReadWritePackage(List<OnTriggerSetAssignment> assignments, VariableService variableService, EventAdapterService eventAdapterService, String statementName)
            throws ExprValidationException {
        this.metaData = new VariableMetaData[assignments.size()];
        this.readersForGlobalVars = new VariableReader[assignments.size()];
        this.mustCoerce = new boolean[assignments.size()];
        this.writers = new WriteDesc[assignments.size()];

        this.variableTypes = new HashMap<String, Object>();
        this.eventAdapterService = eventAdapterService;
        this.variableService = variableService;

        Map<EventTypeSPI, CopyMethodDesc> eventTypeWrittenProps = new HashMap<EventTypeSPI, CopyMethodDesc>();
        int count = 0;
        List<VariableTriggerSetDesc> assignmentList = new ArrayList<VariableTriggerSetDesc>();

        for (OnTriggerSetAssignment expressionWithAssignments : assignments) {
            Pair<String, ExprNode> possibleVariableAssignment = ExprNodeUtilityRich.checkGetAssignmentToVariableOrProp(expressionWithAssignments.getExpression());
            if (possibleVariableAssignment == null) {
                throw new ExprValidationException("Missing variable assignment expression in assignment number " + count);
            }
            ExprEvaluator evaluator = ExprNodeCompiler.allocateEvaluator(possibleVariableAssignment.getSecond().getForge(), eventAdapterService.getEngineImportService(), this.getClass(), false, statementName);
            assignmentList.add(new VariableTriggerSetDesc(possibleVariableAssignment.getFirst(), evaluator));

            String fullVariableName = possibleVariableAssignment.getFirst();
            String variableName = fullVariableName;
            String subPropertyName = null;

            int indexOfDot = variableName.indexOf('.');
            if (indexOfDot != -1) {
                subPropertyName = variableName.substring(indexOfDot + 1, variableName.length());
                variableName = variableName.substring(0, indexOfDot);
            }

            VariableMetaData variableMetadata = variableService.getVariableMetaData(variableName);
            metaData[count] = variableMetadata;
            if (variableMetadata == null) {
                throw new ExprValidationException("Variable by name '" + variableName + "' has not been created or configured");
            }
            if (variableMetadata.isConstant()) {
                throw new ExprValidationException("Variable by name '" + variableName + "' is declared constant and may not be set");
            }
            if (variableMetadata.getContextPartitionName() == null) {
                readersForGlobalVars[count] = variableService.getReader(variableName, EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID);
            }

            if (subPropertyName != null) {
                if (variableMetadata.getEventType() == null) {
                    throw new ExprValidationException("Variable by name '" + variableName + "' does not have a property named '" + subPropertyName + "'");
                }
                EventType type = variableMetadata.getEventType();
                if (!(type instanceof EventTypeSPI)) {
                    throw new ExprValidationException("Variable by name '" + variableName + "' event type '" + type.getName() + "' not writable");
                }
                EventTypeSPI spi = (EventTypeSPI) type;
                EventPropertyWriter writer = spi.getWriter(subPropertyName);
                EventPropertyGetter getter = spi.getGetter(subPropertyName);
                if (writer == null) {
                    throw new ExprValidationException("Variable by name '" + variableName + "' the property '" + subPropertyName + "' is not writable");
                }

                variableTypes.put(fullVariableName, spi.getPropertyType(subPropertyName));
                CopyMethodDesc writtenProps = eventTypeWrittenProps.get(spi);
                if (writtenProps == null) {
                    writtenProps = new CopyMethodDesc(variableName, new ArrayList<String>());
                    eventTypeWrittenProps.put(spi, writtenProps);
                }
                writtenProps.getPropertiesCopied().add(subPropertyName);

                writers[count] = new WriteDesc(spi, variableName, writer, getter);
            } else {

                // determine types
                Class expressionType = possibleVariableAssignment.getSecond().getForge().getEvaluationType();

                if (variableMetadata.getEventType() != null) {
                    if ((expressionType != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(expressionType, variableMetadata.getEventType().getUnderlyingType()))) {
                        throw new VariableValueException("Variable '" + variableName
                                + "' of declared event type '" + variableMetadata.getEventType().getName() + "' underlying type '" + variableMetadata.getEventType().getUnderlyingType().getName() +
                                "' cannot be assigned a value of type '" + expressionType.getName() + "'");
                    }
                    variableTypes.put(variableName, variableMetadata.getEventType().getUnderlyingType());
                } else {

                    Class variableType = variableMetadata.getType();
                    variableTypes.put(variableName, variableType);

                    // determine if the expression type can be assigned
                    if (variableType != java.lang.Object.class) {
                        if ((JavaClassHelper.getBoxedType(expressionType) != variableType) &&
                                (expressionType != null)) {
                            if ((!JavaClassHelper.isNumeric(variableType)) ||
                                    (!JavaClassHelper.isNumeric(expressionType))) {
                                throw new ExprValidationException(VariableServiceUtil.getAssigmentExMessage(variableName, variableType, expressionType));
                            }

                            if (!(JavaClassHelper.canCoerce(expressionType, variableType))) {
                                throw new ExprValidationException(VariableServiceUtil.getAssigmentExMessage(variableName, variableType, expressionType));
                            }

                            mustCoerce[count] = true;
                        }
                    }
                }
            }

            count++;
        }

        this.assignments = assignmentList.toArray(new VariableTriggerSetDesc[assignmentList.size()]);

        if (eventTypeWrittenProps.isEmpty()) {
            copyMethods = Collections.EMPTY_MAP;
            return;
        }

        copyMethods = new HashMap<EventTypeSPI, EventBeanCopyMethod>();
        for (Map.Entry<EventTypeSPI, CopyMethodDesc> entry : eventTypeWrittenProps.entrySet()) {
            List<String> propsWritten = entry.getValue().getPropertiesCopied();
            String[] props = propsWritten.toArray(new String[propsWritten.size()]);
            EventBeanCopyMethod copyMethod = entry.getKey().getCopyMethod(props);
            if (copyMethod == null) {
                throw new ExprValidationException("Variable '" + entry.getValue().getVariableName()
                        + "' of declared type " + JavaClassHelper.getClassNameFullyQualPretty(entry.getKey().getUnderlyingType()) +
                        "' cannot be assigned to");
            }
            copyMethods.put(entry.getKey(), copyMethod);
        }
    }

    /**
     * Write new variable values and commit, evaluating assignment expressions using the given
     * events per stream.
     * <p>
     * Populates an optional map of new values if a non-null map is passed.
     *
     * @param variableService      variable service
     * @param eventsPerStream      events per stream
     * @param valuesWritten        null or an empty map to populate with written values
     * @param exprEvaluatorContext expression evaluation context
     */
    public void writeVariables(VariableService variableService,
                               EventBean[] eventsPerStream,
                               Map<String, Object> valuesWritten,
                               ExprEvaluatorContext exprEvaluatorContext) {
        Set<String> variablesBeansCopied = null;
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
                VariableMetaData variableMetaData = metaData[count];
                int agentInstanceId = variableMetaData.getContextPartitionName() == null ? EPStatementStartMethod.DEFAULT_AGENT_INSTANCE_ID : exprEvaluatorContext.getAgentInstanceId();
                Object value = assignment.evaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);

                if (writers[count] != null) {
                    VariableReader reader = variableService.getReader(variableMetaData.getVariableName(), exprEvaluatorContext.getAgentInstanceId());
                    EventBean current = (EventBean) reader.getValue();
                    if (current == null) {
                        value = null;
                    } else {
                        WriteDesc writeDesc = writers[count];
                        boolean copy = variablesBeansCopied.add(writeDesc.getVariableName());
                        if (copy) {
                            EventBean copied = copyMethods.get(writeDesc.getType()).copy(current);
                            current = copied;
                        }
                        variableService.write(variableMetaData.getVariableNumber(), agentInstanceId, current);
                        writeDesc.getWriter().write(value, current);
                    }
                } else if (variableMetaData.getEventType() != null) {
                    EventBean eventBean = eventAdapterService.adapterForType(value, variableMetaData.getEventType());
                    variableService.write(variableMetaData.getVariableNumber(), agentInstanceId, eventBean);
                } else {
                    if ((value != null) && (mustCoerce[count])) {
                        value = JavaClassHelper.coerceBoxed((Number) value, variableMetaData.getType());
                    }
                    variableService.write(variableMetaData.getVariableNumber(), agentInstanceId, value);
                }

                count++;

                if (valuesWritten != null) {
                    valuesWritten.put(assignment.variableName, value);
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
     * Returns a map of variable names and type of variable.
     *
     * @return variables
     */
    public Map<String, Object> getVariableTypes() {
        return variableTypes;
    }

    /**
     * Iterate returning all values.
     *
     * @param agentInstanceId context partition id
     * @return map of values
     */
    public Map<String, Object> iterate(int agentInstanceId) {
        Map<String, Object> values = new HashMap<String, Object>();

        int count = 0;
        for (VariableTriggerSetDesc assignment : assignments) {
            Object value;
            if (readersForGlobalVars[count] == null) {
                VariableReader reader = variableService.getReader(assignment.variableName, agentInstanceId);
                if (reader == null) {
                    continue;
                }
                value = reader.getValue();
            } else {
                value = readersForGlobalVars[count].getValue();
            }

            if (value == null) {
                values.put(assignment.variableName, null);
            } else if (writers[count] != null) {
                EventBean current = (EventBean) value;
                values.put(assignment.variableName, writers[count].getGetter().get(current));
            } else if (value instanceof EventBean) {
                values.put(assignment.variableName, ((EventBean) value).getUnderlying());
            } else {
                values.put(assignment.variableName, value);
            }
            count++;
        }
        return values;
    }

    private static class CopyMethodDesc {
        private final String variableName;
        private final List<String> propertiesCopied;

        public CopyMethodDesc(String variableName, List<String> propertiesCopied) {
            this.variableName = variableName;
            this.propertiesCopied = propertiesCopied;
        }

        public String getVariableName() {
            return variableName;
        }

        public List<String> getPropertiesCopied() {
            return propertiesCopied;
        }
    }

    private static class WriteDesc {

        private final EventTypeSPI type;
        private final String variableName;
        private final EventPropertyWriter writer;
        private final EventPropertyGetter getter;

        public WriteDesc(EventTypeSPI type, String variableName, EventPropertyWriter writer, EventPropertyGetter getter) {
            this.type = type;
            this.variableName = variableName;
            this.writer = writer;
            this.getter = getter;
        }

        public String getVariableName() {
            return variableName;
        }

        public EventPropertyWriter getWriter() {
            return writer;
        }

        public EventTypeSPI getType() {
            return type;
        }

        public EventPropertyGetter getGetter() {
            return getter;
        }
    }

    private static class VariableTriggerSetDesc {
        private String variableName;
        private ExprEvaluator evaluator;

        public VariableTriggerSetDesc(String variableName, ExprEvaluator evaluator) {
            this.variableName = variableName;
            this.evaluator = evaluator;
        }
    }
}
