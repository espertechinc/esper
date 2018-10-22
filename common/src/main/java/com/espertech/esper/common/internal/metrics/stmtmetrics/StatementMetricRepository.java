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

import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeMetricsReporting;
import com.espertech.esper.common.client.metric.StatementMetric;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.type.StringPatternSet;
import com.espertech.esper.common.internal.type.StringPatternSetUtil;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A repository for all statement metrics that organizes statements into statement groups.
 * <p>
 * At a minimum there is one group (the default) of index zero.
 */
public class StatementMetricRepository {
    private final ConfigurationRuntimeMetricsReporting specification;
    private final StatementMetricArray[] groupMetrics;
    private final Map<DeploymentIdNamePair, Integer> statementGroups;

    /**
     * Ctor.
     *
     * @param runtimeURI    runtime URI
     * @param specification specifies statement groups
     */
    public StatementMetricRepository(String runtimeURI, ConfigurationRuntimeMetricsReporting specification) {
        this.specification = specification;
        int numGroups = specification.getStatementGroups().size() + 1;  // +1 for default group (remaining stmts)
        this.groupMetrics = new StatementMetricArray[numGroups];

        // default group
        groupMetrics[0] = new StatementMetricArray(runtimeURI, "group-default", 100, false);

        // initialize all other groups
        int countGroups = 1;
        for (Map.Entry<String, ConfigurationRuntimeMetricsReporting.StmtGroupMetrics> entry : specification.getStatementGroups().entrySet()) {
            ConfigurationRuntimeMetricsReporting.StmtGroupMetrics config = entry.getValue();

            int initialNumStmts = config.getNumStatements();
            if (initialNumStmts < 10) {
                initialNumStmts = 10;
            }
            groupMetrics[countGroups] = new StatementMetricArray(runtimeURI, "group-" + countGroups, initialNumStmts, config.isReportInactive());
            countGroups++;
        }

        statementGroups = new HashMap<>();
    }

    /**
     * Add a statement, inspecting the statement name and adding it to a statement group or the default group, if none.
     *
     * @param statement name to inspect
     * @return handle for statement
     */
    public StatementMetricHandle addStatement(DeploymentIdNamePair statement) {
        // determine group
        int countGroups = 1;
        int groupNumber = -1;
        for (Map.Entry<String, ConfigurationRuntimeMetricsReporting.StmtGroupMetrics> entry : specification.getStatementGroups().entrySet()) {
            List<Pair<StringPatternSet, Boolean>> patterns = entry.getValue().getPatterns();
            boolean result = StringPatternSetUtil.evaluate(entry.getValue().isDefaultInclude(), patterns, statement.getName());

            if (result) {
                groupNumber = countGroups;
                break;
            }
            countGroups++;
        }

        // assign to default group if none other apply
        if (groupNumber == -1) {
            groupNumber = 0;
        }

        int index = groupMetrics[groupNumber].addStatementGetIndex(statement);

        statementGroups.put(statement, groupNumber);

        return new StatementMetricHandle(groupNumber, index);
    }

    /**
     * Remove statement.
     *
     * @param statement to remove
     */
    public void removeStatement(DeploymentIdNamePair statement) {
        Integer group = statementGroups.remove(statement);
        if (group != null) {
            groupMetrics[group].removeStatement(statement);
        }
    }

    /**
     * Account statement times.
     *
     * @param handle   statement handle
     * @param cpu      time
     * @param wall     time
     * @param numInput number of input rows
     */
    public void accountTimes(StatementMetricHandle handle, long cpu, long wall, int numInput) {
        StatementMetricArray array = groupMetrics[handle.getGroupNum()];
        array.getRwLock().acquireReadLock();
        try {
            StatementMetric metric = array.getAddMetric(handle.getIndex());
            metric.addCPUTime(cpu);
            metric.addWallTime(wall);
            metric.addNumInput(numInput);
        } finally {
            array.getRwLock().releaseReadLock();
        }
    }

    /**
     * Account row output.
     *
     * @param handle     statement handle
     * @param numIStream num rows insert stream
     * @param numRStream num rows remove stream
     */
    public void accountOutput(StatementMetricHandle handle, int numIStream, int numRStream) {
        StatementMetricArray array = groupMetrics[handle.getGroupNum()];
        array.getRwLock().acquireReadLock();
        try {
            StatementMetric metric = array.getAddMetric(handle.getIndex());
            metric.addNumOutputIStream(numIStream);
            metric.addNumOutputRStream(numRStream);
        } finally {
            array.getRwLock().releaseReadLock();
        }
    }

    /**
     * Report for a given statement group.
     *
     * @param group to report
     * @return metrics or null if none
     */
    public StatementMetric[] reportGroup(int group) {
        return groupMetrics[group].flushMetrics();
    }
}
