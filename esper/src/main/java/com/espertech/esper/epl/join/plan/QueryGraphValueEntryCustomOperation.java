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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.HashMap;
import java.util.Map;

public class QueryGraphValueEntryCustomOperation implements QueryGraphValueEntry {
    private final Map<Integer, ExprNode> positionalExpressions = new HashMap<>();

    public Map<Integer, ExprNode> getPositionalExpressions() {
        return positionalExpressions;
    }
}

