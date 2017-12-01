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
 * Implements the iterator with table evaluation concern.
 */
public class UnsafeIteratorWTableImpl<E> implements Iterator<E> {
    private final TableExprEvaluatorContext tableExprEvaluatorContext;
    private final Iterator<E> inner;

    public UnsafeIteratorWTableImpl(TableExprEvaluatorContext tableExprEvaluatorContext, Iterator<E> inner) {
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
        this.inner = inner;
    }

    public boolean hasNext() {
        return inner.hasNext();
    }

    public E next() {
        E e = inner.next();
        tableExprEvaluatorContext.releaseAcquiredLocks();
        return e;
    }

    public void remove() {
        inner.remove();
    }
}
