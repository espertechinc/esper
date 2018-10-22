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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Arrays;
import java.util.Locale;

public class LogSinkFactory implements DataFlowOperatorFactory {

    private ExprEvaluator title;
    private ExprEvaluator layout;
    private ExprEvaluator format;
    private ExprEvaluator log;
    private ExprEvaluator linefeed;
    private EventType[] eventTypes;

    private String dataflowName;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        dataflowName = context.getDataFlowName();
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        String titleText = DataFlowParameterResolution.resolveStringOptional("title", title, context);
        String layoutText = DataFlowParameterResolution.resolveStringOptional("layout", layout, context);
        boolean logFlag = DataFlowParameterResolution.resolveWithDefault("log", log, true, Boolean.class, context);
        boolean linefeedFlag = DataFlowParameterResolution.resolveWithDefault("linefeed", linefeed, true, Boolean.class, context);

        ConsoleOpRenderer renderer;
        String formatText = DataFlowParameterResolution.resolveStringOptional("format", format, context);
        if (formatText == null) {
            renderer = new ConsoleOpRendererSummary();
        } else {
            LogSinkOutputFormat formatEnum = LogSinkOutputFormat.valueOf(formatText.trim().toLowerCase(Locale.ENGLISH));
            if (formatEnum == LogSinkOutputFormat.summary) {
                renderer = new ConsoleOpRendererSummary();
            } else if (formatEnum == LogSinkOutputFormat.json || formatEnum == LogSinkOutputFormat.xml) {
                renderer = new ConsoleOpRendererXmlJSon(formatEnum, context.getAgentInstanceContext().getEPRuntimeRenderEvent());
            } else {
                throw new EPException("Format '" + formatText + "' is not supported, expecting any of " + Arrays.toString(LogSinkOutputFormat.values()));
            }
        }

        return new LogSinkOp(this, context.getDataFlowInstanceId(), renderer, titleText, layoutText, logFlag, linefeedFlag);
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(EventType[] eventTypes) {
        this.eventTypes = eventTypes;
    }

    public String getDataflowName() {
        return dataflowName;
    }

    public void setTitle(ExprEvaluator title) {
        this.title = title;
    }

    public void setLayout(ExprEvaluator layout) {
        this.layout = layout;
    }

    public void setFormat(ExprEvaluator format) {
        this.format = format;
    }

    public void setLog(ExprEvaluator log) {
        this.log = log;
    }

    public void setLinefeed(ExprEvaluator linefeed) {
        this.linefeed = linefeed;
    }
}
