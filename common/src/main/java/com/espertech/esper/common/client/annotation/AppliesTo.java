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
package com.espertech.esper.common.client.annotation;

/**
 * Annotation to target certain constructs.
 */
public enum AppliesTo {
    /**
     * For use with annotations as a default value, not used otherwise (internal use only)
     */
    UNDEFINED,

    /**
     * Group-by for aggregations
     */
    AGGREGATION_GROUPBY,

    /**
     * Context partition id management
     */
    CONTEXT_PARTITIONID,

    /**
     * Contexts - Category Context
     */
    CONTEXT_CATEGORY,

    /**
     * Contexts - Hash Context
     */
    CONTEXT_HASH,

    /**
     * Contexts - Non-overlapping and overlapping
     */
    CONTEXT_INITTERM,

    /**
     * Contexts - Distinct for overlapping contexts
     */
    CONTEXT_INITTERM_DISTINCT,

    /**
     * Contexts - Keyed Context
     */
    CONTEXT_KEYED,

    /**
     * Contexts - Keyed Context termination
     */
    CONTEXT_KEYED_TERM,

    /**
     * Index hashed
     */
    INDEX_HASH,

    /**
     * Index in-set-of-values
     */
    INDEX_IN,

    /**
     * Index btree
     */
    INDEX_SORTED,

    /**
     * Index unindexed
     */
    INDEX_UNINDEXED,

    /**
     * Index spatial or other
     */
    INDEX_OTHER,

    /**
     * Prior
     */
    WINDOW_PRIOR,

    /**
     * Rank window
     */
    WINDOW_RANK,

    /**
     * Pattern every-distinct
     */
    PATTERN_EVERYDISTINCT,

    /**
     * Pattern followed-by
     */
    PATTERN_FOLLOWEDBY,

    /**
     * Match-recognize partitioned state
     */
    ROWRECOG_PARTITIONED,

    /**
     * Match-recognize unpartitioned state
     */
    ROWRECOG_UNPARTITIONED,

    /**
     * Match-recognize schedule state
     */
    ROWRECOG_SCHEDULE,

    /**
     * Pattern-Root node (internal use only)
     */
    PATTERN_ROOT,

    /**
     * Pattern-And node
     */
    PATTERN_AND,

    /**
     * Pattern-Or node
     */
    PATTERN_OR,

    /**
     * Pattern-Guard node
     */
    PATTERN_GUARD,

    /**
     * Pattern-Match-Until node
     */
    PATTERN_MATCHUNTIL,

    /**
     * Pattern-Filter node
     */
    PATTERN_FILTER,

    /**
     * Pattern-Observer node
     */
    PATTERN_OBSERVER,

    /**
     * Pattern-Not node
     */
    PATTERN_NOT,

    /**
     * Pattern-Every node
     */
    PATTERN_EVERY,

    /**
     * Result Set Aggregate-Grouped Output Limit Helper
     */
    RESULTSET_AGGREGATEGROUPED_OUTPUTFIRST,

    /**
     * Result Set Row-Per-Group Output Limit Helper
     */
    RESULTSET_ROWPERGROUP_OUTPUTFIRST,

    /**
     * Output rate limiting
     */
    RESULTSET_OUTPUTLIMIT,

    /**
     * Result Set Rollup Output Limit Helper
     */
    RESULTSET_ROLLUP_OUTPUTSNAPSHOT,

    /**
     * Result Set Rollup Output Limit Helper
     */
    RESULTSET_ROLLUP_OUTPUTALL,

    /**
     * Result Set Rollup Output Limit Helper
     */
    RESULTSET_ROLLUP_OUTPUTFIRST,

    /**
     * Result Set Rollup Output Limit Helper
     */
    RESULTSET_ROLLUP_OUTPUTLAST,

    /**
     * Result Set Fully-Aggregated Output All
     */
    RESULTSET_FULLYAGGREGATED_OUTPUTALL,

    /**
     * Result Set Simple Output All
     */
    RESULTSET_SIMPLE_OUTPUTALL,

    /**
     * Result Set Simple Row-Per-Event Output All
     */
    RESULTSET_ROWPEREVENT_OUTPUTALL,

    /**
     * Result Set Row-Per-Group Output All
     */
    RESULTSET_ROWPERGROUP_OUTPUTALL,

    /**
     * Result Set Row-Per-Group Output All with Option
     */
    RESULTSET_ROWPERGROUP_OUTPUTALL_OPT,

    /**
     * Result Set Row-Per-Group Output All with Option
     */
    RESULTSET_ROWPERGROUP_OUTPUTLAST_OPT,

    /**
     * Result Set Row-Per-Group Unbound Helper
     */
    RESULTSET_ROWPERGROUP_UNBOUND,

    /**
     * Result Set Aggregate-Grouped Output All
     */
    RESULTSET_AGGREGATEGROUPED_OUTPUTALL,

    /**
     * Result Set Aggregate-Grouped Output All with Options
     */
    RESULTSET_AGGREGATEGROUPED_OUTPUTALL_OPT,

    /**
     * Result Set Aggregate-Grouped Output Last with Options
     */
    RESULTSET_AGGREGATEGROUPED_OUTPUTLAST_OPT,

    /**
     * Unique-window
     */
    WINDOW_UNIQUE,

    /**
     * Time-accumulative window
     */
    WINDOW_TIMEACCUM,

    /**
     * Time-batch window
     */
    WINDOW_TIMEBATCH,

    /**
     * Length-batch window
     */
    WINDOW_TIMELENGTHBATCH,

    /**
     * Grouped window
     */
    WINDOW_GROUP,

    /**
     * Length window
     */
    WINDOW_LENGTH,

    /**
     * Time window
     */
    WINDOW_TIME,

    /**
     * Length-batch window
     */
    WINDOW_LENGTHBATCH,

    /**
     * Expression window
     */
    WINDOW_EXPRESSION,

    /**
     * Expression batch window
     */
    WINDOW_EXPRESSIONBATCH,

    /**
     * First-length window
     */
    WINDOW_FIRSTLENGTH,

    /**
     * First-time window
     */
    WINDOW_FIRSTTIME,

    /**
     * First-unique window
     */
    WINDOW_FIRSTUNIQUE,

    /**
     * First-event window
     */
    WINDOW_FIRSTEVENT,

    /**
     * Externally-timed window
     */
    WINDOW_EXTTIMED,

    /**
     * Externally-timed batch window
     */
    WINDOW_EXTTIMEDBATCH,

    /**
     * Univariate stat view
     */
    WINDOW_UNIVARIATESTAT,

    /**
     * Correlation stat view
     */
    WINDOW_CORRELATION,

    /**
     * Size stat view
     */
    WINDOW_SIZE,

    /**
     * Weighted average stat view
     */
    WINDOW_WEIGHTEDAVG,

    /**
     * Regression lineest stat view
     */
    WINDOW_REGRESSIONLINEST,

    /**
     * Union view
     */
    WINDOW_UNION,

    /**
     * Intersect view
     */
    WINDOW_INTERSECT,

    /**
     * Last-event window
     */
    WINDOW_LASTEVENT,

    /**
     * Sorted window
     */
    WINDOW_SORTED,

    /**
     * Time order window
     */
    WINDOW_TIMEORDER,

    /**
     * Time-to-live window
     */
    WINDOW_TIMETOLIVE,

    /**
     * Keep-all window
     */
    WINDOW_KEEPALL,

    /**
     * Match-recognize view (internal use only)
     */
    WINDOW_ROWRECOG;
}
