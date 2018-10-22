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
package com.espertech.esper.runtime.client;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;

/**
 * Executes fire-and-forget non-continuous on-demand queries against named windows or tables.
 * <p>
 * Compile queries use the compile-query methods of the compiler.
 * </p>
 */
public interface EPFireAndForgetService {
    /**
     * Execute a fire-and-forget query.
     *
     * @param compiled is the compiled EPL query to execute
     * @return query result
     */
    EPFireAndForgetQueryResult executeQuery(EPCompiled compiled);

    /**
     * Execute a fire-and-forget query for use with named windows and tables that have a context declared and that may therefore have multiple context partitions,
     * allows to target context partitions for query execution selectively.
     *
     * @param compiled  is the compiled EPL query to execute
     * @param selectors selects context partitions to consider
     * @return result
     */
    EPFireAndForgetQueryResult executeQuery(EPCompiled compiled, ContextPartitionSelector[] selectors);

    /**
     * Prepare an unparameterized fire-and-forget query before execution and for repeated execution.
     *
     * @param compiled is the compiled EPL query to prepare
     * @return proxy to execute upon, that also provides the event type of the returned results
     */
    EPFireAndForgetPreparedQuery prepareQuery(EPCompiled compiled);

    /**
     * Prepare a parameterized fire-and-forget query for repeated parameter setting and execution.
     * Set all values on the returned holder then execute using {@link #executeQuery(EPFireAndForgetPreparedQueryParameterized)}.
     *
     * @param compiled is the compiled EPL query to prepare
     * @return parameter holder upon which to set values
     */
    EPFireAndForgetPreparedQueryParameterized prepareQueryWithParameters(EPCompiled compiled);

    /**
     * Execute a fire-and-forget parameterized query.
     *
     * @param parameterizedQuery contains the query and parameter values
     * @return query result
     */
    EPFireAndForgetQueryResult executeQuery(EPFireAndForgetPreparedQueryParameterized parameterizedQuery);

    /**
     * Execute a fire-and-forget parameterized query.
     *
     * @param parameterizedQuery contains the query and parameter values
     * @param selectors          selects context partitions to consider
     * @return query result
     */
    EPFireAndForgetQueryResult executeQuery(EPFireAndForgetPreparedQueryParameterized parameterizedQuery, ContextPartitionSelector[] selectors);
}
