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
package com.espertech.esper.common.internal.epl.expression.etc;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprEvalUnderlyingEvaluatorTable implements ExprEvaluator, ExprForge {
    private final int streamNum;
    private final Class resultType;
    private final TableMetaData tableMetadata;

    public ExprEvalUnderlyingEvaluatorTable(int streamNum, Class resultType, TableMetaData tableMetadata) {
        this.streamNum = streamNum;
        this.resultType = resultType;
        this.tableMetadata = tableMetadata;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public Class getEvaluationType() {
        return resultType;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return new ExprNodeRenderable() {
            public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
                writer.append(this.getClass().getSimpleName());
            }
        };
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpressionField eventToPublic = TableDeployTimeResolver.makeTableEventToPublicField(tableMetadata, codegenClassScope, this.getClass());
        CodegenMethod method = parent.makeChild(Object[].class, ExprEvalUnderlyingEvaluatorTable.class, codegenClassScope);
        method.getBlock().ifNullReturnNull(exprSymbol.getAddEPS(method))
                .declareVar(EventBean.class, "event", arrayAtIndex(exprSymbol.getAddEPS(method), constant(streamNum)))
                .ifRefNullReturnNull("event")
                .methodReturn(exprDotMethod(eventToPublic, "convertToUnd", ref("event"), exprSymbol.getAddEPS(method), exprSymbol.getAddIsNewData(method), exprSymbol.getAddExprEvalCtx(method)));
        return localMethod(method);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }
}
