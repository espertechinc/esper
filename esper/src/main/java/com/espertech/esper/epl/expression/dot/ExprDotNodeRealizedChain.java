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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.epl.join.plan.FilterExprAnalyzerAffector;

public class ExprDotNodeRealizedChain {
    private final ExprDotForge[] chain;
    private final ExprDotForge[] chainWithUnpack;
    private final FilterExprAnalyzerAffector filterAnalyzerDesc;

    public ExprDotNodeRealizedChain(ExprDotForge[] chain, ExprDotForge[] chainWithUnpack, FilterExprAnalyzerAffector filterAnalyzerDesc) {
        this.chain = chain;
        this.chainWithUnpack = chainWithUnpack;
        this.filterAnalyzerDesc = filterAnalyzerDesc;
    }

    public ExprDotForge[] getChain() {
        return chain;
    }

    public ExprDotForge[] getChainWithUnpack() {
        return chainWithUnpack;
    }

    public FilterExprAnalyzerAffector getFilterAnalyzerDesc() {
        return filterAnalyzerDesc;
    }
}
