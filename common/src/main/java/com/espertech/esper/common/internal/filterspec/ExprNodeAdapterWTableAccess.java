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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.table.core.TableExprEvaluatorContext;

public final class ExprNodeAdapterWTableAccess extends ExprNodeAdapterBase {
    private final ExprNodeAdapterBase evalBase;
    private final TableExprEvaluatorContext tableExprEvaluatorContext;

    public ExprNodeAdapterWTableAccess(FilterSpecParamExprNode factory, ExprEvaluatorContext evaluatorContext, ExprNodeAdapterBase evalBase, TableExprEvaluatorContext tableExprEvaluatorContext) {
        super(factory, evaluatorContext);
        this.evalBase = evalBase;
        this.tableExprEvaluatorContext = tableExprEvaluatorContext;
    }

    public boolean evaluate(EventBean theEvent) {
        try {
            return evalBase.evaluate(theEvent);
        } finally {
            tableExprEvaluatorContext.releaseAcquiredLocks();
        }
    }
}
