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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;

public class ExprNodeUtilExprStreamNumEnumCollEval implements ExprEvaluator {
    private final ExprEnumerationEval enumeration;

    public ExprNodeUtilExprStreamNumEnumCollEval(ExprEnumerationEval enumeration) {
        this.enumeration = enumeration;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return enumeration.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
    }

}
