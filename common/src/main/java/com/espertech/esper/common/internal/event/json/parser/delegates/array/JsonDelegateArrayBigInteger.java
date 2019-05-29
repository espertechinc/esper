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
import com.espertech.esper.common.internal.event.json.parser.delegates.endvalue.JsonEndValueForgeBigInteger;

import java.math.BigInteger;

public class JsonDelegateArrayBigInteger extends JsonDelegateArrayBase {
    public JsonDelegateArrayBigInteger(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public void endOfArrayValue(String name) {
        collection.add(JsonEndValueForgeBigInteger.jsonToBigInteger(stringValue, name));
    }

    public Object getResult() {
        return collection.toArray(new BigInteger[collection.size()]);
    }
}
