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
package com.espertech.esper.regressionlib.support.epl;

import com.espertech.esper.common.internal.epl.expression.core.ExprConstantNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.querygraph.*;

public class SupportExprNodeFactory {
    public static QueryGraphValueEntryHashKeyedForge makeKeyed(String property) {
        return new QueryGraphValueEntryHashKeyedForgeExpr(new ExprIdentNodeImpl(property), false);
    }

    public static QueryGraphValueEntryRangeForge makeRangeLess(String prop) {
        return new QueryGraphValueEntryRangeRelOpForge(QueryGraphRangeEnum.LESS, new ExprIdentNodeImpl(prop), false);
    }

    public static QueryGraphValueEntryRangeInForge makeRangeIn(String start, String end) {
        return new QueryGraphValueEntryRangeInForge(QueryGraphRangeEnum.RANGE_OPEN, new ExprIdentNodeImpl(start), new ExprIdentNodeImpl(end), false);
    }

    public static ExprNode[] makeIdentExprNodes(String... props) {
        ExprNode[] nodes = new ExprNode[props.length];
        for (int i = 0; i < props.length; i++) {
            nodes[i] = new ExprIdentNodeImpl(props[i]);
        }
        return nodes;
    }

    public static ExprNode[] makeConstAndIdentNode(String constant, String property) {
        return new ExprNode[]{new ExprConstantNodeImpl(constant), new ExprIdentNodeImpl(property)};
    }

    public static ExprNode[] makeConstAndConstNode(String constantOne, String constantTwo) {
        return new ExprNode[]{new ExprConstantNodeImpl(constantOne), new ExprConstantNodeImpl(constantTwo)};
    }

    public static ExprNode makeIdentExprNode(String property) {
        return new ExprIdentNodeImpl(property);
    }

    public static ExprNode makeConstExprNode(String constant) {
        return new ExprConstantNodeImpl(constant);
    }
}
