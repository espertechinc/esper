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

public class AMQPSettingsSink extends AMQPSettingsBase {
    private ObjectToAMQPCollector collector;

    public AMQPSettingsSink() {
    }

    public ObjectToAMQPCollector getCollector() {
        return collector;
    }

    public void setCollector(ObjectToAMQPCollector collector) {
        this.collector = collector;
    }

    public String toString() {
        return super.toString() + "  AMQPSettingsSink{" +
                "objectToAmqpTransform=" + collector +
                '}';
    }
}
