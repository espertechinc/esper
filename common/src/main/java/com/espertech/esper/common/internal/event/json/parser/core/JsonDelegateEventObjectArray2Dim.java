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

import com.espertech.esper.common.internal.event.json.parser.delegates.array2dim.JsonDelegateArray2DimBase;

public class JsonDelegateEventObjectArray2Dim extends JsonDelegateArray2DimBase {
    private final JsonDelegateFactory factory;
    private final Class componentType;

    public JsonDelegateEventObjectArray2Dim(JsonHandlerDelegator baseHandler, JsonDelegateBase parent, JsonDelegateFactory factory, Class componentType) {
        super(baseHandler, parent);
        this.factory = factory;
        this.componentType = componentType;
    }

    public JsonDelegateBase startArrayInner() {
        return new JsonDelegateEventObjectArray(baseHandler, this, factory, componentType.getComponentType());
    }

    public Object getResult() {
        return JsonDelegateEventObjectArray.collectionToTypedArray(collection, componentType);
    }
}
