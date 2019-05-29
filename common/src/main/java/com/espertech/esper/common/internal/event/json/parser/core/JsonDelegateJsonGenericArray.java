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
import java.util.Collection;

public class JsonDelegateJsonGenericArray extends JsonDelegateJsonGenericBase {
    private final Collection collection = new ArrayList();

    public JsonDelegateJsonGenericArray(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public boolean endObjectValue(String name) {
        return false;
    }

    @Override
    public void endArrayValue(String name) {
        collection.add(valueToObject());
    }

    public Object getResult() {
        return collection.toArray();
    }
}
