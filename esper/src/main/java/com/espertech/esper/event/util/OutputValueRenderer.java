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
package com.espertech.esper.event.util;

/**
 * For rendering an output value returned by a property.
 */
public interface OutputValueRenderer {
    /**
     * Renders the value to the buffer.
     *
     * @param object to render
     * @param buf    buffer to populate
     */
    public void render(Object object, StringBuilder buf);
}
