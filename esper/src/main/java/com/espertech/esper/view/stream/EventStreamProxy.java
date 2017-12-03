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
package com.espertech.esper.view.stream;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.annotation.Audit;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterSpecParam;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.EventBeanSummarizer;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.EventStream;

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class EventStreamProxy implements java.lang.reflect.InvocationHandler {

    private final String engineURI;
    private final String statementName;
    private final String eventTypeAndFilter;
    private final EventStream eventStream;

    public static EventStream getAuditProxy(String engineURI, String statementName, Annotation[] annotations, FilterSpecCompiled filterSpec, EventStream designated) {
        Audit audit = AuditEnum.STREAM.getAudit(annotations);
        if (audit == null) {
            return designated;
        }

        StringWriter writer = new StringWriter();
        writer.write(filterSpec.getFilterForEventType().getName());
        if (filterSpec.getParameters() != null && filterSpec.getParameters().length > 0) {
            writer.write('(');
            String delimiter = "";
            for (FilterSpecParam[] paramLine : filterSpec.getParameters()) {
                writer.write(delimiter);
                writeFilter(writer, paramLine);
                delimiter = " or ";
            }
            writer.write(')');
        }

        return (EventStream) EventStreamProxy.newInstance(engineURI, statementName, writer.toString(), designated);
    }

    private static void writeFilter(StringWriter writer, FilterSpecParam[] paramLine) {
        String delimiter = "";
        for (FilterSpecParam param : paramLine) {
            writer.write(delimiter);
            writer.write(param.getLookupable().getExpression());
            writer.write(param.getFilterOperator().getTextualOp());
            writer.write("...");
            delimiter = ",";
        }
    }

    public static Object newInstance(String engineURI, String statementName, String eventTypeAndFilter, EventStream eventStream) {
        return java.lang.reflect.Proxy.newProxyInstance(
                eventStream.getClass().getClassLoader(),
                JavaClassHelper.getSuperInterfaces(eventStream.getClass()),
                new EventStreamProxy(engineURI, statementName, eventTypeAndFilter, eventStream));
    }

    public EventStreamProxy(String engineURI, String statementName, String eventTypeAndFilter, EventStream eventStream) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.eventTypeAndFilter = eventTypeAndFilter;
        this.eventStream = eventStream;
    }

    public Object invoke(Object proxy, Method m, Object[] args)
            throws Throwable {

        if (m.getName().equals("insert")) {
            if (AuditPath.isInfoEnabled()) {
                Object arg = args[0];
                String events = "(undefined)";
                if (arg instanceof EventBean[]) {
                    events = EventBeanSummarizer.summarize((EventBean[]) arg);
                } else if (arg instanceof EventBean) {
                    events = EventBeanSummarizer.summarize((EventBean) arg);
                }
                AuditPath.auditLog(engineURI, statementName, AuditEnum.STREAM, eventTypeAndFilter + " inserted " + events);
            }
        }

        return m.invoke(eventStream, args);
    }
}

