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
package com.espertech.esper.common.internal.epl.agg.groupby;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.agg.core.AggSvcGroupByReclaimAgedEvalFunc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

public class AggSvcGroupByReclaimAgedEvalFuncFactoryConst implements AggSvcGroupByReclaimAgedEvalFuncFactory, AggSvcGroupByReclaimAgedEvalFunc {
    public final static EPTypeClass EPTYPE = new EPTypeClass(AggSvcGroupByReclaimAgedEvalFuncFactoryConst.class);

    private final double valueDouble;

    public AggSvcGroupByReclaimAgedEvalFuncFactoryConst(double valueDouble) {
        this.valueDouble = valueDouble;
    }

    public AggSvcGroupByReclaimAgedEvalFunc make(ExprEvaluatorContext exprEvaluatorContext) {
        return this;
    }

    public Double getLongValue() {
        return valueDouble;
    }
}
