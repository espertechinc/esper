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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.approx.CountMinSketchAggState;
import com.espertech.esper.epl.approx.CountMinSketchSpec;
import com.espertech.esper.epl.approx.CountMinSketchState;
import com.espertech.esper.epl.expression.accessagg.ExprAggCountMinSketchNode;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateFactoryCountMinSketch implements AggregationStateFactory {
    protected final ExprAggCountMinSketchNode parent;
    protected final CountMinSketchSpec specification;

    public AggregationStateFactoryCountMinSketch(ExprAggCountMinSketchNode parent, CountMinSketchSpec specification) {
        this.parent = parent;
        this.specification = specification;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        return new CountMinSketchAggState(CountMinSketchState.makeState(specification), specification.getAgent());
    }

    public ExprNode getAggregationExpression() {
        return parent;
    }

    public CountMinSketchSpec getSpecification() {
        return specification;
    }

    public ExprAggCountMinSketchNode getParent() {
        return parent;
    }
}
