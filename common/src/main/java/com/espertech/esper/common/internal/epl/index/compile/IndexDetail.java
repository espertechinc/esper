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
package com.espertech.esper.common.internal.epl.index.compile;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;

public class IndexDetail {
    public final static EPTypeClass EPTYPE = new EPTypeClass(IndexDetail.class);

    private final IndexMultiKey indexMultiKey;
    private final QueryPlanIndexItem queryPlanIndexItem;

    public IndexDetail(IndexMultiKey indexMultiKey, QueryPlanIndexItem queryPlanIndexItem) {
        this.indexMultiKey = indexMultiKey;
        this.queryPlanIndexItem = queryPlanIndexItem;
    }

    public IndexMultiKey getIndexMultiKey() {
        return indexMultiKey;
    }

    public QueryPlanIndexItem getQueryPlanIndexItem() {
        return queryPlanIndexItem;
    }
}
