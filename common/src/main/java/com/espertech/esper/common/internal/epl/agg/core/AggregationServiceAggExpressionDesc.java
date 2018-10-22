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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;

import java.util.ArrayList;
import java.util.List;

public class AggregationServiceAggExpressionDesc {
    private ExprAggregateNode aggregationNode;
    private AggregationForgeFactory factory;

    private List<ExprAggregateNode> equivalentNodes;

    /**
     * Ctor.
     *
     * @param aggregationNode expression
     * @param factory         method factory
     */
    public AggregationServiceAggExpressionDesc(ExprAggregateNode aggregationNode, AggregationForgeFactory factory) {
        this.aggregationNode = aggregationNode;
        this.factory = factory;
    }

    /**
     * Returns the equivalent aggregation functions.
     *
     * @return list of agg nodes
     */
    public List<ExprAggregateNode> getEquivalentNodes() {
        return equivalentNodes;
    }

    /**
     * Returns the method factory.
     *
     * @return factory
     */
    public AggregationForgeFactory getFactory() {
        return factory;
    }

    /**
     * Assigns a column number.
     *
     * @param columnNum column number
     */
    public void setColumnNum(Integer columnNum) {
        aggregationNode.setColumn(columnNum);
        if (equivalentNodes != null) {
            for (ExprAggregateNode node : equivalentNodes) {
                node.setColumn(columnNum);
            }
        }
    }

    /**
     * Add an equivalent aggregation function node
     *
     * @param aggNodeToAdd node to add
     */
    public void addEquivalent(ExprAggregateNode aggNodeToAdd) {
        if (equivalentNodes == null) {
            equivalentNodes = new ArrayList<ExprAggregateNode>();
        }
        equivalentNodes.add(aggNodeToAdd);
    }

    /**
     * Returns the expression.
     *
     * @return expression
     */
    public ExprAggregateNode getAggregationNode() {
        return aggregationNode;
    }
}
