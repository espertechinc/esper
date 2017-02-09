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
package com.espertech.esper.epl.expression.prior;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.view.window.RelativeAccessByEventNIndex;

/**
 * Represents the 'prior' prior event function in an expression node tree.
 */
public class ExprPriorEvalStrategyRelativeAccess extends ExprPriorEvalStrategyBase {
    private final transient RelativeAccessByEventNIndex relativeAccess;

    public ExprPriorEvalStrategyRelativeAccess(RelativeAccessByEventNIndex relativeAccess) {
        this.relativeAccess = relativeAccess;
    }

    public EventBean getSubstituteEvent(EventBean originalEvent, boolean isNewData, int constantIndexNumber) {
        return relativeAccess.getRelativeToEvent(originalEvent, constantIndexNumber);
    }
}
