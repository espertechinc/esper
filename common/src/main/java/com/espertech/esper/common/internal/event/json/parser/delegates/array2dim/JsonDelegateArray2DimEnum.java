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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateBase;
import com.espertech.esper.common.internal.event.json.parser.core.JsonDelegateEventObjectArray;
import com.espertech.esper.common.internal.event.json.parser.core.JsonHandlerDelegator;
import com.espertech.esper.common.internal.event.json.parser.delegates.array.JsonDelegateArrayEnum;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Method;

public class JsonDelegateArray2DimEnum extends JsonDelegateArray2DimBase {
    private final Class enumType;
    private final Class enumTypeArray;
    private final Method valueOf;

    public JsonDelegateArray2DimEnum(JsonHandlerDelegator baseHandler, JsonDelegateBase parent, Class enumType) {
        super(baseHandler, parent);
        this.enumType = enumType;
        this.enumTypeArray = JavaClassHelper.getArrayType(enumType);
        try {
            valueOf = enumType.getMethod("valueOf", new Class[]{String.class});
        } catch (NoSuchMethodException e) {
            throw new EPException("Failed to find valueOf method for " + enumType);
        }
    }

    public JsonDelegateBase startArrayInner() {
        return new JsonDelegateArrayEnum(baseHandler, this, enumType, valueOf);
    }

    public Object getResult() {
        return JsonDelegateEventObjectArray.collectionToTypedArray(collection, enumTypeArray);
    }
}
