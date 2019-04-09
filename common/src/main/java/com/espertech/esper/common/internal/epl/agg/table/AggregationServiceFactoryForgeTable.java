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
package com.espertech.esper.common.internal.epl.agg.table;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentUtil;
import com.espertech.esper.common.internal.epl.agg.core.AggregationGroupByRollupDescForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryForgeWProviderGen;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableColumnMethodPairForge;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationServiceFactoryForgeTable implements AggregationServiceFactoryForgeWProviderGen {
    private final TableMetaData metadata;
    private final TableColumnMethodPairForge[] methodPairs;
    private final int[] accessColumnsZeroOffset;
    private final AggregationAgentForge[] accessAgents;
    private final AggregationGroupByRollupDescForge groupByRollupDesc;

    public AggregationServiceFactoryForgeTable(TableMetaData metadata, TableColumnMethodPairForge[] methodPairs, int[] accessColumnsZeroOffset, AggregationAgentForge[] accessAgents, AggregationGroupByRollupDescForge groupByRollupDesc) {
        this.metadata = metadata;
        this.methodPairs = methodPairs;
        this.accessColumnsZeroOffset = accessColumnsZeroOffset;
        this.accessAgents = accessAgents;
        this.groupByRollupDesc = groupByRollupDesc;
    }

    public CodegenExpression makeProvider(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationServiceFactoryTable.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(AggregationServiceFactoryTable.class, "factory", newInstance(AggregationServiceFactoryTable.class))
            .exprDotMethod(ref("factory"), "setTable", TableDeployTimeResolver.makeResolveTable(metadata, symbols.getAddInitSvc(method)))
            .exprDotMethod(ref("factory"), "setMethodPairs", TableColumnMethodPairForge.makeArray(methodPairs, method, symbols, classScope))
            .exprDotMethod(ref("factory"), "setAccessColumnsZeroOffset", constant(accessColumnsZeroOffset))
            .exprDotMethod(ref("factory"), "setAccessAgents", AggregationAgentUtil.makeArray(accessAgents, method, symbols, classScope))
            .exprDotMethod(ref("factory"), "setGroupByRollupDesc", groupByRollupDesc == null ? constantNull() : groupByRollupDesc.codegen(method, classScope))
            .methodReturn(ref("factory"));
        return localMethod(method);
    }
}
