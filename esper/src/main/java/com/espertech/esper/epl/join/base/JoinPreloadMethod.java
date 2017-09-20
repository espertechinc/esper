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

import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.view.internal.BufferView;

/**
 * Method for preloading events for a given stream onto the stream's indexes, from a buffer already associated with a stream.
 */
public interface JoinPreloadMethod {
    /**
     * Initialize a stream from the stream buffers data.
     *
     * @param stream to initialize and load indexes
     * @param exprEvaluatorContext evaluator context
     */
    public void preloadFromBuffer(int stream, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Initialize the result set process for the purpose of grouping and aggregation
     * from the join result set.
     *
     * @param resultSetProcessor is the grouping and aggregation result processing
     */
    public void preloadAggregation(ResultSetProcessor resultSetProcessor);

    /**
     * Sets the buffee to use.
     *
     * @param buffer buffer to use
     * @param i      stream
     */
    public void setBuffer(BufferView buffer, int i);

    public boolean isPreloading();
}
