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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;

public class AMQPToObjectCollectorJson implements AMQPToObjectCollector {
    private JsonEventType jsonEventType;

    public void collect(AMQPToObjectCollectorContext context) {
        if (jsonEventType == null) {
            init(context.getOutputEventType());
        }
        Object underlying = jsonEventType.parse(new String(context.getBytes()));
        context.getEmitter().submit(underlying);
    }

    private void init(EventType outputEventType) {
        if (!(outputEventType instanceof JsonEventType)) {
            throw new EPException("Expected a JSON event type but received " + (outputEventType == null ? "undefined type" : outputEventType.getName()));
        }
        jsonEventType = (JsonEventType) outputEventType;
    }
}
