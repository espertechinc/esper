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
package com.espertech.esper.epl.join.base;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;

import java.util.Set;

/**
 * Strategy for executing a join.
 */
public interface JoinExecutionStrategy {
    /**
     * Execute join. The first dimension in the 2-dim arrays is the stream that generated the events,
     * and the second dimension is the actual events generated.
     *
     * @param newDataPerStream - new events for each stream
     * @param oldDataPerStream - old events for each stream
     */
    public void join(EventBean[][] newDataPerStream,
                     EventBean[][] oldDataPerStream);

    /**
     * A static join is for use with iterating over join statements.
     *
     * @return set of rows, each row with two or more events, one for each stream
     */
    public Set<MultiKey<EventBean>> staticJoin();
}