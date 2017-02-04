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

package com.espertech.esper.regression.datetime;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndA;
import com.espertech.esper.supportregression.bean.SupportTimeStartEndB;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestPerfDTIntervalOps extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        listener = null;
    }

    public void testPerf() {

        ConfigurationEventTypeLegacy config = new ConfigurationEventTypeLegacy();
        config.setStartTimestampPropertyName("longdateStart");
        config.setEndTimestampPropertyName("longdateEnd");
        epService.getEPAdministrator().getConfiguration().addEventType("A", SupportTimeStartEndA.class.getName(), config);
        epService.getEPAdministrator().getConfiguration().addEventType("B", SupportTimeStartEndB.class.getName(), config);

        epService.getEPAdministrator().createEPL("create window AWindow#keepall as A");
        epService.getEPAdministrator().createEPL("insert into AWindow select * from A");

        // preload
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("A" + i, "2002-05-30T09:00:00.000", 100));
        }
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("AEarlier", "2002-05-30T08:00:00.000", 100));
        epService.getEPRuntime().sendEvent(SupportTimeStartEndA.make("ALater", "2002-05-30T10:00:00.000", 100));

        // assert BEFORE
        String eplBefore = "select a.key as c0 from AWindow as a, B b unidirectional where a.before(b)";
        runAssertion(eplBefore, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSec = "select a.key as c0 from AWindow as a, B b unidirectional where a.longdateEnd.before(b.longdateStart)";
        runAssertion(eplBeforeMSec, "2002-05-30T09:00:00.000", 0, "AEarlier");
        
        String eplBeforeMSecMix1 = "select a.key as c0 from AWindow as a, B b unidirectional where a.longdateEnd.before(b)";
        runAssertion(eplBeforeMSecMix1, "2002-05-30T09:00:00.000", 0, "AEarlier");

        String eplBeforeMSecMix2 = "select a.key as c0 from AWindow as a, B b unidirectional where a.before(b.longdateStart)";
        runAssertion(eplBeforeMSecMix2, "2002-05-30T09:00:00.000", 0, "AEarlier");

        // assert AFTER
        String eplAfter = "select a.key as c0 from AWindow as a, B b unidirectional where a.after(b)";
        runAssertion(eplAfter, "2002-05-30T09:00:00.000", 0, "ALater");

        // assert COINCIDES
        String eplCoincides = "select a.key as c0 from AWindow as a, B b unidirectional where a.coincides(b)";
        runAssertion(eplCoincides, "2002-05-30T08:00:00.000", 100, "AEarlier");

        // assert DURING
        String eplDuring = "select a.key as c0 from AWindow as a, B b unidirectional where a.during(b)";
        runAssertion(eplDuring, "2002-05-30T07:59:59.000", 2000, "AEarlier");

        // assert FINISHES
        String eplFinishes = "select a.key as c0 from AWindow as a, B b unidirectional where a.finishes(b)";
        runAssertion(eplFinishes, "2002-05-30T07:59:59.950", 150, "AEarlier");

        // assert FINISHED-BY
        String eplFinishedBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.finishedBy(b)";
        runAssertion(eplFinishedBy, "2002-05-30T08:00:00.050", 50, "AEarlier");

        // assert INCLUDES
        String eplIncludes = "select a.key as c0 from AWindow as a, B b unidirectional where a.includes(b)";
        runAssertion(eplIncludes, "2002-05-30T08:00:00.050", 20, "AEarlier");

        // assert MEETS
        String eplMeets = "select a.key as c0 from AWindow as a, B b unidirectional where a.meets(b)";
        runAssertion(eplMeets, "2002-05-30T08:00:00.100", 0, "AEarlier");

        // assert METBY
        String eplMetBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.metBy(b)";
        runAssertion(eplMetBy, "2002-05-30T07:59:59.950", 50, "AEarlier");

        // assert OVERLAPS
        String eplOverlaps = "select a.key as c0 from AWindow as a, B b unidirectional where a.overlaps(b)";
        runAssertion(eplOverlaps, "2002-05-30T08:00:00.050", 100, "AEarlier");

        // assert OVERLAPPEDY
        String eplOverlappedBy = "select a.key as c0 from AWindow as a, B b unidirectional where a.overlappedBy(b)";
        runAssertion(eplOverlappedBy, "2002-05-30T09:59:59.950", 100, "ALater");
        runAssertion(eplOverlappedBy, "2002-05-30T07:59:59.950", 100, "AEarlier");

        // assert STARTS
        String eplStarts = "select a.key as c0 from AWindow as a, B b unidirectional where a.starts(b)";
        runAssertion(eplStarts, "2002-05-30T08:00:00.000", 150, "AEarlier");

        // assert STARTEDBY
        String eplEnds = "select a.key as c0 from AWindow as a, B b unidirectional where a.startedBy(b)";
        runAssertion(eplEnds, "2002-05-30T08:00:00.000", 50, "AEarlier");
    }

    private void runAssertion(String epl, String timestampB, long durationB, String expectedAKey) {

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        // query
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            epService.getEPRuntime().sendEvent(SupportTimeStartEndB.make("B", timestampB, durationB));
            assertEquals(expectedAKey, listener.assertOneGetNewAndReset().get("c0"));
        }
        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;
        assertTrue("Delta=" + delta/1000d, delta < 500);

        stmt.destroy();
    }
}
