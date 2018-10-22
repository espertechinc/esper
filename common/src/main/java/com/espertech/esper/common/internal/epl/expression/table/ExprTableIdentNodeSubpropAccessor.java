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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionTableReader;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationTableAccessAggReaderCodegenField;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.agg.core.AggregationTableReadDesc;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionNode;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.common.internal.event.core.ObjectArrayBackedEventBean;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprTableIdentNodeSubpropAccessor extends ExprNodeBase implements ExprForgeInstrumentable, ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval {
    private final int streamNum;
    private final String optionalStreamName;
    private final TableMetaData table;
    private final TableMetadataColumnAggregation tableAccessColumn;
    private final ExprNode aggregateAccessMultiValueNode;
    private AggregationTableReadDesc tableAccessDesc;

    public ExprTableIdentNodeSubpropAccessor(int streamNum, String optionalStreamName, TableMetaData table, TableMetadataColumnAggregation tableAccessColumn, ExprNode aggregateAccessMultiValueNode) {
        this.streamNum = streamNum;
        this.optionalStreamName = optionalStreamName;
        this.table = table;
        this.tableAccessColumn = tableAccessColumn;
        this.aggregateAccessMultiValueNode = aggregateAccessMultiValueNode;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (tableAccessColumn.isMethodAgg()) {
            throw new ExprValidationException("Invalid combination of aggregation state and aggregation accessor");
        }
        ExprAggMultiFunctionNode mfNode = (ExprAggMultiFunctionNode) aggregateAccessMultiValueNode;
        mfNode.validatePositionals(validationContext);
        tableAccessDesc = mfNode.validateAggregationTableRead(validationContext, tableAccessColumn, table);
        return null;
    }

    public CodegenExpression evaluateCodegenUninstrumented(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField reader = classScope.addOrGetFieldSharable(new AggregationTableAccessAggReaderCodegenField(tableAccessDesc.getReader(), classScope, this.getClass()));
        return CodegenLegoCast.castSafeFromObjectType(requiredType, staticMethod(ExprTableIdentNodeSubpropAccessor.class, "evaluateTableWithReader", constant(streamNum), reader,
                constant(tableAccessColumn.getColumn()), symbols.getAddEPS(parent), symbols.getAddIsNewData(parent), symbols.getAddExprEvalCtx(parent)));
    }

    public CodegenExpression evaluateCodegen(Class requiredType, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        return evaluateCodegenUninstrumented(requiredType, parent, symbols, classScope);
    }

    public Class getEvaluationType() {
        return tableAccessDesc.getReader().getResultType();
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprNodeRenderable getForgeRenderable() {
        return this;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField reader = classScope.addOrGetFieldSharable(new AggregationTableAccessAggReaderCodegenField(tableAccessDesc.getReader(), classScope, this.getClass()));
        return staticMethod(ExprTableIdentNodeSubpropAccessor.class, "evaluateTableWithReaderCollectionEvents", constant(streamNum), reader,
                constant(tableAccessColumn.getColumn()), symbols.getAddEPS(parent), symbols.getAddIsNewData(parent), symbols.getAddExprEvalCtx(parent));
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return tableAccessDesc.getEventTypeCollection();
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return tableAccessDesc.getComponentTypeCollection();
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        CodegenExpressionField reader = classScope.addOrGetFieldSharable(new AggregationTableAccessAggReaderCodegenField(tableAccessDesc.getReader(), classScope, this.getClass()));
        return staticMethod(ExprTableIdentNodeSubpropAccessor.class, "evaluateTableWithReaderCollectionScalar", constant(streamNum), reader,
                constant(tableAccessColumn.getColumn()), symbols.getAddEPS(parent), symbols.getAddIsNewData(parent), symbols.getAddExprEvalCtx(parent));
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return tableAccessDesc.getEventTypeSingle();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (optionalStreamName != null) {
            writer.append(optionalStreamName);
            writer.append(".");
        }
        writer.append(tableAccessColumn.getColumnName());
        writer.append(".");
        aggregateAccessMultiValueNode.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
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

    public ExprForgeConstantType getForgeConstantType() {
        return ExprForgeConstantType.NONCONST;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum            stream number
     * @param reader               reader
     * @param aggColNum            agg col
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext expr ctx
     * @return value
     */
    public static Object evaluateTableWithReader(int streamNum, AggregationMultiFunctionTableReader reader, int aggColNum, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow((ObjectArrayBackedEventBean) event);
        return reader.getValue(aggColNum, row, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum            stream number
     * @param reader               reader
     * @param aggColNum            agg col
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext expr ctx
     * @return value
     */
    public static Collection evaluateTableWithReaderCollectionEvents(int streamNum, AggregationMultiFunctionTableReader reader, int aggColNum, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow((ObjectArrayBackedEventBean) event);
        return reader.getValueCollectionEvents(aggColNum, row, eventsPerStream, isNewData, exprEvaluatorContext);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param streamNum            stream number
     * @param reader               reader
     * @param aggColNum            agg col
     * @param eventsPerStream      events
     * @param isNewData            new-data flag
     * @param exprEvaluatorContext expr ctx
     * @return value
     */
    public static Collection evaluateTableWithReaderCollectionScalar(int streamNum, AggregationMultiFunctionTableReader reader, int aggColNum, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean event = eventsPerStream[streamNum];
        if (event == null) {
            return null;
        }
        AggregationRow row = ExprTableEvalStrategyUtil.getRow((ObjectArrayBackedEventBean) event);
        return reader.getValueCollectionScalar(aggColNum, row, eventsPerStream, isNewData, exprEvaluatorContext);
    }
}
