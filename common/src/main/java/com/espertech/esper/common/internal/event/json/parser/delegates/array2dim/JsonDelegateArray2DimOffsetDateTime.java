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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.delegates.array.JsonDelegateArrayOffsetDateTime;

import java.time.OffsetDateTime;

public class JsonDelegateArray2DimOffsetDateTime extends JsonDelegateArray2DimBase {
    public final static EPTypeClass EPTYPE = new EPTypeClass(JsonDelegateArray2DimOffsetDateTime.class);
    public JsonDelegateArray2DimOffsetDateTime(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public JsonDelegateBase startArrayInner() {
        return new JsonDelegateArrayOffsetDateTime(baseHandler, this);
    }

    public Object getResult() {
        return collection.toArray(new OffsetDateTime[collection.size()][]);
    }
}
