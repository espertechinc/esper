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

public class QueryGraphValueEntryHashKeyedProp extends QueryGraphValueEntryHashKeyed {
    private static final long serialVersionUID = -3745044093486590108L;

    private final String keyProperty;

    public QueryGraphValueEntryHashKeyedProp(ExprNode keyExpr, String keyProperty) {
        super(keyExpr);
        this.keyProperty = keyProperty;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public String toQueryPlan() {
        return keyProperty;
    }
}

