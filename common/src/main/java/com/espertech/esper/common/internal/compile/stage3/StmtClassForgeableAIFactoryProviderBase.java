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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
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

    protected abstract EPTypeClass typeOfFactory();

    protected abstract CodegenMethod codegenConstructorInit(CodegenMethodScope parent, CodegenClassScope classScope);

    public StmtClassForgeableAIFactoryProviderBase(String className, CodegenPackageScope packageScope) {
        this.className = className;
        this.packageScope = packageScope;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        List<CodegenTypedParam> ctorParms = new ArrayList<>();
        ctorParms.add(new CodegenTypedParam(EPStatementInitServices.EPTYPE, EPStatementInitServices.REF.getRef(), false));
        CodegenCtor codegenCtor = new CodegenCtor(this.getClass(), includeDebugSymbols, ctorParms);
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        List<CodegenTypedParam> members = new ArrayList<>();
        members.add(new CodegenTypedParam(typeOfFactory(), MEMBERNAME_STATEMENTAIFACTORY));

        if (packageScope.getFieldsClassNameOptional() != null) {
            codegenCtor.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "init", EPStatementInitServices.REF);
        }
        codegenCtor.getBlock().assignMember(MEMBERNAME_STATEMENTAIFACTORY, localMethod(codegenConstructorInit(codegenCtor, classScope), SAIFFInitializeSymbol.REF_STMTINITSVC));

        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenMethod getFactoryMethod = CodegenMethod.makeParentNode(StatementAgentInstanceFactory.EPTYPE, this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
        getFactoryMethod.getBlock().methodReturn(ref(MEMBERNAME_STATEMENTAIFACTORY));

        CodegenMethod assignMethod = null;
        CodegenMethod unassignMethod = null;
        if (packageScope.getFieldsClassNameOptional() != null && packageScope.hasAssignableStatementFields()) {
            assignMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(StatementAIFactoryAssignments.EPTYPE, "assignments");
            assignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "assign", ref("assignments"));

            unassignMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), CodegenSymbolProviderEmpty.INSTANCE, classScope);
            unassignMethod.getBlock().staticMethod(packageScope.getFieldsClassNameOptional(), "unassign");
        }

        CodegenMethod setValueMethod = null;
        if (classScope.getPackageScope().isHasSubstitution()) {
            setValueMethod = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), StmtClassForgeableStmtFields.class, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index").addParam(EPTypePremade.OBJECT.getEPType(), "value");
            CodegenSubstitutionParamEntry.codegenSetterMethod(classScope, setValueMethod);
        }

        CodegenStackGenerator.recursiveBuildStack(getFactoryMethod, "getFactory", methods);
        if (assignMethod != null) {
            CodegenStackGenerator.recursiveBuildStack(assignMethod, "assign", methods);
            CodegenStackGenerator.recursiveBuildStack(unassignMethod, "unassign", methods);
        }
        if (setValueMethod != null) {
            CodegenStackGenerator.recursiveBuildStack(setValueMethod, "setValue", methods);
        }
        CodegenStackGenerator.recursiveBuildStack(codegenCtor, "ctor", methods);

        return new CodegenClass(CodegenClassType.STATEMENTAIFACTORYPROVIDER, StatementAIFactoryProvider.EPTYPE, className, classScope, members, codegenCtor, methods, Collections.emptyList());
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.AIFACTORYPROVIDER;
    }
}
