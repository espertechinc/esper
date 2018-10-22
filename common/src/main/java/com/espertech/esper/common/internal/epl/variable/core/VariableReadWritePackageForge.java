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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A convenience class for dealing with reading and updating multiple variable values.
 */
public class VariableReadWritePackageForge {
    private final VariableTriggerSetForge[] assignments;
    private final VariableMetaData[] variables;
    private final boolean[] mustCoerce;
    private final VariableTriggerWriteDescForge[] writers;
    private final Map<EventTypeSPI, EventBeanCopyMethodForge> copyMethods;
    private final Map<String, Object> variableTypes;

    public VariableReadWritePackageForge(List<OnTriggerSetAssignment> assignments, StatementCompileTimeServices services)
            throws ExprValidationException {
        this.variables = new VariableMetaData[assignments.size()];
        this.mustCoerce = new boolean[assignments.size()];
        this.writers = new VariableTriggerWriteDescForge[assignments.size()];
        this.variableTypes = new HashMap<>();

        Map<EventTypeSPI, CopyMethodDesc> eventTypeWrittenProps = new HashMap<EventTypeSPI, CopyMethodDesc>();
        int count = 0;
        List<VariableTriggerSetForge> assignmentList = new ArrayList<VariableTriggerSetForge>();

        for (OnTriggerSetAssignment expressionWithAssignments : assignments) {
            Pair<String, ExprNode> possibleVariableAssignment = ExprNodeUtilityValidate.checkGetAssignmentToVariableOrProp(expressionWithAssignments.getExpression());
            if (possibleVariableAssignment == null) {
                throw new ExprValidationException("Missing variable assignment expression in assignment number " + count);
            }
            ExprForge evaluator = possibleVariableAssignment.getSecond().getForge();
            assignmentList.add(new VariableTriggerSetForge(possibleVariableAssignment.getFirst(), evaluator));

            String fullVariableName = possibleVariableAssignment.getFirst();
            String variableName = fullVariableName;
            String subPropertyName = null;

            int indexOfDot = variableName.indexOf('.');
            if (indexOfDot != -1) {
                subPropertyName = variableName.substring(indexOfDot + 1, variableName.length());
                variableName = variableName.substring(0, indexOfDot);
            }

            VariableMetaData variableMetadata = services.getVariableCompileTimeResolver().resolve(variableName);
            if (variableMetadata == null) {
                throw new ExprValidationException("Variable by name '" + variableName + "' has not been created or configured");
            }

            variables[count] = variableMetadata;

            if (variableMetadata.isConstant()) {
                throw new ExprValidationException("Variable by name '" + variableName + "' is declared constant and may not be set");
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
                EventPropertyWriterSPI writer = spi.getWriter(subPropertyName);
                EventPropertyGetterSPI getter = spi.getGetterSPI(subPropertyName);
                Class getterType = spi.getPropertyType(subPropertyName);
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

                writers[count] = new VariableTriggerWriteDescForge(spi, variableName, writer, getter, getterType, evaluator.getEvaluationType());
            } else {

                // determine types
                Class expressionType = possibleVariableAssignment.getSecond().getForge().getEvaluationType();

                if (variableMetadata.getEventType() != null) {
                    if ((expressionType != null) && (!JavaClassHelper.isSubclassOrImplementsInterface(expressionType, variableMetadata.getEventType().getUnderlyingType()))) {
                        throw new ExprValidationException("Variable '" + variableName
                                + "' of declared event type '" + variableMetadata.getEventType().getName() + "' underlying type '" + variableMetadata.getEventType().getUnderlyingType().getName() +
                                "' cannot be assigned a value of type '" + expressionType.getName() + "'");
                    }
                    variableTypes.put(variableName, variableMetadata.getEventType().getUnderlyingType());
                } else {

                    Class variableType = variableMetadata.getType();
                    variableTypes.put(variableName, variableType);

                    // determine if the expression type can be assigned
                    if (variableType != Object.class) {
                        if ((JavaClassHelper.getBoxedType(expressionType) != variableType) &&
                                (expressionType != null)) {
                            if ((!JavaClassHelper.isNumeric(variableType)) ||
                                    (!JavaClassHelper.isNumeric(expressionType))) {
                                throw new ExprValidationException(VariableUtil.getAssigmentExMessage(variableName, variableType, expressionType));
                            }

                            if (!(JavaClassHelper.canCoerce(expressionType, variableType))) {
                                throw new ExprValidationException(VariableUtil.getAssigmentExMessage(variableName, variableType, expressionType));
                            }

                            mustCoerce[count] = true;
                        }
                    }
                }
            }

            count++;
        }

        this.assignments = assignmentList.toArray(new VariableTriggerSetForge[assignmentList.size()]);

        if (eventTypeWrittenProps.isEmpty()) {
            copyMethods = Collections.EMPTY_MAP;
            return;
        }

        copyMethods = new HashMap<>();
        for (Map.Entry<EventTypeSPI, CopyMethodDesc> entry : eventTypeWrittenProps.entrySet()) {
            List<String> propsWritten = entry.getValue().getPropertiesCopied();
            String[] props = propsWritten.toArray(new String[propsWritten.size()]);
            EventBeanCopyMethodForge copyMethod = entry.getKey().getCopyMethodForge(props);
            if (copyMethod == null) {
                throw new ExprValidationException("Variable '" + entry.getValue().getVariableName()
                        + "' of declared type " + JavaClassHelper.getClassNameFullyQualPretty(entry.getKey().getUnderlyingType()) +
                        "' cannot be assigned to");
            }
            copyMethods.put(entry.getKey(), copyMethod);
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

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableReadWritePackage.class, this.getClass(), classScope);
        CodegenExpressionRef ref = ref("rw");
        method.getBlock()
                .declareVar(VariableReadWritePackage.class, ref.getRef(), newInstance(VariableReadWritePackage.class))
                .exprDotMethod(ref, "setCopyMethods", makeCopyMethods(copyMethods, method, symbols, classScope))
                .exprDotMethod(ref, "setAssignments", makeAssignments(assignments, method, symbols, classScope))
                .exprDotMethod(ref, "setVariables", makeVariables(variables, method, symbols, classScope))
                .exprDotMethod(ref, "setWriters", makeWriters(writers, method, symbols, classScope))
                .exprDotMethod(ref, "setReadersForGlobalVars", makeReadersForGlobalVars(variables, method, symbols, classScope))
                .exprDotMethod(ref, "setMustCoerce", constant(mustCoerce))
                .methodReturn(ref);
        return localMethod(method);
    }

    private static CodegenExpression makeReadersForGlobalVars(VariableMetaData[] variables, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableReader[].class, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableReader[].class, "readers", newArrayByLength(VariableReader.class, constant(variables.length)));
        for (int i = 0; i < variables.length; i++) {
            if (variables[i].getOptionalContextName() == null) {
                CodegenExpression resolve = staticMethod(VariableDeployTimeResolver.class, "resolveVariableReader",
                        constant(variables[i].getVariableName()),
                        constant(variables[i].getVariableVisibility()),
                        constant(variables[i].getVariableModuleName()), constantNull(),
                        symbols.getAddInitSvc(method));
                method.getBlock().assignArrayElement("readers", constant(i), resolve);
            }
        }
        method.getBlock().methodReturn(ref("readers"));
        return localMethod(method);
    }

    private static CodegenExpression makeWriters(VariableTriggerWriteDescForge[] writers, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerWriteDesc[].class, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableTriggerWriteDesc[].class, "writers", newArrayByLength(VariableTriggerWriteDesc.class, constant(writers.length)));
        for (int i = 0; i < writers.length; i++) {
            CodegenExpression writer = writers[i] == null ? constantNull() : writers[i].make(method, symbols, classScope);
            method.getBlock().assignArrayElement("writers", constant(i), writer);
        }
        method.getBlock().methodReturn(ref("writers"));
        return localMethod(method);
    }

    private static CodegenExpression makeVariables(VariableMetaData[] variables, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Variable[].class, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(Variable[].class, "vars", newArrayByLength(Variable.class, constant(variables.length)));
        for (int i = 0; i < variables.length; i++) {
            CodegenExpression resolve = VariableDeployTimeResolver.makeResolveVariable(variables[i], symbols.getAddInitSvc(method));
            method.getBlock().assignArrayElement("vars", constant(i), resolve);
        }
        method.getBlock().methodReturn(ref("vars"));
        return localMethod(method);
    }

    private static CodegenExpression makeAssignments(VariableTriggerSetForge[] assignments, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerSetDesc[].class, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableTriggerSetDesc[].class, "sets", newArrayByLength(VariableTriggerSetDesc.class, constant(assignments.length)));
        for (int i = 0; i < assignments.length; i++) {
            CodegenExpression set = newInstance(VariableTriggerSetDesc.class, constant(assignments[i].variableName),
                    ExprNodeUtilityCodegen.codegenEvaluator(assignments[i].getForge(), method, VariableReadWritePackageForge.class, classScope));
            method.getBlock().assignArrayElement("sets", constant(i), set);
        }
        method.getBlock().methodReturn(ref("sets"));
        return localMethod(method);
    }

    private static CodegenExpression makeCopyMethods(Map<EventTypeSPI, EventBeanCopyMethodForge> copyMethods, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (copyMethods.isEmpty()) {
            return staticMethod(Collections.class, "emptyMap");
        }
        CodegenMethod method = parent.makeChild(Map.class, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(Map.class, "methods", newInstance(HashMap.class, constant(copyMethods.size())));
        for (Map.Entry<EventTypeSPI, EventBeanCopyMethodForge> entry : copyMethods.entrySet()) {
            CodegenExpression type = EventTypeUtility.resolveTypeCodegen(entry.getKey(), symbols.getAddInitSvc(method));
            CodegenExpression copyMethod = entry.getValue().makeCopyMethodClassScoped(classScope);
            method.getBlock().exprDotMethod(ref("methods"), "put", type, copyMethod);
        }
        method.getBlock().methodReturn(ref("methods"));
        return localMethod(method);
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

    private static class VariableTriggerSetForge {
        private final String variableName;
        private final ExprForge forge;

        public VariableTriggerSetForge(String variableName, ExprForge forge) {
            this.variableName = variableName;
            this.forge = forge;
        }

        public String getVariableName() {
            return variableName;
        }

        public ExprForge getForge() {
            return forge;
        }
    }
}
