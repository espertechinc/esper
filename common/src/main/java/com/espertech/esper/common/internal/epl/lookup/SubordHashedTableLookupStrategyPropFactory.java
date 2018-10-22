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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTable;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWView;

import java.util.Arrays;

/**
 * Index lookup strategy for subqueries.
 */
public class SubordHashedTableLookupStrategyPropFactory implements SubordTableLookupStrategyFactory {
    private final String[] properties;
    private final int[] keyStreamNums;
    protected final ExprEvaluator evaluator;

    public SubordHashedTableLookupStrategyPropFactory(String[] properties, int[] keyStreamNums, ExprEvaluator evaluator) {
        this.properties = properties;
        this.keyStreamNums = keyStreamNums;
        this.evaluator = evaluator;
    }

    public SubordTableLookupStrategy makeStrategy(EventTable[] eventTable, AgentInstanceContext agentInstanceContext, VirtualDWView vdw) {
        return new SubordHashedTableLookupStrategyProp(this, (PropertyHashedEventTable) eventTable[0]);
    }

    /**
     * Returns properties to use from lookup event to look up in index.
     *
     * @return properties to use from lookup event
     */
    public String[] getProperties() {
        return properties;
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName() +
                " indexProps=" + Arrays.toString(properties) +
                " keyStreamNums=" + Arrays.toString(keyStreamNums);
    }

    public LookupStrategyDesc getLookupStrategyDesc() {
        return new LookupStrategyDesc(properties.length == 1 ? LookupStrategyType.SINGLEPROP : LookupStrategyType.MULTIPROP, properties);
    }

    public int[] getKeyStreamNums() {
        return keyStreamNums;
    }
}
