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
package com.espertech.esper.common.internal.event.render;

import com.espertech.esper.common.client.render.EventPropertyRenderer;
import com.espertech.esper.common.client.render.EventPropertyRendererContext;

public class EventPropertyRendererDefault implements EventPropertyRenderer {

    public static final EventPropertyRendererDefault INSTANCE = new EventPropertyRendererDefault();

    public void render(EventPropertyRendererContext context) {

    }
}
