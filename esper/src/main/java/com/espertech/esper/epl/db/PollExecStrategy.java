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
package com.espertech.esper.epl.db;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.List;

/**
 * Interface for polling data from a data source such as a relational database.
 * <p>
 * Lifecycle methods are for managing connection resources.
 */
public interface PollExecStrategy {
    /**
     * Start the poll, called before any poll operation.
     */
    public void start();

    /**
     * Poll events using the keys provided.
     *
     * @param lookupValues         is keys for exeuting a query or such
     * @param exprEvaluatorContext context
     * @return a list of events for the keys
     */
    public List<EventBean> poll(Object[] lookupValues, ExprEvaluatorContext exprEvaluatorContext);

    /**
     * Indicate we are done polling and can release resources.
     */
    public void done();

    /**
     * Indicate we are no going to use this object again.
     */
    public void destroy();
}
