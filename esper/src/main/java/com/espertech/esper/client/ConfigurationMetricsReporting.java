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
package com.espertech.esper.client;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.type.StringPatternSet;
import com.espertech.esper.type.StringPatternSetLike;
import com.espertech.esper.type.StringPatternSetRegex;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuratiom for metrics reporting.
 */
public class ConfigurationMetricsReporting implements Serializable {
    private boolean jmxEngineMetrics;
    private boolean enableMetricsReporting;
    private boolean isThreading;
    private long engineInterval;
    private long statementInterval;
    private Map<String, StmtGroupMetrics> statementGroups;
    private static final long serialVersionUID = -7265780298667075895L;

    /**
     * Ctor.
     */
    public ConfigurationMetricsReporting() {
        jmxEngineMetrics = false;
        enableMetricsReporting = false;
        isThreading = true;
        engineInterval = 10 * 1000; // 10 seconds
        statementInterval = 10 * 1000;
        statementGroups = new LinkedHashMap<String, StmtGroupMetrics>();
    }

    /**
     * Add a statement group, allowing control of metrics reporting interval per statement or
     * per multiple statements. The reporting interval and be changed at runtime.
     * <p>
     * Add pattern include and exclude criteria to control which
     *
     * @param name   of statement group, not connected to statement name, assigned as an
     *               arbitrary identifier for runtime changes to the interval
     * @param config the statement group metrics configuration
     */
    public void addStmtGroup(String name, StmtGroupMetrics config) {
        statementGroups.put(name, config);
    }

    /**
     * Returns true if metrics reporting is turned on, false if not.
     *
     * @return indicator whether metrics reporting is turned on
     */
    public boolean isEnableMetricsReporting() {
        return enableMetricsReporting;
    }

    /**
     * Set to true to turn metrics reporting on, or false to turn metrics reporting off.
     *
     * @param enableMetricsReporting indicator whether metrics reporting should be turned on
     */
    public void setEnableMetricsReporting(boolean enableMetricsReporting) {
        this.enableMetricsReporting = enableMetricsReporting;
    }

    /**
     * Returns true to indicate that metrics reporting takes place in a separate thread (default),
     * or false to indicate that metrics reporting takes place as part of timer processing.
     *
     * @return indicator whether metrics reporting is threaded
     */
    public boolean isThreading() {
        return isThreading;
    }

    /**
     * Set to true to indicate that metrics reporting should take place in a separate thread,
     * or false to indicate that metrics reporting takes place as part of timer processing.
     *
     * @param threading indicator whether metrics reporting is threaded
     */
    public void setThreading(boolean threading) {
        isThreading = threading;
    }

    /**
     * Returns the engine metrics production interval in milliseconds.
     *
     * @return engine metrics production interval
     */
    public long getEngineInterval() {
        return engineInterval;
    }

    /**
     * Sets the engine metrics production interval in milliseconds.
     * <p>
     * Use a negative or zero value to disable engine metrics production.
     *
     * @param engineInterval engine metrics production interval
     */
    public void setEngineInterval(long engineInterval) {
        this.engineInterval = engineInterval;
    }

    /**
     * Returns the statement metrics production interval in milliseconds,
     * unless statement groups have been defined that override this setting for certain statements.
     *
     * @return statement metrics production interval
     */
    public long getStatementInterval() {
        return statementInterval;
    }

    /**
     * Sets the statement metrics production interval in milliseconds,
     * unless statement groups have been defined that override this setting for certain statements.
     *
     * @param statementInterval statement metrics production interval
     */
    public void setStatementInterval(long statementInterval) {
        this.statementInterval = statementInterval;
    }

    /**
     * Returns a map of statement group and metrics configuration for the statement group.
     *
     * @return map of statement group and metrics configuration
     */
    public Map<String, StmtGroupMetrics> getStatementGroups() {
        return statementGroups;
    }

    /**
     * Returns true if the engine registers JMX mbeans, with the platform mbean server,
     * that provide key engine metrics.
     *
     * @return indicator
     */
    public boolean isJmxEngineMetrics() {
        return jmxEngineMetrics;
    }

    /**
     * Set to true to have the the engine register JMX mbeans, with the platform mbean server,
     * that provide key engine metrics.
     *
     * @param jmxEngineMetrics indicator whether enabled or not
     */
    public void setJmxEngineMetrics(boolean jmxEngineMetrics) {
        this.jmxEngineMetrics = jmxEngineMetrics;
    }

    /**
     * Sets a new interval for a statement group identified by name.
     *
     * @param stmtGroupName name of statement group as assigned through configuration
     * @param newInterval   new interval, or a -1 or zero value to disable reporting
     */
    public void setStatementGroupInterval(String stmtGroupName, long newInterval) {
        StmtGroupMetrics metrics = statementGroups.get(stmtGroupName);
        if (metrics != null) {
            metrics.setInterval(newInterval);
        } else {
            throw new ConfigurationException("Statement group by name '" + stmtGroupName + "' could not be found");
        }
    }

    /**
     * Class to configure statement metrics reporting for a group of one or more statements.
     */
    public static class StmtGroupMetrics implements Serializable {
        private List<Pair<StringPatternSet, Boolean>> patterns;
        private int numStatements;
        private long interval;
        private boolean reportInactive;
        private boolean defaultInclude;
        private static final long serialVersionUID = 5449418752480520879L;

        /**
         * Ctor.
         */
        public StmtGroupMetrics() {
            patterns = new ArrayList<Pair<StringPatternSet, Boolean>>();
            interval = 10000;
            numStatements = 100;
        }

        /**
         * Include all statements in the statement group that match the SQL like-expression by statement name.
         *
         * @param likeExpression to match
         */
        public void addIncludeLike(String likeExpression) {
            patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(likeExpression), true));
        }

        /**
         * Exclude all statements from the statement group that match the SQL like-expression by statement name.
         *
         * @param likeExpression to match
         */
        public void addExcludeLike(String likeExpression) {
            patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(likeExpression), false));
        }

        /**
         * Include all statements in the statement group that match the regular expression by statement name.
         *
         * @param regexExpression to match
         */
        public void addIncludeRegex(String regexExpression) {
            patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(regexExpression), true));
        }

        /**
         * Exclude all statements in the statement group that match the regular expression by statement name.
         *
         * @param regexExpression to match
         */
        public void addExcludeRegEx(String regexExpression) {
            patterns.add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(regexExpression), false));
        }

        /**
         * Returns the reporting interval for statement metrics for statements in the statement group.
         *
         * @return interval
         */
        public long getInterval() {
            return interval;
        }

        /**
         * Sets the reporting interval for statement metrics for statements in the statement group.
         *
         * @param interval a negative or zero value to disable reporting for this group of statements
         */
        public void setInterval(long interval) {
            this.interval = interval;
        }

        /**
         * Returns a list of patterns that indicate whether a statement, by the statement name matching or
         * not matching each pattern, falls within the statement group.
         * <p>
         * Include-patterns are boolean true in the pair of pattern and boolean. Exclude-patterns are
         * boolean false.
         *
         * @return list of include and exclude pattern
         */
        public List<Pair<StringPatternSet, Boolean>> getPatterns() {
            return patterns;
        }

        /**
         * Returns the initial capacity number of statements held by the statement group.
         *
         * @return initial capacity
         */
        public int getNumStatements() {
            return numStatements;
        }

        /**
         * Sets the initial capacity number of statements held by the statement group.
         *
         * @param numStatements initial capacity
         */
        public void setNumStatements(int numStatements) {
            this.numStatements = numStatements;
        }

        /**
         * Returns true to indicate that inactive statements (statements without events or timer activity)
         * are also reported.
         *
         * @return true for reporting inactive statements
         */
        public boolean isReportInactive() {
            return reportInactive;
        }

        /**
         * Set to true to indicate that inactive statements (statements without events or timer activity)
         * are also reported, or false to omit reporting for inactive statements.
         *
         * @param reportInactive set to true for reporting inactive statements
         */
        public void setReportInactive(boolean reportInactive) {
            this.reportInactive = reportInactive;
        }

        /**
         * If this flag is set then all statement names are automatically included in this
         * statement group, and through exclude-pattern certain statement names can be omitted
         * <p>
         * If this flag is not set then all statement names are automatically excluded in this
         * statement group, and through include-pattern certain statement names can be included.
         * <p>
         * The default is false, i.e. statements must be explicitly included.
         *
         * @return true for include all statements, false for explicitly include
         */
        public boolean isDefaultInclude() {
            return defaultInclude;
        }

        /**
         * Set this flag to true and all statement names are automatically included in this
         * statement group, and through exclude-pattern certain statement names can be omitted
         * <p>
         * Set this flag to false and all statement names are automatically excluded in this
         * statement group, and through include-pattern certain statement names can be included.
         * <p>
         * The default is false, i.e. statements must be explicitly included.
         *
         * @param defaultInclude true for include all statements, false for explicitly include statements
         */
        public void setDefaultInclude(boolean defaultInclude) {
            this.defaultInclude = defaultInclude;
        }
    }
}
