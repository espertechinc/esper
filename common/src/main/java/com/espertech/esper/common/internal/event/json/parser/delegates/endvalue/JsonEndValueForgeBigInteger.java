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

import java.math.BigInteger;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleNumberException;

public class JsonEndValueForgeBigInteger implements JsonEndValueForge {
    public final static JsonEndValueForgeBigInteger INSTANCE = new JsonEndValueForgeBigInteger();

    private JsonEndValueForgeBigInteger() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeBigInteger.class, "jsonToBigInteger", refs.getValueString(), refs.getName());
    }

    public static BigInteger jsonToBigInteger(String value, String name) {
        return value == null ? null : jsonToBigIntegerNonNull(value, name);
    }

    public static BigInteger jsonToBigIntegerNonNull(String stringValue, String name) {
        try {
            return new BigInteger(stringValue);
        } catch (NumberFormatException ex) {
            throw handleNumberException(name, BigInteger.class, stringValue, ex);
        }
    }
}
