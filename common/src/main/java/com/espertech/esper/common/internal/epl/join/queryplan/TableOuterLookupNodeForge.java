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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanNodeForgeVisitor;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.util.Arrays;
import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Specifies exection of a table lookup with outer join using the a specified lookup plan.
 */
public class TableOuterLookupNodeForge extends QueryPlanNodeForge {
    private TableLookupPlanForge tableLookupPlan;

    /**
     * Ctor.
     *
     * @param tableLookupPlan - plan for performing lookup
     */
    public TableOuterLookupNodeForge(TableLookupPlanForge tableLookupPlan) {
        this.tableLookupPlan = tableLookupPlan;
    }

    /**
     * Returns lookup plan.
     *
     * @return lookup plan
     */
    public TableLookupPlanForge getLookupStrategySpec() {
        return tableLookupPlan;
    }

    public void print(IndentWriter writer) {
        writer.println("TableOuterLookupNode " +
                " tableLookupPlan=" + tableLookupPlan);
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        usedIndexes.addAll(Arrays.asList(tableLookupPlan.getIndexNum()));
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(TableOuterLookupNode.class, tableLookupPlan.make(parent, symbols, classScope));
    }

    public void accept(QueryPlanNodeForgeVisitor visitor) {
        visitor.visit(this);
    }
}
