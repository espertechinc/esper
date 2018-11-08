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
package com.espertech.esper.runtime.internal.metrics.stmtmetrics;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeMetricsReporting;
import com.espertech.esper.common.client.metric.MetricEvent;
import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.metrics.stmtmetrics.*;
import com.espertech.esper.common.internal.schedule.SchedulingService;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.common.internal.util.MetricUtil;
import com.espertech.esper.runtime.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Metrics reporting.
 * <p>
 * Reports for all statements even if not in a statement group, i.e. statement in default group.
 */
public class MetricReportingServiceImpl implements MetricReportingServiceSPI, MetricEventRouter, DeploymentStateListener {
    private static final Logger log = LoggerFactory.getLogger(MetricReportingServiceImpl.class);

    private final ConfigurationRuntimeMetricsReporting specification;
    private final String runtimeURI;

    private volatile MetricExecutionContext executionContext;

    private boolean isScheduled;
    private final MetricScheduleService schedule;
    private final StatementMetricRepository stmtMetricRepository;

    private MetricExecEngine metricExecEngine;
    private MetricExecStatement metricExecStmtGroupDefault;
    private Map<String, MetricExecStatement> statementGroupExecutions;

    private final Map<DeploymentIdNamePair, StatementMetricHandle> statementMetricHandles;
    private final MetricsExecutor metricsExecutor;

    private final CopyOnWriteArraySet<MetricsStatementResultListener> statementOutputHooks = new CopyOnWriteArraySet<>();

    /**
     * Ctor.
     *
     * @param specification configuration
     * @param runtimeURI    runtime URI
     */
    public MetricReportingServiceImpl(ConfigurationRuntimeMetricsReporting specification, String runtimeURI) {
        this.specification = specification;
        this.runtimeURI = runtimeURI;
        if (!specification.isEnableMetricsReporting()) {
            schedule = null;
            stmtMetricRepository = null;
            statementMetricHandles = null;
            metricsExecutor = null;
            return;
        }

        if (specification.isEnableMetricsReporting()) {
            MetricUtil.initialize();
        }
        schedule = new MetricScheduleService();

        stmtMetricRepository = new StatementMetricRepository(runtimeURI, specification);
        statementGroupExecutions = new LinkedHashMap<>();
        statementMetricHandles = new HashMap<>();

        if (specification.isThreading()) {
            metricsExecutor = new MetricsExecutorThreaded(runtimeURI);
        } else {
            metricsExecutor = new MetricsExecutorUnthreaded();
        }
    }

    public boolean isMetricsReportingEnabled() {
        return specification.isEnableMetricsReporting();
    }

    public void addStatementResultListener(MetricsStatementResultListener listener) {
        statementOutputHooks.add(listener);
    }

    public void removeStatementResultListener(MetricsStatementResultListener listener) {
        statementOutputHooks.remove(listener);
    }

    public CopyOnWriteArraySet<MetricsStatementResultListener> getStatementOutputHooks() {
        return statementOutputHooks;
    }

    public void setContext(FilterService filterService, SchedulingService schedulingService, EventServiceSendEventCommon eventServiceSendEventInternal) {
        MetricExecutionContext metricsExecutionContext = new MetricExecutionContext(filterService, schedulingService, eventServiceSendEventInternal, stmtMetricRepository);

        // create all runtime and statement executions
        metricExecEngine = new MetricExecEngine(this, runtimeURI, schedule, specification.getRuntimeInterval());
        metricExecStmtGroupDefault = new MetricExecStatement(this, schedule, specification.getStatementInterval(), 0);

        int countGroups = 1;
        for (Map.Entry<String, ConfigurationRuntimeMetricsReporting.StmtGroupMetrics> entry : specification.getStatementGroups().entrySet()) {
            ConfigurationRuntimeMetricsReporting.StmtGroupMetrics config = entry.getValue();
            MetricExecStatement metricsExecution = new MetricExecStatement(this, schedule, config.getInterval(), countGroups);
            this.statementGroupExecutions.put(entry.getKey(), metricsExecution);
            countGroups++;
        }

        // last assign this volatile variable so the time event processing may schedule callbacks 
        executionContext = metricsExecutionContext;
    }

    public void processTimeEvent(long timeEventTime) {
        if (!specification.isEnableMetricsReporting()) {
            return;
        }

        schedule.setTime(timeEventTime);
        if (!isScheduled) {
            if (executionContext != null) {
                scheduleExecutions();
                isScheduled = true;
            } else {
                return; // not initialized yet, race condition and must wait till initialized
            }
        }

        // fast evaluation against nearest scheduled time
        Long nearestTime = schedule.getNearestTime();
        if ((nearestTime == null) || (nearestTime > timeEventTime)) {
            return;
        }

        // get executions
        List<MetricExec> executions = new ArrayList<MetricExec>(2);
        schedule.evaluate(executions);
        if (executions.isEmpty()) {
            return;
        }

        // execute
        if (executionContext == null) {
            log.debug(".processTimeEvent No execution context");
            return;
        }

        for (MetricExec execution : executions) {
            metricsExecutor.execute(execution, executionContext);
        }
    }

    public void destroy() {
        schedule.clear();
        metricsExecutor.destroy();
    }

    public void route(MetricEvent metricEvent) {
        executionContext.getEpRuntimeSendEvent().sendEventBean(metricEvent, metricEvent.getClass().getName());
    }

    public void accountTime(StatementMetricHandle metricsHandle, long deltaCPU, long deltaWall, int numInputEvents) {
        stmtMetricRepository.accountTimes(metricsHandle, deltaCPU, deltaWall, numInputEvents);
    }

    public void accountOutput(StatementMetricHandle handle, int numIStream, int numRStream, Object epStatement, Object runtime) {
        stmtMetricRepository.accountOutput(handle, numIStream, numRStream);
        if (!statementOutputHooks.isEmpty()) {
            EPStatement statement = (EPStatement) epStatement;
            EPRuntime service = (EPRuntime) runtime;
            for (MetricsStatementResultListener listener : statementOutputHooks) {
                listener.update(numIStream, numRStream, statement, service);
            }
        }
    }

    public StatementMetricHandle getStatementHandle(int statementId, String deploymentId, String statementName) {
        if (!specification.isEnableMetricsReporting()) {
            return new StatementMetricHandle(false);
        }

        DeploymentIdNamePair statement = new DeploymentIdNamePair(deploymentId, statementName);
        StatementMetricHandle handle = stmtMetricRepository.addStatement(statement);
        statementMetricHandles.put(statement, handle);
        return handle;
    }

    public void onDeployment(DeploymentStateEventDeployed event) {
    }

    public void onUndeployment(DeploymentStateEventUndeployed event) {
        if (!specification.isEnableMetricsReporting()) {
            return;
        }
        for (EPStatement stmt : event.getStatements()) {
            DeploymentIdNamePair pair = new DeploymentIdNamePair(stmt.getDeploymentId(), stmt.getName());
            stmtMetricRepository.removeStatement(pair);
            statementMetricHandles.remove(pair);
        }
    }

    public void setMetricsReportingInterval(String stmtGroupName, long newInterval) {
        if (stmtGroupName == null) {
            metricExecStmtGroupDefault.setInterval(newInterval);
            return;
        }

        MetricExecStatement exec = this.statementGroupExecutions.get(stmtGroupName);
        if (exec == null) {
            throw new IllegalArgumentException("Statement group by name '" + stmtGroupName + "' could not be found");
        }
        exec.setInterval(newInterval);
    }

    private boolean isConsiderSchedule(long value) {
        if ((value > 0) && (value < Long.MAX_VALUE)) {
            return true;
        }
        return false;
    }

    public void setMetricsReportingStmtDisabled(String deploymentId, String statementName) {
        StatementMetricHandle handle = statementMetricHandles.get(new DeploymentIdNamePair(deploymentId, statementName));
        if (handle == null) {
            throw new ConfigurationException("Statement by name '" + statementName + "' not found in metrics collection");
        }
        handle.setEnabled(false);
    }

    public void setMetricsReportingStmtEnabled(String deploymentId, String statementName) {
        StatementMetricHandle handle = statementMetricHandles.get(new DeploymentIdNamePair(deploymentId, statementName));
        if (handle == null) {
            throw new ConfigurationException("Statement by name '" + statementName + "' not found in metrics collection");
        }
        handle.setEnabled(true);
    }

    public void setMetricsReportingEnabled() {
        if (!specification.isEnableMetricsReporting()) {
            throw new ConfigurationException("Metrics reporting must be enabled through initialization-time configuration");
        }
        scheduleExecutions();
    }

    public void setMetricsReportingDisabled() {
        schedule.clear();
    }

    private void scheduleExecutions() {
        if (!specification.isEnableMetricsReporting()) {
            return;
        }

        if (isConsiderSchedule(metricExecEngine.getInterval())) {
            schedule.add(metricExecEngine.getInterval(), metricExecEngine);
        }

        // schedule each statement group, count the "default" group as the first group
        if (isConsiderSchedule(metricExecStmtGroupDefault.getInterval())) {
            schedule.add(metricExecStmtGroupDefault.getInterval(), metricExecStmtGroupDefault);
        }

        for (MetricExecStatement metricsExecution : statementGroupExecutions.values()) {
            if (isConsiderSchedule(metricsExecution.getInterval())) {
                schedule.add(metricsExecution.getInterval(), metricsExecution);
            }
        }
    }
}
