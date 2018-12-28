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
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationBuilderExpr;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprTableIdentNode extends ExprNodeBase implements ExprForgeInstrumentable {

    private final TableMetaData tableMetadata;
    private final String streamOrPropertyName;
    private final String unresolvedPropertyName;
    private final Class returnType;
    private final int streamNum;
    private final String columnName;
    private final int columnNum;

    public ExprTableIdentNode(TableMetaData tableMetadata, String streamOrPropertyName, String unresolvedPropertyName, Class returnType, int streamNum, String columnName, int columnNum) {
        this.tableMetadata = tableMetadata;
        this.streamOrPropertyName = streamOrPropertyName;
        this.unresolvedPropertyName = unresolvedPropertyName;
        this.returnType = returnType;
        this.streamNum = streamNum;
        this.columnName = columnName;
        this.columnNum = columnNum;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        ExprIdentNodeImpl.toPrecedenceFreeEPL(writer, streamOrPropertyName, unresolvedPropertyName);
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
        CodegenMethod method = parent.makeChild(requiredType, this.getClass(), codegenClassScope);
        method.getBlock().declareVar(Object.class, "result", staticMethod(ExprTableIdentNode.class, "tableColumnAggValue", constant(streamNum), constant(columnNum),
                symbols.getAddEPS(method), symbols.getAddIsNewData(method), symbols.getAddExprEvalCtx(method)));
        if (requiredType == Object.class) {
            method.getBlock().methodReturn(ref("result"));
        } else {
            method.getBlock().methodReturn(cast(JavaClassHelper.getBoxedType(requiredType), ref("result")));
        }
        return localMethod(method);
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope codegenClassScope) {
        return new InstrumentationBuilderExpr(this.getClass(), this, "ExprTableSubproperty", requiredType, parent, symbols, codegenClassScope)
                .qparams(new CodegenExpression[]{constant(tableMetadata.getTableName()), constant(unresolvedPropertyName)})
                .build();
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
        return returnType;
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

    public int getColumnNum() {
        return columnNum;
    }

    public String getUnresolvedPropertyName() {
        return unresolvedPropertyName;
    }

    public String getColumnName() {
        return columnName;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum       stream num
     * @param eventsPerStream events
     * @return value
     */
    public static AggregationRow tableColumnRow(int streamNum, EventBean[] eventsPerStream) {
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) eventsPerStream[streamNum];
        return ExprTableEvalStrategyUtil.getRow(oa);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum       stream num
     * @param column          col
     * @param eventsPerStream events
     * @param isNewData       new-data flow
     * @param ctx             context
     * @return value
     */
    public static Object tableColumnAggValue(int streamNum, int column, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext ctx) {
        ObjectArrayBackedEventBean oa = (ObjectArrayBackedEventBean) eventsPerStream[streamNum];
        AggregationRow row = ExprTableEvalStrategyUtil.getRow(oa);
        return row.getValue(column, eventsPerStream, isNewData, ctx);
    }
}
