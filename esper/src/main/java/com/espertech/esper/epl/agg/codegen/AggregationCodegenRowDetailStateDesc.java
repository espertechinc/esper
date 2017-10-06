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
package com.espertech.esper.epl.agg.codegen;

import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.expression.core.ExprForge;

public class AggregationCodegenRowDetailStateDesc {
    private final ExprForge[][] methodForges;
    private final AggregationMethodFactory[] methodFactories;
    private final AggregationStateFactoryForge[] accessStateForges;

    public AggregationCodegenRowDetailStateDesc(ExprForge[][] methodForges, AggregationMethodFactory[] methodFactories, AggregationStateFactoryForge[] accessStateForges) {
        this.methodForges = methodForges;
        this.methodFactories = methodFactories;
        this.accessStateForges = accessStateForges;
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
}
