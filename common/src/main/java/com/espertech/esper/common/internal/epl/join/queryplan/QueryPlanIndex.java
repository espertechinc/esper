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
package com.espertech.esper.common.internal.epl.join.queryplan;

import java.util.Map;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndex {
    private Map<TableLookupIndexReqKey, QueryPlanIndexItem> items;

    public QueryPlanIndex(Map<TableLookupIndexReqKey, QueryPlanIndexItem> items) {
        this.items = items;
    }

    public Map<TableLookupIndexReqKey, QueryPlanIndexItem> getItems() {
        return items;
    }
}
