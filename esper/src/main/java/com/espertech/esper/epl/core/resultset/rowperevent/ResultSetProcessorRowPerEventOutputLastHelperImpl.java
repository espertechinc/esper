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
package com.espertech.esper.epl.core.resultset.rowperevent;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;

import java.util.Set;

public class ResultSetProcessorRowPerEventOutputLastHelperImpl implements ResultSetProcessorRowPerEventOutputLastHelper {
    private final ResultSetProcessorRowPerEvent processor;

    private EventBean lastEventIStreamForOutputLast;
    private EventBean lastEventRStreamForOutputLast;

    public ResultSetProcessorRowPerEventOutputLastHelperImpl(ResultSetProcessorRowPerEvent processor) {
        this.processor = processor;
    }

    public void processView(EventBean[] newData, EventBean[] oldData, boolean isGenerateSynthetic) {
        UniformPair<EventBean[]> pair = processor.processViewResult(newData, oldData, isGenerateSynthetic);
        apply(pair);
    }

    public void processJoin(Set<MultiKey<EventBean>> newEvents, Set<MultiKey<EventBean>> oldEvents, boolean isGenerateSynthetic) {
        UniformPair<EventBean[]> pair = processor.processJoinResult(newEvents, oldEvents, isGenerateSynthetic);
        apply(pair);
    }

    public UniformPair<EventBean[]> output() {
        UniformPair<EventBean[]> newOldEvents = null;
        if (lastEventIStreamForOutputLast != null) {
            EventBean[] istream = new EventBean[]{lastEventIStreamForOutputLast};
            newOldEvents = new UniformPair<>(istream, null);
        }
        if (lastEventRStreamForOutputLast != null) {
            EventBean[] rstream = new EventBean[]{lastEventRStreamForOutputLast};
            if (newOldEvents == null) {
                newOldEvents = new UniformPair<>(null, rstream);
            } else {
                newOldEvents.setSecond(rstream);
            }
        }

        lastEventIStreamForOutputLast = null;
        lastEventRStreamForOutputLast = null;
        return newOldEvents;
    }

    public void destroy() {
        // no action required
    }

    private void apply(UniformPair<EventBean[]> pair) {
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
}
