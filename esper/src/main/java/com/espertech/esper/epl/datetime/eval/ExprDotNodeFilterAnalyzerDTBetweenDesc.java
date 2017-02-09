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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.join.util.RangeFilterAnalyzer;

public class ExprDotNodeFilterAnalyzerDTBetweenDesc implements ExprDotNodeFilterAnalyzerDesc {
    private final EventType[] typesPerStream;
    private final int targetStreamNum;
    private final String targetPropertyName;
    private final ExprNode start;
    private final ExprNode end;
    private final boolean includeLow;
    private final boolean includeHigh;

    public ExprDotNodeFilterAnalyzerDTBetweenDesc(EventType[] typesPerStream, int targetStreamNum, String targetPropertyName, ExprNode start, ExprNode end, boolean includeLow, boolean includeHigh) {
        this.typesPerStream = typesPerStream;
        this.targetStreamNum = targetStreamNum;
        this.targetPropertyName = targetPropertyName;
        this.start = start;
        this.end = end;
        this.includeLow = includeLow;
        this.includeHigh = includeHigh;
    }

    public void apply(QueryGraph queryGraph) {
        ExprIdentNode targetExpr = ExprNodeUtility.getExprIdentNode(typesPerStream, targetStreamNum, targetPropertyName);
        RangeFilterAnalyzer.apply(targetExpr, start, end, includeLow, includeHigh, false, queryGraph);
    }
}

