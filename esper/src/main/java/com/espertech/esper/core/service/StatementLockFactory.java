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

import java.lang.annotation.Annotation;

/**
 * Factory for the managed lock that provides statement resource protection.
 */
public interface StatementLockFactory {
    /**
     * Create lock for statement
     *
     * @param statementName is the statement name
     * @param annotations   annotation
     * @param stateless     indicator whether stateless
     * @return lock
     */
    public StatementAgentInstanceLock getStatementLock(String statementName, Annotation[] annotations, boolean stateless);
}
