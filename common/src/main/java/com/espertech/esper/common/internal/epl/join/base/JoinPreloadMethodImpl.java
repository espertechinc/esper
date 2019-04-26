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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.view.util.BufferView;

import java.util.HashSet;
import java.util.Set;

/**
 * Implements a method for pre-loading (initializing) join indexes from a filled buffer.
 */
public class JoinPreloadMethodImpl implements JoinPreloadMethod {
    private final int numStreams;
    private final BufferView[] bufferViews;
    private final JoinSetComposer joinSetComposer;

    /**
     * Ctor.
     *
     * @param numStreams      number of streams
     * @param joinSetComposer the composer holding stream indexes
     */
    public JoinPreloadMethodImpl(int numStreams, JoinSetComposer joinSetComposer) {
        this.numStreams = numStreams;
        this.bufferViews = new BufferView[numStreams];
        this.joinSetComposer = joinSetComposer;
    }

    /**
     * Sets the buffer for a stream to preload events from.
     *
     * @param view   buffer
     * @param stream the stream number for the buffer
     */
    public void setBuffer(BufferView view, int stream) {
        bufferViews[stream] = view;
    }

    public void preloadFromBuffer(int stream, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] preloadEvents = bufferViews[stream].getNewDataBuffer().getAndFlush();
        EventBean[][] eventsPerStream = new EventBean[numStreams][];
        eventsPerStream[stream] = preloadEvents;
        joinSetComposer.init(eventsPerStream, exprEvaluatorContext);
    }

    public void preloadAggregation(ResultSetProcessor resultSetProcessor) {
        Set<MultiKeyArrayOfKeys<EventBean>> newEvents = joinSetComposer.staticJoin();
        Set<MultiKeyArrayOfKeys<EventBean>> oldEvents = new HashSet<MultiKeyArrayOfKeys<EventBean>>();
        resultSetProcessor.processJoinResult(newEvents, oldEvents, false);
    }

    @Override
    public boolean isPreloading() {
        return true;
    }
}
