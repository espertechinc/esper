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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClassType;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenStackGenerator;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateFactory;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;

import java.util.Collections;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class StmtClassForgeableJsonDelegateFactory implements StmtClassForgeable {

    private final String className;
    private final CodegenPackageScope packageScope;
    private final String delegateClassName;
    private final String underlyingClassName;

    public StmtClassForgeableJsonDelegateFactory(String className, CodegenPackageScope packageScope, String delegateClassName, String underlyingClassName) {
        this.className = className;
        this.packageScope = packageScope;
        this.delegateClassName = delegateClassName;
        this.underlyingClassName = underlyingClassName;
    }

    public CodegenClass forge(boolean includeDebugSymbols, boolean fireAndForget) {
        CodegenClassMethods methods = new CodegenClassMethods();
        CodegenClassScope classScope = new CodegenClassScope(includeDebugSymbols, packageScope, className);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(JsonDelegateBase.class, StmtClassForgeableJsonDelegateFactory.class, CodegenSymbolProviderEmpty.INSTANCE, classScope)
            .addParam(JsonHandlerDelegator.class, "delegator")
            .addParam(JsonDelegateBase.class, "parent");
        makeMethod.getBlock().methodReturn(newInstance(delegateClassName, ref("delegator"), ref("parent"), newInstance(underlyingClassName)));
        CodegenStackGenerator.recursiveBuildStack(makeMethod, "make", methods);

        CodegenClass clazz = new CodegenClass(CodegenClassType.JSONDELEGATEFACTORY, className, classScope, Collections.emptyList(), null, methods, Collections.emptyList());
        clazz.getSupers().addInterfaceImplemented(JsonDelegateFactory.class);
        return clazz;
    }

    public String getClassName() {
        return className;
    }

    public StmtClassForgeableType getForgeableType() {
        return StmtClassForgeableType.JSONDELEGATEFACTORY;
    }
}
