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

import java.util.Map;

import static com.espertech.esper.common.internal.event.json.parser.core.JsonValueType.*;

public abstract class JsonDelegateBase {
    protected final JsonHandlerDelegator baseHandler;
    protected final JsonDelegateBase parent;

    protected String stringValue;
    protected JsonValueType valueType;
    protected Object objectValue;

    public abstract JsonDelegateBase startObject(String name);

    public abstract JsonDelegateBase startArray(String name);

    public abstract boolean endObjectValue(String name);

    public void endArrayValue(String name) {
    }

    public abstract Object getResult();

    public JsonDelegateBase(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        this.baseHandler = baseHandler;
        this.parent = parent;
    }

    public void endString(String string) {
        this.stringValue = string;
        this.valueType = STRING;
    }

    public void endNumber(String string) {
        this.stringValue = string;
        this.valueType = NUMBER;
    }

    public void endNull() {
        this.valueType = NULL;
        this.stringValue = null;
        this.objectValue = null;
    }

    public void endBoolean(boolean value) {
        this.valueType = BOOLEAN;
        if (value) {
            this.objectValue = Boolean.TRUE;
            this.stringValue = "true";
        } else {
            this.objectValue = Boolean.FALSE;
            this.stringValue = "false";
        }
    }

    public void setObjectValue(Object object) {
        this.objectValue = object;
    }

    public JsonDelegateBase getParent() {
        return parent;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param event event
     * @param name  name
     */
    public void addGeneralJson(Map<String, Object> event, String name) {
        event.put(name, valueToObject());
    }

    protected Object valueToObject() {
        if (valueType == STRING) {
            return stringValue;
        } else if (valueType == NUMBER) {
            return jsonNumberFromString(stringValue);
        } else if (valueType == NULL) {
            return null;
        } else {
            return objectValue;
        }
    }

    public static Object jsonNumberFromString(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return Double.parseDouble(text);
        }
    }
}
