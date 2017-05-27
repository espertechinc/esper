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
package com.espertech.esper.regression.view;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExecViewGroupWithinGroup implements RegressionExecution {
    private final static String SYMBOL_MSFT = "MSFT";
    private final static String SYMBOL_GE = "GE";

    private final static String FEED_INFO = "INFO";
    private final static String FEED_REU = "REU";

    public void run(EPServiceProvider epService) throws Exception {
        // Listen to all ticks
        EPStatement viewGrouped = epService.getEPAdministrator().createEPL(
                "select irstream datapoints as size, symbol, feed, volume from " + SupportMarketDataBean.class.getName() +
                        "#groupwin(symbol)#groupwin(feed)#groupwin(volume)#uni(price) order by symbol, feed, volume");
        SupportUpdateListener listener = new SupportUpdateListener();

        // Counts per symbol, feed and volume the events
        viewGrouped.addListener(listener);

        ArrayList<Map<String, Object>> mapList = new ArrayList<>();

        // Set up a map of expected values

        Map<String, Object> expectedValues[] = new HashMap[10];
        for (int i = 0; i < expectedValues.length; i++) {
            expectedValues[i] = new HashMap<>();
        }

        // Send one event, check results
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);

        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1L, 0);
        mapList.add(expectedValues[0]);
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), mapList);
        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1L, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), mapList);
        EPAssertionUtil.assertPropsPerRow(viewGrouped.iterator(), mapList);

        // Send a couple of events
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 2);
        sendEvent(epService, SYMBOL_GE, FEED_INFO, 1);
        sendEvent(epService, SYMBOL_GE, FEED_REU, 99);
        sendEvent(epService, SYMBOL_MSFT, FEED_INFO, 100);

        populateMap(expectedValues[1], SYMBOL_MSFT, FEED_INFO, 100, 0);
        mapList.clear();
        mapList.add(expectedValues[1]);
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), mapList);
        populateMap(expectedValues[1], SYMBOL_MSFT, FEED_INFO, 100, 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), mapList);

        populateMap(expectedValues[0], SYMBOL_GE, FEED_INFO, 1, 3);
        populateMap(expectedValues[2], SYMBOL_GE, FEED_INFO, 2, 1);
        populateMap(expectedValues[3], SYMBOL_GE, FEED_REU, 99, 1);
        mapList.clear();
        mapList.add(expectedValues[0]);
        mapList.add(expectedValues[2]);
        mapList.add(expectedValues[3]);
        mapList.add(expectedValues[1]);
        EPAssertionUtil.assertPropsPerRow(viewGrouped.iterator(), mapList);
    }

    private void populateMap(Map<String, Object> map, String symbol, String feed, long volume, long size) {
        map.put("symbol", symbol);
        map.put("feed", feed);
        map.put("volume", volume);
        map.put("size", size);
    }

    private void sendEvent(EPServiceProvider epService, String symbol, String feed, long volume) {
        SupportMarketDataBean theEvent = new SupportMarketDataBean(symbol, 0, volume, feed);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
