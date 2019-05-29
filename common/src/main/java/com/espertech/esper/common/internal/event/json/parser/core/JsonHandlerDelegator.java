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

import com.espertech.esper.common.client.json.minimaljson.JsonHandler;

public class JsonHandlerDelegator extends JsonHandler<Object, Object> {
    private JsonDelegateBase currentDelegate;
    private String currentName;

    public void setDelegate(JsonDelegateBase top) {
        this.currentDelegate = top;
    }

    public void startObjectValue(Object object, String name) {
        this.currentName = name;
    }

    public Object startObject() {
        if (currentName != null) {
            currentDelegate.valueType = JsonValueType.OBJECT;
            JsonDelegateBase next = currentDelegate.startObject(currentName);
            if (next == null) { // assign unknown delegate
                next = new JsonDelegateUnknown(this, currentDelegate);
            }
            this.currentDelegate = next;
        }
        return null;
    }

    public Object startArray() {
        if (currentName != null) {
            currentDelegate.valueType = JsonValueType.ARRAY;
            JsonDelegateBase next = currentDelegate.startArray(currentName);
            if (next == null) { // assign unknown delegate
                next = new JsonDelegateUnknown(this, currentDelegate);
            }
            this.currentDelegate = next;
        }
        return null;
    }

    public void endString(String string) {
        this.currentDelegate.endString(string);
    }

    public void endNumber(String string) {
        this.currentDelegate.endNumber(string);
    }

    public void endNull() {
        this.currentDelegate.endNull();
    }

    public void endBoolean(boolean value) {
        this.currentDelegate.endBoolean(value);
    }

    public void endObjectValue(Object object, String name) {
        this.currentDelegate.endObjectValue(name);
    }

    public void endArrayValue(Object array) {
        this.currentDelegate.endArrayValue(currentName);
    }

    public void endArray(Object array) {
        Object result = this.currentDelegate.getResult();
        currentDelegate = currentDelegate.getParent();
        if (currentDelegate != null) {
            currentDelegate.setObjectValue(result);
        }
    }

    public void endObject(Object object) {
        Object result = this.currentDelegate.getResult();
        currentDelegate = currentDelegate.getParent();
        if (currentDelegate != null) {
            currentDelegate.setObjectValue(result);
        }
    }
}
