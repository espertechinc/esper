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

public class AMQPSourceFactory implements DataFlowOperatorFactory {

    private AMQPSettingsSourceFactory settings;
    private EventType outputEventType;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        AMQPSettingsSourceValues settingsValues = settings.evaluate(context);
        return new AMQPSource(settingsValues, outputEventType);
    }

    public void setSettings(AMQPSettingsSourceFactory settings) {
        this.settings = settings;
    }

    public void setOutputEventType(EventType outputEventType) {
        this.outputEventType = outputEventType;
    }
}
