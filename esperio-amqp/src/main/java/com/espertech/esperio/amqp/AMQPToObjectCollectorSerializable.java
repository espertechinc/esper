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

import com.espertech.esper.util.SerializerUtil;

public class AMQPToObjectCollectorSerializable implements AMQPToObjectCollector {
    public void collect(AMQPToObjectCollectorContext context) {
        context.getEmitter().submit(SerializerUtil.byteArrToObject(context.getBytes()));
    }
}
