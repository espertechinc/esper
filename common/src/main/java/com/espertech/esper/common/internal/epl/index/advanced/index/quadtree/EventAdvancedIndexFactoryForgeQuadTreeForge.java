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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.lookup.*;

import java.util.Map;

public abstract class EventAdvancedIndexFactoryForgeQuadTreeForge implements EventAdvancedIndexFactoryForge {

    public AdvancedIndexConfigContextPartition configureContextPartition(EventType eventType, AdvancedIndexDescWExpr indexDesc, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext, EventTableOrganization organization, EventAdvancedIndexConfigStatementForge advancedIndexConfigStatement) {
        return AdvancedIndexFactoryProviderQuadTree.configureQuadTree(organization.getIndexName(), parameters, exprEvaluatorContext);
    }

    public SubordTableLookupStrategyFactoryQuadTreeForge getSubordinateLookupStrategy(String operationName, Map<Integer, ExprNode> positionalExpressions, boolean isNWOnTrigger, int numOuterstreams) {
        ExprForge x = positionalExpressions.get(0).getForge();
        ExprForge y = positionalExpressions.get(1).getForge();
        ExprForge width = positionalExpressions.get(2).getForge();
        ExprForge height = positionalExpressions.get(3).getForge();
        String[] expressions = new String[positionalExpressions.size()];
        for (Map.Entry<Integer, ExprNode> entry : positionalExpressions.entrySet()) {
            expressions[entry.getKey()] = ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(entry.getValue());
        }
        LookupStrategyDesc lookupStrategyDesc = new LookupStrategyDesc(LookupStrategyType.ADVANCED, expressions);
        return new SubordTableLookupStrategyFactoryQuadTreeForge(x, y, width, height, isNWOnTrigger, numOuterstreams, lookupStrategyDesc);
    }
}
