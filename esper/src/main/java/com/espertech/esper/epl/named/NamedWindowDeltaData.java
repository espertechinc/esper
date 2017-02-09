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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;

/**
 * A holder for events posted by a named window as an insert and remove stream.
 */
public class NamedWindowDeltaData {
    private final EventBean[] newData;
    private final EventBean[] oldData;

    /**
     * Ctor.
     *
     * @param newData is the insert stream events, or null if none
     * @param oldData is the remove stream events, or null if none
     */
    public NamedWindowDeltaData(EventBean[] newData, EventBean[] oldData) {
        this.newData = newData;
        this.oldData = oldData;
    }

    /**
     * Ctor aggregates two deltas into a single delta.
     *
     * @param deltaOne is the insert and remove stream events of a first result
     * @param deltaTwo is the insert and remove stream events of a second result
     */
    public NamedWindowDeltaData(NamedWindowDeltaData deltaOne, NamedWindowDeltaData deltaTwo) {
        this.newData = aggregate(deltaOne.getNewData(), deltaTwo.getNewData());
        this.oldData = aggregate(deltaOne.getOldData(), deltaTwo.getOldData());
    }

    /**
     * Returns the insert stream events.
     *
     * @return insert stream
     */
    public EventBean[] getNewData() {
        return newData;
    }

    /**
     * Returns the remove stream events.
     *
     * @return remove stream
     */
    public EventBean[] getOldData() {
        return oldData;
    }

    private static EventBean[] aggregate(EventBean[] arrOne, EventBean[] arrTwo) {
        if (arrOne == null) {
            return arrTwo;
        }
        if (arrTwo == null) {
            return arrOne;
        }
        EventBean[] arr = new EventBean[arrOne.length + arrTwo.length];
        System.arraycopy(arrOne, 0, arr, 0, arrOne.length);
        System.arraycopy(arrTwo, 0, arr, arrOne.length, arrTwo.length);
        return arr;
    }
}
