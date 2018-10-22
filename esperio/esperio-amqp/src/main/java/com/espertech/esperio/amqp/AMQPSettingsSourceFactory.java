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
package com.espertech.esperio.amqp;

import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Map;

public class AMQPSettingsSourceFactory extends AMQPSettingsFactoryBase {
    private Map<String, Object> collector;
    private ExprEvaluator prefetchCount;
    private ExprEvaluator consumeAutoAck;

    public AMQPSettingsSourceFactory() {
    }

    public AMQPSettingsSourceValues evaluate(DataFlowOpInitializeContext context) {
        AMQPSettingsSourceValues values = new AMQPSettingsSourceValues();
        super.evaluateAndSet(values, context);
        values.setCollector(DataFlowParameterResolution.resolveOptionalInstance("collector", collector, AMQPToObjectCollector.class, context));
        values.setPrefetchCount(DataFlowParameterResolution.resolveWithDefault("prefetchCount", prefetchCount, 100, int.class, context));
        values.setConsumeAutoAck(DataFlowParameterResolution.resolveWithDefault("consumeAutoAck", consumeAutoAck, true, boolean.class, context));
        return values;
    }

    public Map<String, Object> getCollector() {
        return collector;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public ExprEvaluator getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(ExprEvaluator prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    public ExprEvaluator getConsumeAutoAck() {
        return consumeAutoAck;
    }

    public void setConsumeAutoAck(ExprEvaluator consumeAutoAck) {
        this.consumeAutoAck = consumeAutoAck;
    }
}
