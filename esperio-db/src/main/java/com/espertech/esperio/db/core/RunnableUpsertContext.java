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

import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.db.DatabaseConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunnableUpsertContext {
    private final static Logger log = LoggerFactory.getLogger(RunnableUpsertContext.class);

    private final String name;
    private final DatabaseConnectionFactory connectionFactory;
    private final MultiKeyMultiValueTable table;
    private final EventPropertyGetter[] keyGetters;
    private final EventPropertyGetter[] valueGetters;
    private final Integer retry;
    private final Double retryWait;

    public RunnableUpsertContext(String name, DatabaseConnectionFactory connectionFactory, MultiKeyMultiValueTable table, EventPropertyGetter[] keyGetters, EventPropertyGetter[] valueGetters, Integer retry, Double retryWait) {
        this.name = name;
        this.connectionFactory = connectionFactory;
        this.table = table;
        this.keyGetters = keyGetters;
        this.valueGetters = valueGetters;
        this.retry = retry;
        this.retryWait = retryWait;
    }

    public String getName() {
        return name;
    }

    public DatabaseConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public MultiKeyMultiValueTable getTable() {
        return table;
    }

    public EventPropertyGetter[] getKeyGetters() {
        return keyGetters;
    }

    public EventPropertyGetter[] getValueGetters() {
        return valueGetters;
    }

    public Integer getRetry() {
        return retry;
    }

    public Double getRetryWait() {
        return retryWait;
    }
}
