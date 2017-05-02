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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.index.quadtree.SubordTableLookupStrategyFactoryQuadTree;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;

import java.util.Map;

public interface EventAdvancedIndexFactory {
    AdvancedIndexConfigContextPartition configureContextPartition(EventType eventType, AdvancedIndexDesc indexDesc, ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext, EventTableOrganization organization, EventAdvancedIndexConfigStatement advancedIndexConfigStatement);
    EventTable make(EventAdvancedIndexConfigStatement configStatement, AdvancedIndexConfigContextPartition configContextPartition, EventTableOrganization organization);
    boolean providesIndexForOperation(String operationName, Map<Integer, ExprNode> expressions);
    SubordTableLookupStrategyFactoryQuadTree getSubordinateLookupStrategy(String operationName, Map<Integer, ExprNode> expressions, boolean isNWOnTrigger, int numOuterstreams);
}
