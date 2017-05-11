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

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.epl.lookup.SubordTableLookupStrategyFactory;
import com.espertech.esper.epl.virtualdw.VirtualDWView;

public class SubordTableLookupStrategyFactoryQuadTree implements SubordTableLookupStrategyFactory {

    private final ExprEvaluator x;
    private final ExprEvaluator y;
    private final ExprEvaluator width;
    private final ExprEvaluator height;
    private final boolean isNWOnTrigger;
    private final int streamCountOuter;
    private final LookupStrategyDesc lookupStrategyDesc;

    public SubordTableLookupStrategyFactoryQuadTree(ExprEvaluator x, ExprEvaluator y, ExprEvaluator width, ExprEvaluator height, boolean isNWOnTrigger, int streamCountOuter, LookupStrategyDesc lookupStrategyDesc) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.isNWOnTrigger = isNWOnTrigger;
        this.streamCountOuter = streamCountOuter;
        this.lookupStrategyDesc = lookupStrategyDesc;
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, VirtualDWView vdw) {
        if (isNWOnTrigger) {
            return new SubordTableLookupStrategyQuadTreeNW((EventTableQuadTree) eventTable[0], this);
        }
        return new SubordTableLookupStrategyQuadTreeSubq((EventTableQuadTree) eventTable[0], this, streamCountOuter);
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    public ExprEvaluator getX() {
        return x;
    }

    public ExprEvaluator getY() {
        return y;
    }

    public ExprEvaluator getWidth() {
        return width;
    }

    public ExprEvaluator getHeight() {
        return height;
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return lookupStrategyDesc;
    }
}
