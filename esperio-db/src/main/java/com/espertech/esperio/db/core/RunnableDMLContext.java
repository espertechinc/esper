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
package com.espertech.esperio.db.core;

import com.espertech.esper.epl.db.DatabaseConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnableDMLContext {
    private final static Logger log = LoggerFactory.getLogger(RunnableDMLContext.class);

    private final String name;
    private final DatabaseConnectionFactory connectionFactory;
    private final DMLStatement dmlStatement;
    private final Integer retry;
    private final Double retryWait;

    public RunnableDMLContext(String name, DatabaseConnectionFactory connectionFactory, DMLStatement dmlStatement, Integer retry, Double retryWait) {
        this.name = name;
        this.connectionFactory = connectionFactory;
        this.dmlStatement = dmlStatement;
        this.retry = retry;
        this.retryWait = retryWait;
    }

    public String getName() {
        return name;
    }

    public DatabaseConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public DMLStatement getDmlStatement() {
        return dmlStatement;
    }

    public Integer getRetry() {
        return retry;
    }

    public Double getRetryWait() {
        return retryWait;
    }
}
