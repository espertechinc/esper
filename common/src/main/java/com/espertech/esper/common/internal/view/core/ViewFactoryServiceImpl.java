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

import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewFactory;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.view.derived.*;
import com.espertech.esper.common.internal.view.expression.ExpressionBatchViewFactory;
import com.espertech.esper.common.internal.view.expression.ExpressionWindowViewFactory;
import com.espertech.esper.common.internal.view.exttimedbatch.ExternallyTimedBatchViewFactory;
import com.espertech.esper.common.internal.view.exttimedwin.ExternallyTimedWindowViewFactory;
import com.espertech.esper.common.internal.view.firstevent.FirstEventViewFactory;
import com.espertech.esper.common.internal.view.firstlength.FirstLengthWindowViewFactory;
import com.espertech.esper.common.internal.view.firsttime.FirstTimeViewFactory;
import com.espertech.esper.common.internal.view.firstunique.FirstUniqueByPropertyViewFactory;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactory;
import com.espertech.esper.common.internal.view.intersect.IntersectViewFactory;
import com.espertech.esper.common.internal.view.keepall.KeepAllViewFactory;
import com.espertech.esper.common.internal.view.lastevent.LastEventViewFactory;
import com.espertech.esper.common.internal.view.length.LengthWindowViewFactory;
import com.espertech.esper.common.internal.view.lengthbatch.LengthBatchViewFactory;
import com.espertech.esper.common.internal.view.prior.PriorEventViewFactory;
import com.espertech.esper.common.internal.view.rank.RankWindowViewFactory;
import com.espertech.esper.common.internal.view.sort.SortWindowViewFactory;
import com.espertech.esper.common.internal.view.time_accum.TimeAccumViewFactory;
import com.espertech.esper.common.internal.view.timebatch.TimeBatchViewFactory;
import com.espertech.esper.common.internal.view.timelengthbatch.TimeLengthBatchViewFactory;
import com.espertech.esper.common.internal.view.timetolive.TimeOrderViewFactory;
import com.espertech.esper.common.internal.view.timewin.TimeWindowViewFactory;
import com.espertech.esper.common.internal.view.union.UnionViewFactory;
import com.espertech.esper.common.internal.view.unique.UniqueByPropertyViewFactory;

public class ViewFactoryServiceImpl implements ViewFactoryService {

    public final static ViewFactoryServiceImpl INSTANCE = new ViewFactoryServiceImpl();

    private ViewFactoryServiceImpl() {
    }

    public LengthWindowViewFactory length(StateMgmtSetting stateMgmtSettings) {
        return new LengthWindowViewFactory();
    }

    public PriorEventViewFactory prior(StateMgmtSetting stateMgmtSettings) {
        return new PriorEventViewFactory();
    }

    public TimeWindowViewFactory time(StateMgmtSetting stateMgmtSettings) {
        return new TimeWindowViewFactory();
    }

    public KeepAllViewFactory keepall(StateMgmtSetting stateMgmtSettings) {
        return new KeepAllViewFactory();
    }

    public TimeBatchViewFactory timebatch(StateMgmtSetting stateMgmtSettings) {
        return new TimeBatchViewFactory();
    }

    public TimeLengthBatchViewFactory timelengthbatch(StateMgmtSetting stateMgmtSettings) {
        return new TimeLengthBatchViewFactory();
    }

    public LengthBatchViewFactory lengthbatch(StateMgmtSetting stateMgmtSettings) {
        return new LengthBatchViewFactory();
    }

    public SortWindowViewFactory sort(StateMgmtSetting stateMgmtSettings) {
        return new SortWindowViewFactory();
    }

    public RankWindowViewFactory rank(StateMgmtSetting stateMgmtSettings) {
        return new RankWindowViewFactory();
    }

    public TimeAccumViewFactory timeaccum(StateMgmtSetting stateMgmtSettings) {
        return new TimeAccumViewFactory();
    }

    public UniqueByPropertyViewFactory unique(StateMgmtSetting stateMgmtSettings) {
        return new UniqueByPropertyViewFactory();
    }

    public FirstUniqueByPropertyViewFactory firstunique(StateMgmtSetting stateMgmtSettings) {
        return new FirstUniqueByPropertyViewFactory();
    }

    public FirstTimeViewFactory firsttime(StateMgmtSetting stateMgmtSettings) {
        return new FirstTimeViewFactory();
    }

    public ExternallyTimedBatchViewFactory exttimebatch(StateMgmtSetting stateMgmtSettings) {
        return new ExternallyTimedBatchViewFactory();
    }

    public ExternallyTimedWindowViewFactory exttime(StateMgmtSetting stateMgmtSettings) {
        return new ExternallyTimedWindowViewFactory();
    }

    public TimeOrderViewFactory timeorder(StateMgmtSetting stateMgmtSettings) {
        return new TimeOrderViewFactory();
    }

    public LastEventViewFactory lastevent(StateMgmtSetting stateMgmtSettings) {
        return new LastEventViewFactory();
    }

    public FirstEventViewFactory firstevent(StateMgmtSetting stateMgmtSettings) {
        return new FirstEventViewFactory();
    }

    public FirstLengthWindowViewFactory firstlength(StateMgmtSetting stateMgmtSettings) {
        return new FirstLengthWindowViewFactory();
    }

    public SizeViewFactory size(StateMgmtSetting stateMgmtSettings) {
        return new SizeViewFactory();
    }

    public UnivariateStatisticsViewFactory uni(StateMgmtSetting stateMgmtSettings) {
        return new UnivariateStatisticsViewFactory();
    }

    public WeightedAverageViewFactory weightedavg(StateMgmtSetting stateMgmtSettings) {
        return new WeightedAverageViewFactory();
    }

    public RegressionLinestViewFactory regression(StateMgmtSetting stateMgmtSettings) {
        return new RegressionLinestViewFactory();
    }

    public CorrelationViewFactory correlation(StateMgmtSetting stateMgmtSettings) {
        return new CorrelationViewFactory();
    }

    public GroupByViewFactory group(StateMgmtSetting stateMgmtSettings) {
        return new GroupByViewFactory();
    }

    public IntersectViewFactory intersect(StateMgmtSetting stateMgmtSettings) {
        return new IntersectViewFactory();
    }

    public UnionViewFactory union(StateMgmtSetting stateMgmtSettings) {
        return new UnionViewFactory();
    }

    public ExpressionBatchViewFactory exprbatch(StateMgmtSetting stateMgmtSettings) {
        return new ExpressionBatchViewFactory();
    }

    public ExpressionWindowViewFactory expr(StateMgmtSetting stateMgmtSettings) {
        return new ExpressionWindowViewFactory();
    }

    public RowRecogNFAViewFactory rowRecog(StateMgmtSetting stateMgmtSettings) {
        return new RowRecogNFAViewFactory();
    }
}
