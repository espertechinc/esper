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
package com.espertech.esper.common.client.render;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.WriterConfig;
import com.espertech.esper.common.client.json.minimaljson.WritingBuffer;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

import java.io.IOException;
import java.io.StringWriter;

public class JSONEventRendererJsonEventType implements JSONEventRenderer {
    public final static JSONEventRendererJsonEventType INSTANCE = new JSONEventRendererJsonEventType();

    private JSONEventRendererJsonEventType() {
    }

    public String render(String title, EventBean theEvent) {
        JsonEventObject event = (JsonEventObject) theEvent.getUnderlying();
        StringWriter writer = new StringWriter();
        writer.append("{\"").append(title).append("\":");
        try {
            event.writeTo(writer, WriterConfig.MINIMAL);
        } catch (IOException e) {
            throw new EPException("Failed to write json: " + e.getMessage(), e);
        }
        writer.append("}");
        return writer.toString();
    }

    public String render(EventBean theEvent) {
        Object underlying = theEvent.getUnderlying();
        if (underlying instanceof JsonEventObject) {
            JsonEventObject event = (JsonEventObject) underlying;
            return event.toString(WriterConfig.MINIMAL);
        }
        JsonEventType eventType = (JsonEventType) theEvent.getEventType();
        StringWriter writer = new StringWriter();
        try {
            WritingBuffer buffer = new WritingBuffer(writer, 128);
            eventType.getDelegateFactory().write(WriterConfig.MINIMAL.createWriter(buffer), underlying);
            buffer.flush();
        } catch (IOException exception) {
            // StringWriter does not throw IOExceptions
            throw new RuntimeException(exception);
        }
        return writer.toString();
    }
}
