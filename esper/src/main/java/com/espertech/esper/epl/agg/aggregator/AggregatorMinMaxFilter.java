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
package com.espertech.esper.epl.agg.aggregator;

import com.espertech.esper.epl.agg.service.common.AggregatorUtil;
import com.espertech.esper.epl.expression.core.MinMaxTypeEnum;

/**
 * Min/max aggregator for all values.
 */
public class AggregatorMinMaxFilter extends AggregatorMinMax {
    public AggregatorMinMaxFilter(MinMaxTypeEnum minMaxTypeEnum) {
        super(minMaxTypeEnum);
    }

    @Override
    public void enter(Object parameters) {
        Object[] paramArray = (Object[]) parameters;
        if (!AggregatorUtil.checkFilter(paramArray)) {
            return;
        }
        super.enter(paramArray[0]);
    }

    @Override
    public void leave(Object parameters) {
        Object[] paramArray = (Object[]) parameters;
        if (!AggregatorUtil.checkFilter(paramArray)) {
            return;
        }
        super.leave(paramArray[0]);
    }
}
