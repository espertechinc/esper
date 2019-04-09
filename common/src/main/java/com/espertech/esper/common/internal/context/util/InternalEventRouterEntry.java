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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanWriter;
import com.espertech.esper.common.internal.util.TypeWidener;

/**
 * Pre-Processing entry for routing an event internally.
 */
public class InternalEventRouterEntry {
    private final int priority;
    private final boolean isDrop;
    private final ExprEvaluator optionalWhereClause;
    private final ExprEvaluator[] assignments;
    private final EventBeanWriter writer;
    private final TypeWidener[] wideners;
    private final InternalRoutePreprocessView outputView;
    private final StatementContext statementContext;
    private final boolean hasSubselect;

    /**
     * Ctor.
     *
     * @param priority            priority of statement
     * @param drop                whether to drop the event if matched
     * @param optionalWhereClause where clause, or null if none provided
     * @param assignments         event property assignments
     * @param writer              writes values to an event
     * @param wideners            for widening types to write
     * @param outputView          for indicating output
     * @param statementContext    statement context
     * @param hasSubselect        indicator whether there are subselects
     */
    public InternalEventRouterEntry(int priority, boolean drop, ExprEvaluator optionalWhereClause, ExprEvaluator[] assignments, EventBeanWriter writer, TypeWidener[] wideners, InternalRoutePreprocessView outputView, StatementContext statementContext, boolean hasSubselect) {
        this.priority = priority;
        this.isDrop = drop;
        this.optionalWhereClause = optionalWhereClause;
        this.assignments = assignments;
        this.writer = writer;
        this.wideners = wideners;
        this.outputView = outputView;
        this.statementContext = statementContext;
        this.hasSubselect = hasSubselect;
    }

    /**
     * Returns the execution priority.
     *
     * @return priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns indicator whether dropping events if the where-clause matches.
     *
     * @return drop events
     */
    public boolean isDrop() {
        return isDrop;
    }

    /**
     * Returns the where-clause or null if none defined
     *
     * @return where-clause
     */
    public ExprEvaluator getOptionalWhereClause() {
        return optionalWhereClause;
    }

    /**
     * Returns the expressions providing values for assignment.
     *
     * @return assignment expressions
     */
    public ExprEvaluator[] getAssignments() {
        return assignments;
    }

    /**
     * Returns the writer to the event for writing property values.
     *
     * @return writer
     */
    public EventBeanWriter getWriter() {
        return writer;
    }

    /**
     * Returns the type wideners to use or null if none required.
     *
     * @return wideners.
     */
    public TypeWidener[] getWideners() {
        return wideners;
    }

    /**
     * Returns the output view.
     *
     * @return output view
     */
    public InternalRoutePreprocessView getOutputView() {
        return outputView;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public boolean isHasSubselect() {
        return hasSubselect;
    }
}
