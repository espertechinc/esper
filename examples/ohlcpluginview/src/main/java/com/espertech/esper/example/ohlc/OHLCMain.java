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
package com.espertech.esper.example.ohlc;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.time.CurrentTimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class OHLCMain {
    private static final Logger log = LoggerFactory.getLogger(OHLCMain.class);

    public static void main(String[] args) {
        OHLCMain sample = new OHLCMain();
        try {
            sample.run("OHLCEngineURI");
        } catch (RuntimeException ex) {
            log.error("Unexpected exception :" + ex.getMessage(), ex);
        }
    }

    public void run(String engineURI) {
        log.info("Setting up EPL");

        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);   // external timer for testing
        config.addEventType("OHLCTick", OHLCTick.class);
        config.addPlugInView("examples", "ohlcbarminute", OHLCBarPlugInViewFactory.class.getName());

        EPServiceProvider epService = EPServiceProviderManager.getProvider(engineURI, config);
        epService.initialize();     // Since running in a unit test may use the same engine many times

        // set time as an arbitrary start time
        sendTimer(epService, toTime("9:01:50"));

        Object[][] statements = new Object[][]{
                {"S1", "select * from OHLCTick#groupwin(ticker)#ohlcbarminute(timestamp, price)"},
        };

        for (Object[] statement : statements) {
            String stmtName = (String) statement[0];
            String expression = (String) statement[1];
            log.info("Creating statement: " + expression);
            EPStatement stmt = epService.getEPAdministrator().createEPL(expression, stmtName);

            if (stmtName.equals("S1")) {
                OHLCUpdateListener listener = new OHLCUpdateListener();
                stmt.addListener(listener);
            }
        }

        log.info("Sending test events");

        Object[][] input = new Object[][]{
                {"9:01:51", null},  // lets start simulating at 9:01:51
                {"9:01:52", "IBM", 100.5, "9:01:52"},  // lets have an event arrive on time
                {"9:02:03", "IBM", 100.0, "9:02:03"},
                {"9:02:10", "IBM", 99.0, "9:02:04"},  // lets have an event arrive later; this timer event also triggers a bucket
                {"9:02:20", "IBM", 98.0, "9:02:16"},
                {"9:02:30", "NOC", 11.0, "9:02:30"},
                {"9:02:45", "NOC", 12.0, "9:02:45"},
                {"9:02:55", "NOC", 13.0, "9:02:55"},
                {"9:03:02", "IBM", 101.0, "9:02:58"},   // this event arrives late but counts in the same bucket
                {"9:03:06", "IBM", 109.0, "9:02:59"},   // this event arrives too late: it should be ignored (5 second cutoff time, see view)
                {"9:03:07", "IBM", 103.0, "9:03:00"},   // this event should count for the next bucket
                {"9:03:55", "NOC", 12.5, "9:03:55"},
                {"9:03:58", "NOC", 12.75, "9:03:58"},
                {"9:04:00", "IBM", 104.0, "9:03:59"},
                {"9:04:02", "IBM", 105.0, "9:04:00"},   // next bucket starts with this event
                {"9:04:07", null},   // should complete next bucket even though there is no event arriving
                {"9:04:30", null},   // pretend no events
                {"9:04:59", null},
                {"9:05:00", null},
                {"9:05:10", null},
                {"9:05:15", "IBM", 105.5, "9:05:13"},
                {"9:05:59", null},
                {"9:06:07", null},
        };

        for (int i = 0; i < input.length; i++) {
            String timestampArrival = (String) input[i][0];
            log.info("Sending timer event " + timestampArrival);
            sendTimer(epService, toTime(timestampArrival));

            String ticker = (String) input[i][1];
            if (ticker != null) {
                double price = ((Number) input[i][2]).doubleValue();
                String timestampTick = (String) input[i][3];
                OHLCTick theEvent = new OHLCTick(ticker, price, toTime(timestampTick));

                log.info("Sending event " + theEvent);
                epService.getEPRuntime().sendEvent(theEvent);
            }
        }
    }

    private static void sendTimer(EPServiceProvider epService, long timestamp) {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(timestamp));
    }

    private static long toTime(String time) {
        String[] fields = time.split(":");
        int hour = Integer.parseInt(fields[0]);
        int min = Integer.parseInt(fields[1]);
        int sec = Integer.parseInt(fields[2]);
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2008, 1, 1, hour, min, sec);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}

