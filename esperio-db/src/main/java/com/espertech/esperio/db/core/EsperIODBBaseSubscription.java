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

import com.espertech.esper.adapter.BaseSubscription;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.filter.FilterHandleCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Executor;

public class EsperIODBBaseSubscription extends BaseSubscription {
    private final static Logger log = LoggerFactory.getLogger(EsperIODBBaseSubscription.class);

    private final RunnableFactory runnableFactory;
    private final Executor executor;

    public EsperIODBBaseSubscription(RunnableFactory runnableFactory, Executor executor) {
        this.runnableFactory = runnableFactory;
        this.executor = executor;
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        try {
            Runnable runnable = runnableFactory.makeRunnable(theEvent);
            executor.execute(runnable);
        } catch (Throwable t) {
            log.error("Error executing database action:" + t.getMessage(), t);
        }
    }

    public boolean isSubSelect() {
        return false;
    }

    public int getStatementId() {
        return -1;
    }
}
