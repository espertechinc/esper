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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.HashMap;
import java.util.Map;

public class QueryGraphValueEntryCustomOperation implements QueryGraphValueEntry {
    private Map<Integer, ExprEvaluator> positionalExpressions = new HashMap<>();

    public void setPositionalExpressions(Map<Integer, ExprEvaluator> positionalExpressions) {
        this.positionalExpressions = positionalExpressions;
    }

    public Map<Integer, ExprEvaluator> getPositionalExpressions() {
        return positionalExpressions;
    }
}

