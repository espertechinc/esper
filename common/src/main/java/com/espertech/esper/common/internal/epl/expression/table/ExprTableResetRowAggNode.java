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
package com.espertech.esper.common.internal.epl.expression.table;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprTableResetRowAggNode extends ExprNodeBase implements ExprForgeInstrumentable {

    private final TableMetaData tableMetadata;
    private final int streamNum;

    public ExprTableResetRowAggNode(TableMetaData tableMetadata, int streamNum) {
        this.tableMetadata = tableMetadata;
        this.streamNum = streamNum;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(tableMetadata.getTableName()).append(".reset()");
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean isConstantResult() {
        return false;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        return false;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        return null;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(void.class, this.getClass(), codegenClassScope);
        method.getBlock().expression(staticMethod(ExprTableResetRowAggNode.class, "tableAggReset", constant(streamNum), symbols.getAddEPS(method)));
        return localMethod(method);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return evaluateCodegenUninstrumented(requiredType, parent, symbols, codegenClassScope);
    }

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    public ExprEvaluator getExprEvaluator() {
        return new ExprEvaluator() {
            public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
                throw new UnsupportedOperationException("Cannot evaluate at compile time");
            }
        };
    }

    public Class getEvaluationType() {
        return void.class;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNode getForgeRenderable() {
        return this;
    }

    public TableMetaData getTableMetadata() {
        return tableMetadata;
    }

    public int getStreamNum() {
        return streamNum;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum       stream num
     * @param eventsPerStream events
     */
    public static void tableAggReset(int streamNum, EventBean[] eventsPerStream) {
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) eventsPerStream[streamNum];
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(oa);
        row.clear();
    }
}
