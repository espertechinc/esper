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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.type.OuterJoinType;

/**
 * Analyzes an outer join descriptor list and builds a query graph model from it.
 * The 'on' expression identifiers are extracted
 * and placed in the query graph model as navigable relationships (by key and index
 * properties) between streams.
 */
public class OuterJoinAnalyzer {
    /**
     * Analyzes the outer join descriptor list to build a query graph model.
     *
     * @param outerJoinDescList - list of outer join descriptors
     * @param queryGraph        - model containing relationships between streams that is written into
     * @return queryGraph object
     */
    public static QueryGraph analyze(OuterJoinDesc[] outerJoinDescList, QueryGraph queryGraph) {
        for (OuterJoinDesc outerJoinDesc : outerJoinDescList) {
            // add optional on-expressions
            if (outerJoinDesc.getOptLeftNode() != null) {
                ExprIdentNode identNodeLeft = outerJoinDesc.getOptLeftNode();
                ExprIdentNode identNodeRight = outerJoinDesc.getOptRightNode();

                add(queryGraph, identNodeLeft, identNodeRight);

                if (outerJoinDesc.getAdditionalLeftNodes() != null) {
                    for (int i = 0; i < outerJoinDesc.getAdditionalLeftNodes().length; i++) {
                        add(queryGraph, outerJoinDesc.getAdditionalLeftNodes()[i], outerJoinDesc.getAdditionalRightNodes()[i]);
                    }
                }
            }
        }

        return queryGraph;
    }

    private static void add(QueryGraph queryGraph, ExprIdentNode identNodeLeft, ExprIdentNode identNodeRight) {
        queryGraph.addStrictEquals(identNodeLeft.getStreamId(), identNodeLeft.getResolvedPropertyName(), identNodeLeft,
                identNodeRight.getStreamId(), identNodeRight.getResolvedPropertyName(), identNodeRight);
    }

    public static boolean optionalStreamsIfAny(OuterJoinDesc[] outerJoinDescList) {
        if (outerJoinDescList == null || outerJoinDescList.length == 0) {
            return false;
        }
        for (OuterJoinDesc outerJoinDesc : outerJoinDescList) {
            if (outerJoinDesc.getOuterJoinType() != OuterJoinType.INNER) {
                return true;
            }
        }
        return false;
    }
}
