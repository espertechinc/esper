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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

public class GroupByClauseExpressions {
    private final ExprNode[] groupByNodes;
    private final int[][] groupByRollupLevels;
    private final ExprNode[][] selectClausePerLevel;
    private final ExprNode[] optHavingNodePerLevel;
    private final ExprNode[][] optOrderByPerLevel;

    public GroupByClauseExpressions(ExprNode[] groupByNodes) {
        this(groupByNodes, null, null, null, null);
    }

    public GroupByClauseExpressions(ExprNode[] groupByNodes, int[][] groupByRollupLevels, ExprNode[][] selectClauseCopy, ExprNode[] optHavingNodeCopy, ExprNode[][] optOrderByPerLevel) {
        this.groupByNodes = groupByNodes;
        this.groupByRollupLevels = groupByRollupLevels;
        this.selectClausePerLevel = selectClauseCopy;
        this.optHavingNodePerLevel = optHavingNodeCopy;
        this.optOrderByPerLevel = optOrderByPerLevel;
    }

    public int[][] getGroupByRollupLevels() {
        return groupByRollupLevels;
    }

    public ExprNode[][] getSelectClausePerLevel() {
        return selectClausePerLevel;
    }

    public ExprNode[][] getOptOrderByPerLevel() {
        return optOrderByPerLevel;
    }

    public ExprNode[] getOptHavingNodePerLevel() {
        return optHavingNodePerLevel;
    }

    public ExprNode[] getGroupByNodes() {
        return groupByNodes;
    }
}
