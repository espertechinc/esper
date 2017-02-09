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

public class AMQPSettingsSource extends AMQPSettingsBase {
    private AMQPToObjectCollector collector;
    private int prefetchCount = 100;
    private boolean consumeAutoAck = true;

    public AMQPSettingsSource() {
    }

    public AMQPToObjectCollector getCollector() {
        return collector;
    }

    public void setCollector(AMQPToObjectCollector collector) {
        this.collector = collector;
    }

    public int getPrefetchCount() {
        return prefetchCount;
    }

    public void setPrefetchCount(int prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    public boolean isConsumeAutoAck() {
        return consumeAutoAck;
    }

    public void setConsumeAutoAck(boolean consumeAutoAck) {
        this.consumeAutoAck = consumeAutoAck;
    }

    public String toString() {
        return super.toString() + "  AMQPSettingsSource{" +
                "amqpToObjectTransform=" + collector +
                ", prefetchCount=" + prefetchCount +
                ", consumeAutoAck=" + consumeAutoAck +
                '}';
    }
}
