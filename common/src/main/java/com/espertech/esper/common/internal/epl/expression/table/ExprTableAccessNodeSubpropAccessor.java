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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.agg.core.AggregationTableReadDesc;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyEnum;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;

public class ExprTableAccessNodeSubpropAccessor extends ExprTableAccessNode implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForge {

    private final String subpropName;
    private final ExprNode aggregateAccessMultiValueNode;
    private AggregationTableReadDesc tableAccessDesc;

    public ExprTableAccessNodeSubpropAccessor(String tableName, String subpropName, ExprNode aggregateAccessMultiValueNode) {
        super(tableName);
        this.subpropName = subpropName;
        this.aggregateAccessMultiValueNode = aggregateAccessMultiValueNode;
    }

    public ExprAggregateNodeBase getAggregateAccessMultiValueNode() {
        return (ExprAggregateNodeBase) aggregateAccessMultiValueNode;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public ExprForge getForge() {
        return this;
    }

    public Class getEvaluationType() {
        return tableAccessDesc.getReader().getResultType();
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    protected String getInstrumentationQName() {
        return "ExprTableSubpropAccessor";
    }

    protected CodegenExpression[] getInstrumentationQParams() {
        return new CodegenExpression[]{
                constant(tableMeta.getTableName()), constant(subpropName), constant(ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(aggregateAccessMultiValueNode))
        };
    }

    protected void validateBindingInternal(ExprValidationContext validationContext) throws ExprValidationException {
        // validate group keys
        validateGroupKeys(tableMeta, validationContext);
        TableMetadataColumnAggregation column = (TableMetadataColumnAggregation) validateSubpropertyGetCol(tableMeta, subpropName);

        // validate accessor factory i.e. the parameters types and the match to the required state
        if (column.isMethodAgg()) {
            throw new ExprValidationException("Invalid combination of aggregation state and aggregation accessor");
        }
        ExprAggMultiFunctionNode mfNode = (ExprAggMultiFunctionNode) aggregateAccessMultiValueNode;
        mfNode.validatePositionals(validationContext);
        tableAccessDesc = mfNode.validateAggregationTableRead(validationContext, column, tableMeta);
    }

    public ExprTableEvalStrategyFactoryForge getTableAccessFactoryForge() {
        ExprTableEvalStrategyFactoryForge forge = new ExprTableEvalStrategyFactoryForge(tableMeta, groupKeyEvaluators);
        TableMetadataColumnAggregation column = (TableMetadataColumnAggregation) tableMeta.getColumns().get(subpropName);
        forge.setAggColumnNum(column.getColumn());
        boolean ungrouped = !tableMeta.isKeyed();
        forge.setStrategyEnum(ungrouped ? ExprTableEvalStrategyEnum.UNGROUPED_AGG_ACCESSREAD : ExprTableEvalStrategyEnum.GROUPED_AGG_ACCESSREAD);
        forge.setAccessAggStrategy(tableAccessDesc.getReader());
        return forge;
    }

    public String getSubpropName() {
        return subpropName;
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

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return tableAccessDesc.getEventTypeSingle();
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        toPrecedenceFreeEPLInternal(writer, subpropName);
        writer.append(".");
        aggregateAccessMultiValueNode.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        ExprTableAccessNodeSubpropAccessor that = (ExprTableAccessNodeSubpropAccessor) other;
        if (!subpropName.equals(that.subpropName)) {
            return false;
        }
        return ExprNodeUtilityCompare.deepEquals(aggregateAccessMultiValueNode, that.aggregateAccessMultiValueNode, false);
    }
}
