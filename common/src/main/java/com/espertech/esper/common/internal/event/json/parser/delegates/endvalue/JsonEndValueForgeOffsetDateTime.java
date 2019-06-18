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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleParseException;

public class JsonEndValueForgeOffsetDateTime implements JsonEndValueForge {
    public final static JsonEndValueForgeOffsetDateTime INSTANCE = new JsonEndValueForgeOffsetDateTime();

    private JsonEndValueForgeOffsetDateTime() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeOffsetDateTime.class, "jsonToOffsetDateTime", refs.getValueString(), refs.getName());
    }

    public static OffsetDateTime jsonToOffsetDateTime(String value, String name) {
        return value == null ? null : jsonToOffsetDateTimeNonNull(value, name);
    }

    public static OffsetDateTime jsonToOffsetDateTimeNonNull(String stringValue, String name) {
        try {
            return OffsetDateTime.parse(stringValue);
        } catch (DateTimeParseException ex) {
            throw handleParseException(name, OffsetDateTime.class, stringValue, ex);
        }
    }
}
