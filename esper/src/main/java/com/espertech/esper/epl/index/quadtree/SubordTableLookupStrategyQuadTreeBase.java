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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;

import java.util.Collection;

public class SubordTableLookupStrategyQuadTreeBase {
    private final EventTableQuadTree index;
    private final SubordTableLookupStrategyFactoryQuadTree factory;

    public SubordTableLookupStrategyQuadTreeBase(EventTableQuadTree index, SubordTableLookupStrategyFactoryQuadTree factory) {
        this.index = index;
        this.factory = factory;
    }

    protected Collection<EventBean> lookupInternal(EventBean[] events, ExprEvaluatorContext context) {
        double x = eval(factory.getX(), events, context, "x");
        double y = eval(factory.getY(), events, context, "y");
        double width = eval(factory.getWidth(), events, context, "width");
        double height = eval(factory.getHeight(), events, context, "height");
        return index.queryRange(x, y, width, height);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public LookupStrategyDesc getStrategyDesc() {
        return factory.getLookupStrategyDesc();
    }

    private double eval(ExprEvaluator eval, EventBean[] events, ExprEvaluatorContext context, String name) {
        Number number = (Number) eval.evaluate(events, true, context);
        if (number == null) {
            throw new EPException("Invalid null value for '" + name + "'");
        }
        return number.doubleValue();
    }
}
