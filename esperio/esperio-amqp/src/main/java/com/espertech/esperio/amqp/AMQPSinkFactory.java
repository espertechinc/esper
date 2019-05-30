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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQPSinkFactory implements DataFlowOperatorFactory {
    private static final Logger log = LoggerFactory.getLogger(AMQPSinkFactory.class);

    private AMQPSettingsSinkFactory settings;
    private EventType eventType;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        AMQPSettingsSinkValues settingsValues = settings.evaluate(context);
        return new AMQPSink(settingsValues, eventType, context.getAgentInstanceContext());
    }

    public AMQPSettingsSinkFactory getSettings() {
        return settings;
    }

    public void setSettings(AMQPSettingsSinkFactory settings) {
        this.settings = settings;
    }

    public static Logger getLog() {
        return log;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
