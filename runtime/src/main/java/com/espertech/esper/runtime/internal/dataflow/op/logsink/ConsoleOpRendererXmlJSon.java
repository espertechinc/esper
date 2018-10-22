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
package com.espertech.esper.runtime.internal.dataflow.op.logsink;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.render.EPRenderEventService;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.render.XMLEventRenderer;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class ConsoleOpRendererXmlJSon implements ConsoleOpRenderer {
    private final LogSinkOutputFormat format;
    private final EPRenderEventService runtimeRenderEvent;

    private final Map<EventType, JSONEventRenderer> jsonRendererCache = new HashMap<EventType, JSONEventRenderer>();
    private final Map<EventType, XMLEventRenderer> xmlRendererCache = new HashMap<EventType, XMLEventRenderer>();

    public ConsoleOpRendererXmlJSon(LogSinkOutputFormat format, EPRenderEventService runtimeRenderEvent) {
        this.format = format;
        this.runtimeRenderEvent = runtimeRenderEvent;
    }

    public void render(EventBean theEvent, StringWriter writer) {
        String result;
        if (format == LogSinkOutputFormat.json) {
            JSONEventRenderer renderer = jsonRendererCache.get(theEvent.getEventType());
            if (renderer == null) {
                renderer = getJsonRenderer(theEvent.getEventType());
                jsonRendererCache.put(theEvent.getEventType(), renderer);
            }
            result = renderer.render(theEvent.getEventType().getName(), theEvent);
        } else {
            XMLEventRenderer renderer = xmlRendererCache.get(theEvent.getEventType());
            if (renderer == null) {
                renderer = getXmlRenderer(theEvent.getEventType());
                xmlRendererCache.put(theEvent.getEventType(), renderer);
            }
            result = renderer.render(theEvent.getEventType().getName(), theEvent);
        }
        writer.append(result);
    }

    protected JSONEventRenderer getJsonRenderer(EventType eventType) {
        return runtimeRenderEvent.getJSONRenderer(eventType, RenderingOptions.getJsonOptions());
    }

    protected XMLEventRenderer getXmlRenderer(EventType eventType) {
        return runtimeRenderEvent.getXMLRenderer(eventType, RenderingOptions.getXmlOptions());
    }
}
