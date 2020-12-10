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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.client.EventType;

public abstract class ResultSetProcessorFactoryForgeBase implements ResultSetProcessorFactoryForge {
    protected final EventType resultEventType;
    protected final EventType[] typesPerStream;

    public ResultSetProcessorFactoryForgeBase(EventType resultEventType, EventType[] typesPerStream) {
        this.resultEventType = resultEventType;
        this.typesPerStream = typesPerStream;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public EventType[] getTypesPerStream() {
        return typesPerStream;
    }
}
