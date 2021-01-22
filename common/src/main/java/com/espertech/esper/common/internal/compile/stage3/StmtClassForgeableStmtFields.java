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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.name.*;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenRepetitiveValueBuilder;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignments;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodAssignerSetter;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgeableStmtFields implements StmtClassForgeable {

    private final String className;
    private final CodegenPackageScope packageScope;
    private final boolean dataflowOperatorFields;

    public StmtClassForgeableStmtFields(String className, CodegenPackageScope packageScope) {
        this(className, packageScope, false);
    }

    public StmtClassForgeableStmtFields(String className, CodegenPackageScope packageScope, boolean dataflowOperatorFields) {
        this.className = className;
        this.packageScope = packageScope;
        this.dataflowOperatorFields = dataflowOperatorFields;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        if (!dataflowOperatorFields && !packageScope.hasAnyFields()) {
            return null;
        }
        List<MemberFieldPair> memberFields = getMembers();
        int maxMembersPerClass = Math.max(1, packageScope.getConfig().getInternalUseOnlyMaxMembersPerClass());

        List<CodegenInnerClass> innerClasses = Collections.emptyList();
        List<CodegenTypedParam> members;
        if (memberFields.size() <= maxMembersPerClass) {
            members = toMembers(memberFields);
        } else {
            List<List<MemberFieldPair>> assignments = CollectionUtil.subdivide(memberFields, maxMembersPerClass);
            innerClasses = new ArrayList<>(assignments.size());
            members = makeInnerClasses(assignments, innerClasses);
        }

        // ctor
        CodegenCtor ctor = new CodegenCtor(this.getClass(), includeDebugSymbols, Collections.emptyList());
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        // init method
        CodegenMethod initMethod = packageScope.getInitMethod();
        new CodegenRepetitiveValueBuilder<>(packageScope.getFieldsUnshared().entrySet(), initMethod, classScope, this.getClass())
                .addParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef())
                .setConsumer((entry, index, leaf) -> {
                    leaf.getBlock().assignRef(entry.getKey().getNameWithMember(), entry.getValue());
                }).build();

        // build methods
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(initMethod, "init", methods);

        // assignment methods
        if (packageScope.hasAssignableStatementFields()) {
            CodegenMethod assignMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(StatementAIFactoryAssignments.EPTYPE, "assignments").setStatic(true);
            CodegenMethod unassignMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).setStatic(true);
            generateAssignAndUnassign(assignMethod, unassignMethod, packageScope.getFieldsNamed());
            CodegenStackGenerator.recursiveBuildStack(assignMethod, "assign", methods);
            CodegenStackGenerator.recursiveBuildStack(unassignMethod, "unassign", methods);
        }

        return new CodegenClass(CodegenClassType.STATEMENTFIELDS, StatementFields.EPTYPE, className, classScope, members, ctor, methods, innerClasses);
    }

    private List<CodegenTypedParam> makeInnerClasses(List<List<MemberFieldPair>> assignments, List<CodegenInnerClass> innerClasses) {
        int indexAssignment = 0;
        List<CodegenTypedParam> members = new ArrayList<>(assignments.size());

        for (List<MemberFieldPair> assignment : assignments) {
            String classNameAssignment = "A" + indexAssignment;
            String memberNameAssignment = "a" + indexAssignment;

            // set assigned member name
            List<CodegenTypedParam> assignmentMembers = new ArrayList<>(assignment.size());
            for (MemberFieldPair memberField : assignment) {
                assignmentMembers.add(memberField.member);
                memberField.field.setAssignmentMemberName(memberNameAssignment);
            }

            // add inner class
            CodegenInnerClass innerClass = new CodegenInnerClass(classNameAssignment, null, assignmentMembers, new CodegenClassMethods());
            innerClasses.add(innerClass);

            // initialize member
            CodegenTypedParam member = new CodegenTypedParam(innerClass.getClassName(), memberNameAssignment).setStatic(true).setInitializer(newInstance(innerClass.getClassName()));
            members.add(member);

            indexAssignment++;
        }
        return members;
    }

    private List<CodegenTypedParam> toMembers(List<MemberFieldPair> memberFields) {
        List<CodegenTypedParam> members = new ArrayList<>(memberFields.size());
        for (MemberFieldPair memberField : memberFields) {
            members.add(memberField.member);
        }
        return members;
    }

    private List<MemberFieldPair> getMembers() {
        // members
        List<MemberFieldPair> members = new ArrayList<>();

        generateNamedMembers(members);

        // numbered members
        for (Map.Entry<CodegenField, CodegenExpression> entry : packageScope.getFieldsUnshared().entrySet()) {
            CodegenField field = entry.getKey();
            CodegenTypedParam member = new CodegenTypedParam(field.getType(), field.getName()).setStatic(true).setFinal(false);
            members.add(new MemberFieldPair(member, field));
        }

        // substitution-parameter members
        generateSubstitutionParamMembers(members);

        return members;
    }

    private void generateSubstitutionParamMembers(List<MemberFieldPair> members) {
        List<CodegenSubstitutionParamEntry> numbered = packageScope.getSubstitutionParamsByNumber();
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = packageScope.getSubstitutionParamsByName();

        if (numbered.isEmpty() && named.isEmpty()) {
            return;
        }
        if (!numbered.isEmpty() && !named.isEmpty()) {
            throw new IllegalStateException("Both named and numbered substitution parameters are non-empty");
        }

        List<CodegenSubstitutionParamEntry> fields;
        if (!numbered.isEmpty()) {
            fields = numbered;
        } else {
            fields = new ArrayList<>(named.values());
        }

        for (int i = 0; i < fields.size(); i++) {
            CodegenField field = fields.get(i).getField();
            String name = field.getName();
            CodegenTypedParam member = new CodegenTypedParam(fields.get(i).getType(), name).setStatic(true).setFinal(false);
            members.add(new MemberFieldPair(member, field));
        }
    }

    private void generateNamedMembers(List<MemberFieldPair> fields) {
        for (Map.Entry<CodegenFieldName, CodegenField> entry : packageScope.getFieldsNamed().entrySet()) {
            CodegenTypedParam member = new CodegenTypedParam(entry.getValue().getType(), entry.getKey().getName()).setFinal(false).setStatic(true);
            fields.add(new MemberFieldPair(member, entry.getValue()));
        }
    }

    private static void generateAssignAndUnassign(CodegenMethod assign, CodegenMethod unassign, LinkedHashMap<CodegenFieldName, CodegenField> names) {

        for (Map.Entry<CodegenFieldName, CodegenField> entry : names.entrySet()) {
            CodegenFieldName name = entry.getKey();
            CodegenField field = entry.getValue();
            if (name instanceof CodegenFieldNameAgg) {
                generate(exprDotMethod(ref("assignments"), "getAggregationResultFuture"), field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNamePrevious) {
                CodegenFieldNamePrevious previous = (CodegenFieldNamePrevious) name;
                generate(arrayAtIndex(exprDotMethod(ref("assignments"), "getPreviousStrategies"), constant(previous.getStreamNumber())), field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNamePrior) {
                CodegenFieldNamePrior prior = (CodegenFieldNamePrior) name;
                generate(arrayAtIndex(exprDotMethod(ref("assignments"), "getPriorStrategies"), constant(prior.getStreamNumber())), field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameViewAgg) {
                generate(constantNull(), field, assign, unassign, true);  // we assign null as the view can assign a value
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryResult) {
                CodegenFieldNameSubqueryResult subq = (CodegenFieldNameSubqueryResult) name;
                CodegenExpression subqueryLookupStrategy = exprDotMethod(ref("assignments"), "getSubqueryLookup", constant(subq.getSubqueryNumber()));
                generate(subqueryLookupStrategy, field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryPrior) {
                CodegenFieldNameSubqueryPrior subq = (CodegenFieldNameSubqueryPrior) name;
                CodegenExpression prior = exprDotMethod(ref("assignments"), "getSubqueryPrior", constant(subq.getSubqueryNumber()));
                generate(prior, field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryPrevious) {
                CodegenFieldNameSubqueryPrevious subq = (CodegenFieldNameSubqueryPrevious) name;
                CodegenExpression prev = exprDotMethod(ref("assignments"), "getSubqueryPrevious", constant(subq.getSubqueryNumber()));
                generate(prev, field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryAgg) {
                CodegenFieldNameSubqueryAgg subq = (CodegenFieldNameSubqueryAgg) name;
                CodegenExpression agg = exprDotMethod(ref("assignments"), "getSubqueryAggregation", constant(subq.getSubqueryNumber()));
                generate(agg, field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameTableAccess) {
                CodegenFieldNameTableAccess tableAccess = (CodegenFieldNameTableAccess) name;
                CodegenExpression tableAccessLookupStrategy = exprDotMethod(ref("assignments"), "getTableAccess", constant(tableAccess.getTableAccessNumber()));
                // Table strategies don't get unassigned as they don't hold on to table instance
                generate(tableAccessLookupStrategy, field, assign, unassign, false);
                continue;
            }

            if (name instanceof CodegenFieldNameMatchRecognizePrevious) {
                generate(exprDotMethod(ref("assignments"), "getRowRecogPreviousStrategy"), field, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameMatchRecognizeAgg) {
                generate(constantNull(), field, assign, unassign, true);  // we assign null as the view can assign a value
                continue;
            }

            throw new IllegalStateException("Unrecognized field " + entry.getKey());
        }
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.FIELDS;
    }

    public static void makeSubstitutionSetter(CodegenPackageScope packageScope, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass assignerSetterClass = newAnonymousClass(method.getBlock(), FAFQueryMethodAssignerSetter.EPTYPE);
        method.getBlock().methodReturn(assignerSetterClass);

        CodegenMethod assignMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), StmtClassForgeableStmtFields.class, classScope).addParam(StatementAIFactoryAssignments.EPTYPE, "assignments");
        assignerSetterClass.addMethod("assign", assignMethod);
        if (!packageScope.getFieldsNamed().isEmpty()) {
            assignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "assign", ref("assignments"));
        }

        CodegenMethod setValueMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), StmtClassForgeableStmtFields.class, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index").addParam(EPTypePremade.OBJECT.getEPType(), "value");
        assignerSetterClass.addMethod("setValue", setValueMethod);
        CodegenSubstitutionParamEntry.codegenSetterMethod(classScope, setValueMethod);
    }

    private static void generate(CodegenExpression init, CodegenField field, CodegenMethod assign, CodegenMethod unassign, boolean generateUnassign) {
        assign.getBlock().assignRef(field.getNameWithMember(), init);

        // Table strategies are not unassigned since they do not hold on to the table instance
        if (generateUnassign) {
            unassign.getBlock().assignRef(field.getNameWithMember(), constantNull());
        }
    }

    private final static class MemberFieldPair {
        private final CodegenTypedParam member;
        private final CodegenField field;

        public MemberFieldPair(CodegenTypedParam member, CodegenField field) {
            this.member = member;
            this.field = field;
        }

        public CodegenTypedParam getMember() {
            return member;
        }

        public CodegenField getField() {
            return field;
        }
    }
}
