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
package com.espertech.esper.common.internal.epl.variable.core;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.EventPropertyWriter;

public class VariableTriggerWriteDesc {
    private EventType type;
    private String variableName;
    private EventPropertyWriter writer;
    private EventPropertyValueGetter getter;

    public void setType(EventType type) {
        this.type = type;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setWriter(EventPropertyWriter writer) {
        this.writer = writer;
    }

    public void setGetter(EventPropertyValueGetter getter) {
        this.getter = getter;
    }

    public String getVariableName() {
        return variableName;
    }

    public EventPropertyWriter getWriter() {
        return writer;
    }

    public EventType getType() {
        return type;
    }

    public EventPropertyValueGetter getGetter() {
        return getter;
    }
}
