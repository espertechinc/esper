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

import java.net.MalformedURLException;
import java.net.URL;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleParseException;

public class JsonEndValueForgeURL implements JsonEndValueForge {
    public final static JsonEndValueForgeURL INSTANCE = new JsonEndValueForgeURL();

    private JsonEndValueForgeURL() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeURL.class, "jsonToURL", refs.getValueString(), refs.getName());
    }

    public static URL jsonToURL(String value, String name) {
        return value == null ? null : jsonToURLNonNull(value, name);
    }

    public static URL jsonToURLNonNull(String stringValue, String name) {
        try {
            return new URL(stringValue);
        } catch (MalformedURLException ex) {
            throw handleParseException(name, URL.class, stringValue, ex);
        }
    }
}
