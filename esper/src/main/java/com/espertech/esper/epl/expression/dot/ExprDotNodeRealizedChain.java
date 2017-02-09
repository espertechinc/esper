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

import com.espertech.esper.epl.datetime.eval.ExprDotNodeFilterAnalyzerDesc;

public class ExprDotNodeRealizedChain {
    private final ExprDotEval[] chain;
    private final ExprDotEval[] chainWithUnpack;
    private final ExprDotNodeFilterAnalyzerDesc filterAnalyzerDesc;

    public ExprDotNodeRealizedChain(ExprDotEval[] chain, ExprDotEval[] chainWithUnpack, ExprDotNodeFilterAnalyzerDesc filterAnalyzerDesc) {
        this.chain = chain;
        this.chainWithUnpack = chainWithUnpack;
        this.filterAnalyzerDesc = filterAnalyzerDesc;
    }

    public ExprDotEval[] getChain() {
        return chain;
    }

    public ExprDotEval[] getChainWithUnpack() {
        return chainWithUnpack;
    }

    public ExprDotNodeFilterAnalyzerDesc getFilterAnalyzerDesc() {
        return filterAnalyzerDesc;
    }
}
