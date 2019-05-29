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
package com.espertech.esper.common.internal.event.json.parser.delegates.array;

import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeLong;

import java.util.Iterator;

public class JsonDelegateArrayLongPrimitive extends JsonDelegateArrayBase {
    public JsonDelegateArrayLongPrimitive(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public void endOfArrayValue(String name) {
        collection.add(JsonEndValueForgeLong.jsonToLongNonNull(stringValue, name));
    }

    public Object getResult() {
        long[] array = new long[collection.size()];
        Iterator<Long> it = collection.iterator();
        int count = 0;
        while (it.hasNext()) {
            array[count++] = it.next();
        }
        return array;
    }
}
