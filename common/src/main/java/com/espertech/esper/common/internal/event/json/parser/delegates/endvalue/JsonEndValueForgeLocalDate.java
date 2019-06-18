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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleParseException;

public class JsonEndValueForgeLocalDate implements JsonEndValueForge {
    public final static JsonEndValueForgeLocalDate INSTANCE = new JsonEndValueForgeLocalDate();

    private JsonEndValueForgeLocalDate() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeLocalDate.class, "jsonToLocalDate", refs.getValueString(), refs.getName());
    }

    public static LocalDate jsonToLocalDate(String value, String name) {
        return value == null ? null : jsonToLocalDateNonNull(value, name);
    }

    public static LocalDate jsonToLocalDateNonNull(String stringValue, String name) {
        try {
            return LocalDate.parse(stringValue);
        } catch (DateTimeParseException ex) {
            throw handleParseException(name, LocalDate.class, stringValue, ex);
        }
    }
}
