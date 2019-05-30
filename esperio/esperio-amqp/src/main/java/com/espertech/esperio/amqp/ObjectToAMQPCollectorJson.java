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
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.internal.util.JavaClassHelper;

public class ObjectToAMQPCollectorJson implements ObjectToAMQPCollector {

    public void collect(ObjectToAMQPCollectorContext context) {
        if (context.getObject() instanceof String) {
            context.getEmitter().send(context.getObject().toString().getBytes());
            return;
        }

        if (!(context.getObject() instanceof JsonEventObject)) {
            throw new EPException("Expected JSON event object (JsonEventObject) or string but received " + JavaClassHelper.getClassNameFullyQualPretty(context.getObject().getClass()));
        }
        JsonEventObject jsonEventObject = (JsonEventObject) context.getObject();
        context.getEmitter().send(jsonEventObject.toString().getBytes());
    }
}
