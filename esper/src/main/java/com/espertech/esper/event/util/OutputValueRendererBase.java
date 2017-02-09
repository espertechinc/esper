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
 * Renderer for a Object values that can simply be output via to-string.
 */
public class OutputValueRendererBase implements OutputValueRenderer {
    public void render(Object object, StringBuilder buf) {
        if (object == null) {
            buf.append("null");
            return;
        }

        buf.append(object.toString());
    }
}
