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
package com.espertech.esper.common.internal.epl.expression.prior;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

/**
 * Represents the 'prior' prior event resolution strategy for use in an expression node tree.
 */
public interface PriorEvalStrategy {
    PriorEvalStrategy[] EMPTY_ARRAY = new PriorEvalStrategy[0];

    EventBean getSubstituteEvent(EventBean originalEvent, boolean isNewData, int constantIndexNumber, int relativeIndex, ExprEvaluatorContext exprEvaluatorContext, int streamNum);
}
