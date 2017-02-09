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
package com.espertech.esper.client.hook;

import com.espertech.esper.client.EventBean;

/**
 * For use with virtual data windows, handles any insert stream and remove stream events that a
 * virtual data window may post to consuming statements.
 */
public interface VirtualDataWindowOutStream {

    /**
     * Post insert stream (new data) and remove stream (old data) events.
     *
     * @param newData insert stream, or null if no insert stream events
     * @param oldData remove stream, or null if no remove stream events
     */
    public void update(EventBean[] newData, EventBean[] oldData);
}
