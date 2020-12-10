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

import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogNFAViewFactoryForge;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactoryForge;
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
import com.espertech.esper.common.internal.view.intersect.IntersectViewFactoryForge;
import com.espertech.esper.common.internal.view.keepall.KeepAllViewForge;
import com.espertech.esper.common.internal.view.lastevent.LastEventViewForge;
import com.espertech.esper.common.internal.view.length.LengthWindowViewForge;
import com.espertech.esper.common.internal.view.lengthbatch.LengthBatchViewForge;
import com.espertech.esper.common.internal.view.prior.PriorEventViewForge;
import com.espertech.esper.common.internal.view.rank.RankWindowViewForge;
import com.espertech.esper.common.internal.view.sort.SortWindowViewForge;
import com.espertech.esper.common.internal.view.time_accum.TimeAccumViewForge;
import com.espertech.esper.common.internal.view.timebatch.TimeBatchViewForge;
import com.espertech.esper.common.internal.view.timelengthbatch.TimeLengthBatchViewForge;
import com.espertech.esper.common.internal.view.timetolive.TimeOrderViewForge;
import com.espertech.esper.common.internal.view.timetolive.TimeToLiveViewForge;
import com.espertech.esper.common.internal.view.timewin.TimeWindowViewForge;
import com.espertech.esper.common.internal.view.union.UnionViewFactoryForge;
import com.espertech.esper.common.internal.view.unique.UniqueByPropertyViewForge;

public interface ViewFactoryForgeVisitor<T> {
    T visit(LengthWindowViewForge forge);
    T visit(SortWindowViewForge forge);
    T visit(TimeLengthBatchViewForge forge);
    T visit(SizeViewForge forge);
    T visit(UnivariateStatisticsViewForge forge);
    T visit(MergeViewFactoryForge forge);
    T visit(UniqueByPropertyViewForge forge);
    T visit(TimeAccumViewForge forge);
    T visit(ExternallyTimedBatchViewForge forge);
    T visit(IntersectViewFactoryForge forge);
    T visit(PriorEventViewForge forge);
    T visit(FirstLengthWindowViewForge forge);
    T visit(TimeOrderViewForge forge);
    T visit(ExpressionBatchViewForge forge);
    T visit(LastEventViewForge forge);
    T visit(TimeToLiveViewForge forge);
    T visit(FirstTimeViewForge forge);
    T visit(TimeWindowViewForge forge);
    T visit(RowRecogNFAViewFactoryForge forge);
    T visit(RankWindowViewForge forge);
    T visit(ExternallyTimedWindowViewForge forge);
    T visit(WeightedAverageViewForge forge);
    T visit(LengthBatchViewForge forge);
    T visit(RegressionLinestViewForge forge);
    T visit(CorrelationViewForge forge);
    T visit(KeepAllViewForge forge);
    T visit(ExpressionWindowViewForge forge);
    T visit(UnionViewFactoryForge forge);
    T visit(TimeBatchViewForge forge);
    T visit(FirstUniqueByPropertyViewForge forge);
    T visit(FirstEventViewForge forge);
    T visit(GroupByViewFactoryForge forge);
    T visit(VirtualDWViewFactoryForge forge);
    T visitExtension(ViewFactoryForge extension);
}
