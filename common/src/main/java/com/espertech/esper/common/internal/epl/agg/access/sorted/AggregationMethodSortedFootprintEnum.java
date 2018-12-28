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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPInputEnum;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPParam;
import com.espertech.esper.common.internal.epl.util.EPLExpressionParamType;

public enum AggregationMethodSortedFootprintEnum {
    KEYONLY(new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam("the key value", EPLExpressionParamType.ANY))
    }),
    NOPARAM(new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY)
    }),
    SUBMAP(new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam("the from-key value", EPLExpressionParamType.ANY),
            new DotMethodFPParam("the from-inclusive flag", EPLExpressionParamType.BOOLEAN),
            new DotMethodFPParam("the to-key value", EPLExpressionParamType.ANY),
            new DotMethodFPParam("the to-inclusive flag", EPLExpressionParamType.BOOLEAN))
    });

    private final DotMethodFP[] fp;

    AggregationMethodSortedFootprintEnum(DotMethodFP[] fp) {
        this.fp = fp;
    }

    public DotMethodFP[] getFp() {
        return fp;
    }
}
