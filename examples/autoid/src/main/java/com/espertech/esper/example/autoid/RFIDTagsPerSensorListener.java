/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.example.autoid;

import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.EventBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RFIDTagsPerSensorListener implements UpdateListener
{
    public void update(EventBean[] newEvents, EventBean[] oldEvents)
    {
        if (newEvents != null)
        {
            logRate(newEvents[0]);
        }
    }

    private void logRate(EventBean theEvent)
    {
        String sensorId = (String) theEvent.get("sensorId");
        double numTags = (Double) theEvent.get("numTagsPerSensor");

        log.info("Sensor " + sensorId + " totals " + numTags + " tags");
    }

    private static final Log log = LogFactory.getLog(RFIDTagsPerSensorListener.class);
}
