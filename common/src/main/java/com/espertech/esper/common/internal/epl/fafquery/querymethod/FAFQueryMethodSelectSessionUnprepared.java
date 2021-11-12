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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodSelectSessionUnprepared implements FAFQuerySessionUnprepared {
    private final FAFQueryMethodSelect select;

    public FAFQueryMethodSelectSessionUnprepared(FAFQueryMethodSelect select) {
        this.select = select;
    }

    public EPPreparedQueryResult execute(AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService) {
        return executeSelect(select, serviceStatusProvider, assignerSetter, contextPartitionSelectors, contextManagementService);
    }

    protected static EPPreparedQueryResult executeSelect(FAFQueryMethodSelect select, AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService) {
        if (!serviceStatusProvider.get()) {
            throw FAFQueryMethodUtil.runtimeDestroyed();
        }
        FireAndForgetProcessor[] processors = select.getProcessors();
        if (processors.length > 0 && contextPartitionSelectors != null && contextPartitionSelectors.length != processors.length) {
            throw new IllegalArgumentException("The number of context partition selectors does not match the number of named windows or tables in the from-clause");
        }

        try {
            return select.getSelectExec().execute(select, contextPartitionSelectors, assignerSetter, contextManagementService);
        } finally {
            if (select.isHasTableAccess()) {
                select.getSelectExec().releaseTableLocks(processors);
            }
        }
    }
}
