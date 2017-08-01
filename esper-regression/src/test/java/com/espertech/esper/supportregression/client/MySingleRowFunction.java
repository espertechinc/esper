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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.supportregression.bean.ISupportBaseAB;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MySingleRowFunction {
    private final static List<EPLMethodInvocationContext> methodInvokeContexts = new ArrayList<EPLMethodInvocationContext>();

    public static List<EPLMethodInvocationContext> getMethodInvokeContexts() {
        return methodInvokeContexts;
    }

    public static int computePower3(int i) {
        return i * i * i;
    }

    public static int computePower3WithContext(int i, EPLMethodInvocationContext context) {
        methodInvokeContexts.add(context);
        return i * i * i;
    }

    public static String surroundx(String target) {
        return "X" + target + "X";
    }

    public static InnerSingleRow getChainTop() {
        return new InnerSingleRow();
    }

    public static void throwexception() {
        throw new RuntimeException("This is a 'throwexception' generated exception");
    }

    public static boolean isNullValue(EventBean event, String propertyName) {
        return event.get(propertyName) == null;
    }

    public static String getValueAsString(EventBean event, String propertyName) {
        Object result = event.get(propertyName);
        return result != null ? result.toString() : null;
    }

    public static class InnerSingleRow {
        public int chainValue(int i, int j) {
            return i * j;
        }
    }

    public static boolean eventsCheckStrings(Collection<EventBean> events, String property, String value) {
        for (EventBean event : events) {
            if (event.get(property).equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static String varargsOnlyInt(int... values) {
        Object[] objects = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        return toCSV(objects);
    }

    public static String varargsW1Param(String first, double... values) {
        Object[] objects = new Object[values.length + 1];
        objects[0] = first;
        for (int i = 0; i < values.length; i++) {
            objects[i + 1] = values[i];
        }
        return toCSV(objects);
    }

    public static String varargsW2Param(int first, double second, Long... values) {
        Object[] objects = new Object[values.length + 2];
        objects[0] = first;
        objects[1] = second;
        for (int i = 0; i < values.length; i++) {
            objects[i + 2] = values[i];
        }
        return toCSV(objects);
    }

    public static String varargsOnlyWCtx(EPLMethodInvocationContext ctx, int... values) {
        return "CTX+" + varargsOnlyInt(values);
    }

    public static String varargsW1ParamWCtx(String first, EPLMethodInvocationContext ctx, Integer... values) {
        return "CTX+" + first + "," + toCSV(values);
    }

    public static String varargsW2ParamWCtx(String first, String second, EPLMethodInvocationContext ctx, Integer... values) {
        return "CTX+" + first + "," + second + "," + toCSV(values);
    }

    public static String varargsOnlyObject(Object... values) {
        return toCSV(values);
    }

    public static String varargsOnlyString(String... values) {
        return toCSV(values);
    }

    public static String varargsObjectsWCtx(EPLMethodInvocationContext ctx, Object... values) {
        return "CTX+" + toCSV(values);
    }

    public static String varargsOnlyBoxedFloat(Float... values) {
        return toCSV(values);
    }

    public static String varargsOnlyBoxedShort(Short... values) {
        return toCSV(values);
    }

    public static String varargsOnlyBoxedByte(Byte... values) {
        return toCSV(values);
    }

    public static String varargsOnlyBigInt(BigInteger... values) {
        return toCSV(values);
    }

    public static String varargsW1ParamObjectsWCtx(int param, EPLMethodInvocationContext ctx, Object... values) {
        return "CTX+" + "," + param + "," + toCSV(values);
    }

    public static String varargsOnlyNumber(Number... values) {
        return toCSV(values);
    }

    public static String varargsOnlyISupportBaseAB(ISupportBaseAB... values) {
        return toCSV(values);
    }

    private static String toCSV(Object[] values) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (Object item : values) {
            writer.append(delimiter);
            writer.append(item == null ? "null" : item.toString());
            delimiter = ",";
        }
        return writer.toString();
    }
}
