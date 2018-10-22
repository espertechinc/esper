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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.onset;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerBaseForge;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.variable.core.VariableReadWritePackageForge;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class StatementAgentInstanceFactoryOnTriggerSetForge extends StatementAgentInstanceFactoryOnTriggerBaseForge {

    private final VariableReadWritePackageForge variableReadWrite;
    private final String resultSetProcessorProviderClassName;

    public StatementAgentInstanceFactoryOnTriggerSetForge(ViewableActivatorForge activator, EventType resultEventType, Map<ExprSubselectNode, SubSelectFactoryForge> subselects, Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses, VariableReadWritePackageForge variableReadWrite, String resultSetProcessorProviderClassName) {
        super(activator, resultEventType, subselects, tableAccesses);
        this.variableReadWrite = variableReadWrite;
        this.resultSetProcessorProviderClassName = resultSetProcessorProviderClassName;
    }

    public Class typeOfSubclass() {
        return StatementAgentInstanceFactoryOnTriggerSet.class;
    }

    public void inlineInitializeOnTriggerBase(CodegenExpressionRef saiff, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(saiff, "setVariableReadWrite", variableReadWrite.make(method, symbols, classScope))
                .exprDotMethod(saiff, "setResultSetProcessorFactoryProvider", newInstance(resultSetProcessorProviderClassName, symbols.getAddInitSvc(method)));
    }
}
