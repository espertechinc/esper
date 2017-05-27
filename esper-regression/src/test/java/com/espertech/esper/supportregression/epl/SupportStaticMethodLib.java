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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.supportregression.bean.*;
import org.apache.avro.generic.GenericData;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class SupportStaticMethodLib {
    private static List<Object[]> invocations = new ArrayList<Object[]>();
    private static List<EPLMethodInvocationContext> methodInvocationContexts = new ArrayList<EPLMethodInvocationContext>();

    public static List<Object[]> getInvocations() {
        return invocations;
    }

    public static List<EPLMethodInvocationContext> getMethodInvocationContexts() {
        return methodInvocationContexts;
    }

    public static EventBean[] eventBeanArrayForString(String value, EPLMethodInvocationContext context) {
        String[] split = value.split(",");
        EventBean[] events = new EventBean[split.length];
        for (int i = 0; i < split.length; i++) {
            events[i] = context.getEventBeanService().adapterForMap(Collections.singletonMap("p0", split[i]), "MyItemEvent");
        }
        return events;
    }

    public static Collection<EventBean> eventBeanCollectionForString(String value, EPLMethodInvocationContext context) {
        return Arrays.asList(eventBeanArrayForString(value, context));
    }

    public static Iterator<EventBean> eventBeanIteratorForString(String value, EPLMethodInvocationContext context) {
        return eventBeanCollectionForString(value, context).iterator();
    }

    public static boolean compareEvents(SupportMarketDataBean beanOne, SupportBean beanTwo) {
        return beanOne.getSymbol().equals(beanTwo.getTheString());
    }

    public static Map fetchMapArrayMRMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static LinkedHashMap fetchObjectArrayEventBeanMetadata() {
        LinkedHashMap<String, Class> values = new LinkedHashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static LinkedHashMap fetchOAArrayMRMetadata() {
        LinkedHashMap<String, Class> values = new LinkedHashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static Map fetchSingleValueMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("result", Integer.class);
        return values;
    }

    public static Map[] fetchResult12(Integer value) {
        if (value == null) {
            return new Map[0];
        }

        Map[] result = new Map[2];
        result[0] = new HashMap<String, Integer>();
        result[0].put("value", 1);
        result[1] = new HashMap<String, Integer>();
        result[1].put("value", 2);
        return result;
    }

    public static Map fetchResult12Metadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("value", Integer.class);
        return values;
    }

    public static Map[] fetchResult23(Integer value) {
        if (value == null) {
            return new Map[0];
        }

        Map[] result = new Map[2];
        result[0] = new HashMap<String, Integer>();
        result[0].put("value", 2);
        result[1] = new HashMap<String, Integer>();
        result[1].put("value", 3);
        return result;
    }

    public static Map fetchResult23Metadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("value", Integer.class);
        values.put("valueTwo", Integer.class);
        return values;
    }

    public static String join(SupportBean bean) {
        return bean.getTheString() + " " + Integer.toString(bean.getIntPrimitive());
    }

    public static Map[] fetchResult100() {
        Map[] result = new Map[100];
        int count = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                result[count] = new HashMap<String, Integer>();
                result[count].put("col1", i);
                result[count].put("col2", j);
                count++;
            }
        }
        return result;
    }

    public static Map fetchResult100Metadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("col1", Integer.class);
        values.put("col2", Integer.class);
        return values;
    }

    public static Map[] fetchBetween(Integer lower, Integer upper) {
        if (lower == null || upper == null) {
            return new Map[0];
        }

        if (upper < lower) {
            return new Map[0];
        }

        int delta = upper - lower + 1;
        Map[] result = new Map[delta];
        int count = 0;
        for (int i = lower; i <= upper; i++) {
            Map<String, Integer> values = new HashMap<String, Integer>();
            values.put("value", i);
            result[count++] = values;
        }
        return result;
    }

    public static Map[] fetchBetweenString(Integer lower, Integer upper) {
        if (lower == null || upper == null) {
            return new Map[0];
        }

        if (upper < lower) {
            return new Map[0];
        }

        int delta = upper - lower + 1;
        Map[] result = new Map[delta];
        int count = 0;
        for (int i = lower; i <= upper; i++) {
            Map<String, String> values = new HashMap<String, String>();
            values.put("value", Integer.toString(i));
            result[count++] = values;
        }
        return result;
    }

    public static Map fetchBetweenMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("value", Integer.class);
        return values;
    }

    public static Map fetchBetweenStringMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("value", String.class);
        return values;
    }

    public static Map[] fetchMapArrayMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return new Map[0];
        }

        Map[] rows = new Map[id];
        for (int i = 0; i < id; i++) {
            Map<String, Object> values = new HashMap<String, Object>();
            rows[i] = values;

            values.put("mapstring", "|" + theString + "_" + i + "|");
            values.put("mapint", i + 100);
        }

        return rows;
    }

    public static Object[][] fetchOAArrayMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return new Object[0][];
        }

        Object[][] rows = new Object[id][];
        for (int i = 0; i < id; i++) {
            Object[] values = new Object[2];
            rows[i] = values;

            values[0] = "|" + theString + "_" + i + "|";
            values[1] = i + 100;
        }

        return rows;
    }

    public static Map fetchMapMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static Map fetchMap(String theString, int id) {
        if (id < 0) {
            return null;
        }

        Map<String, Object> values = new HashMap<String, Object>();
        if (id == 0) {
            return values;
        }

        values.put("mapstring", "|" + theString + "|");
        values.put("mapint", id + 1);
        return values;
    }

    public static Map fetchMapEventBeanMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static Map fetchMapEventBean(EventBean eventBean, String propOne, String propTwo) {
        String theString = (String) eventBean.get(propOne);
        int id = (Integer) eventBean.get(propTwo);

        if (id < 0) {
            return null;
        }

        Map<String, Object> values = new HashMap<String, Object>();
        if (id == 0) {
            return values;
        }

        values.put("mapstring", "|" + theString + "|");
        values.put("mapint", id + 1);
        return values;
    }

    public static Map fetchIdDelimitedMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("result", String.class);
        return values;
    }

    public static Map fetchIdDelimited(Integer value) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("result", "|" + value + "|");
        return values;
    }

    public static Map convertEventMap(Map<String, Object> values) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("one", values.get("one"));
        result.put("two", "|" + values.get("two") + "|");
        return result;
    }

    public static Object[] convertEventObjectArray(Object[] values) {
        return new Object[]{values[0], "|" + values[1] + "|"};
    }

    public static GenericData.Record convertEventAvro(GenericData.Record row) {
        String val1 = row.get("one").toString();
        String val2 = row.get("two").toString();
        GenericData.Record upd = new GenericData.Record(row.getSchema());
        upd.put("one", val1);
        upd.put("two", "|" + val2 + "|");
        return upd;
    }

    public static SupportBean convertEvent(SupportMarketDataBean bean) {
        return new SupportBean(bean.getSymbol(), (bean.getVolume()).intValue());
    }

    public static Object staticMethod(Object object) {
        return object;
    }

    public static Object staticMethodWithContext(Object object, EPLMethodInvocationContext context) {
        methodInvocationContexts.add(context);
        return object;
    }

    public static int arrayLength(Object object) {
        if (!object.getClass().isArray()) {
            return -1;
        }
        return Array.getLength(object);
    }

    public static void throwException() throws Exception {
        throw new Exception("throwException text here");
    }

    public static SupportBean throwExceptionBeanReturn() throws Exception {
        throw new Exception("throwException text here");
    }

    public static boolean isStringEquals(String value, String compareTo) {
        return value.equals(compareTo);
    }

    public static double minusOne(double value) {
        return value - 1;
    }

    public static int plusOne(int value) {
        return value + 1;
    }

    public static String appendPipe(String theString, String value) {
        return theString + "|" + value;
    }

    public static SupportBean_S0 fetchObjectAndSleep(String fetchId, int passThroughNumber, long msecSleepTime) {
        try {
            Thread.sleep(msecSleepTime);
        } catch (InterruptedException e) {
        }
        return new SupportBean_S0(passThroughNumber, "|" + fetchId + "|");
    }

    public static FetchedData fetchObjectNoArg() {
        return new FetchedData("2");
    }

    public static FetchedData fetchObject(String id) {
        if (id == null) {
            return null;
        }
        return new FetchedData("|" + id + "|");
    }

    public static FetchedData[] fetchArrayNoArg() {
        return new FetchedData[]{new FetchedData("1")};
    }

    public static FetchedData[] fetchArrayGen(int numGenerate) {
        if (numGenerate < 0) {
            return null;
        }
        if (numGenerate == 0) {
            return new FetchedData[0];
        }
        if (numGenerate == 1) {
            return new FetchedData[]{new FetchedData("A")};
        }

        FetchedData[] fetched = new FetchedData[numGenerate];
        for (int i = 0; i < numGenerate; i++) {
            int c = 'A' + i;
            fetched[i] = new FetchedData(Character.toString((char) c));
        }
        return fetched;
    }

    public static long passthru(long value) {
        return value;
    }

    public static void sleep(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted during sleep", e);
        }
    }

    public static boolean sleepReturnTrue(long msec) {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted during sleep", e);
        }
        return true;
    }

    public static String delimitPipe(String theString) {
        if (theString == null) {
            return "|<null>|";
        }
        return "|" + theString + "|";
    }

    public static class FetchedData {
        private String id;

        public FetchedData(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static boolean volumeGreaterZero(SupportMarketDataBean bean) {
        return bean.getVolume() > 0;
    }

    public static boolean volumeGreaterZeroEventBean(EventBean bean) {
        long volume = (Long) bean.get("volume");
        return volume > 0;
    }

    public static BigInteger myBigIntFunc(BigInteger val) {
        return val;
    }

    public static BigDecimal myBigDecFunc(BigDecimal val) {
        return val;
    }

    public static Map<String, String> myMapFunc() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("A", "A1");
        map.put("B", "B1");
        return map;
    }

    public static int[] myArrayFunc() {
        return new int[]{100, 200, 300};
    }

    public static int arraySumIntBoxed(Integer[] array) {
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                continue;
            }
            sum += array[i];
        }
        return sum;
    }

    public static double arraySumDouble(Double[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                continue;
            }
            sum += array[i];
        }
        return sum;
    }

    public static double arraySumString(String[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                continue;
            }
            sum += Double.parseDouble(array[i]);
        }
        return sum;
    }

    public static boolean alwaysTrue(Object[] input) {
        invocations.add(input);
        return true;
    }

    public static double arraySumObject(Object[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                continue;
            }
            if (array[i] instanceof Number) {
                sum += ((Number) array[i]).doubleValue();
            } else {
                sum += Double.parseDouble(array[i].toString());
            }
        }
        return sum;
    }

    public static SupportBean makeSupportBean(String theString, Integer intPrimitive) {
        return new SupportBean(theString, intPrimitive);
    }

    public static SupportBeanNumeric makeSupportBeanNumeric(Integer intOne, Integer intTwo) {
        return new SupportBeanNumeric(intOne, intTwo);
    }

    public static Object[] fetchObjectArrayEventBean(String theString, int id) {
        if (id < 0) {
            return null;
        }

        Map<String, Object> values = new HashMap<String, Object>();
        if (id == 0) {
            return new Object[2];
        }

        Object[] fields = new Object[2];
        fields[0] = "|" + theString + "|";
        fields[1] = id + 1;
        return fields;
    }

    public static LinkedHashMap<String, Object> fetchTwoRows3ColsMetadata() {
        LinkedHashMap<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("pkey0", String.class);
        values.put("pkey1", Integer.class);
        values.put("c0", Long.class);
        return values;
    }

    public static Map[] fetchTwoRows3Cols() {
        Map[] result = new Map[]{new HashMap<String, Object>(), new HashMap<String, Object>()};

        result[0].put("pkey0", "E1");
        result[0].put("pkey1", 10);
        result[0].put("c0", 100L);

        result[1].put("pkey0", "E2");
        result[1].put("pkey1", 20);
        result[1].put("c0", 200L);

        return result;
    }

    public static MyMethodReturn[] fetchPOJOArray(String mystring, int myint) {
        if (myint < 0) {
            return null;
        }
        if (myint == 0) {
            return new MyMethodReturn[]{new MyMethodReturn(null, null)};
        }
        return new MyMethodReturn[]{new MyMethodReturn("|" + mystring + "|", myint + 1)};
    }

    public static Collection<MyMethodReturn> fetchPOJOCollection(String mystring, int myint) {
        if (myint < 0) {
            return null;
        }
        if (myint == 0) {
            return Collections.singletonList(new MyMethodReturn(null, null));
        }
        return Collections.singletonList(new MyMethodReturn("|" + mystring + "|", myint + 1));
    }

    public static Iterator<MyMethodReturn> fetchPOJOIterator(String mystring, int myint) {
        if (myint < 0) {
            return null;
        }
        return fetchPOJOCollection(mystring, myint).iterator();
    }

    public static MyMethodReturn[] fetchPOJOArrayMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return new MyMethodReturn[0];
        }

        MyMethodReturn[] rows = new MyMethodReturn[id];
        for (int i = 0; i < id; i++) {
            rows[i] = new MyMethodReturn("|" + theString + "_" + i + "|", i + 100);
        }

        return rows;
    }

    public static Collection<MyMethodReturn> fetchPOJOCollectionMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return Collections.emptyList();
        }

        List<MyMethodReturn> rows = new ArrayList<MyMethodReturn>(id);
        for (int i = 0; i < id; i++) {
            rows.add(new MyMethodReturn("|" + theString + "_" + i + "|", i + 100));
        }

        return rows;
    }

    public static Iterator<MyMethodReturn> fetchPOJOIteratorMR(String theString, int id) {
        if (id < 0) {
            return null;
        }
        return fetchPOJOCollectionMR(theString, id).iterator();
    }

    public static Map fetchMapCollectionMRMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static Collection<Map> fetchMapCollectionMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return Collections.emptyList();
        }

        List<Map> rows = new ArrayList<Map>(id);
        for (int i = 0; i < id; i++) {
            Map<String, Object> values = new HashMap<String, Object>();
            rows.add(values);

            values.put("mapstring", "|" + theString + "_" + i + "|");
            values.put("mapint", i + 100);
        }

        return rows;
    }

    public static Map fetchMapIteratorMRMetadata() {
        return fetchMapCollectionMRMetadata();
    }

    public static Iterator<Map> fetchMapIteratorMR(String theString, int id) {
        if (id < 0) {
            return null;
        }
        return fetchMapCollectionMR(theString, id).iterator();
    }

    public static LinkedHashMap fetchOACollectionMRMetadata() {
        LinkedHashMap<String, Class> values = new LinkedHashMap<String, Class>();
        values.put("mapstring", String.class);
        values.put("mapint", Integer.class);
        return values;
    }

    public static Collection<Object[]> fetchOACollectionMR(String theString, int id) {
        if (id < 0) {
            return null;
        }

        if (id == 0) {
            return Collections.emptyList();
        }

        List<Object[]> rows = new ArrayList<Object[]>(id);
        for (int i = 0; i < id; i++) {
            Object[] values = new Object[2];
            rows.add(values);

            values[0] = "|" + theString + "_" + i + "|";
            values[1] = i + 100;
        }

        return rows;
    }

    public static LinkedHashMap fetchOAIteratorMRMetadata() {
        return fetchOACollectionMRMetadata();
    }

    public static Iterator<Object[]> fetchOAIteratorMR(String theString, int id) {
        if (id < 0) {
            return null;
        }
        return fetchOACollectionMR(theString, id).iterator();
    }

    public static class MyMethodReturn {
        private final String mapstring;
        private final Integer mapint;

        public MyMethodReturn(String mapstring, Integer mapint) {
            this.mapstring = mapstring;
            this.mapint = mapint;
        }

        public String getMapstring() {
            return mapstring;
        }

        public Integer getMapint() {
            return mapint;
        }
    }

    public static Map[] overloadedMethodForJoin() {
        return getOverloadedMethodForJoinResult("A", "B");
    }

    public static Map[] overloadedMethodForJoin(int first) {
        return getOverloadedMethodForJoinResult(Integer.toString(first), "B");
    }

    public static Map[] overloadedMethodForJoin(String first) {
        return getOverloadedMethodForJoinResult(first, "B");
    }

    public static Map[] overloadedMethodForJoin(String first, int second) {
        return getOverloadedMethodForJoinResult(first, Integer.toString(second));
    }

    public static Map[] overloadedMethodForJoin(int first, int second) {
        return getOverloadedMethodForJoinResult(Integer.toString(first), Integer.toString(second));
    }

    public static Map overloadedMethodForJoinMetadata() {
        Map<String, Class> values = new HashMap<String, Class>();
        values.put("col1", String.class);
        values.put("col2", String.class);
        return values;
    }

    public static SupportBean_S0[] invalidOverloadForJoin(String first) {
        return null;
    }

    public static SupportBean_S1[] invalidOverloadForJoin(Integer first) {
        return null;
    }

    private static Map[] getOverloadedMethodForJoinResult(String first, String second) {
        Map<String, Object> values = new HashMap<>();
        values.put("col1", first);
        values.put("col2", second);
        return new Map[]{values};
    }
}
