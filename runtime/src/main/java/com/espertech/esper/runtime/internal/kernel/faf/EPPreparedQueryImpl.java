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
package com.espertech.esper.runtime.internal.kernel.faf;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.context.ContextPartitionSelector;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQuery;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.EPPreparedQueryResult;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethod;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodAssignerSetter;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodProvider;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides prepared query functionality.
 */
public class EPPreparedQueryImpl implements EPFireAndForgetPreparedQuery {
    private final AtomicBoolean serviceStatusProvider;
    private final FAFQueryMethodProvider queryMethodProvider;
    private final FAFQueryMethod queryMethod;
    private final EPServicesContext epServicesContext;

    public EPPreparedQueryImpl(AtomicBoolean serviceStatusProvider, FAFQueryMethodProvider queryMethodProvider, FAFQueryMethod queryMethod, EPServicesContext epServicesContext) {
        this.serviceStatusProvider = serviceStatusProvider;
        this.queryMethodProvider = queryMethodProvider;
        this.queryMethod = queryMethod;
        this.epServicesContext = epServicesContext;
    }

    public EPFireAndForgetQueryResult execute() {
        return executeInternal(null);
    }

    public EPFireAndForgetQueryResult execute(ContextPartitionSelector[] contextPartitionSelectors) {
        if (contextPartitionSelectors == null) {
            throw new IllegalArgumentException("No context partition selectors provided");
        }
        return executeInternal(contextPartitionSelectors);
    }

    private EPFireAndForgetQueryResult executeInternal(ContextPartitionSelector[] contextPartitionSelectors) {
        try {
            FAFQueryMethodAssignerSetter setter = queryMethodProvider.getSubstitutionFieldSetter();
            EPPreparedQueryResult result = queryMethod.execute(serviceStatusProvider, setter, contextPartitionSelectors, epServicesContext.getContextManagementService());
            return new EPQueryResultImpl(result);
        } catch (Throwable t) {
            throw new EPException(t.getMessage(), t);
        }
    }

    public EventType getEventType() {
        return queryMethodProvider.getQueryMethod().getEventType();
    }
}
