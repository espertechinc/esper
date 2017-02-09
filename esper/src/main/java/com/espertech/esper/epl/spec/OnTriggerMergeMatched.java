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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.io.Serializable;
import java.util.List;

/**
 * Specification for the merge statement insert/update/delete-part.
 */
public class OnTriggerMergeMatched implements Serializable {
    private static final long serialVersionUID = -8789870272699226779L;

    private final boolean matchedUnmatched;
    private ExprNode optionalMatchCond;
    private final List<OnTriggerMergeAction> actions;

    public OnTriggerMergeMatched(boolean matchedUnmatched, ExprNode optionalMatchCond, List<OnTriggerMergeAction> actions) {
        this.matchedUnmatched = matchedUnmatched;
        this.optionalMatchCond = optionalMatchCond;
        this.actions = actions;
    }

    public ExprNode getOptionalMatchCond() {
        return optionalMatchCond;
    }

    public void setOptionalMatchCond(ExprNode optionalMatchCond) {
        this.optionalMatchCond = optionalMatchCond;
    }

    public boolean isMatchedUnmatched() {
        return matchedUnmatched;
    }

    public List<OnTriggerMergeAction> getActions() {
        return actions;
    }
}

