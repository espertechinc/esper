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

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.lookup.EventAdvancedIndexConfigStatement;

public class AdvancedIndexConfigStatementPointRegionQuadtree implements EventAdvancedIndexConfigStatement {
    private ExprEvaluator xEval;
    private ExprEvaluator yEval;

    public ExprEvaluator getxEval() {
        return xEval;
    }

    public ExprEvaluator getyEval() {
        return yEval;
    }

    public void setxEval(ExprEvaluator xEval) {
        this.xEval = xEval;
    }

    public void setyEval(ExprEvaluator yEval) {
        this.yEval = yEval;
    }
}
