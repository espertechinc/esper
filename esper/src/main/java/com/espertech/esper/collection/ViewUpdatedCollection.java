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
package com.espertech.esper.collection;

import com.espertech.esper.client.EventBean;

/**
 * A general-purpose collection interface for collections updated by view data.
 * <p>
 * Views post delta-data in terms of new data (insert stream) events and old data (remove stream) event that
 * leave a window.
 */
public interface ViewUpdatedCollection {
    /**
     * Accepts view insert and remove stream.
     *
     * @param newData is the insert stream events or null if no data
     * @param oldData is the remove stream events or null if no data
     */
    public void update(EventBean[] newData, EventBean[] oldData);

    /**
     * De-allocate resources held by the collection.
     */
    public void destroy();

    public int getNumEventsInsertBuf();
}
