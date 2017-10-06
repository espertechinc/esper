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
package com.espertech.esper.epl.agg.util;

import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.expression.core.ExprForge;

public class AggregationLocalGroupByLevelForge {

    private final ExprForge[][] methodForges;
    private final AggregationMethodFactory[] methodFactories;
    private final AggregationStateFactoryForge[] accessStateForges;
    private final ExprForge[] partitionForges;
    private final boolean isDefaultLevel;

    public AggregationLocalGroupByLevelForge(ExprForge[][] methodForges, AggregationMethodFactory[] methodFactories, AggregationStateFactoryForge[] accessStateForges, ExprForge[] partitionForges, boolean defaultLevel) {
        this.methodForges = methodForges;
        this.methodFactories = methodFactories;
        this.accessStateForges = accessStateForges;
        this.partitionForges = partitionForges;
        isDefaultLevel = defaultLevel;
    }

    public ExprForge[][] getMethodForges() {
        return methodForges;
    }

    public AggregationMethodFactory[] getMethodFactories() {
        return methodFactories;
    }

    public AggregationStateFactoryForge[] getAccessStateForges() {
        return accessStateForges;
    }

    public ExprForge[] getPartitionForges() {
        return partitionForges;
    }

    public boolean isDefaultLevel() {
        return isDefaultLevel;
    }
}
