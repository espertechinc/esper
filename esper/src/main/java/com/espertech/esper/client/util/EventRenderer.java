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
package com.espertech.esper.client.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;

/**
 * Provider for rendering services of {@link EventBean} events.
 */
public interface EventRenderer {
    /**
     * Returns a render for the JSON format, valid only for the given event type and its subtypes.
     *
     * @param eventType to return renderer for
     * @param options   rendering options
     * @return JSON format renderer
     */
    public JSONEventRenderer getJSONRenderer(EventType eventType, JSONRenderingOptions options);

    /**
     * Returns a render for the JSON format, valid only for the given event type and its subtypes.
     *
     * @param eventType to return renderer for
     * @return JSON format renderer
     */
    public JSONEventRenderer getJSONRenderer(EventType eventType);

    /**
     * Quick-access method to render a given event in the JSON format.
     * <p>
     * Use the #getJSONRenderer to obtain a renderer instance that allows repeated rendering of the same type of event.
     * For performance reasons obtaining a dedicated renderer instance is the preferred method compared to repeated rendering via this method.
     *
     * @param title    the JSON root title
     * @param theEvent the event to render
     * @return JSON formatted text
     */
    public String renderJSON(String title, EventBean theEvent);

    /**
     * Quick-access method to render a given event in the JSON format.
     * <p>
     * Use the #getJSONRenderer to obtain a renderer instance that allows repeated rendering of the same type of event.
     * For performance reasons obtaining a dedicated renderer instance is the preferred method compared to repeated rendering via this method.
     *
     * @param title    the JSON root title
     * @param theEvent the event to render
     * @param options  are JSON rendering options
     * @return JSON formatted text
     */
    public String renderJSON(String title, EventBean theEvent, JSONRenderingOptions options);

    /**
     * Returns a render for the XML format, valid only for the given event type and its subtypes.
     *
     * @param eventType to return renderer for
     * @return XML format renderer
     */
    public XMLEventRenderer getXMLRenderer(EventType eventType);

    /**
     * Returns a render for the XML format, valid only for the given event type and its subtypes.
     *
     * @param eventType to return renderer for
     * @param options   rendering options
     * @return XML format renderer
     */
    public XMLEventRenderer getXMLRenderer(EventType eventType, XMLRenderingOptions options);

    /**
     * Quick-access method to render a given event in the XML format.
     * <p>
     * Use the #getXMLRenderer to obtain a renderer instance that allows repeated rendering of the same type of event.
     * For performance reasons obtaining a dedicated renderer instance is the preferred method compared to repeated rendering via this method.
     *
     * @param rootElementName the root element name that may also include namespace information
     * @param theEvent        the event to render
     * @return XML formatted text
     */
    public String renderXML(String rootElementName, EventBean theEvent);

    /**
     * Quick-access method to render a given event in the XML format.
     * <p>
     * Use the #getXMLRenderer to obtain a renderer instance that allows repeated rendering of the same type of event.
     * For performance reasons obtaining a dedicated renderer instance is the preferred method compared to repeated rendering via this method.
     *
     * @param rootElementName the root element name that may also include namespace information
     * @param theEvent        the event to render
     * @param options         are XML rendering options
     * @return XML formatted text
     */
    public String renderXML(String rootElementName, EventBean theEvent, XMLRenderingOptions options);
}
