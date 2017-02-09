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
package com.espertech.esper.core.service;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.metric.StatementMetricHandle;

/**
 * Interface for a statement-level service for coordinating the insert/remove stream generation,
 * native deliver to subscribers and the presence/absence of listener or subscribers to a statement.
 */
public interface StatementResultService {
    /**
     * For initialization of the service to provide statement context.
     *
     * @param epStatement           the statement
     * @param epServiceProvider     the engine instance
     * @param isInsertInto          true if this is insert into
     * @param isPattern             true if this is a pattern statement
     * @param isDistinct            true if using distinct
     * @param statementMetricHandle handle for metrics reporting
     * @param isForClause           indicator that for-clause exists
     */
    public void setContext(EPStatementSPI epStatement, EPServiceProviderSPI epServiceProvider,
                           boolean isInsertInto, boolean isPattern, boolean isDistinct, boolean isForClause, StatementMetricHandle statementMetricHandle);

    /**
     * For initialize of the service providing select clause column types and names.
     *
     * @param selectClauseTypes        types of columns in the select clause
     * @param selectClauseColumnNames  column names
     * @param forClauseDelivery        for-clause
     * @param groupDeliveryExpressions grouped-delivery
     * @param exprEvaluatorContext     context
     */
    public void setSelectClause(Class[] selectClauseTypes, String[] selectClauseColumnNames,
                                boolean forClauseDelivery, ExprEvaluator[] groupDeliveryExpressions, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Returns true to indicate that synthetic events should be produced, for
     * use in select expression processing.
     *
     * @return true to produce synthetic events
     */
    public boolean isMakeSynthetic();

    /**
     * Returns true to indicate that natural events should be produced, for
     * use in select expression processing.
     *
     * @return true to produce natural (object[] column) events
     */
    public boolean isMakeNatural();

    /**
     * Dispatch the remaining results, if any, to listeners as the statement is about to be stopped.
     */
    public void dispatchOnStop();

    /**
     * Indicate a change in update listener.
     *
     * @param updateListeners is the new listeners and subscriber
     * @param isRecovery      indicator whether recovering
     */
    public void setUpdateListeners(EPStatementListenerSet updateListeners, boolean isRecovery);

    /**
     * Stores for dispatching the statement results.
     *
     * @param results is the insert and remove stream data
     */
    public void indicate(UniformPair<EventBean[]> results);

    /**
     * Execution of result indication.
     */
    public void execute();

    public String getStatementName();

    public int getStatementId();

    public EPStatementListenerSet getStatementListenerSet();
}
