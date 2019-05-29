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
package com.espertech.esper.common.internal.event.json.parser.delegates.array2dim;

import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.delegates.array.JsonDelegateArrayFloatPrimitive;

import java.util.Iterator;

public class JsonDelegateArray2DimFloatPrimitive extends JsonDelegateArray2DimBase {
    public JsonDelegateArray2DimFloatPrimitive(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public JsonDelegateBase startArrayInner() {
        return new JsonDelegateArrayFloatPrimitive(baseHandler, this);
    }

    public Object getResult() {
        float[][] array = new float[collection.size()][];
        Iterator<float[]> it = collection.iterator();
        int count = 0;
        while (it.hasNext()) {
            array[count++] = it.next();
        }
        return array;
    }
}
