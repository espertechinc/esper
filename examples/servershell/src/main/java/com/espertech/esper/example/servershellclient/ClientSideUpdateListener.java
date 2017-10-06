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
package com.espertech.esper.example.servershellclient;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class ClientSideUpdateListener implements Serializable, UpdateListener {
    private final static Logger log = LoggerFactory.getLogger(ClientSideUpdateListener.class);

    public void update(EventBean[] newEvents, EventBean[] oldEvents) {
        log.info("Single duration over 9.9: IPAddress: " + newEvents[0].get("ipAddress") +
                " Duration: " + newEvents[0].get("duration"));
    }

}
