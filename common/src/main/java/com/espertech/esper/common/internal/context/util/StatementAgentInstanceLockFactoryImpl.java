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

import com.espertech.esper.common.client.annotation.NoLock;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;

import java.lang.annotation.Annotation;

/**
 * Provides statement-level locks.
 */
public class StatementAgentInstanceLockFactoryImpl implements StatementAgentInstanceLockFactory {
    private final boolean fairlocks;
    private final boolean disableLocking;

    public StatementAgentInstanceLockFactoryImpl(boolean fairlocks, boolean disableLocking) {
        this.fairlocks = fairlocks;
        this.disableLocking = disableLocking;
    }

    public StatementAgentInstanceLock getStatementLock(String statementName, Annotation[] annotations, boolean stateless, StatementType statementType) {
        if (statementType.isOnTriggerInfra()) {
            throw new UnsupportedOperationException("Operation not available for statement type " + statementType);
        }
        boolean foundNoLock = AnnotationUtil.hasAnnotation(annotations, NoLock.class);
        if (disableLocking || foundNoLock || stateless) {
            return new StatementAgentInstanceLockNoLockImpl(statementName);
        }
        return new StatementAgentInstanceLockRW(fairlocks);
    }
}
