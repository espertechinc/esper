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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.internal.epl.methodbase.DotMethodFP;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPInputEnum;
import com.espertech.esper.common.internal.epl.methodbase.DotMethodFPParam;
import com.espertech.esper.common.internal.epl.util.EPLExpressionParamType;

public class EnumMethodEnumParams {

    public static final DotMethodFP[] NOOP_REVERSE = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY),
    };

    public static final DotMethodFP[] COUNTOF_FIRST_LAST = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "predicate", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(predicate, index)", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(predicate, index, size)", EPLExpressionParamType.BOOLEAN)),
    };

    public static final DotMethodFP[] TAKELAST = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(0, "count", EPLExpressionParamType.NUMERIC)),
    };

    public static final DotMethodFP[] TAKE = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(0, "count", EPLExpressionParamType.NUMERIC)),
    };

    public static final DotMethodFP[] AGGREGATE_FP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(0, "initialization-value", EPLExpressionParamType.ANY),
            new DotMethodFPParam(2, "(result, next)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(0, "initialization-value", EPLExpressionParamType.ANY),
            new DotMethodFPParam(3, "(result, next, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(0, "initialization-value", EPLExpressionParamType.ANY),
            new DotMethodFPParam(4, "(result, next, index, size)", EPLExpressionParamType.ANY)),
    };

    public static final DotMethodFP[] ALLOF_ANYOF = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "predicate", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(predicate, index)", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(predicate, index, size)", EPLExpressionParamType.BOOLEAN)),
    };

    public static final DotMethodFP[] ORDERBY_DISTINCT_ARRAYOF_MOSTLEAST_MINMAX = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(value-selector, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(value-selector, index, size)", EPLExpressionParamType.ANY)),
    };

    public static final DotMethodFP[] SELECTFROM_MINMAXBY = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(value-selector, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(value-selector, index, size)", EPLExpressionParamType.ANY)),
    };

    public static final DotMethodFP[] AVERAGE_SUMOF = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.SCALAR_NUMERIC),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.NUMERIC)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(value-selector, index)", EPLExpressionParamType.NUMERIC)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(value-selector, index, size)", EPLExpressionParamType.NUMERIC))
    };

    public static final DotMethodFP[] TOMAP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(1, "key-selector", EPLExpressionParamType.ANY),
            new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(2, "(key-selector, index)", EPLExpressionParamType.ANY),
            new DotMethodFPParam(2, "(value-selector, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(3, "(key-selector, index, size)", EPLExpressionParamType.ANY),
            new DotMethodFPParam(3, "(value-selector, index, size)", EPLExpressionParamType.ANY))
    };

    public static final DotMethodFP[] GROUP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "key-selector", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(key-selector, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(key-selector, index, size)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(1, "key-selector", EPLExpressionParamType.ANY),
            new DotMethodFPParam(1, "value-selector", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(2, "(key-selector, index)", EPLExpressionParamType.ANY),
            new DotMethodFPParam(2, "(value-selector, index)", EPLExpressionParamType.ANY)),
        new DotMethodFP(DotMethodFPInputEnum.ANY,
            new DotMethodFPParam(3, "(key-selector, index, size)", EPLExpressionParamType.ANY),
            new DotMethodFPParam(3, "(value-selector, index, size)", EPLExpressionParamType.ANY))
    };

    public static final DotMethodFP[] WHERE_FP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(1, "predicate", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(2, "(predicate, index)", EPLExpressionParamType.BOOLEAN)),
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(3, "(predicate, index, size)", EPLExpressionParamType.BOOLEAN))
    };

    public static final DotMethodFP[] SET_LOGIC_FP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.ANY, new DotMethodFPParam(0, "collection", EPLExpressionParamType.ANY)),
    };

    public static final DotMethodFP[] SEQ_EQUALS_FP = new DotMethodFP[]{
        new DotMethodFP(DotMethodFPInputEnum.SCALAR_ANY, new DotMethodFPParam(0, "sequence", EPLExpressionParamType.ANY)),
    };
}
