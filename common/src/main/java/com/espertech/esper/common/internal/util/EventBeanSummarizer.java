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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.EventBean;

import java.io.StringWriter;

public class EventBeanSummarizer {
    public static String summarize(EventBean theEvent) {
        if (theEvent == null) {
            return "(null)";
        }
        StringWriter writer = new StringWriter();
        summarize(theEvent, writer);
        return writer.toString();
    }

    public static void summarize(EventBean theEvent, StringWriter writer) {
        if (theEvent == null) {
            writer.write("(null)");
            return;
        }
        writer.append(theEvent.getEventType().getName());
        writer.append("[");
        summarizeUnderlying(theEvent.getUnderlying(), writer);
        writer.append("]");
    }

    public static String summarizeUnderlying(Object underlying) {
        if (underlying == null) {
            return "(null)";
        }
        StringWriter writer = new StringWriter();
        summarizeUnderlying(underlying, writer);
        return writer.toString();
    }

    public static void summarizeUnderlying(Object underlying, StringWriter writer) {
        JavaClassHelper.getObjectValuePretty(underlying, writer);
    }

    public static String summarize(EventBean[] events) {
        if (events == null) {
            return "(null)";
        }
        if (events.length == 0) {
            return "(empty)";
        }
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (int i = 0; i < events.length; i++) {
            writer.write(delimiter);
            writer.write("event ");
            writer.write(Integer.toString(i));
            writer.write(":");
            summarize(events[i], writer);
            delimiter = ", ";
        }
        return writer.toString();
    }
}
