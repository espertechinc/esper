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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.metric.MetricReportingService;
import com.espertech.esper.epl.metric.StatementMetricHandle;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnMergeViewFactory extends NamedWindowOnExprBaseViewFactory {
    private final NamedWindowOnMergeHelper namedWindowOnMergeHelper;
    private final StatementResultService statementResultService;
    private final StatementMetricHandle createNamedWindowMetricHandle;
    private final MetricReportingService metricReportingService;

    public NamedWindowOnMergeViewFactory(EventType namedWindowEventType, NamedWindowOnMergeHelper namedWindowOnMergeHelper, StatementResultService statementResultService, StatementMetricHandle createNamedWindowMetricHandle, MetricReportingService metricReportingService) {
        super(namedWindowEventType);
        this.namedWindowOnMergeHelper = namedWindowOnMergeHelper;
        this.statementResultService = statementResultService;
        this.createNamedWindowMetricHandle = createNamedWindowMetricHandle;
        this.metricReportingService = metricReportingService;
    }

    public NamedWindowOnExprView make(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        if (namedWindowOnMergeHelper.getInsertUnmatched() != null) {
            return new NamedWindowOnMergeInsertUnmatched(namedWindowRootViewInstance, agentInstanceContext, this);
        }
        return new NamedWindowOnMergeView(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this);
    }

    public NamedWindowOnMergeHelper getNamedWindowOnMergeHelper() {
        return namedWindowOnMergeHelper;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public StatementMetricHandle getCreateNamedWindowMetricHandle() {
        return createNamedWindowMetricHandle;
    }

    public MetricReportingService getMetricReportingService() {
        return metricReportingService;
    }
}