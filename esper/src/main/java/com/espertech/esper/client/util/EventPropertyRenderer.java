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

/**
 * Interface for use with the JSON or XML event renderes to handle custom event property rendering.
 * <p>
 * Implementations of this interface are called for each event property and may utilize the
 * context object provided to render the event property value to a string.
 * </p>
 * <p>The context itself contains a reference to the default renderer that can be delegated to for
 * properties that use the default rendering.</p>
 * <p>Do not retain a handle to the renderer context as the context object changes for each event property.</p>
 */
public interface EventPropertyRenderer {
    /**
     * Render an event property.
     *
     * @param context provides information about the property
     */
    public void render(EventPropertyRendererContext context);
}
