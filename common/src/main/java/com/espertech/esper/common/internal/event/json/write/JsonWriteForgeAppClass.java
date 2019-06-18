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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;

public class JsonWriteForgeAppClass implements JsonWriteForge {
    private final String delegateFactoryClassName;
    private final String writeMethodName;

    public JsonWriteForgeAppClass(String delegateFactoryClassName, String writeMethodName) {
        this.delegateFactoryClassName = delegateFactoryClassName;
        this.writeMethodName = writeMethodName;
    }

    public CodegenExpression codegenWrite(JsonWriteForgeRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonWriteUtil.class, writeMethodName, refs.getWriter(), refs.getField(), newInstance(delegateFactoryClassName));
    }
}
