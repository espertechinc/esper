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
package com.espertech.esper.view.internal;

import com.espertech.esper.event.FlushedEventBuffer;

/**
 * Observer interface to a stream publishing new and old events.
 */
public interface BufferObserver {
    /**
     * Receive new and old events from a stream.
     *
     * @param streamId       - the stream number sending the events
     * @param newEventBuffer - buffer for new events
     * @param oldEventBuffer - buffer for old events
     */
    public void newData(int streamId, FlushedEventBuffer newEventBuffer, FlushedEventBuffer oldEventBuffer);
}
