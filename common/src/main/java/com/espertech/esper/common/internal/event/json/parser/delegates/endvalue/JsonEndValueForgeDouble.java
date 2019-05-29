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

public class JsonEndValueForgeDouble implements JsonEndValueForge {
    public final static JsonEndValueForgeDouble INSTANCE = new JsonEndValueForgeDouble();

    private JsonEndValueForgeDouble() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeDouble.class, "jsonToDouble", refs.getValueString(), refs.getName());
    }

    public static Double jsonToDouble(String value, String name) {
        return value == null ? null : jsonToDoubleNonNull(value, name);
    }

    public static double jsonToDoubleNonNull(String stringValue, String name) {
        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException ex) {
            throw handleNumberException(name, Double.class, stringValue, ex);
        }
    }
}
