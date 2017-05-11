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
package com.espertech.esper.epl.index.quadtree;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.lookup.EventAdvancedIndexConfigStatement;

public class AdvancedIndexConfigStatementMXCIFQuadtree implements EventAdvancedIndexConfigStatement {
    private final ExprEvaluator xEval;
    private final ExprEvaluator yEval;
    private final ExprEvaluator widthEval;
    private final ExprEvaluator heightEval;

    public AdvancedIndexConfigStatementMXCIFQuadtree(ExprEvaluator xEval, ExprEvaluator yEval, ExprEvaluator widthEval, ExprEvaluator heightEval) {
        this.xEval = xEval;
        this.yEval = yEval;
        this.widthEval = widthEval;
        this.heightEval = heightEval;
    }

    public ExprEvaluator getxEval() {
        return xEval;
    }

    public ExprEvaluator getyEval() {
        return yEval;
    }

    public ExprEvaluator getWidthEval() {
        return widthEval;
    }

    public ExprEvaluator getHeightEval() {
        return heightEval;
    }
}
