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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;
import com.espertech.esper.epl.metric.MetricReportingServiceSPI;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.epl.table.merge.TableOnMergeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableStateInstance;

public class TableOnMergeViewFactory implements TableOnViewFactory {
    private final TableMetadata tableMetadata;
    private final TableOnMergeHelper onMergeHelper;
    private final StatementResultService statementResultService;
    private final StatementMetricHandle metricsHandle;
    private final MetricReportingServiceSPI metricReportingService;

    public TableOnMergeViewFactory(TableMetadata tableMetadata, TableOnMergeHelper onMergeHelper, StatementResultService statementResultService, StatementMetricHandle metricsHandle, MetricReportingServiceSPI metricReportingService) {
        this.tableMetadata = tableMetadata;
        this.onMergeHelper = onMergeHelper;
        this.statementResultService = statementResultService;
        this.metricsHandle = metricsHandle;
        this.metricReportingService = metricReportingService;
    }

    public TableOnView make(SubordWMatchExprLookupStrategy lookupStrategy, TableStateInstance tableState, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        if (onMergeHelper.getInsertUnmatched() != null) {
            return new TableOnMergeInsertUnmatched(tableState, agentInstanceContext, tableMetadata, this);
        }
        return new TableOnMergeView(lookupStrategy, tableState, agentInstanceContext, tableMetadata, this);
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public TableOnMergeHelper getOnMergeHelper() {
        return onMergeHelper;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public StatementMetricHandle getMetricsHandle() {
        return metricsHandle;
    }

    public MetricReportingServiceSPI getMetricReportingService() {
        return metricReportingService;
    }
}
