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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.lang.reflect.Method;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class JsonEndValueForgeEnum implements JsonEndValueForge {
    private final Class type;

    public JsonEndValueForgeEnum(Class type) {
        this.type = type;
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return conditional(equalsNull(refs.getValueString()), constantNull(), staticMethod(type, "valueOf", refs.getValueString()));
    }

    public static Object jsonToEnum(String stringValue, Method valueOf) {
        if (stringValue == null) {
            return null;
        }
        try {
            return valueOf.invoke(null, stringValue);
        } catch (Exception ex) {
            throw new EPException("Failed to invoke enum-type valueOf method: " + ex.getMessage(), ex);
        }
    }
}
