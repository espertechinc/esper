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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;

import java.util.List;

public class SubordinateQueryIndexPlan {
    private final QueryPlanIndexItemForge indexItem;
    private final IndexMultiKey indexPropKey;
    private final List<StmtClassForgeableFactory> multiKeyForgeables;

    public SubordinateQueryIndexPlan(QueryPlanIndexItemForge indexItem, IndexMultiKey indexPropKey, List<StmtClassForgeableFactory> multiKeyForgeables) {
        this.indexItem = indexItem;
        this.indexPropKey = indexPropKey;
        this.multiKeyForgeables = multiKeyForgeables;
    }

    public QueryPlanIndexItemForge getIndexItem() {
        return indexItem;
    }

    public IndexMultiKey getIndexPropKey() {
        return indexPropKey;
    }

    public List<StmtClassForgeableFactory> getMultiKeyForgeables() {
        return multiKeyForgeables;
    }
}
