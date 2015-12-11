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

package com.espertech.esper.regression.resultset;

import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.support.client.SupportConfigFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class TestGroupedTimeWinUniqueSortMinMax extends TestCase {

    private Configuration setup()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Sensor", Sensor.class);
        return config;
    }

    private void logEvent (Object theEvent) {
        log.info("Sending " + theEvent);
    }

    public void testSensorQuery() throws Exception {
        Configuration configuration = setup();
        configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(true);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        MatchListener listener = new MatchListener();

        String stmtString =
              "SELECT max(high.type) as type, \n" +
              " max(high.measurement) as highMeasurement, max(high.confidence) as confidenceOfHigh, max(high.device) as deviceOfHigh\n" +
              ",min(low.measurement) as lowMeasurement, min(low.confidence) as confidenceOfLow, min(low.device) as deviceOfLow\n" +
              "FROM\n" +
              " Sensor.std:groupwin(type).win:time(1 hour).std:unique(device).ext:sort(1, measurement desc) as high " +
              ",Sensor.std:groupwin(type).win:time(1 hour).std:unique(device).ext:sort(1, measurement asc) as low ";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtString);
        log.info(stmtString);
        stmt.addListener(listener);

        EPRuntime runtime = epService.getEPRuntime();
        List<Sensor> events = new ArrayList<Sensor>();
        events.add(new Sensor("Temperature", "Device1", 68.0, 96.5));
        events.add(new Sensor("Temperature", "Device2", 65.0, 98.5));
        events.add(new Sensor("Temperature", "Device1", 62.0, 95.3));
        events.add(new Sensor("Temperature", "Device2", 71.3, 99.3));
        for (Sensor theEvent : events) {
            logEvent (theEvent);
            runtime.sendEvent(theEvent);
        }
        EventBean lastEvent = listener.getLastEventBean();
        assertTrue (lastEvent != null);
        assertEquals (62.0,lastEvent.get("lowMeasurement"));
        assertEquals ("Device1",lastEvent.get("deviceOfLow"));
        assertEquals (95.3,lastEvent.get("confidenceOfLow"));
        assertEquals (71.3,lastEvent.get("highMeasurement"));
        assertEquals ("Device2",lastEvent.get("deviceOfHigh"));
        assertEquals (99.3,lastEvent.get("confidenceOfHigh"));

        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        epService.destroy();
    }

    static public class Sensor {

        public Sensor() {
        }

        public Sensor(String type, String device, Double measurement, Double confidence) {
            this.type = type;
            this.device = device;
            this.measurement = measurement;
            this.confidence = confidence;
         }

        public void setType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getDevice() {
            return device;
        }

        public void setMeasurement(Double measurement) {
            this.measurement = measurement;
        }

        public Double getMeasurement() {
            return measurement;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        public Double getConfidence() {
            return confidence;
        }

        private String type;
        private String device;
        private Double measurement;
        private Double confidence;
    }

    class MatchListener implements UpdateListener {
        private int count = 0;
        private Object lastEvent = null;
        private EventBean lastEventBean = null;

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            log.info("New events.................");
            if (newEvents != null) {
                for (int i = 0; i < newEvents.length; i++) {
                    EventBean e = newEvents[i];
                    EventType t = e.getEventType();
                    String[] propNames = t.getPropertyNames();
                    log.info("event[" + i + "] of type " + t);
                    for (int j=0; j < propNames.length; j++) {
                        log.info("    " + propNames[j] + ": " + e.get(propNames[j]));
                    }
                    count++;
                    lastEvent = e.getUnderlying();
                    lastEventBean = e;
                }
            }
            log.info("Removing events.................");
            if (oldEvents != null) {
                for (int i = 0; i < oldEvents.length; i++) {
                    EventBean e = oldEvents[i];
                    EventType t = e.getEventType();
                    String[] propNames = t.getPropertyNames();
                    log.info("event[" + i + "] of type " + t);
                    for (int j=0; j < propNames.length; j++) {
                        log.info("    " + propNames[j] + ": " + e.get(propNames[j]));
                    }
                    count--;
                }
            }
            log.info("......................................");
        }

        public int getCount() {
            return count;
        }

        public Object getLastEvent() {
            return lastEvent;
        }

        public EventBean getLastEventBean() {
            return lastEventBean;
        }
    }

    private static final Log log = LogFactory.getLog(TestGroupedTimeWinUniqueSortMinMax.class);
}
