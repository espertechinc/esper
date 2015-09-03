/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;

public class ResultSetProcessorAggregateAllOutputLastHelper
{
    private EventBean lastEventIStreamForOutputLast;
    private EventBean lastEventRStreamForOutputLast;

    public void apply(UniformPair<EventBean[]> pair) {
        if (pair == null) {
            return;
        }
        if (pair.getFirst() != null && pair.getFirst().length > 0) {
            lastEventIStreamForOutputLast = pair.getFirst()[pair.getFirst().length - 1];
        }
        if (pair.getSecond() != null && pair.getSecond().length > 0) {
            lastEventRStreamForOutputLast = pair.getSecond()[pair.getSecond().length - 1];
        }
    }

    public UniformPair<EventBean[]> renderAndReset() {
        UniformPair<EventBean[]> newOldEvents = null;
        if (lastEventIStreamForOutputLast != null) {
            EventBean[] istream = new EventBean[] {lastEventIStreamForOutputLast};
            newOldEvents = new UniformPair<EventBean[]>(istream, null);
        }
        if (lastEventRStreamForOutputLast != null) {
            EventBean[] rstream = new EventBean[] {lastEventRStreamForOutputLast};
            if (newOldEvents == null) {
                newOldEvents = new UniformPair<EventBean[]>(null, rstream);
            }
            else {
                newOldEvents.setSecond(rstream);
            }
        }

        lastEventIStreamForOutputLast = null;
        lastEventRStreamForOutputLast = null;
        return newOldEvents;
    }
}
