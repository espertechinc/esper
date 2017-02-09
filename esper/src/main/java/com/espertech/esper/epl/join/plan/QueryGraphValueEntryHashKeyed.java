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

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

public abstract class QueryGraphValueEntryHashKeyed implements QueryGraphValueEntry, Serializable {
    private static final long serialVersionUID = -2005004980276270795L;

    private final ExprNode keyExpr;

    public QueryGraphValueEntryHashKeyed(ExprNode keyExpr) {
        this.keyExpr = keyExpr;
    }

    public ExprNode getKeyExpr() {
        return keyExpr;
    }

    public abstract String toQueryPlan();

    public static String toQueryPlan(List<QueryGraphValueEntryHashKeyed> keyProperties) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (QueryGraphValueEntryHashKeyed item : keyProperties) {
            writer.write(delimiter);
            writer.write(item.toQueryPlan());
            delimiter = ", ";
        }
        return writer.toString();
    }

}

