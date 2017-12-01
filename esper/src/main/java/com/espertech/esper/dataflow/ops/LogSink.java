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
package com.espertech.esper.dataflow.ops;

import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.util.*;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.event.EventBeanSPI;
import com.espertech.esper.util.EventBeanSummarizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@DataFlowOperator
public class LogSink implements DataFlowOpLifecycle {

    private static final Logger LOGME = LoggerFactory.getLogger(LogSink.class);

    @DataFlowOpParameter
    private String title;

    @DataFlowOpParameter
    private String layout;

    @DataFlowOpParameter
    private String format;

    @DataFlowOpParameter
    private boolean log = true;

    @DataFlowOpParameter
    private boolean linefeed = true;

    private String dataflowName;
    private String dataFlowInstanceId;
    private ConsoleOpRenderer renderer;

    private EventBeanSPI[] shellPerStream;

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        if (!context.getOutputPorts().isEmpty()) {
            throw new IllegalArgumentException("LogSink operator does not provide an output stream");
        }

        dataflowName = context.getDataflowName();
        dataFlowInstanceId = context.getDataflowInstanceId();

        shellPerStream = new EventBeanSPI[context.getInputPorts().size()];
        for (Map.Entry<Integer, DataFlowOpInputPort> entry : context.getInputPorts().entrySet()) {
            EventType eventType = entry.getValue().getTypeDesc().getEventType();
            if (eventType != null) {
                shellPerStream[entry.getKey()] = context.getStatementContext().getEventAdapterService().getShellForType(eventType);
            }
        }

        if (format == null) {
            renderer = new ConsoleOpRendererSummary();
        } else {
            try {
                LogSinkOutputFormat formatEnum = LogSinkOutputFormat.valueOf(format.trim().toLowerCase(Locale.ENGLISH));
                if (formatEnum == LogSinkOutputFormat.summary) {
                    renderer = new ConsoleOpRendererSummary();
                } else {
                    renderer = new ConsoleOpRendererXmlJSon(formatEnum, context.getEngine().getEPRuntime());
                }
            } catch (RuntimeException ex) {
                throw new ExprValidationException("Format '" + format + "' is not supported, expecting any of " + Arrays.toString(LogSinkOutputFormat.values()));
            }
        }

        return null;
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void close(DataFlowOpCloseContext openContext) {
    }

    public void onInput(int port, Object theEvent) {

        String line;
        if (layout == null) {

            StringWriter writer = new StringWriter();

            writer.write("[");
            writer.write(dataflowName);
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
            String result = layout.replace("%df", dataflowName).replace("%p", Integer.toString(port));
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

    public static enum LogSinkOutputFormat {
        json,
        xml,
        summary
    }

    public interface ConsoleOpRenderer {
        public void render(EventBean eventBean, StringWriter writer);
    }

    public static class ConsoleOpRendererSummary implements ConsoleOpRenderer {
        public void render(EventBean theEvent, StringWriter writer) {
            EventBeanSummarizer.summarize(theEvent, writer);
        }
    }

    public static class ConsoleOpRendererXmlJSon implements ConsoleOpRenderer {
        private final LogSinkOutputFormat format;
        private final EPRuntime runtime;

        private final Map<EventType, JSONEventRenderer> jsonRendererCache = new HashMap<EventType, JSONEventRenderer>();
        private final Map<EventType, XMLEventRenderer> xmlRendererCache = new HashMap<EventType, XMLEventRenderer>();

        public ConsoleOpRendererXmlJSon(LogSinkOutputFormat format, EPRuntime runtime) {
            this.format = format;
            this.runtime = runtime;
        }

        public void render(EventBean theEvent, StringWriter writer) {
            String result;
            if (format == LogSinkOutputFormat.json) {
                JSONEventRenderer renderer = jsonRendererCache.get(theEvent.getEventType());
                if (renderer == null) {
                    renderer = getJsonRenderer(theEvent.getEventType());
                    jsonRendererCache.put(theEvent.getEventType(), renderer);
                }
                result = renderer.render(theEvent.getEventType().getName(), theEvent);
            } else {
                XMLEventRenderer renderer = xmlRendererCache.get(theEvent.getEventType());
                if (renderer == null) {
                    renderer = getXmlRenderer(theEvent.getEventType());
                    xmlRendererCache.put(theEvent.getEventType(), renderer);
                }
                result = renderer.render(theEvent.getEventType().getName(), theEvent);
            }
            writer.append(result);
        }

        protected JSONEventRenderer getJsonRenderer(EventType eventType) {
            return runtime.getEventRenderer().getJSONRenderer(eventType, RenderingOptions.getJsonOptions());
        }

        protected XMLEventRenderer getXmlRenderer(EventType eventType) {
            return runtime.getEventRenderer().getXMLRenderer(eventType, RenderingOptions.getXmlOptions());
        }
    }

    public static class RenderingOptions {
        private static XMLRenderingOptions xmlOptions;
        private static JSONRenderingOptions jsonOptions;

        static {
            xmlOptions = new XMLRenderingOptions();
            xmlOptions.setPreventLooping(true);
            xmlOptions.setRenderer(ConsoleOpEventPropertyRenderer.INSTANCE);

            jsonOptions = new JSONRenderingOptions();
            jsonOptions.setPreventLooping(true);
            jsonOptions.setRenderer(ConsoleOpEventPropertyRenderer.INSTANCE);
        }

        public static XMLRenderingOptions getXmlOptions() {
            return xmlOptions;
        }

        public static void setXmlOptions(XMLRenderingOptions xmlOptions) {
            RenderingOptions.xmlOptions = xmlOptions;
        }

        public static JSONRenderingOptions getJsonOptions() {
            return jsonOptions;
        }

        public static void setJsonOptions(JSONRenderingOptions jsonOptions) {
            RenderingOptions.jsonOptions = jsonOptions;
        }
    }

    public static class ConsoleOpEventPropertyRenderer implements EventPropertyRenderer {
        public final static ConsoleOpEventPropertyRenderer INSTANCE = new ConsoleOpEventPropertyRenderer();

        public void render(EventPropertyRendererContext context) {
            if (context.getPropertyValue() instanceof Object[]) {
                context.getStringBuilder().append(Arrays.toString((Object[]) context.getPropertyValue()));
            } else {
                context.getDefaultRenderer().render(context.getPropertyValue(), context.getStringBuilder());
            }
        }
    }
}
