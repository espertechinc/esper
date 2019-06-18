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
package com.espertech.esper.common.internal.event.json.write;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonWriteForgeDelegate implements JsonWriteForge {
    private final String delegateFactoryClassName;

    public JsonWriteForgeDelegate(String delegateFactoryClassName) {
        this.delegateFactoryClassName = delegateFactoryClassName;
    }

    public CodegenExpression codegenWrite(JsonWriteForgeRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(delegateFactoryClassName, "writeStatic", refs.getWriter(), refs.getField());
    }
}
