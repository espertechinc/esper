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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubordinateQueryIndexDescForge {
    private final IndexKeyInfo optionalIndexKeyInfo;
    private final String indexName;
    private final String indexModuleName;
    private final IndexMultiKey indexMultiKey;
    private final QueryPlanIndexItemForge optionalQueryPlanIndexItem; // not required for explicit indexes

    public SubordinateQueryIndexDescForge(IndexKeyInfo optionalIndexKeyInfo, String indexName, String indexModuleName, IndexMultiKey indexMultiKey, QueryPlanIndexItemForge optionalQueryPlanIndexItem) {
        this.optionalIndexKeyInfo = optionalIndexKeyInfo;
        this.indexName = indexName;
        this.indexModuleName = indexModuleName;
        this.indexMultiKey = indexMultiKey;
        this.optionalQueryPlanIndexItem = optionalQueryPlanIndexItem;
    }

    public IndexKeyInfo getOptionalIndexKeyInfo() {
        return optionalIndexKeyInfo;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexModuleName() {
        return indexModuleName;
    }

    public IndexMultiKey getIndexMultiKey() {
        return indexMultiKey;
    }

    public QueryPlanIndexItemForge getOptionalQueryPlanIndexItem() {
        return optionalQueryPlanIndexItem;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubordinateQueryIndexDesc.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(IndexMultiKey.class, "indexMultiKey", indexMultiKey.make(method, classScope))
                .declareVar(QueryPlanIndexItem.class, "queryPlanIndexItem", optionalQueryPlanIndexItem == null ? constantNull() : optionalQueryPlanIndexItem.make(method, classScope));
        method.getBlock().methodReturn(newInstance(SubordinateQueryIndexDesc.class,
                constantNull(), constant(indexName), ref("indexMultiKey"), ref("queryPlanIndexItem")));
        return localMethod(method);
    }
}
