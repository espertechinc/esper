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

package com.espertech.esper.regression.client;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MySingleRowFunction
{
    private final static List<EPLMethodInvocationContext> methodInvokeContexts = new ArrayList<EPLMethodInvocationContext>();

    public static List<EPLMethodInvocationContext> getMethodInvokeContexts() {
        return methodInvokeContexts;
    }

    public static int computePower3(int i)
    {
        return i * i * i;
    }

    public static int computePower3WithContext(int i, EPLMethodInvocationContext context) {
        methodInvokeContexts.add(context);
        return i * i * i;
    }

    public static String surroundx(String target)
    {
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
            return i*j;
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
}
