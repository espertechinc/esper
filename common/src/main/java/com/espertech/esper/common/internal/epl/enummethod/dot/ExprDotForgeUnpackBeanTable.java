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
package com.espertech.esper.common.internal.epl.enummethod.dot;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEval;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEvalVisitor;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeUnpackBeanTable implements ExprDotForge, ExprDotEval {

    private final EPChainableType returnType;
    private final TableMetaData tableMetadata;

    public ExprDotForgeUnpackBeanTable(EventType lambdaType, TableMetaData tableMetadata) {
        this.tableMetadata = tableMetadata;
        this.returnType = EPChainableTypeHelper.singleValue(tableMetadata.getPublicEventType().getUnderlyingEPType());
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw new UnsupportedOperationException("Table-row eval not available at compile time");
    }

    public CodegenExpression codegen(CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(tableMetadata, classScope, this.getClass());
        CodegenMethod methodNode = parent.makeChild(EPTypePremade.OBJECTARRAY.getEPType(), ExprDotForgeUnpackBeanTable.class, classScope).addParam(EventBean.EPTYPE, "target");

        CodegenExpressionRef refEPS = symbols.getAddEPS(methodNode);
        CodegenExpression refIsNewData = symbols.getAddIsNewData(methodNode);
        CodegenExpressionRef refExprEvalCtx = symbols.getAddExprEvalCtx(methodNode);

        methodNode.getBlock()
                .ifRefNullReturnNull("target")
                .methodReturn(exprDotMethod(eventToPublic, "convertToUnd", ref("target"), refEPS, refIsNewData, refExprEvalCtx));
        return localMethod(methodNode, inner);
    }

    public EPChainableType getTypeInfo() {
        return returnType;
    }

    public void visit(ExprDotEvalVisitor visitor) {
        visitor.visitUnderlyingEvent();
    }

    public ExprDotEval getDotEvaluator() {
        return this;
    }

    public ExprDotForge getDotForge() {
        return this;
    }
}
