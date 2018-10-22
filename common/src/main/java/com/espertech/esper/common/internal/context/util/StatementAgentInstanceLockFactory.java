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
package com.espertech.esper.common.internal.context.util;

import com.espertech.esper.common.client.util.StatementType;

import java.lang.annotation.Annotation;

/**
 * Factory for the managed lock that provides statement resource protection.
 */
public interface StatementAgentInstanceLockFactory {
    /**
     * Create lock for statement
     *
     * @param statementName is the statement name
     * @param annotations   annotation
     * @param stateless     indicator whether stateless
     * @param statementType statement type
     * @return lock
     */
    public StatementAgentInstanceLock getStatementLock(String statementName, Annotation[] annotations, boolean stateless, StatementType statementType);
}
