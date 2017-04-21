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
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.factory.StatementAgentInstancePostLoadIndexVisitor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Set;

/**
 * Interface for populating a join tuple result set from new data and old data for each stream.
 */
public interface JoinSetComposer {
    /**
     * Returns true whether initialization events per stream to populate join indexes can be processed (init method).
     *
     * @return whether init is supported
     */
    public boolean allowsInit();

    /**
     * Provides initialization events per stream to composer to populate join indexes, if required
     *
     * @param eventsPerStream is an array of events for each stream, with null elements to indicate no events for a stream
     * @param exprEvaluatorContext evaluator context
     */
    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Return join tuple result set from new data and old data for each stream.
     *
     * @param newDataPerStream     - for each stream the event array (can be null).
     * @param oldDataPerStream     - for each stream the event array (can be null).
     * @param exprEvaluatorContext expression evaluation context
     * @return join tuples
     */
    public UniformPair<Set<MultiKey<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * For use in iteration over join statements, this must build a join tuple result set from
     * all events in indexes, executing query strategies for each.
     *
     * @return static join result
     */
    public Set<MultiKey<EventBean>> staticJoin();

    /**
     * Destroy stateful index tables, if any.
     */
    public void destroy();

    public void visitIndexes(StatementAgentInstancePostLoadIndexVisitor visitor);
}
