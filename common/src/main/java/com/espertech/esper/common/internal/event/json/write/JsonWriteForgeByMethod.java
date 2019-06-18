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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonWriteForgeByMethod implements JsonWriteForge {

    private final String methodName;

    public JsonWriteForgeByMethod(String methodName) {
        this.methodName = methodName;
    }

    public CodegenExpression codegenWrite(JsonWriteForgeRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        if (methodName.equals("writeJsonValue") || methodName.equals("writeJsonArray")) {
            return staticMethod(JsonWriteUtil.class, methodName, refs.getWriter(), refs.getName(), refs.getField());
        } else {
            return staticMethod(JsonWriteUtil.class, methodName, refs.getWriter(), refs.getField());
        }
    }
}
