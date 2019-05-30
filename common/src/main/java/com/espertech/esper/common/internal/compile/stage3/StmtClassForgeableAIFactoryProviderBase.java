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
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.core.StatementAgentInstanceFactory;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryAssignments;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public abstract class StmtClassForgeableAIFactoryProviderBase implements StmtClassForgeable {
    protected final static String MEMBERNAME_STATEMENTAIFACTORY = "statementAIFactory";

    private final String className;
    private final CodegenPackageScope packageScope;

    protected abstract Class typeOfFactory();

    protected abstract CodegenMethod codegenConstructorInit(CodegenMethodScope parent, CodegenClassScope classScope);

    public StmtClassForgeableAIFactoryProviderBase(String className, CodegenPackageScope packageScope) {
        this.className = className;
        this.packageScope = packageScope;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        List<CodegenTypedParam> ctorParms = new ArrayList<>();
        ctorParms.add(new CodegenTypedParam(EPStatementInitServices.class, EPStatementInitServices.REF.getRef(), false));
        CodegenCtor codegenCtor = new CodegenCtor(this.getClass(), includeDebugSymbols, ctorParms);
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        List<CodegenTypedParam> members = new ArrayList<>();
        members.add(new CodegenTypedParam(typeOfFactory(), MEMBERNAME_STATEMENTAIFACTORY));

        if (packageScope.getFieldsClassNameOptional() != null) {
            codegenCtor.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "init", EPStatementInitServices.REF);
        }
        codegenCtor.getBlock().assignMember(MEMBERNAME_STATEMENTAIFACTORY, localMethod(codegenConstructorInit(codegenCtor, classScope), SAIFFInitializeSymbol.REF_STMTINITSVC));

        CodegenMethod getFactoryMethod = CodegenMethod.makeParentNode(StatementAgentInstanceFactory.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_STATEMENTAIFACTORY));

        CodegenMethod assignMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(StatementAIFactoryAssignments.class, "assignments");
        if (packageScope.getFieldsClassNameOptional() != null) {
            assignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "assign", ref("assignments"));
        }

        CodegenMethod unassignMethod = CodegenMethod.makeParentNode(void.class, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        if (packageScope.getFieldsClassNameOptional() != null) {
            unassignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "unassign");
        }

        CodegenMethod setValueMethod = CodegenMethod.makeParentNode(void.class, StmtClassForgeableStmtFields.class, classScope).addParam(int.class, "index").addParam(Object.class, "value");
        CodegenSubstitutionParamEntry.codegenSetterMethod(classScope, setValueMethod);

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenStackGenerator.recursiveBuildStack(getFactoryMethod, "getFactory", methods);
        CodegenStackGenerator.recursiveBuildStack(assignMethod, "assign", methods);
        CodegenStackGenerator.recursiveBuildStack(unassignMethod, "unassign", methods);
        CodegenStackGenerator.recursiveBuildStack(setValueMethod, "setValue", methods);
        CodegenStackGenerator.recursiveBuildStack(codegenCtor, "ctor", methods);

        return new CodegenClass(CodegenClassType.STATEMENTAIFACTORYPROVIDER, StatementAIFactoryProvider.class, className, classScope, members, codegenCtor, methods, Collections.emptyList());
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.AIFACTORYPROVIDER;
    }
}
