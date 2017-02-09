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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collections;
import java.util.List;

/**
 * End state in the regex NFA states.
 */
public class RegexNFAStateEnd extends RegexNFAStateBase {
    /**
     * Ctor.
     */
    public RegexNFAStateEnd() {
        super("endstate", null, -1, false, null);
    }

    public boolean matches(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException();
    }

    public List<RegexNFAState> getNextStates() {
        return Collections.EMPTY_LIST;
    }

    public boolean isExprRequiresMultimatchState() {
        throw new UnsupportedOperationException();
    }
}
