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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotEnumerationSourceForgeForProps;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNodeUtility;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumn;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnPlain;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.io.StringWriter;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyEnum.*;

public class ExprTableAccessNodeSubprop extends ExprTableAccessNode implements ExprEvaluator, ExprEnumerationForge, ExprEnumerationEval, ExprForge {
    private final String subpropName;

    private EPTypeClass bindingReturnType;
    private transient EPChainableType optionalEnumerationType;
    private transient ExprEnumerationGivenEventForge optionalPropertyEnumEvaluator;

    public ExprTableAccessNodeSubprop(String tableName, String subpropName) {
        super(tableName);
        this.subpropName = subpropName;
    }

    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    public EPTypeClass getEvaluationType() {
        return bindingReturnType;
    }

    public ExprForge getForge() {
        return this;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public ExprTableEvalStrategyFactoryForge getTableAccessFactoryForge() {
        TableMetadataColumn column = tableMeta.getColumns().get(subpropName);
        boolean ungrouped = !tableMeta.isKeyed();
        ExprTableEvalStrategyFactoryForge forge = new ExprTableEvalStrategyFactoryForge(tableMeta, groupKeyEvaluators);

        if (column instanceof TableMetadataColumnPlain) {
            TableMetadataColumnPlain plain = (TableMetadataColumnPlain) column;
            forge.setPropertyIndex(plain.getIndexPlain());
            forge.setStrategyEnum(ungrouped ? UNGROUPED_PLAINCOL : GROUPED_PLAINCOL);
            forge.setOptionalEnumEval(optionalPropertyEnumEvaluator);
        } else {
            TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
            forge.setAggColumnNum(aggcol.getColumn());
            forge.setStrategyEnum(ungrouped ? UNGROUPED_AGG_SIMPLE : GROUPED_AGG_SIMPLE);
        }
        return forge;
    }

    protected String getInstrumentationQName() {
        return "ExprTableSubproperty";
    }

    protected CodegenExpression[] getInstrumentationQParams() {
        return new CodegenExpression[]{constant(tableMeta.getTableName()), constant(subpropName)};
    }

    protected void validateBindingInternal(ExprValidationContext validationContext)
            throws ExprValidationException {
        validateGroupKeys(tableMeta, validationContext);
        TableMetadataColumn column = validateSubpropertyGetCol(tableMeta, subpropName);
        EPType propType = tableMeta.getPublicEventType().getPropertyEPType(subpropName);
        bindingReturnType = propType == null || propType == EPTypeNull.INSTANCE ? null : (EPTypeClass) propType;
        if (column instanceof TableMetadataColumnPlain) {
            ExprDotEnumerationSourceForgeForProps enumerationSource = ExprDotNodeUtility.getPropertyEnumerationSource(subpropName, 0, tableMeta.getInternalEventType(), true, true);
            optionalEnumerationType = enumerationSource.getReturnType();
            optionalPropertyEnumEvaluator = enumerationSource.getEnumerationGivenEvent();
        } else {
            TableMetadataColumnAggregation aggcol = (TableMetadataColumnAggregation) column;
            optionalEnumerationType = aggcol.getOptionalEnumerationType();
        }
    }

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
        toPrecedenceFreeEPLInternal(writer, subpropName, flags);
    }

    public String getSubpropName() {
        return subpropName;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return EPChainableTypeHelper.optionalIsEventTypeColl(optionalEnumerationType);
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public EPTypeClass getComponentTypeCollection() throws ExprValidationException {
        return EPChainableTypeHelper.getCollectionOrArrayComponentTypeOrNull(optionalEnumerationType);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        return EPChainableTypeHelper.optionalIsEventTypeSingle(optionalEnumerationType);
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    protected boolean equalsNodeInternal(ExprTableAccessNode other) {
        ExprTableAccessNodeSubprop that = (ExprTableAccessNodeSubprop) other;
        return subpropName.equals(that.subpropName);
    }
}
