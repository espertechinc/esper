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
package com.espertech.esper.common.internal.metrics.stmtmetrics;

import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.schedule.SchedulingService;

/**
 * Execution context for metrics reporting executions.
 */
public class MetricExecutionContext {
    private final FilterService filterService;
    private final SchedulingService schedulingService;
    private final EventServiceSendEventCommon epRuntimeSendEvent;
    private final StatementMetricRepository statementMetricRepository;

    public MetricExecutionContext(FilterService filterService, SchedulingService schedulingService, EventServiceSendEventCommon epRuntimeSendEvent, StatementMetricRepository statementMetricRepository) {
        this.filterService = filterService;
        this.schedulingService = schedulingService;
        this.epRuntimeSendEvent = epRuntimeSendEvent;
        this.statementMetricRepository = statementMetricRepository;
    }

    public FilterService getFilterService() {
        return filterService;
    }

    public SchedulingService getSchedulingService() {
        return schedulingService;
    }

    public EventServiceSendEventCommon getEPRuntimeSendEvent() {
        return epRuntimeSendEvent;
    }

    /**
     * Returns statement metric holder
     *
     * @return holder for metrics
     */
    public StatementMetricRepository getStatementMetricRepository() {
        return statementMetricRepository;
    }
}
