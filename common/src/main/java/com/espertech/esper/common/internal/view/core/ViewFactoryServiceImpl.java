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

    public LengthWindowViewFactory length() {
        return new LengthWindowViewFactory();
    }

    public PriorEventViewFactory prior() {
        return new PriorEventViewFactory();
    }

    public TimeWindowViewFactory time() {
        return new TimeWindowViewFactory();
    }

    public KeepAllViewFactory keepall() {
        return new KeepAllViewFactory();
    }

    public TimeBatchViewFactory timebatch() {
        return new TimeBatchViewFactory();
    }

    public TimeLengthBatchViewFactory timelengthbatch() {
        return new TimeLengthBatchViewFactory();
    }

    public LengthBatchViewFactory lengthbatch() {
        return new LengthBatchViewFactory();
    }

    public SortWindowViewFactory sort() {
        return new SortWindowViewFactory();
    }

    public RankWindowViewFactory rank() {
        return new RankWindowViewFactory();
    }

    public TimeAccumViewFactory timeaccum() {
        return new TimeAccumViewFactory();
    }

    public UniqueByPropertyViewFactory unique() {
        return new UniqueByPropertyViewFactory();
    }

    public FirstUniqueByPropertyViewFactory firstunique() {
        return new FirstUniqueByPropertyViewFactory();
    }

    public FirstTimeViewFactory firsttime() {
        return new FirstTimeViewFactory();
    }

    public ExternallyTimedBatchViewFactory exttimebatch() {
        return new ExternallyTimedBatchViewFactory();
    }

    public ExternallyTimedWindowViewFactory exttime() {
        return new ExternallyTimedWindowViewFactory();
    }

    public TimeOrderViewFactory timeorder() {
        return new TimeOrderViewFactory();
    }

    public LastEventViewFactory lastevent() {
        return new LastEventViewFactory();
    }

    public FirstEventViewFactory firstevent() {
        return new FirstEventViewFactory();
    }

    public FirstLengthWindowViewFactory firstlength() {
        return new FirstLengthWindowViewFactory();
    }

    public SizeViewFactory size() {
        return new SizeViewFactory();
    }

    public UnivariateStatisticsViewFactory uni() {
        return new UnivariateStatisticsViewFactory();
    }

    public WeightedAverageViewFactory weightedavg() {
        return new WeightedAverageViewFactory();
    }

    public RegressionLinestViewFactory regression() {
        return new RegressionLinestViewFactory();
    }

    public CorrelationViewFactory correlation() {
        return new CorrelationViewFactory();
    }

    public GroupByViewFactory group() {
        return new GroupByViewFactory();
    }

    public IntersectViewFactory intersect() {
        return new IntersectViewFactory();
    }

    public UnionViewFactory union() {
        return new UnionViewFactory();
    }

    public ExpressionBatchViewFactory exprbatch() {
        return new ExpressionBatchViewFactory();
    }

    public ExpressionWindowViewFactory expr() {
        return new ExpressionWindowViewFactory();
    }

    public RowRecogNFAViewFactory rowRecog() {
        return new RowRecogNFAViewFactory();
    }
}
