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

import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

import java.io.StringWriter;
import java.util.List;

public abstract class QueryGraphValueEntryHashKeyedForge implements QueryGraphValueEntryForge {
    private final ExprNode keyExpr;

    public QueryGraphValueEntryHashKeyedForge(ExprNode keyExpr) {
        this.keyExpr = keyExpr;
    }

    public ExprNode getKeyExpr() {
        return keyExpr;
    }

    public abstract String toQueryPlan();

    public static String toQueryPlan(List<QueryGraphValueEntryHashKeyedForge> keyProperties) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (QueryGraphValueEntryHashKeyedForge item : keyProperties) {
            writer.write(delimiter);
            writer.write(item.toQueryPlan());
            delimiter = ", ";
        }
        return writer.toString();
    }

    public static EventPropertyGetterSPI[] getGettersIfPropsOnly(QueryGraphValueEntryHashKeyedForge[] keys) {
        if (keys == null) {
            return null;
        }
        EventPropertyGetterSPI[] getterSPIS = new EventPropertyGetterSPI[keys.length];
        for (int i = 0; i < keys.length; i++) {
            if (!(keys[i] instanceof QueryGraphValueEntryHashKeyedForgeProp)) {
                return null;
            }
            getterSPIS[i] = ((QueryGraphValueEntryHashKeyedForgeProp) keys[i]).getEventPropertyGetter();
        }
        return getterSPIS;
    }

    public static ExprForge[] getForges(QueryGraphValueEntryHashKeyedForge[] keys) {
        if (keys == null) {
            return null;
        }
        ExprForge[] forges = new ExprForge[keys.length];
        for (int i = 0; i < keys.length; i++) {
            forges[i] = keys[i].getKeyExpr().getForge();
        }
        return forges;
    }
}
