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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryCustomForge implements QueryGraphValueEntryForge {
    private final Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> operations = new LinkedHashMap<>();

    public Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> getOperations() {
        return operations;
    }

    public void mergeInto(Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> customIndexOps) {
        for (Map.Entry<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> operation : operations.entrySet()) {
            QueryGraphValueEntryCustomOperationForge existing = customIndexOps.get(operation.getKey());
            if (existing == null) {
                customIndexOps.put(operation.getKey(), operation.getValue());
                continue;
            }
            existing.getPositionalExpressions().putAll(operation.getValue().getPositionalExpressions());
        }
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryCustom.class, this.getClass(), classScope);

        CodegenExpression map;
        if (operations.isEmpty()) {
            map = staticMethod(Collections.class, "emptyMap");
        } else {
            method.getBlock().declareVar(Map.class, "map", newInstance(LinkedHashMap.class, constant(CollectionUtil.capacityHashMap(operations.size()))));
            for (Map.Entry<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> entry : operations.entrySet()) {
                method.getBlock().exprDotMethod(ref("map"), "put", entry.getKey().make(parent, symbols, classScope), entry.getValue().make(parent, symbols, classScope));
            }
            map = ref("map");
        }

        method.getBlock()
                .declareVar(QueryGraphValueEntryCustom.class, "custom", newInstance(QueryGraphValueEntryCustom.class))
                .exprDotMethod(ref("custom"), "setOperations", map)
                .methodReturn(ref("custom"));
        return localMethod(method);
    }
}

