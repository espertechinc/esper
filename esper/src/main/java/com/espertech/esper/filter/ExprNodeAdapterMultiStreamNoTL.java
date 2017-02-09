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
package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.variable.VariableService;

public class ExprNodeAdapterMultiStreamNoTL extends ExprNodeAdapterMultiStream {
    public ExprNodeAdapterMultiStreamNoTL(int filterSpecId, int filterSpecParamPathNum, ExprNode exprNode, ExprEvaluatorContext evaluatorContext, VariableService variableService, EventBean[] prototype) {
        super(filterSpecId, filterSpecParamPathNum, exprNode, evaluatorContext, variableService, prototype);
    }

    @Override
    public boolean evaluate(EventBean theEvent) {
        if (variableService != null) {
            variableService.setLocalVersion();
        }

        EventBean[] eventsPerStream = new EventBean[prototypeArray.length];
        System.arraycopy(prototypeArray, 0, eventsPerStream, 0, prototypeArray.length);
        eventsPerStream[0] = theEvent;
        return super.evaluatePerStream(eventsPerStream);
    }
}
