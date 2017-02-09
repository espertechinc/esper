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
package com.espertech.esper.example.rfidassetzone;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetGroupCountListener implements UpdateListener {
    private static final Logger log = LoggerFactory.getLogger(AssetGroupCountListener.class);

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        int groupId = (Integer) newEvents[0].get("groupId");
        int zone = (Integer) newEvents[0].get("zone");
        long cnt = (Long) newEvents[0].get("cnt");

        log.info(".update " +
                " groupId=" + groupId +
                " zone=" + zone +
                " cnt=" + cnt);
    }
}
