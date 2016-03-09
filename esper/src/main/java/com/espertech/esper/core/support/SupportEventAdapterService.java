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

package com.espertech.esper.core.support;

import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventAdapterServiceImpl;
import com.espertech.esper.event.EventTypeIdGeneratorImpl;

public class SupportEventAdapterService
{
    private static EventAdapterService eventAdapterService;

    static
    {
        eventAdapterService = new EventAdapterServiceImpl(new EventTypeIdGeneratorImpl(), 5);
    }

    public static void reset()
    {
        eventAdapterService = new EventAdapterServiceImpl(new EventTypeIdGeneratorImpl(), 5);
    }
    public static EventAdapterService getService()
    {
        return eventAdapterService;
    }
}
