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

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleParseException;

public class JsonEndValueForgeZonedDateTime implements JsonEndValueForge {
    public final static JsonEndValueForgeZonedDateTime INSTANCE = new JsonEndValueForgeZonedDateTime();

    private JsonEndValueForgeZonedDateTime() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeZonedDateTime.class, "jsonToZonedDateTime", refs.getValueString(), refs.getName());
    }

    public static ZonedDateTime jsonToZonedDateTime(String value, String name) {
        return value == null ? null : jsonToZonedDateTimeNonNull(value, name);
    }

    public static ZonedDateTime jsonToZonedDateTimeNonNull(String stringValue, String name) {
        try {
            return ZonedDateTime.parse(stringValue);
        } catch (DateTimeParseException ex) {
            throw handleParseException(name, ZonedDateTime.class, stringValue, ex);
        }
    }
}
