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

import com.espertech.esper.client.annotation.NoLock;
import com.espertech.esper.epl.annotation.AnnotationUtil;

import java.lang.annotation.Annotation;

/**
 * Provides statement-level locks.
 */
public class StatementLockFactoryImpl implements StatementLockFactory {
    private final boolean fairlocks;
    private final boolean disableLocking;

    public StatementLockFactoryImpl(boolean fairlocks, boolean disableLocking) {
        this.fairlocks = fairlocks;
        this.disableLocking = disableLocking;
    }

    public StatementAgentInstanceLock getStatementLock(String statementName, Annotation[] annotations, boolean stateless) {
        boolean foundNoLock = AnnotationUtil.findAnnotation(annotations, NoLock.class) != null;
        if (disableLocking || foundNoLock || stateless) {
            return new StatementNoLockImpl(statementName);
        }
        return new StatementAgentInstanceRWLockImpl(fairlocks);
    }
}
