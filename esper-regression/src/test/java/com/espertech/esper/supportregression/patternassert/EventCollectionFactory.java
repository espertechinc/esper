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
package com.espertech.esper.supportregression.patternassert;

import com.espertech.esper.supportregression.bean.*;

import java.util.LinkedHashMap;

public class EventCollectionFactory implements SupportBeanConstants {
    /**
     * Make a A to G data set for testing with external clocking
     */
    public static EventCollection getEventSetOne(long baseTime, long numMSecBetweenEvents) {
        LinkedHashMap<String, Object> testData = makeMixedSet();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, baseTime, numMSecBetweenEvents);
        return new EventCollection(testData, times);
    }

    /**
     * Make a A only data set for testing with external clocking
     */
    public static EventCollection getSetTwoExternalClock(long baseTime, long numMSecBetweenEvents) {
        LinkedHashMap<String, Object> testData = makeTestDataUniform();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, baseTime, numMSecBetweenEvents);
        return new EventCollection(testData, times);
    }

    public static EventCollection getSetThreeExternalClock(long baseTime, long numMSecBetweenEvents) {
        LinkedHashMap<String, Object> testData = makeTestDataNumeric();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, baseTime, numMSecBetweenEvents);
        return new EventCollection(testData, times);
    }

    public static EventCollection getSetFourExternalClock(long baseTime, long numMSecBetweenEvents) {
        LinkedHashMap<String, Object> testData = makeTestDataS0();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, baseTime, numMSecBetweenEvents);
        return new EventCollection(testData, times);
    }

    public static EventCollection getSetFiveInterfaces() {
        LinkedHashMap<String, Object> testData = makeTestDataInterfaces();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, 0, 100);
        return new EventCollection(testData, times);
    }

    public static EventCollection getSetSixComplexProperties() {
        LinkedHashMap<String, Object> testData = makeTestDataComplexProps();
        LinkedHashMap<String, Long> times = makeExternalClockTimes(testData, 0, 100);
        return new EventCollection(testData, times);
    }

    private static LinkedHashMap<String, Object> makeMixedSet() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        testData.put("A1", new SupportBean_A("A1"));
        testData.put("B1", new SupportBean_B("B1"));
        testData.put("C1", new SupportBean_C("C1"));
        testData.put("B2", new SupportBean_B("B2"));
        testData.put("A2", new SupportBean_A("A2"));
        testData.put("D1", new SupportBean_D("D1"));
        testData.put("E1", new SupportBean_E("E1"));
        testData.put("F1", new SupportBean_F("F1"));
        testData.put("D2", new SupportBean_D("D2"));
        testData.put("B3", new SupportBean_B("B3"));
        testData.put("G1", new SupportBean_G("G1"));
        testData.put("D3", new SupportBean_D("D3"));

        return testData;
    }

    // Make time values sending events exactly every seconds, starting at time zero, first event after 1 second
    private static LinkedHashMap<String, Long> makeExternalClockTimes(LinkedHashMap<String, Object> testData,
                                                                      long baseTime,
                                                                      long numMSecBetweenEvents) {
        LinkedHashMap<String, Long> testDataTimers = new LinkedHashMap<String, Long>();

        testDataTimers.put(EventCollection.ON_START_EVENT_ID, baseTime);

        for (String id : testData.keySet()) {
            baseTime += numMSecBetweenEvents;
            testDataTimers.put(id, baseTime);
        }

        return testDataTimers;
    }

    private static LinkedHashMap<String, Object> makeTestDataUniform() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        testData.put("B1", new SupportBean_A("B1"));
        testData.put("B2", new SupportBean_A("B2"));
        testData.put("B3", new SupportBean_A("B3"));
        testData.put("A4", new SupportBean_A("A4"));
        testData.put("A5", new SupportBean_A("A5"));
        testData.put("A6", new SupportBean_A("A6"));

        return testData;
    }

    private static LinkedHashMap<String, Object> makeTestDataNumeric() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        testData.put("N1", new SupportBean_N(01, -56, 44.0, -60.5, true, true));
        testData.put("N2", new SupportBean_N(66, 59, 48.0, 70.999, true, false));
        testData.put("N3", new SupportBean_N(87, -5, 44.5, -23.5, false, true));
        testData.put("N4", new SupportBean_N(86, -98, 42.1, -79.5, true, true));
        testData.put("N5", new SupportBean_N(00, -33, 48.0, 44.45, true, false));
        testData.put("N6", new SupportBean_N(55, -55, 44.0, -60.5, false, true));
        testData.put("N7", new SupportBean_N(34, 92, 39.0, -66.5, false, true));
        testData.put("N8", new SupportBean_N(100, 66, 47.5, 45.0, true, false));

        return testData;
    }

    private static LinkedHashMap<String, Object> makeTestDataS0() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        // B arrives 3 times
        // G arrives twice, in a row
        // F and C arrive twice
        testData.put("e1", new SupportBean_S0(1, "A"));
        testData.put("e2", new SupportBean_S0(2, "B"));   // B
        testData.put("e3", new SupportBean_S0(3, "C"));                   // C
        testData.put("e4", new SupportBean_S0(4, "D"));
        testData.put("e5", new SupportBean_S0(5, "E"));
        testData.put("e6", new SupportBean_S0(6, "B"));   // B
        testData.put("e7", new SupportBean_S0(7, "F"));               // F
        testData.put("e8", new SupportBean_S0(8, "C"));                   // C
        testData.put("e9", new SupportBean_S0(9, "G"));           // G
        testData.put("e10", new SupportBean_S0(10, "G"));           // G
        testData.put("e11", new SupportBean_S0(11, "B"));   // B
        testData.put("e12", new SupportBean_S0(12, "F"));               // F

        return testData;
    }

    /**
     * ISupportBaseAB
     * ISupportA
     * ISupportB
     * ISupportABCImpl
     *
     * @return
     */

    private static LinkedHashMap<String, Object> makeTestDataInterfaces() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        testData.put("e1", new ISupportCImpl("C1"));
        testData.put("e2", new ISupportABCImpl("A1", "B1", "BaseB", "C1"));
        testData.put("e3", new ISupportAImpl("A1", "BaseAB"));
        testData.put("e4", new ISupportBImpl("B1", "BaseAB"));
        testData.put("e5", new ISupportDImpl("D1", "BaseD", "BaseDBase"));
        testData.put("e6", new ISupportBCImpl("B2", "BaseAB2", "C2"));
        testData.put("e7", new ISupportBaseABImpl("BaseAB3"));
        testData.put("e8", new SupportOverrideOneA("OA1", "O1", "OBase"));
        testData.put("e9", new SupportOverrideOneB("OB1", "O2", "OBase"));
        testData.put("e10", new SupportOverrideOne("O3", "OBase"));
        testData.put("e11", new SupportOverrideBase("OBase"));
        testData.put("e12", new ISupportAImplSuperGImplPlus("G1", "A3", "BaseAB4", "B4", "C2"));
        testData.put("e13", new ISupportAImplSuperGImpl("G2", "A14", "BaseAB5"));

        return testData;
    }

    private static LinkedHashMap<String, Object> makeTestDataComplexProps() {
        LinkedHashMap<String, Object> testData = new LinkedHashMap<String, Object>();

        testData.put("e1", SupportBeanComplexProps.makeDefaultBean());
        testData.put("e2", SupportBeanCombinedProps.makeDefaultBean());

        return testData;
    }
}
