/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.event;

import com.espertech.esper.plugin.*;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventSender;
import com.espertech.esper.core.service.EPRuntimeEventSender;

import java.net.URI;

public class SupportEventRepresentation implements PlugInEventRepresentation
{
    private static PlugInEventRepresentationContext initContext;
    private static PlugInEventTypeHandlerContext acceptTypeContext;
    private static PlugInEventTypeHandlerContext eventTypeContext;
    private static PlugInEventBeanReflectorContext acceptBeanContext;
    private static PlugInEventBeanReflectorContext eventBeanContext;

    public void init(PlugInEventRepresentationContext eventRepresentationContext)
    {
        this.initContext = eventRepresentationContext;
    }

    public boolean acceptsType(PlugInEventTypeHandlerContext acceptTypeContext)
    {
        this.acceptTypeContext = acceptTypeContext;
        return true;
    }

    public PlugInEventTypeHandler getTypeHandler(PlugInEventTypeHandlerContext eventTypeContext)
    {
        this.eventTypeContext = eventTypeContext;
        return new PlugInEventTypeHandler() {

            public EventType getType()
            {
                return null;
            }

            public EventSender getSender(EPRuntimeEventSender runtimeEventSender)
            {
                return null; 
            }
        };
    }

    public boolean acceptsEventBeanResolution(PlugInEventBeanReflectorContext context)
    {
        this.acceptBeanContext = context;
        return true;
    }

    public PlugInEventBeanFactory getEventBeanFactory(PlugInEventBeanReflectorContext context)
    {
        this.eventBeanContext = context;
        return new PlugInEventBeanFactory() {
            public EventBean create(Object theEvent, URI resolutionURI)
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
    }

    public static PlugInEventRepresentationContext getInitContext()
    {
        return initContext;
    }

    public static PlugInEventTypeHandlerContext getAcceptTypeContext()
    {
        return acceptTypeContext;
    }

    public static PlugInEventTypeHandlerContext getEventTypeContext()
    {
        return eventTypeContext;
    }

    public static PlugInEventBeanReflectorContext getAcceptBeanContext()
    {
        return acceptBeanContext;
    }

    public static PlugInEventBeanReflectorContext getEventBeanContext()
    {
        return eventBeanContext;
    }
}
