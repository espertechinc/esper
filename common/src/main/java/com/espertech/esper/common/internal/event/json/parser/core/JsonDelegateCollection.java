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

import java.util.ArrayList;

public class JsonDelegateCollection extends JsonDelegateBase {
    private final JsonDelegateFactory factory;
    private final ArrayList events = new ArrayList(4);

    public JsonDelegateCollection(JsonHandlerDelegator baseHandler, JsonDelegateBase parent, JsonDelegateFactory factory) {
        super(baseHandler, parent);
        this.factory = factory;
    }

    public JsonDelegateBase startObject(String name) {
        return factory.make(baseHandler, this);
    }

    public JsonDelegateBase startArray(String name) {
        return null;
    }

    public boolean endObjectValue(String name) {
        return false;
    }

    @Override
    public void endArrayValue(String name) {
        events.add(objectValue);
    }

    public Object getResult() {
        return events;
    }
}
