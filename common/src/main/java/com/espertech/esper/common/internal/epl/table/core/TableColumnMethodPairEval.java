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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

public class TableColumnMethodPairEval {
    private final ExprEvaluator evaluator;
    private final int column;

    public TableColumnMethodPairEval(ExprEvaluator evaluator, int column) {
        this.evaluator = evaluator;
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    public ExprEvaluator getEvaluator() {
        return evaluator;
    }
}
