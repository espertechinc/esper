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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;

public class SubordinateQueryIndexDesc {
    private final IndexKeyInfo indexKeyInfo;
    private final String indexName;
    private final IndexMultiKey indexMultiKey;
    private final QueryPlanIndexItem queryPlanIndexItem;

    public SubordinateQueryIndexDesc(IndexKeyInfo indexKeyInfo, String indexName, IndexMultiKey indexMultiKey, QueryPlanIndexItem queryPlanIndexItem) {
        this.indexKeyInfo = indexKeyInfo;
        this.indexName = indexName;
        this.indexMultiKey = indexMultiKey;
        this.queryPlanIndexItem = queryPlanIndexItem;
    }

    public IndexKeyInfo getIndexKeyInfo() {
        return indexKeyInfo;
    }

    public String getIndexName() {
        return indexName;
    }

    public IndexMultiKey getIndexMultiKey() {
        return indexMultiKey;
    }

    public QueryPlanIndexItem getQueryPlanIndexItem() {
        return queryPlanIndexItem;
    }
}
