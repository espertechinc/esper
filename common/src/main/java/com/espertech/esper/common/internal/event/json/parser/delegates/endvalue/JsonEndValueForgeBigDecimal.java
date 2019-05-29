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

import java.math.BigDecimal;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleNumberException;

public class JsonEndValueForgeBigDecimal implements JsonEndValueForge {
    public final static JsonEndValueForgeBigDecimal INSTANCE = new JsonEndValueForgeBigDecimal();

    private JsonEndValueForgeBigDecimal() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeBigDecimal.class, "jsonToBigDecimal", refs.getValueString(), refs.getName());
    }

    public static BigDecimal jsonToBigDecimal(String value, String name) {
        return value == null ? null : jsonToBigDecimalNonNull(value, name);
    }

    public static BigDecimal jsonToBigDecimalNonNull(String stringValue, String name) {
        try {
            return new BigDecimal(stringValue);
        } catch (NumberFormatException ex) {
            throw handleNumberException(name, BigDecimal.class, stringValue, ex);
        }
    }
}
