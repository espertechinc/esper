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

import java.util.Map;

public class AMQPSettingsSinkFactory extends AMQPSettingsFactoryBase {
    private Map<String, Object> collector;

    public AMQPSettingsSinkFactory() {
    }

    public AMQPSettingsSinkValues evaluate(DataFlowOpInitializeContext context) {
        AMQPSettingsSinkValues values = new AMQPSettingsSinkValues();
        super.evaluateAndSet(values, context);
        values.setCollector(DataFlowParameterResolution.resolveOptionalInstance("collector", collector, ObjectToAMQPCollector.class, context));
        return values;
    }

    public Map<String, Object> getCollector() {
        return collector;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public String toString() {
        return super.toString() + "  AMQPSettingsSink{" +
            "objectToAmqpTransform=" + collector +
            '}';
    }
}
