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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Contains the query plan for all streams.
 */
public class QueryPlanForge {
    private QueryPlanIndexForge[] indexSpecs;
    private QueryPlanNodeForge[] execNodeSpecs;

    /**
     * Ctor.
     *
     * @param indexSpecs    - specs for indexes to create
     * @param execNodeSpecs - specs for execution nodes to create
     */
    public QueryPlanForge(QueryPlanIndexForge[] indexSpecs, QueryPlanNodeForge[] execNodeSpecs) {
        this.indexSpecs = indexSpecs;
        this.execNodeSpecs = execNodeSpecs;
    }

    /**
     * Return index specs.
     *
     * @return index specs
     */
    public QueryPlanIndexForge[] getIndexSpecs() {
        return indexSpecs;
    }

    /**
     * Return execution node specs.
     *
     * @return execution node specs
     */
    public QueryPlanNodeForge[] getExecNodeSpecs() {
        return execNodeSpecs;
    }

    public String toString() {
        return toQueryPlan();
    }

    public String toQueryPlan() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("QueryPlanNode\n");
        buffer.append(QueryPlanIndexForge.print(indexSpecs));
        buffer.append(QueryPlanNodeForge.print(execNodeSpecs));
        return buffer.toString();
    }

    public CodegenExpression make(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(QueryPlan.class, makeIndexes(method, symbols, classScope), makeStrategies(method, symbols, classScope));
    }

    private CodegenExpression makeStrategies(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return CodegenMakeableUtil.makeArray("spec", QueryPlanNode.class, execNodeSpecs, this.getClass(), parent, symbols, classScope);
    }

    private CodegenExpression makeIndexes(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return CodegenMakeableUtil.makeArray("indexes", QueryPlanIndex.class, indexSpecs, this.getClass(), parent, symbols, classScope);
    }
}
