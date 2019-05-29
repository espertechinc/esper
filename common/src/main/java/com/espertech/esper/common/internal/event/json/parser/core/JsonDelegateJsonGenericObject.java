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
package com.espertech.esper.common.internal.event.json.parser.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonDelegateJsonGenericObject extends JsonDelegateJsonGenericBase {
    private final Map<String, Object> jsonObject = new LinkedHashMap<>();

    public JsonDelegateJsonGenericObject(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public boolean endObjectValue(String name) {
        addGeneralJson(jsonObject, name);
        return true;
    }

    public Object getResult() {
        return jsonObject;
    }
}
