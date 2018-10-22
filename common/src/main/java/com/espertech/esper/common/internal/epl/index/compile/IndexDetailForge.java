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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIndexesInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class IndexDetailForge {
    private final IndexMultiKey indexMultiKey;
    private final QueryPlanIndexItemForge queryPlanIndexItem;

    public IndexDetailForge(IndexMultiKey indexMultiKey, QueryPlanIndexItemForge queryPlanIndexItem) {
        this.indexMultiKey = indexMultiKey;
        this.queryPlanIndexItem = queryPlanIndexItem;
    }

    public CodegenExpression make(CodegenMethodScope parent, ModuleIndexesInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(IndexDetail.class, indexMultiKey.make(parent, classScope), queryPlanIndexItem.make(parent, classScope));
    }
}
