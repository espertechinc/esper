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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.internal.view.derived.*;
import com.espertech.esper.common.internal.view.expression.ExpressionBatchViewForge;
import com.espertech.esper.common.internal.view.expression.ExpressionWindowViewForge;
import com.espertech.esper.common.internal.view.exttimedbatch.ExternallyTimedBatchViewForge;
import com.espertech.esper.common.internal.view.exttimedwin.ExternallyTimedWindowViewForge;
import com.espertech.esper.common.internal.view.firstevent.FirstEventViewForge;
import com.espertech.esper.common.internal.view.firstlength.FirstLengthWindowViewForge;
import com.espertech.esper.common.internal.view.firsttime.FirstTimeViewForge;
import com.espertech.esper.common.internal.view.firstunique.FirstUniqueByPropertyViewForge;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactoryForge;
import com.espertech.esper.common.internal.view.groupwin.MergeViewFactoryForge;
import com.espertech.esper.common.internal.view.keepall.KeepAllViewForge;
import com.espertech.esper.common.internal.view.lastevent.LastEventViewForge;
import com.espertech.esper.common.internal.view.length.LengthWindowViewForge;
import com.espertech.esper.common.internal.view.lengthbatch.LengthBatchViewForge;
import com.espertech.esper.common.internal.view.rank.RankWindowViewForge;
import com.espertech.esper.common.internal.view.sort.SortWindowViewForge;
import com.espertech.esper.common.internal.view.time_accum.TimeAccumViewForge;
import com.espertech.esper.common.internal.view.timebatch.TimeBatchViewForge;
import com.espertech.esper.common.internal.view.timelengthbatch.TimeLengthBatchViewForge;
import com.espertech.esper.common.internal.view.timetolive.TimeOrderViewForge;
import com.espertech.esper.common.internal.view.timetolive.TimeToLiveViewForge;
import com.espertech.esper.common.internal.view.timewin.TimeWindowViewForge;
import com.espertech.esper.common.internal.view.unique.UniqueByPropertyViewForge;

/**
 * Enum for all build-in views.
 */
public enum ViewEnum {
    /**
     * Length window.
     */
    LENGTH_WINDOW("win", "length", LengthWindowViewForge.class, null),

    /**
     * Time window.
     */
    TIME_WINDOW("win", "time", TimeWindowViewForge.class, null),

    /**
     * Keep-all data window.
     */
    KEEPALL_WINDOW("win", "keepall", KeepAllViewForge.class, null),

    /**
     * Time batch.
     */
    TIME_BATCH("win", "time_batch", TimeBatchViewForge.class, null),

    /**
     * Time length batch.
     */
    TIME_LENGTH_BATCH("win", "time_length_batch", TimeLengthBatchViewForge.class, null),

    /**
     * Length batch window.
     */
    LENGTH_BATCH("win", "length_batch", LengthBatchViewForge.class, null),

    /**
     * Sorted window.
     */
    SORT_WINDOW("ext", "sort", SortWindowViewForge.class, null),

    /**
     * Rank window.
     */
    RANK_WINDOW("ext", "rank", RankWindowViewForge.class, null),

    /**
     * Time accumulating view.
     */
    TIME_ACCUM("win", "time_accum", TimeAccumViewForge.class, null),

    /**
     * Unique.
     */
    UNIQUE_BY_PROPERTY("std", "unique", UniqueByPropertyViewForge.class, null),

    /**
     * First-Unique.
     */
    UNIQUE_FIRST_BY_PROPERTY("std", "firstunique", FirstUniqueByPropertyViewForge.class, null),

    /**
     * Time first window.
     */
    FIRST_TIME_WINDOW("win", "firsttime", FirstTimeViewForge.class, null),

    /**
     * Time order event window.
     */
    TIME_ORDER("ext", "time_order", TimeOrderViewForge.class, null),

    /**
     * Time order event window.
     */
    TIMETOLIVE("ext", "timetolive", TimeToLiveViewForge.class, null),

    /**
     * Externally timed batch.
     */
    EXT_TIMED_BATCH("win", "ext_timed_batch", ExternallyTimedBatchViewForge.class, null),

    /**
     * Externally timed window.
     */
    EXT_TIMED_WINDOW("win", "ext_timed", ExternallyTimedWindowViewForge.class, null),

    /**
     * Last event.
     */
    LAST_EVENT("std", "lastevent", LastEventViewForge.class, null),

    /**
     * First event.
     */
    FIRST_EVENT("std", "firstevent", FirstEventViewForge.class, null),

    /**
     * Length first window.
     */
    FIRST_LENGTH_WINDOW("win", "firstlength", FirstLengthWindowViewForge.class, null),

    /**
     * Size view.
     */
    SIZE("std", "size", SizeViewForge.class, null),

    /**
     * Univariate statistics.
     */
    UNIVARIATE_STATISTICS("stat", "uni", UnivariateStatisticsViewForge.class, null),

    /**
     * Weighted avg.
     */
    WEIGHTED_AVERAGE("stat", "weighted_avg", WeightedAverageViewForge.class, null),

    /**
     * Linest.
     */
    REGRESSION_LINEST("stat", "linest", RegressionLinestViewForge.class, null),

    /**
     * Correlation.
     */
    CORRELATION("stat", "correl", CorrelationViewForge.class, null),

    /**
     * Group-by merge.
     */
    GROUP_MERGE("std", "merge", MergeViewFactoryForge.class, null),

    /**
     * Group-by.
     */
    GROUP_PROPERTY("std", "groupwin", GroupByViewFactoryForge.class, GROUP_MERGE),

    /**
     * Expression batch window.
     */
    EXPRESSION_BATCH_WINDOW("win", "expr_batch", ExpressionBatchViewForge.class, null),

    /**
     * Expression window.
     */
    EXPRESSION_WINDOW("win", "expr", ExpressionWindowViewForge.class, null);

    private final String namespace;
    private final String name;
    private final Class factoryClass;
    private final ViewEnum mergeView;

    ViewEnum(String namespace, String name, Class factoryClass, ViewEnum mergeView) {
        this.namespace = namespace;
        this.name = name;
        this.factoryClass = factoryClass;
        this.mergeView = mergeView;
    }

    /**
     * Returns namespace that the object belongs to.
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns name of the view that can be used to reference the view in a view expression.
     *
     * @return short name of view
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the enumeration value of the view for merging the data generated by another view.
     *
     * @return view enum for the merge view
     */
    public ViewEnum getMergeView() {
        return mergeView;
    }

    /**
     * Returns a view's factory class.
     *
     * @return class of view factory
     */
    public Class getFactoryClass() {
        return factoryClass;
    }

    /**
     * Returns the view enumeration value given the name of the view.
     *
     * @param namespace is the namespace name of the view
     * @param name      is the short name of the view as used in view expressions
     * @return view enumeration value, or null if no such view name is among the enumerated values
     */
    public static ViewEnum forName(String namespace, String name) {
        if (namespace != null) {
            for (ViewEnum viewEnum : ViewEnum.values()) {
                if ((viewEnum.namespace.equals(namespace)) && (viewEnum.name.equals(name))) {
                    return viewEnum;
                }
            }
        } else {
            for (ViewEnum viewEnum : ViewEnum.values()) {
                if (viewEnum.name.equals(name)) {
                    return viewEnum;
                }
            }
        }

        return null;
    }
}
