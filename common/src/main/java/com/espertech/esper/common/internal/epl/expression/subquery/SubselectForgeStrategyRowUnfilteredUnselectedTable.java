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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeStrategyRowUnfilteredUnselectedTable extends SubselectForgeStrategyRowPlain {

    private final TableMetaData table;

    public SubselectForgeStrategyRowUnfilteredUnselectedTable(ExprSubselectRowNode subselect, TableMetaData table) {
        super(subselect);
        this.table = table;
    }

    @Override
    public CodegenExpression evaluateCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, classScope, this.getClass());
        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);
        method.getBlock()
                .ifCondition(relational(exprDotMethod(symbols.getAddMatchingEvents(method), "size"), CodegenExpressionRelational.CodegenRelational.GT, constant(1)))
                .blockReturn(constantNull())
                .declareVar(EventBean.class, "event", staticMethod(EventBeanUtility.class, "getNonemptyFirstEvent", symbols.getAddMatchingEvents(method)))
                .methodReturn(exprDotMethod(eventToPublic, "convertToUnd", ref("event"), symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
        return localMethod(method);
    }
}
