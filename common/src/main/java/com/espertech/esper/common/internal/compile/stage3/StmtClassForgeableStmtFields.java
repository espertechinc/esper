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

import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.name.*;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignments;
import com.espertech.esper.common.internal.context.module.StatementFields;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodAssignerSetter;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgeableStmtFields implements StmtClassForgeable {

    private final String className;
    private final CodegenPackageScope packageScope;
    private final int numStreams;

    public StmtClassForgeableStmtFields(String className, CodegenPackageScope packageScope, int numStreams) {
        this.className = className;
        this.packageScope = packageScope;
        this.numStreams = numStreams;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        // members
        List<CodegenTypedParam> members = new ArrayList<>();

        generateNamedMembers(members);

        // numbered members
        for (Map.Entry<CodegenField, CodegenExpression> entry : packageScope.getFieldsUnshared().entrySet()) {
            CodegenField field = entry.getKey();
            members.add(new CodegenTypedParam(field.getType(), field.getName()).setStatic(true).setFinal(false));
        }

        // substitution-parameter members
        generateSubstitutionParamMembers(members);

        // ctor
        CodegenCtor ctor = new CodegenCtor(this.getClass(), includeDebugSymbols, Collections.emptyList());
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        // init method
        CodegenMethod initMethod = packageScope.getInitMethod();
        for (Map.Entry<CodegenField, CodegenExpression> entry : packageScope.getFieldsUnshared().entrySet()) {
            initMethod.getBlock().assignRef(entry.getKey().getName(), entry.getValue());
        }

        // assignment methods
        CodegenMethod assignMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(StatementAIFactoryAssignments.class, "assignments").setStatic(true);
        CodegenMethod unassignMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).setStatic(true);
        generateAssignAndUnassign(numStreams, assignMethod, unassignMethod, packageScope.getFieldsNamed());

        // build methods
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(initMethod, "init", methods);
        CodegenStackGenerator.recursiveBuildStack(assignMethod, "assign", methods);
        CodegenStackGenerator.recursiveBuildStack(unassignMethod, "unassign", methods);

        return new CodegenClass(CodegenClassType.STATEMENTFIELDS, StatementFields.class, className, classScope, members, ctor, methods, Collections.emptyList());
    }

    private void generateSubstitutionParamMembers(List<CodegenTypedParam> members) {
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
            String name = fields.get(i).getField().getName();
            members.add(new CodegenTypedParam(fields.get(i).getType(), name).setStatic(true).setFinal(false));
        }
    }

    private void generateNamedMembers(List<CodegenTypedParam> fields) {
        for (Map.Entry<CodegenFieldName, CodegenField> entry : packageScope.getFieldsNamed().entrySet()) {
            fields.add(new CodegenTypedParam(entry.getValue().getType(), entry.getKey().getName()).setFinal(false).setStatic(true));
        }
    }

    private static void generateAssignAndUnassign(int numStreams, CodegenMethod assign, CodegenMethod unassign, LinkedHashMap<CodegenFieldName, CodegenField> names) {

        for (Map.Entry<CodegenFieldName, CodegenField> entry : names.entrySet()) {
            CodegenFieldName name = entry.getKey();
            if (name instanceof CodegenFieldNameAgg) {
                generate(exprDotMethod(ref("assignments"), "getAggregationResultFuture"), name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNamePrevious) {
                CodegenFieldNamePrevious previous = (CodegenFieldNamePrevious) name;
                generate(arrayAtIndex(exprDotMethod(ref("assignments"), "getPreviousStrategies"), constant(previous.getStreamNumber())), name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNamePrior) {
                CodegenFieldNamePrior prior = (CodegenFieldNamePrior) name;
                generate(arrayAtIndex(exprDotMethod(ref("assignments"), "getPriorStrategies"), constant(prior.getStreamNumber())), name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameViewAgg) {
                generate(constantNull(), name, assign, unassign, true);  // we assign null as the view can assign a value
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryResult) {
                CodegenFieldNameSubqueryResult subq = (CodegenFieldNameSubqueryResult) name;
                CodegenExpression subqueryLookupStrategy = exprDotMethod(ref("assignments"), "getSubqueryLookup", constant(subq.getSubqueryNumber()));
                generate(subqueryLookupStrategy, name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryPrior) {
                CodegenFieldNameSubqueryPrior subq = (CodegenFieldNameSubqueryPrior) name;
                CodegenExpression prior = exprDotMethod(ref("assignments"), "getSubqueryPrior", constant(subq.getSubqueryNumber()));
                generate(prior, name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryPrevious) {
                CodegenFieldNameSubqueryPrevious subq = (CodegenFieldNameSubqueryPrevious) name;
                CodegenExpression prev = exprDotMethod(ref("assignments"), "getSubqueryPrevious", constant(subq.getSubqueryNumber()));
                generate(prev, name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameSubqueryAgg) {
                CodegenFieldNameSubqueryAgg subq = (CodegenFieldNameSubqueryAgg) name;
                CodegenExpression agg = exprDotMethod(ref("assignments"), "getSubqueryAggregation", constant(subq.getSubqueryNumber()));
                generate(agg, name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameTableAccess) {
                CodegenFieldNameTableAccess tableAccess = (CodegenFieldNameTableAccess) name;
                CodegenExpression tableAccessLookupStrategy = exprDotMethod(ref("assignments"), "getTableAccess", constant(tableAccess.getTableAccessNumber()));
                // Table strategies don't get unassigned as they don't hold on to table instance
                generate(tableAccessLookupStrategy, name, assign, unassign, false);
                continue;
            }

            if (name instanceof CodegenFieldNameMatchRecognizePrevious) {
                generate(exprDotMethod(ref("assignments"), "getRowRecogPreviousStrategy"), name, assign, unassign, true);
                continue;
            }

            if (name instanceof CodegenFieldNameMatchRecognizeAgg) {
                generate(constantNull(), name, assign, unassign, true);  // we assign null as the view can assign a value
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
        CodegenExpressionNewAnonymousClass assignerSetterClass = newAnonymousClass(method.getBlock(), FAFQueryMethodAssignerSetter.class);
        method.getBlock().methodReturn(assignerSetterClass);

        CodegenMethod assignMethod = CodegenMethod.makeParentNode(void.class, StmtClassForgeableStmtFields.class, classScope).addParam(StatementAIFactoryAssignments.class, "assignments");
        assignerSetterClass.addMethod("assign", assignMethod);
        assignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "assign", ref("assignments"));

        CodegenMethod setValueMethod = CodegenMethod.makeParentNode(void.class, StmtClassForgeableStmtFields.class, classScope).addParam(int.class, "index").addParam(Object.class, "value");
        assignerSetterClass.addMethod("setValue", setValueMethod);
        CodegenSubstitutionParamEntry.codegenSetterMethod(classScope, setValueMethod);
    }

    private static void generate(CodegenExpression init, CodegenFieldName name, CodegenMethod assign, CodegenMethod unassign, boolean generateUnassign) {
        assign.getBlock().assignRef(name.getName(), init);

        // Table strategies are not unassigned since they do not hold on to the table instance
        if (generateUnassign) {
            unassign.getBlock().assignRef(name.getName(), constantNull());
        }
    }
}
