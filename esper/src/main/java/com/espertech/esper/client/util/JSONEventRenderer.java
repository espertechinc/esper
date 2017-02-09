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

/**
 * Renderer for an event into the JSON textual format.
 * <p>
 * A renderer is dedicated to rendering only a certain type of events and subtypes of that type, as the
 * render cache type metadata and prepares structures to enable fast rendering.
 * <p>
 * For rendering events of different types, use a quick-access method in {@link EventRenderer}.
 */
public interface JSONEventRenderer {
    /**
     * Render a given event in the JSON format.
     *
     * @param title    the JSON root title
     * @param theEvent the event to render
     * @return JSON formatted text
     */
    public String render(String title, EventBean theEvent);

    /**
     * Render a given event in the JSON format.
     *
     * @param theEvent the event to render
     * @return JSON formatted text
     */
    public String render(EventBean theEvent);

}
