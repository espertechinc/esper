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
import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.assign.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.variable.ExprVariableNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeVariableVisitor;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.util.*;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A convenience class for dealing with reading and updating multiple variable values.
 */
public class VariableReadWritePackageForge {
    private final ExprAssignment[] assignments;
    private final VariableMetaData[] variables;
    private final boolean[] mustCoerce;
    private final VariableTriggerWriteForge[] writers;
    private final Map<EventTypeSPI, EventBeanCopyMethodForge> copyMethods;
    private final Map<String, Object> variableTypes;

    public VariableReadWritePackageForge(List<OnTriggerSetAssignment> assignments, String statementName, StatementCompileTimeServices services)
        throws ExprValidationException {
        this.variables = new VariableMetaData[assignments.size()];
        this.mustCoerce = new boolean[assignments.size()];
        this.writers = new VariableTriggerWriteForge[assignments.size()];
        this.variableTypes = new HashMap<>();

        Map<EventTypeSPI, CopyMethodDesc> eventTypeWrittenProps = new HashMap<EventTypeSPI, CopyMethodDesc>();
        int count = 0;
        List<ExprAssignment> assignmentList = new ArrayList<>();

        for (OnTriggerSetAssignment spec : assignments) {
            ExprAssignment assignmentDesc = spec.getValidated();
            assignmentList.add(assignmentDesc);

            try {
                if (assignmentDesc instanceof ExprAssignmentStraight) {
                    ExprAssignmentStraight assignment = (ExprAssignmentStraight) assignmentDesc;
                    ExprAssignmentLHS identAssignment = assignment.getLhs();

                    String variableName = identAssignment.getIdent();
                    VariableMetaData variableMetadata = services.getVariableCompileTimeResolver().resolve(variableName);
                    if (variableMetadata == null) {
                        throw new ExprValidationException("Variable by name '" + variableName + "' has not been created or configured");
                    }

                    variables[count] = variableMetadata;
                    EPType expressionType = assignment.getRhs().getForge().getEvaluationType();

                    if (assignment.getLhs() instanceof ExprAssignmentLHSIdent) {
                        // determine types
                        if (variableMetadata.getEventType() != null) {
                            if (expressionType != EPTypeNull.INSTANCE && (!JavaClassHelper.isSubclassOrImplementsInterface(expressionType, variableMetadata.getEventType().getUnderlyingEPType()))) {
                                throw new ExprValidationException("Variable '" + variableName
                                    + "' of declared event type '" + variableMetadata.getEventType().getName() + "' underlying type '" + variableMetadata.getEventType().getUnderlyingEPType().getTypeName() +
                                    "' cannot be assigned a value of type '" + expressionType.getTypeName() + "'");
                            }
                            variableTypes.put(variableName, variableMetadata.getEventType().getUnderlyingEPType());
                        } else {

                            EPTypeClass variableType = variableMetadata.getType();
                            variableTypes.put(variableName, variableMetadata.getType());

                            // determine if the expression type can be assigned
                            if (variableType.getType() != Object.class) {
                                if (expressionType != null && expressionType != EPTypeNull.INSTANCE && (!variableType.equals(JavaClassHelper.getBoxedType(expressionType)))) {
                                    EPTypeClass expressionClass = (EPTypeClass) expressionType;
                                    if ((!JavaClassHelper.isNumeric(variableType)) ||
                                        (!JavaClassHelper.isNumeric(expressionType))) {
                                        throw new ExprValidationException(VariableUtil.getAssigmentExMessage(variableName, variableType, expressionClass));
                                    }

                                    if (!(JavaClassHelper.canCoerce(expressionClass.getType(), variableType.getType()))) {
                                        throw new ExprValidationException(VariableUtil.getAssigmentExMessage(variableName, variableType, expressionClass));
                                    }

                                    mustCoerce[count] = true;
                                }
                            }
                        }
                    } else if (assignment.getLhs() instanceof ExprAssignmentLHSIdentWSubprop) {
                        ExprAssignmentLHSIdentWSubprop subpropAssignment = (ExprAssignmentLHSIdentWSubprop) assignment.getLhs();
                        String subPropertyName = subpropAssignment.getSubpropertyName();
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
                        EPType getterType = spi.getPropertyEPType(subPropertyName);
                        if (writer == null) {
                            throw new ExprValidationException("Variable by name '" + variableName + "' the property '" + subPropertyName + "' is not writable");
                        }

                        String fullVariableName = variableName + "." + subPropertyName;
                        variableTypes.put(fullVariableName, spi.getPropertyEPType(subPropertyName));
                        CopyMethodDesc writtenProps = eventTypeWrittenProps.get(spi);
                        if (writtenProps == null) {
                            writtenProps = new CopyMethodDesc(variableName, new ArrayList<String>());
                            eventTypeWrittenProps.put(spi, writtenProps);
                        }
                        writtenProps.getPropertiesCopied().add(subPropertyName);

                        writers[count] = new VariableTriggerWriteDescForge(spi, variableName, writer, getter, getterType, assignment.getRhs().getForge().getEvaluationType());
                    } else if (assignment.getLhs() instanceof ExprAssignmentLHSArrayElement) {
                        ExprAssignmentLHSArrayElement arrayAssign = (ExprAssignmentLHSArrayElement) assignment.getLhs();
                        EPTypeClass variableType = variableMetadata.getType();
                        if (!variableType.getType().isArray()) {
                            throw new ExprValidationException("Variable '" + variableMetadata.getVariableName() + "' is not an array");
                        }
                        TypeWidenerSPI widener;
                        try {
                            widener = TypeWidenerFactory.getCheckPropertyAssignType(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignment.getRhs()), expressionType,
                                JavaClassHelper.getArrayComponentType(variableType), variableMetadata.getVariableName(), false, null, statementName);
                        } catch (TypeWidenerException ex) {
                            throw new ExprValidationException(ex.getMessage(), ex);
                        }
                        writers[count] = new VariableTriggerWriteArrayElementForge(variableName, arrayAssign.getIndexExpression().getForge(), widener);
                    } else {
                        throw new IllegalStateException("Unrecognized left hand side assignment " + assignment.getLhs());
                    }
                } else if (assignmentDesc instanceof ExprAssignmentCurly) {
                    ExprAssignmentCurly curly = (ExprAssignmentCurly) assignmentDesc;
                    if (curly.getExpression() instanceof ExprVariableNode) {
                        throw new ExprValidationException("Missing variable assignment expression in assignment number " + count);
                    }
                    ExprNodeVariableVisitor variableVisitor = new ExprNodeVariableVisitor(services.getVariableCompileTimeResolver());
                    curly.getExpression().accept(variableVisitor);
                    if (variableVisitor.getVariableNames() == null || variableVisitor.getVariableNames().size() != 1) {
                        throw new ExprValidationException("Assignment expression must receive a single variable value");
                    }
                    Map.Entry<String, VariableMetaData> variable = variableVisitor.getVariableNames().entrySet().iterator().next();
                    variables[count] = variable.getValue();
                    writers[count] = new VariableTriggerWriteCurlyForge(variable.getKey(), curly.getExpression().getForge());
                } else {
                    throw new IllegalStateException("Unrecognized assignment expression " + assignmentDesc);
                }

                if (variables[count].isConstant()) {
                    throw new ExprValidationException("Variable by name '" + variables[count].getVariableName() + "' is declared constant and may not be set");
                }
                count++;
            } catch (ExprValidationException ex) {
                throw new ExprValidationException("Failed to validate assignment expression '" +
                    ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(assignmentDesc.getOriginalExpression()) +
                    "': " + ex.getMessage(), ex);
            }
        }

        this.assignments = assignmentList.toArray(new ExprAssignment[assignmentList.size()]);

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
                    + "' of declared type " + ClassHelperPrint.getClassNameFullyQualPretty(entry.getKey().getUnderlyingType()) +
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
        CodegenMethod method = parent.makeChild(VariableReadWritePackage.EPTYPE, this.getClass(), classScope);
        CodegenExpressionRef ref = ref("rw");
        method.getBlock()
            .declareVarNewInstance(VariableReadWritePackage.EPTYPE, ref.getRef())
            .exprDotMethod(ref, "setCopyMethods", makeCopyMethods(copyMethods, method, symbols, classScope))
            .exprDotMethod(ref, "setAssignments", makeAssignments(assignments, variables, method, symbols, classScope))
            .exprDotMethod(ref, "setVariables", makeVariables(variables, method, symbols, classScope))
            .exprDotMethod(ref, "setWriters", makeWriters(writers, method, symbols, classScope))
            .exprDotMethod(ref, "setReadersForGlobalVars", makeReadersForGlobalVars(variables, method, symbols, classScope))
            .exprDotMethod(ref, "setMustCoerce", constant(mustCoerce))
            .methodReturn(ref);
        return localMethod(method);
    }

    private static CodegenExpression makeReadersForGlobalVars(VariableMetaData[] variables, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableReader.EPTYPEARRAY, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableReader.EPTYPEARRAY, "readers", newArrayByLength(VariableReader.EPTYPE, constant(variables.length)));
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

    private static CodegenExpression makeWriters(VariableTriggerWriteForge[] writers, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerWrite.EPTYPEARRAY, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableTriggerWrite.EPTYPEARRAY, "writers", newArrayByLength(VariableTriggerWrite.EPTYPE, constant(writers.length)));
        for (int i = 0; i < writers.length; i++) {
            CodegenExpression writer = writers[i] == null ? constantNull() : writers[i].make(method, symbols, classScope);
            method.getBlock().assignArrayElement("writers", constant(i), writer);
        }
        method.getBlock().methodReturn(ref("writers"));
        return localMethod(method);
    }

    private static CodegenExpression makeVariables(VariableMetaData[] variables, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Variable.EPTYPEARRAY, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(Variable.EPTYPEARRAY, "vars", newArrayByLength(Variable.EPTYPE, constant(variables.length)));
        for (int i = 0; i < variables.length; i++) {
            CodegenExpression resolve = VariableDeployTimeResolver.makeResolveVariable(variables[i], symbols.getAddInitSvc(method));
            method.getBlock().assignArrayElement("vars", constant(i), resolve);
        }
        method.getBlock().methodReturn(ref("vars"));
        return localMethod(method);
    }

    private static CodegenExpression makeAssignments(ExprAssignment[] assignments, VariableMetaData[] variables, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(VariableTriggerSetDesc.EPTYPEARRAY, VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(VariableTriggerSetDesc.EPTYPEARRAY, "sets", newArrayByLength(VariableTriggerSetDesc.EPTYPE, constant(assignments.length)));
        for (int i = 0; i < assignments.length; i++) {
            CodegenExpression set;
            if (assignments[i] instanceof ExprAssignmentStraight) {
                ExprAssignmentStraight straightAssignment = (ExprAssignmentStraight) assignments[i];
                set = newInstance(VariableTriggerSetDesc.EPTYPE, constant(straightAssignment.getLhs().getFullIdentifier()),
                    ExprNodeUtilityCodegen.codegenEvaluator(straightAssignment.getRhs().getForge(), method, VariableReadWritePackageForge.class, classScope));
            } else {
                set = newInstance(VariableTriggerSetDesc.EPTYPE, constant(variables[i].getVariableName()), constantNull());
            }
            method.getBlock().assignArrayElement("sets", constant(i), set);
        }
        method.getBlock().methodReturn(ref("sets"));
        return localMethod(method);
    }

    private static CodegenExpression makeCopyMethods(Map<EventTypeSPI, EventBeanCopyMethodForge> copyMethods, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (copyMethods.isEmpty()) {
            return staticMethod(Collections.class, "emptyMap");
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), VariableReadWritePackageForge.class, classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "methods", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(copyMethods.size())));
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
}
