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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.average;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import static com.espertech.esper.common.internal.epl.agg.method.avg.AggregatorAvgBig.getValueBigDecimalDivide;

public class AggregatorAvgBigDecimal {
    private BigDecimal sum;
    private long cnt;
    private MathContext optionalMathContext;

    public AggregatorAvgBigDecimal(MathContext optionalMathContext) {
        sum = new BigDecimal(0.0);
        this.optionalMathContext = optionalMathContext;
    }

    public void enter(Object object) {
        if (object == null) {
            return;
        }
        cnt++;
        if (object instanceof BigInteger) {
            sum = sum.add(new BigDecimal((BigInteger) object));
            return;
        }
        sum = sum.add((BigDecimal) object);
    }

    public BigDecimal getValue() {
        return getValueBigDecimalDivide(cnt, optionalMathContext, sum);
    }
}
