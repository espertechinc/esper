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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.epl.agg.groupall.AggregationServiceGroupAllForge;
import com.espertech.esper.common.internal.epl.agg.groupby.AggregationServiceGroupByForge;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggSvcLocalGroupByForge;
import com.espertech.esper.common.internal.epl.agg.rollup.AggSvcGroupByRollupForge;
import com.espertech.esper.common.internal.epl.agg.table.AggregationServiceFactoryForgeTable;

public interface AggregationServiceFactoryForgeVisitor<T> {
    T visit(AggregationServiceGroupAllForge forge);

    T visit(AggSvcLocalGroupByForge forge);

    T visit(AggregationServiceFactoryForgeTable forge);

    T visit(AggregationServiceNullFactory forge);

    T visit(AggSvcGroupByRollupForge forge);

    T visit(AggregationServiceGroupByForge forge);
}
