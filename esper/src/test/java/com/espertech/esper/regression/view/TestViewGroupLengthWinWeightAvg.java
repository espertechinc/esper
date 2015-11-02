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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestViewGroupLengthWinWeightAvg extends TestCase
{
    public void testSensorQuery() throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("Sensor", Sensor.class);
        EPServiceProvider epService = EPServiceProviderManager.getProvider("testSensorQuery", config);

        boolean useGroup = true;
        SupportUpdateListener listener = new SupportUpdateListener();
        if (useGroup)
        {
            // 0.69 sec for 100k
            String stmtString = "select * from Sensor.std:groupwin(type).win:length(10000000).stat:weighted_avg(measurement, confidence)";
            //String stmtString = "SELECT * FROM Sensor.std:groupwin(type).win:length(1000).stat:weighted_avg('measurement','confidence')";
            EPStatement stmt = epService.getEPAdministrator().createEPL(stmtString);
            stmt.addListener(listener);
        }
        else
        {
            // 0.53 sec for 100k
            for (int i = 0; i < 10; i++)
            {
                String stmtString = "SELECT * FROM Sensor(type='A" + i + "').win:length(1000000).stat:weighted_avg(measurement,confidence)";
                EPStatement stmt = epService.getEPAdministrator().createEPL(stmtString);
                stmt.addListener(listener);
            }
        }

        // prime
        for (int i = 0; i < 100; i++) {
            epService.getEPRuntime().sendEvent(new Sensor("A", "1", (double) i, (double) i));
        }

        // measure
        long numEvents = 10000;
        long startTime = System.nanoTime();
        for (int i = 0; i < numEvents; i++) {
            //int modulo = i % 10;
            int modulo = 1;
            String type = "A" + modulo;
            epService.getEPRuntime().sendEvent(new Sensor(type, "1", (double) i, (double) i));

            if (i % 1000 == 0)
            {
                //System.out.println("Send " + i + " events");
                listener.reset();
            }
        }
        long endTime = System.nanoTime();
        double delta = (endTime - startTime) / 1000d / 1000d / 1000d;
        // System.out.println("delta=" + delta);
        assertTrue(delta < 1);

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
}
