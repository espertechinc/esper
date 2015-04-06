/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.EventBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;

public class AssetZoneSplitListener implements UpdateListener
{
    private static final Log log = LogFactory.getLog(AssetZoneSplitListener.class);

    private List<Integer> callbacks = new ArrayList<Integer>();

    public void update(EventBean[] newEvents, EventBean[] oldEvents)
    {
        int groupId = (Integer) newEvents[0].get("a.groupId");
        callbacks.add(groupId);
        log.info(".update Received event from group id " + groupId);
    }

    public List<Integer> getCallbacks()
    {
        return callbacks;
    }

    public void reset()
    {
        callbacks.clear();
    }
}
