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

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.epl.lookup.*;

import java.util.Map;

public abstract class EventAdvancedIndexFactoryQuadTree implements EventAdvancedIndexFactory {

    public AdvancedIndexConfigContextPartition configureContextPartition(EventType eventType, AdvancedIndexDesc indexDesc, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext, EventTableOrganization organization, EventAdvancedIndexConfigStatement advancedIndexConfigStatement) {
        return AdvancedIndexFactoryProviderQuadTree.configureQuadTree(organization.getIndexName(), parameters, exprEvaluatorContext);
    }

    public SubordTableLookupStrategyFactoryQuadTree getSubordinateLookupStrategy(String operationName, Map<Integer, ExprNode> positionalExpressions, boolean isNWOnTrigger, int numOuterstreams) {
        ExprEvaluator x = positionalExpressions.get(0).getForge().getExprEvaluator();
        ExprEvaluator y = positionalExpressions.get(1).getForge().getExprEvaluator();
        ExprEvaluator width = positionalExpressions.get(2).getForge().getExprEvaluator();
        ExprEvaluator height = positionalExpressions.get(3).getForge().getExprEvaluator();
        String[] expressions = new String[positionalExpressions.size()];
        for (Map.Entry<Integer, ExprNode> entry : positionalExpressions.entrySet()) {
            expressions[entry.getKey()] = ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(entry.getValue());
        }
        LookupStrategyDesc lookupStrategyDesc = new LookupStrategyDesc(LookupStrategyType.ADVANCED, expressions);
        return new SubordTableLookupStrategyFactoryQuadTree(x, y, width, height, isNWOnTrigger, numOuterstreams, lookupStrategyDesc);
    }
}
