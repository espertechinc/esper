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
package com.espertech.esper.common.internal.epl.index.advanced.index.quadtree;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyType;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactory;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

public class SubordTableLookupStrategyFactoryQuadTree implements SubordTableLookupStrategyFactory {

    private String[] lookupExpressions;
    private ExprEvaluator x;
    private ExprEvaluator y;
    private ExprEvaluator width;
    private ExprEvaluator height;
    private boolean isNWOnTrigger;
    private int streamCountOuter;

    public SubordTableLookupStrategyFactoryQuadTree() {
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
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

    public void setLookupExpressions(String[] lookupExpressions) {
        this.lookupExpressions = lookupExpressions;
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(LookupStrategyType.ADVANCED, lookupExpressions);
    }

    public void setX(ExprEvaluator x) {
        this.x = x;
    }

    public void setY(ExprEvaluator y) {
        this.y = y;
    }

    public void setWidth(ExprEvaluator width) {
        this.width = width;
    }

    public void setHeight(ExprEvaluator height) {
        this.height = height;
    }

    public void setNWOnTrigger(boolean nwOnTrigger) {
        isNWOnTrigger = nwOnTrigger;
    }

    public void setStreamCountOuter(int streamCountOuter) {
        this.streamCountOuter = streamCountOuter;
    }
}
