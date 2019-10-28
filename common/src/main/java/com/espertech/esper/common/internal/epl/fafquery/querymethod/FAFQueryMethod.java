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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.internal.context.mgr.ContextManagementService;
import com.espertech.esper.common.internal.context.util.StatementContextRuntimeServices;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FAF query execute.
 */
public interface FAFQueryMethod {
    void ready(StatementContextRuntimeServices services);

    EPPreparedQueryResult execute(AtomicBoolean serviceStatusProvider, FAFQueryMethodAssignerSetter assignerSetter, ContextPartitionSelector[] contextPartitionSelectors, ContextManagementService contextManagementService);

    EventType getEventType();
}
