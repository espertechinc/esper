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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubSelectFactoryForge {
    private final int subqueryNumber;
    private final ViewableActivatorForge activator;
    private final SubSelectStrategyFactoryForge strategyFactoryForge;

    public SubSelectFactoryForge(int subqueryNumber, ViewableActivatorForge activator, SubSelectStrategyFactoryForge strategyFactoryForge) {
        this.subqueryNumber = subqueryNumber;
        this.activator = activator;
        this.strategyFactoryForge = strategyFactoryForge;
    }

    public List<ViewFactoryForge> getViewForges() {
        return strategyFactoryForge.getViewForges();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(SubSelectFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(SubSelectFactory.class, "factory", newInstance(SubSelectFactory.class))
                .exprDotMethod(ref("factory"), "setSubqueryNumber", constant(subqueryNumber))
                .exprDotMethod(ref("factory"), "setActivator", activator.makeCodegen(method, symbols, classScope))
                .exprDotMethod(ref("factory"), "setStrategyFactory", strategyFactoryForge.makeCodegen(method, symbols, classScope))
                .exprDotMethod(ref("factory"), "setHasAggregation", constant(strategyFactoryForge.hasAggregation()))
                .exprDotMethod(ref("factory"), "setHasPrior", constant(strategyFactoryForge.hasPrior()))
                .exprDotMethod(ref("factory"), "setHasPrevious", constant(strategyFactoryForge.hasPrevious()))
                .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", ref("factory"))
                .methodReturn(ref("factory"));
        return localMethod(method);
    }

    public static CodegenExpression codegenInitMap(Map<ExprSubselectNode, SubSelectFactoryForge> subselects, Class generator, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(Map.class, generator, classScope);
        method.getBlock()
                .declareVar(Map.class, "subselects", newInstance(LinkedHashMap.class, constant(subselects.size() + 2)));
        for (Map.Entry<ExprSubselectNode, SubSelectFactoryForge> entry : subselects.entrySet()) {
            method.getBlock().exprDotMethod(ref("subselects"), "put", constant(entry.getKey().getSubselectNumber()), entry.getValue().make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("subselects"));
        return localMethod(method);
    }
}
