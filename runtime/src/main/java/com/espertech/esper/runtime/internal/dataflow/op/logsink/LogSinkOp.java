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
package com.espertech.esper.runtime.internal.dataflow.op.logsink;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.event.core.EventBeanSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

public class LogSinkOp implements DataFlowOperator {

    private static final Logger LOGME = LoggerFactory.getLogger(LogSinkOp.class);

    private final LogSinkFactory factory;
    private final String dataFlowInstanceId;
    private final ConsoleOpRenderer renderer;
    private final String title;
    private final String layout;
    private final boolean log;
    private final boolean linefeed;

    private EventBeanSPI[] shellPerStream;

    public LogSinkOp(LogSinkFactory factory, String dataFlowInstanceId, ConsoleOpRenderer renderer, String title, String layout, boolean log, boolean linefeed) {
        this.factory = factory;
        this.dataFlowInstanceId = dataFlowInstanceId;
        this.renderer = renderer;
        this.title = title;
        this.layout = layout;
        this.log = log;
        this.linefeed = linefeed;

        shellPerStream = new EventBeanSPI[factory.getEventTypes().length];
        for (int i = 0; i < factory.getEventTypes().length; i++) {
            EventType eventType = factory.getEventTypes()[i];
            if (eventType != null) {
                shellPerStream[i] = EventTypeUtility.getShellForType(eventType);
            }
        }
    }

    public void onInput(int port, Object theEvent) {

        String line;
        if (layout == null) {

            StringWriter writer = new StringWriter();

            writer.write("[");
            writer.write(factory.getDataflowName());
            writer.write("] ");

            if (title != null) {
                writer.write("[");
                writer.write(title);
                writer.write("] ");
            }

            if (dataFlowInstanceId != null) {
                writer.write("[");
                writer.write(dataFlowInstanceId);
                writer.write("] ");
            }

            writer.write("[port ");
            writer.write(Integer.toString(port));
            writer.write("] ");

            getEventOut(port, theEvent, writer);
            line = writer.toString();
        } else {
            String result = layout.replace("%df", factory.getDataflowName()).replace("%p", Integer.toString(port));
            if (dataFlowInstanceId != null) {
                result = result.replace("%i", dataFlowInstanceId);
            }
            if (title != null) {
                result = result.replace("%t", title);
            }

            StringWriter writer = new StringWriter();
            getEventOut(port, theEvent, writer);
            result = result.replace("%e", writer.toString());

            line = result;
        }

        if (!linefeed) {
            line = line.replaceAll("\n", "").replaceAll("\r", "");
        }

        // output
        if (log) {
            LOGME.info(line);
        } else {
            System.out.println(line);
        }
    }

    private void getEventOut(int port, Object theEvent, StringWriter writer) {

        if (theEvent instanceof EventBean) {
            renderer.render((EventBean) theEvent, writer);
            return;
        }

        if (shellPerStream[port] != null) {
            synchronized (this) {
                shellPerStream[port].setUnderlying(theEvent);
                renderer.render(shellPerStream[port], writer);
            }
            return;
        }

        writer.write("Unrecognized underlying: ");
        writer.write(theEvent.toString());
    }
}
