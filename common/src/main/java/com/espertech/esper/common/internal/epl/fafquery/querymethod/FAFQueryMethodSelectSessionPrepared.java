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

import java.util.concurrent.atomic.AtomicBoolean;

import static com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodSelectSessionUnprepared.executeSelect;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodSelectSessionPrepared implements FAFQueryMethodSessionPrepared {

    private final FAFQueryMethodSelect select;

    public FAFQueryMethodSelectSessionPrepared(FAFQueryMethodSelect select) {
        this.select = select;
    }

    public EPPreparedQueryResult execute(AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService) {
        return executeSelect(select, serviceStatusProvider, assignerSetter, contextPartitionSelectors, contextManagementService);
    }

    public void close() {
        select.getSelectExec().close();
    }
}
