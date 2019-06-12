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

public class JsonDelegateUnknown extends JsonDelegateBase {
    protected JsonDelegateUnknown(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public JsonDelegateBase startObject(String name) {
        return new JsonDelegateUnknown(baseHandler, this);
    }

    public JsonDelegateBase startArray(String name) {
        return new JsonDelegateUnknown(baseHandler, this);
    }

    public boolean endObjectValue(String name) {
        return false;
    }

    public Object getResult() {
        return null;
    }
}
