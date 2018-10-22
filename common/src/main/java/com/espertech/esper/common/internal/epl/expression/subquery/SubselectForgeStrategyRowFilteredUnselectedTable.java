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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoBooleanExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public class SubselectForgeStrategyRowFilteredUnselectedTable extends SubselectForgeStrategyRowPlain {

    private final TableMetaData table;

    public SubselectForgeStrategyRowFilteredUnselectedTable(ExprSubselectRowNode subselect, TableMetaData table) {
        super(subselect);
        this.table = table;
    }

    @Override
    public CodegenExpression evaluateCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(table, classScope, this.getClass());
        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);

        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);

        method.getBlock().declareVar(EventBean.class, "filtered", constantNull());
        CodegenBlock foreach = method.getBlock().forEach(EventBean.class, "event", symbols.getAddMatchingEvents(method));
        {
            foreach.assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("event"));
            CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(subselect.filterExpr, method, classScope);
            CodegenLegoBooleanExpression.codegenContinueIfNotNullAndNotPass(foreach, Boolean.class, localMethod(filter, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
            foreach.ifCondition(notEqualsNull(ref("filtered"))).blockReturn(constantNull())
                    .assignRef("filtered", ref("event"));
        }

        method.getBlock().ifRefNullReturnNull("filtered")
                .methodReturn(exprDotMethod(eventToPublic, "convertToUnd", ref("filtered"), symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
        return localMethod(method);
    }
}
