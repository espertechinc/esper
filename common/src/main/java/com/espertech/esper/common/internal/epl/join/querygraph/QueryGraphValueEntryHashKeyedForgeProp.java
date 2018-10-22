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

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;

public class QueryGraphValueEntryHashKeyedForgeProp extends QueryGraphValueEntryHashKeyedForgeExpr {
    private final String keyProperty;
    private final EventPropertyGetterSPI eventPropertyGetter;

    public QueryGraphValueEntryHashKeyedForgeProp(ExprNode keyExpr, String keyProperty, EventPropertyGetterSPI eventPropertyGetter) {
        super(keyExpr, true);
        this.keyProperty = keyProperty;
        this.eventPropertyGetter = eventPropertyGetter;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public String toQueryPlan() {
        return keyProperty;
    }

    public EventPropertyGetterSPI getEventPropertyGetter() {
        return eventPropertyGetter;
    }
}

