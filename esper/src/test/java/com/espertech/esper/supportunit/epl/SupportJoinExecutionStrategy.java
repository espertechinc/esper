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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.epl.join.base.JoinExecutionStrategy;

import java.util.Set;

public class SupportJoinExecutionStrategy implements JoinExecutionStrategy {
    private EventBean[][] lastNewDataPerStream;
    private EventBean[][] lastOldDataPerStream;

    public void join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream) {
        lastNewDataPerStream = newDataPerStream;
        lastOldDataPerStream = oldDataPerStream;
    }

    public EventBean[][] getLastNewDataPerStream() {
        return lastNewDataPerStream;
    }

    public EventBean[][] getLastOldDataPerStream() {
        return lastOldDataPerStream;
    }

    public Set<MultiKey<EventBean>> staticJoin() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
