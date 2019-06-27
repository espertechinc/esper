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

import com.espertech.esper.common.client.json.minimaljson.JsonWriter;

import java.io.IOException;

public interface JsonDelegateFactory {
    JsonDelegateBase make(JsonHandlerDelegator handler, JsonDelegateBase optionalParent);
    void write(JsonWriter writer, Object und) throws IOException;
    Object newUnderlying();
    void setValue(int num, Object value, Object und);
    Object getValue(int num, Object und);
    Object copy(Object und);
}
