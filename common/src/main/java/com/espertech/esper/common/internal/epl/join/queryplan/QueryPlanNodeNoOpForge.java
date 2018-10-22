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

import java.util.HashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

public class QueryPlanNodeNoOpForge extends QueryPlanNodeForge {

    public static final QueryPlanNodeNoOpForge INSTANCE = new QueryPlanNodeNoOpForge();

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return publicConstValue(QueryPlanNodeNoOp.class, "INSTANCE");
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
    }

    @Override
    protected void print(IndentWriter writer) {
        writer.println("No-Op Execution");
    }

    public void accept(QueryPlanNodeForgeVisitor visitor) {
        visitor.visit(this);
    }
}
