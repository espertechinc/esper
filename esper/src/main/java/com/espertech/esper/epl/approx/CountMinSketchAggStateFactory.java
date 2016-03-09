/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.approx;

import com.espertech.esper.epl.agg.access.AggregationServicePassThru;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.core.MethodResolutionService;
import com.espertech.esper.epl.expression.accessagg.ExprAggCountMinSketchNode;
import com.espertech.esper.epl.expression.core.ExprNode;

public class CountMinSketchAggStateFactory implements AggregationStateFactory
{
    private final ExprAggCountMinSketchNode parent;
    private final CountMinSketchSpec specification;

    public CountMinSketchAggStateFactory(ExprAggCountMinSketchNode parent, CountMinSketchSpec specification) {
        this.parent = parent;
        this.specification = specification;
    }

    public AggregationState createAccess(MethodResolutionService methodResolutionService, int agentInstanceId, int groupId, int aggregationId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        return methodResolutionService.makeCountMinSketch(agentInstanceId, groupId, aggregationId, specification);
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
