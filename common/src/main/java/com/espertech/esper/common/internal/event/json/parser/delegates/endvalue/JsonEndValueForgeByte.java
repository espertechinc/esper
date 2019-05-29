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
package com.espertech.esper.common.internal.event.json.parser.delegates.endvalue;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleNumberException;

public class JsonEndValueForgeByte implements JsonEndValueForge {
    public final static JsonEndValueForgeByte INSTANCE = new JsonEndValueForgeByte();

    private JsonEndValueForgeByte() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeByte.class, "jsonToByte", refs.getValueString(), refs.getName());
    }

    public static Byte jsonToByte(String value, String name) {
        try {
            return value == null ? null : Byte.parseByte(value);
        } catch (NumberFormatException ex) {
            throw handleNumberException(name, Byte.class, value, ex);
        }
    }

    public static Object jsonToByteNonNull(String stringValue) {
        return Byte.parseByte(stringValue);
    }
}
