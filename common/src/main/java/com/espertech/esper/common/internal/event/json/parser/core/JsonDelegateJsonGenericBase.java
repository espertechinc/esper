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

public abstract class JsonDelegateJsonGenericBase extends JsonDelegateBase {

    public JsonDelegateJsonGenericBase(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public final JsonDelegateBase startObject(String name) {
        return new JsonDelegateJsonGenericObject(baseHandler, this);
    }

    public final JsonDelegateBase startArray(String name) {
        return new JsonDelegateJsonGenericArray(baseHandler, this);
    }
}
