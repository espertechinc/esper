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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDescForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.util.List;

public class GroupByRollupInfo {
    private final ExprNode[] exprNodes;
    private final AggregationGroupByRollupDescForge rollupDesc;
    private final List<StmtClassForgeableFactory> additionalForgeables;
    private final MultiKeyClassRef optionalMultiKey;

    public GroupByRollupInfo(ExprNode[] exprNodes, AggregationGroupByRollupDescForge rollupDesc, List<StmtClassForgeableFactory> additionalForgeables, MultiKeyClassRef optionalMultiKey) {
        this.exprNodes = exprNodes;
        this.rollupDesc = rollupDesc;
        this.additionalForgeables = additionalForgeables;
        this.optionalMultiKey = optionalMultiKey;
    }

    public ExprNode[] getExprNodes() {
        return exprNodes;
    }

    public AggregationGroupByRollupDescForge getRollupDesc() {
        return rollupDesc;
    }

    public List<StmtClassForgeableFactory> getAdditionalForgeables() {
        return additionalForgeables;
    }

    public MultiKeyClassRef getOptionalMultiKey() {
        return optionalMultiKey;
    }
}
