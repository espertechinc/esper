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
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeByte;

import java.util.Iterator;

public class JsonDelegateArrayBytePrimitive extends JsonDelegateArrayBase {
    public JsonDelegateArrayBytePrimitive(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public void endOfArrayValue(String name) {
        collection.add(JsonEndValueForgeByte.jsonToByteNonNull(stringValue));
    }

    public Object getResult() {
        byte[] array = new byte[collection.size()];
        Iterator<Byte> it = collection.iterator();
        int count = 0;
        while (it.hasNext()) {
            array[count++] = it.next();
        }
        return array;
    }
}
