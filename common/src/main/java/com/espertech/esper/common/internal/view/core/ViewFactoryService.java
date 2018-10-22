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

public interface ViewFactoryService {
    LengthWindowViewFactory length();

    PriorEventViewFactory prior();

    TimeWindowViewFactory time();

    KeepAllViewFactory keepall();

    TimeBatchViewFactory timebatch();

    TimeLengthBatchViewFactory timelengthbatch();

    LengthBatchViewFactory lengthbatch();

    SortWindowViewFactory sort();

    RankWindowViewFactory rank();

    TimeAccumViewFactory timeaccum();

    UniqueByPropertyViewFactory unique();

    FirstUniqueByPropertyViewFactory firstunique();

    FirstTimeViewFactory firsttime();

    TimeOrderViewFactory timeorder();

    ExternallyTimedBatchViewFactory exttimebatch();

    ExternallyTimedWindowViewFactory exttime();

    LastEventViewFactory lastevent();

    FirstEventViewFactory firstevent();

    FirstLengthWindowViewFactory firstlength();

    SizeViewFactory size();

    UnivariateStatisticsViewFactory uni();

    WeightedAverageViewFactory weightedavg();

    RegressionLinestViewFactory regression();

    CorrelationViewFactory correlation();

    GroupByViewFactory group();

    IntersectViewFactory intersect();

    UnionViewFactory union();

    ExpressionBatchViewFactory exprbatch();

    ExpressionWindowViewFactory expr();

    RowRecogNFAViewFactory rowRecog();
}
