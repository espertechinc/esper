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
package com.espertech.esper.view.window;

import com.espertech.esper.client.EventBean;

import java.util.Collection;
import java.util.Iterator;

/**
 * Random access interface to insert stream and remove stream data based on an index.
 */
public interface RandomAccessByIndex {
    /**
     * Returns an new data event given an index.
     *
     * @param index to return new data for
     * @return new data event
     */
    public EventBean getNewData(int index);

    /**
     * Returns an old data event given an index.
     *
     * @param index to return old data for
     * @return old data event
     */
    public EventBean getOldData(int index);

    public EventBean getNewDataTail(int index);

    public Iterator<EventBean> getWindowIterator();

    public Collection<EventBean> getWindowCollectionReadOnly();

    public int getWindowCount();
}
