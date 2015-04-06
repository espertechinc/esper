/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.core.service;

import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.core.start.EPPreparedExecuteMethod;
import com.espertech.esper.core.start.EPPreparedExecuteMethodQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides prepared query functionality.
 */
public class EPPreparedQueryImpl implements EPOnDemandPreparedQuerySPI
{
    private static final Log log = LogFactory.getLog(EPPreparedQueryImpl.class);

    private final EPPreparedExecuteMethod executeMethod;
    private final String epl;

    /**
     * Ctor.
     * @param executeMethod used at execution time to obtain query results
     * @param epl is the EPL to execute
     */
    public EPPreparedQueryImpl(EPPreparedExecuteMethod executeMethod, String epl)
    {
        this.executeMethod = executeMethod;
        this.epl = epl;
    }

    public EPOnDemandQueryResult execute()
    {
        return executeInternal(null);
    }

    public EPOnDemandQueryResult execute(ContextPartitionSelector[] contextPartitionSelectors) {
        if (contextPartitionSelectors == null) {
            throw new IllegalArgumentException("No context partition selectors provided");
        }
        return executeInternal(contextPartitionSelectors);
    }

    private EPOnDemandQueryResult executeInternal(ContextPartitionSelector[] contextPartitionSelectors) {
        try
        {
            EPPreparedQueryResult result = executeMethod.execute(contextPartitionSelectors);
            return new EPQueryResultImpl(result);
        }
        catch (EPStatementException ex)
        {
            throw ex;
        }
        catch (Throwable t)
        {
            String message = "Error executing statement: " + t.getMessage();
            log.error("Error executing on-demand statement '" + epl + "': " + t.getMessage(), t);
            throw new EPStatementException(message, epl);
        }
    }

    public EPPreparedExecuteMethod getExecuteMethod() {
        return executeMethod;
    }

    public EventType getEventType()
    {
        return executeMethod.getEventType();
    }
}
