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
package com.espertech.esper.common.internal.view.access;

import com.espertech.esper.common.internal.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.common.internal.epl.expression.prior.ExprPriorNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates between view factories and requested resource (by expressions) the
 * availability of view resources to expressions.
 */
public class ViewResourceDelegateExpr {
    private final List<ExprPriorNode> priorRequests;
    private final List<ExprPreviousNode> previousRequests;

    public ViewResourceDelegateExpr() {
        this.priorRequests = new ArrayList<>();
        this.previousRequests = new ArrayList<>();
    }

    public List<ExprPriorNode> getPriorRequests() {
        return priorRequests;
    }

    public void addPriorNodeRequest(ExprPriorNode priorNode) {
        priorRequests.add(priorNode);
    }

    public void addPreviousRequest(ExprPreviousNode previousNode) {
        previousRequests.add(previousNode);
    }

    public List<ExprPreviousNode> getPreviousRequests() {
        return previousRequests;
    }
}
