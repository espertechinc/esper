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
package com.espertech.esper.client.dataflow.io;

import java.io.DataOutput;

/**
 * Context for use with {@link ObjectToDataOutputCollector} carries object and data output.
 */
public class ObjectToDataOutputCollectorContext {
    private DataOutput dataOutput;
    private Object event;

    /**
     * Returns the data output
     *
     * @return data output
     */
    public DataOutput getDataOutput() {
        return dataOutput;
    }

    /**
     * Sets the data output
     *
     * @param dataOutput data output
     */
    public void setDataOutput(DataOutput dataOutput) {
        this.dataOutput = dataOutput;
    }

    /**
     * Returns the event object.
     *
     * @return event object
     */
    public Object getEvent() {
        return event;
    }

    /**
     * Sets the event object.
     *
     * @param event event object
     */
    public void setEvent(Object event) {
        this.event = event;
    }
}
