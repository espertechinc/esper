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

import com.espertech.esper.epl.table.mgmt.TableExprEvaluatorContext;

import java.util.Iterator;

/**
 * Implements the safe iterator. The class is passed a lock that is locked already, to release
 * when the close method closes the iterator.
 */
public class SafeIteratorWTableImpl<E> extends SafeIteratorImpl<E> {
    private final TableExprEvaluatorContext tableExprEvaluatorContext;

    public SafeIteratorWTableImpl(StatementAgentInstanceLock iteratorLock, Iterator<E> underlying, TableExprEvaluatorContext tableExprEvaluatorContext) {
        super(iteratorLock, underlying);
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
    }

    @Override
    public void close() {
        super.close();
        tableExprEvaluatorContext.releaseAcquiredLocks();
    }
}
