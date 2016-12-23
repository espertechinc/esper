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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.*;

public class MyGetterUpdateListener implements StatementAwareUpdateListener
{
    private final EventPropertyGetter symbolGetter;
    private final EventPropertyGetter volumeGetter;

    private String lastSymbol;
    private Long lastVolume;
    private EPStatement epStatement;
    private EPServiceProvider epServiceProvider;

    public void update(EventBean[] eventBeans, EventBean[] eventBeans1, EPStatement epStatement, EPServiceProvider epServiceProvider)
    {
        this.epStatement = epStatement;
        this.epServiceProvider = epServiceProvider;
        lastSymbol = (String) symbolGetter.get(eventBeans[0]);
        lastVolume = (Long) volumeGetter.get(eventBeans[0]);
    }

    public MyGetterUpdateListener(EventType eventType)
    {
        symbolGetter = eventType.getGetter("symbol");
        volumeGetter = eventType.getGetter("volume");
    }

    public String getLastSymbol()
    {
        return lastSymbol;
    }

    public Long getLastVolume()
    {
        return lastVolume;
    }

    public EPStatement getEpStatement()
    {
        return epStatement;
    }

    public EPServiceProvider getEpServiceProvider()
    {
        return epServiceProvider;
    }
}
