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

import java.net.URI;
import java.net.URISyntaxException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.staticMethod;
import static com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeUtil.handleParseException;

public class JsonEndValueForgeURI implements JsonEndValueForge {
    public final static JsonEndValueForgeURI INSTANCE = new JsonEndValueForgeURI();

    private JsonEndValueForgeURI() {
    }

    public CodegenExpression captureValue(JsonEndValueRefs refs, CodegenMethod method, CodegenClassScope classScope) {
        return staticMethod(JsonEndValueForgeURI.class, "jsonToURI", refs.getValueString(), refs.getName());
    }

    public static URI jsonToURI(String value, String name) {
        return value == null ? null : jsonToURINonNull(value, name);
    }

    public static URI jsonToURINonNull(String stringValue, String name) {
        try {
            return new URI(stringValue);
        } catch (URISyntaxException ex) {
            throw handleParseException(name, URI.class, stringValue, ex);
        }
    }
}
