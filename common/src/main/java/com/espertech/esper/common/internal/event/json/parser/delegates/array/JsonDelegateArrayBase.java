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

import java.util.ArrayList;
import java.util.Collection;

public abstract class JsonDelegateArrayBase extends JsonDelegateBase {
    protected Collection collection = new ArrayList();

    public abstract void endOfArrayValue(String name);

    public JsonDelegateArrayBase(JsonHandlerDelegator baseHandler, JsonDelegateBase parent) {
        super(baseHandler, parent);
    }

    public final JsonDelegateBase startObject(String name) {
        return null;
    }

    public final JsonDelegateBase startArray(String name) {
        return null;
    }

    public final boolean endObjectValue(String name) {
        return false;
    }

    @Override
    public final void endArrayValue(String name) {
        endOfArrayValue(name);
    }
}
