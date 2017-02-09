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

/**
 * Interface for statement result callbacks.
 */
public interface StatementResultListener {
    /**
     * Provide statement result.
     *
     * @param newEvents         insert stream
     * @param oldEvents         remove stream
     * @param statementName     stmt name
     * @param statement         stmt
     * @param epServiceProvider engine
     */
    public void update(EventBean[] newEvents, EventBean[] oldEvents, String statementName, EPStatementSPI statement, EPServiceProviderSPI epServiceProvider);
}
