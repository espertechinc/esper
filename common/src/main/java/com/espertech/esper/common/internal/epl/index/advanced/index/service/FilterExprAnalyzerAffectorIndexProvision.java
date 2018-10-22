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
package com.espertech.esper.common.internal.epl.index.advanced.index.service;

import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzerAffector;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;

import java.util.List;

public class FilterExprAnalyzerAffectorIndexProvision implements FilterExprAnalyzerAffector {
    private final String operationName;
    private final ExprNode[] indexExpressions;
    private final List<Pair<ExprNode, int[]>> keyExpressions;
    private final int streamNumIndex;

    public FilterExprAnalyzerAffectorIndexProvision(String operationName, ExprNode[] indexExpressions, List<Pair<ExprNode, int[]>> keyExpressions, int streamNumIndex) {
        this.operationName = operationName;
        this.indexExpressions = indexExpressions;
        this.keyExpressions = keyExpressions;
        this.streamNumIndex = streamNumIndex;
    }

    public void apply(QueryGraphForge queryGraph) {
        queryGraph.addCustomIndex(operationName, indexExpressions, keyExpressions, streamNumIndex);
    }
}
