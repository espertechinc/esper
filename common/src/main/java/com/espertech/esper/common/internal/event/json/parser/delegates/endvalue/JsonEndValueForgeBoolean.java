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

public class JsonEndValueForgeBoolean implements JsonEndValueForge {
    public final static JsonEndValueForgeBoolean INSTANCE = new JsonEndValueForgeBoolean();

    private JsonEndValueForgeBoolean() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeBoolean.class, "jsonToBoolean", refs.getValueObject(), refs.getValueString(), refs.getName());
    }

    public static Boolean jsonToBoolean(Object objectValue, String stringValue, String name) {
        if (objectValue != null) {
            return (Boolean) objectValue;
        }
        if (stringValue == null) {
            return null;
        }
        if (stringValue.equals("true")) {
            return true;
        }
        if (stringValue.equals("false")) {
            return false;
        }
        throw JsonEndValueForgeUtil.handleBooleanException(name, stringValue);
    }
}
