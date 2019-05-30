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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.*;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.context.module.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;
import static com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol.REF_STMTINITSVC;

public class StmtClassForgeableStmtProvider implements StmtClassForgeable {
    private final static String MEMBERNAME_INFORMATION = "statementInformationals";
    private final static String MEMBERNAME_FACTORY_PROVIDER = "factoryProvider";

    private final String statementAIFactoryClassName;
    private final String statementProviderClassName;
    private final StatementInformationalsCompileTime statementInformationals;
    private final CodegenPackageScope packageScope;

    public StmtClassForgeableStmtProvider(String statementAIFactoryClassName, String statementProviderClassName, StatementInformationalsCompileTime statementInformationals, CodegenPackageScope packageScope) {
        this.statementAIFactoryClassName = statementAIFactoryClassName;
        this.statementProviderClassName = statementProviderClassName;
        this.statementInformationals = statementInformationals;
        this.packageScope = packageScope;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        // write code to create an implementation of StatementResource
        CodegenClassMethods methods = new CodegenClassMethods();

        // members
        List<CodegenTypedParam> members = new ArrayList<>();
        members.add(new CodegenTypedParam(StatementInformationalsRuntime.class, MEMBERNAME_INFORMATION));
        members.add(new CodegenTypedParam(StatementAIFactoryProvider.class, MEMBERNAME_FACTORY_PROVIDER).setFinal(false));

        // ctor
        CodegenCtor ctor = new CodegenCtor(this.getClass(), includeDebugSymbols, Collections.emptyList());
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, statementProviderClassName);
        ctor.getBlock().assignMember(MEMBERNAME_INFORMATION, statementInformationals.make(ctor, classScope));

        CodegenMethod initializeMethod = makeInitialize(classScope);
        CodegenMethod getStatementAIFactoryProviderMethod = makeGetStatementAIFactoryProvider(classScope);
        CodegenMethod getStatementInformationalsMethod = CodegenMethod.makeParentNode(StatementInformationalsRuntime.class, StmtClassForgeableStmtProvider.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
                .getBlock().methodReturn(ref(MEMBERNAME_INFORMATION));

        CodegenStackGenerator.recursiveBuildStack(getStatementInformationalsMethod, "getInformationals", methods);
        CodegenStackGenerator.recursiveBuildStack(initializeMethod, "initialize", methods);
        CodegenStackGenerator.recursiveBuildStack(getStatementAIFactoryProviderMethod, "getStatementAIFactoryProvider", methods);
        CodegenStackGenerator.recursiveBuildStack(ctor, "ctor", methods);

        return new CodegenClass(CodegenClassType.STATEMENTPROVIDER, StatementProvider.class, statementProviderClassName, classScope, members, ctor, methods, Collections.emptyList());
    }

    public String getClassName() {
        return statementProviderClassName;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.STMTPROVIDER;
    }

    private CodegenMethod makeInitialize(CodegenClassScope classScope) {
        CodegenMethod method = CodegenMethod.makeParentNode(void.class, StmtClassForgeableStmtProvider.class, classScope).addParam(EPStatementInitServices.class, REF_STMTINITSVC.getRef());
        method.getBlock().assignMember(MEMBERNAME_FACTORY_PROVIDER, newInstance(statementAIFactoryClassName, REF_STMTINITSVC));
        return method;
    }

    private static CodegenMethod makeGetStatementAIFactoryProvider(CodegenClassScope classScope) {
        CodegenMethod method = CodegenMethod.makeParentNode(StatementAIFactoryProvider.class, StmtClassForgeableStmtProvider.class, CodegenSymbolProviderEmpty.INSTANCE, classScope);
        method.getBlock().methodReturn(ref(MEMBERNAME_FACTORY_PROVIDER));
        return method;
    }
}
