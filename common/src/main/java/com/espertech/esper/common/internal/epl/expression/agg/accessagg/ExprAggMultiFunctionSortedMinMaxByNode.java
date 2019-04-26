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
package com.espertech.esper.common.internal.epl.expression.agg.accessagg;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.access.core.*;
import com.espertech.esper.common.internal.epl.agg.access.sorted.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprAggMultiFunctionSortedMinMaxByNode extends ExprAggregateNodeBase implements ExprEnumerationForge, ExprAggMultiFunctionNode {
    private final boolean max;
    private final boolean ever;
    private final boolean sortedwin;

    private EventType containedType;
    private AggregationForgeFactory aggregationForgeFactory;

    public ExprAggMultiFunctionSortedMinMaxByNode(boolean max, boolean ever, boolean sortedwin) {
        super(false);
        this.max = max;
        this.ever = ever;
        this.sortedwin = sortedwin;
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        AggregationForgeFactoryAccessSorted factory;

        if (validationContext.getStatementRawInfo().getStatementType() == StatementType.CREATE_TABLE) {
            // handle create-table statements (state creator and default accessor, limited to certain options)
            factory = handleCreateTable(validationContext);
        } else if (validationContext.getStatementRawInfo().getIntoTableName() != null) {
            // handle into-table (state provided, accessor and agent needed, validation done by factory)
            factory = handleIntoTable(validationContext);
        } else {
            // handle standalone
            factory = handleNonTable(validationContext);
        }

        this.containedType = factory.getContainedEventType();
        aggregationForgeFactory = factory;
        return factory;
    }

    private AggregationForgeFactoryAccessSorted handleNonTable(ExprValidationContext validationContext)
        throws ExprValidationException {
        if (positionalParams.length == 0) {
            throw new ExprValidationException("Missing the sort criteria expression");
        }

        // validate that the streams referenced in the criteria are a single stream's
        Set<Integer> streams = ExprNodeUtilityQuery.getIdentStreamNumbers(positionalParams[0]);
        if (streams.size() > 1 || streams.isEmpty()) {
            throw new ExprValidationException(getErrorPrefix() + " requires that any parameter expressions evaluate properties of the same stream");
        }
        int streamNum = streams.iterator().next();

        // validate that there is a remove stream, use "ever" if not
        if (!ever && ExprAggMultiFunctionLinearAccessNode.getIstreamOnly(validationContext.getStreamTypeService(), streamNum)) {
            if (sortedwin) {
                throw new ExprValidationException(getErrorPrefix() + " requires that a data window is declared for the stream");
            }
        }

        // determine typing and evaluation
        containedType = validationContext.getStreamTypeService().getEventTypes()[streamNum];

        Class componentType = containedType.getUnderlyingType();
        Class accessorResultType = componentType;
        AggregationAccessorForge accessor;
        TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(containedType);
        if (!sortedwin) {
            if (tableMetadata != null) {
                accessor = new AggregationAccessorMinMaxByTable(max, tableMetadata);
            } else {
                accessor = new AggregationAccessorMinMaxByNonTable(max);
            }
        } else {
            if (tableMetadata != null) {
                accessor = new AggregationAccessorSortedTable(max, componentType, tableMetadata);
            } else {
                accessor = new AggregationAccessorSortedNonTable(max, componentType);
            }
            accessorResultType = JavaClassHelper.getArrayType(accessorResultType);
        }

        Pair<ExprNode[], boolean[]> criteriaExpressions = getCriteriaExpressions();

        AggregationStateTypeWStream type;
        if (ever) {
            type = max ? AggregationStateTypeWStream.MAXEVER : AggregationStateTypeWStream.MINEVER;
        } else {
            type = AggregationStateTypeWStream.SORTED;
        }
        AggregationStateKeyWStream stateKey = new AggregationStateKeyWStream(streamNum, containedType, type, criteriaExpressions.getFirst(), optionalFilter);

        ExprForge optionalFilterForge = optionalFilter == null ? null : optionalFilter.getForge();
        EventType streamEventType = validationContext.getStreamTypeService().getEventTypes()[streamNum];
        Class[] criteriaTypes = ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions.getFirst());
        DataInputOutputSerdeForge[] criteriaSerdes = new DataInputOutputSerdeForge[criteriaTypes.length];
        for (int i = 0; i < criteriaTypes.length; i++) {
            criteriaSerdes[i] = validationContext.getSerdeResolver().serdeForAggregation(criteriaTypes[i], validationContext.getStatementRawInfo());
        }
        SortedAggregationStateDesc sortedDesc = new
            SortedAggregationStateDesc(max, validationContext.getClasspathImportService(), criteriaExpressions.getFirst(), criteriaTypes, criteriaSerdes,
            criteriaExpressions.getSecond(), ever, streamNum, this, optionalFilterForge, streamEventType);

        List<StmtClassForgeableFactory> serdeForgables = SerdeEventTypeUtility.plan(containedType, validationContext.getStatementRawInfo(), validationContext.getSerdeEventTypeRegistry(), validationContext.getSerdeResolver());
        validationContext.getAdditionalForgeables().addAll(serdeForgables);

        return new AggregationForgeFactoryAccessSorted(this, accessor, accessorResultType, containedType, stateKey, sortedDesc, AggregationAgentDefault.INSTANCE);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getCollectionOfEvents", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    private AggregationForgeFactoryAccessSorted handleIntoTable(ExprValidationContext validationContext)
        throws ExprValidationException {
        int streamNum;
        if (positionalParams.length == 0 ||
            (positionalParams.length == 1 && positionalParams[0] instanceof ExprWildcard)) {
            ExprAggMultiFunctionUtil.validateWildcardStreamNumbers(validationContext.getStreamTypeService(), getAggregationFunctionName());
            streamNum = 0;
        } else if (positionalParams.length == 1 && positionalParams[0] instanceof ExprStreamUnderlyingNode) {
            streamNum = ExprAggMultiFunctionUtil.validateStreamWildcardGetStreamNum(positionalParams[0]);
        } else if (positionalParams.length > 0) {
            throw new ExprValidationException("When specifying into-table a sort expression cannot be provided");
        } else {
            streamNum = 0;
        }

        EventType containedType = validationContext.getStreamTypeService().getEventTypes()[streamNum];
        Class componentType = containedType.getUnderlyingType();
        Class accessorResultType = componentType;
        AggregationAccessorForge accessor;
        if (!sortedwin) {
            accessor = new AggregationAccessorMinMaxByNonTable(max);
        } else {
            accessor = new AggregationAccessorSortedNonTable(max, componentType);
            accessorResultType = JavaClassHelper.getArrayType(accessorResultType);
        }

        AggregationAgentForge agent = AggregationAgentForgeFactory.make(streamNum, optionalFilter, validationContext.getClasspathImportService(), validationContext.getStreamTypeService().isOnDemandStreams(), validationContext.getStatementName());
        return new AggregationForgeFactoryAccessSorted(this, accessor, accessorResultType, containedType, null, null, agent);
    }

    private AggregationForgeFactoryAccessSorted handleCreateTable(ExprValidationContext validationContext)
        throws ExprValidationException {
        if (positionalParams.length == 0) {
            throw new ExprValidationException("Missing the sort criteria expression");
        }

        String message = "For tables columns, the aggregation function requires the 'sorted(*)' declaration";
        if (!sortedwin && !ever) {
            throw new ExprValidationException(message);
        }
        if (validationContext.getStreamTypeService().getStreamNames().length == 0) {
            throw new ExprValidationException("'Sorted' requires that the event type is provided");
        }
        EventType containedType = validationContext.getStreamTypeService().getEventTypes()[0];
        Class componentType = containedType.getUnderlyingType();
        Pair<ExprNode[], boolean[]> criteriaExpressions = getCriteriaExpressions();
        Class accessorResultType = componentType;
        AggregationAccessorForge accessor;
        if (!sortedwin) {
            accessor = new AggregationAccessorMinMaxByNonTable(max);
        } else {
            accessor = new AggregationAccessorSortedNonTable(max, componentType);
            accessorResultType = JavaClassHelper.getArrayType(accessorResultType);
        }
        Class[] criteriaTypes = ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions.getFirst());
        DataInputOutputSerdeForge[] criteriaSerdes = new DataInputOutputSerdeForge[criteriaTypes.length];
        for (int i = 0; i < criteriaTypes.length; i++) {
            criteriaSerdes[i] = validationContext.getSerdeResolver().serdeForAggregation(criteriaTypes[i], validationContext.getStatementRawInfo());
        }
        SortedAggregationStateDesc stateDesc = new SortedAggregationStateDesc(max, validationContext.getClasspathImportService(), criteriaExpressions.getFirst(),
            criteriaTypes, criteriaSerdes, criteriaExpressions.getSecond(), ever, 0, this, null, containedType);

        List<StmtClassForgeableFactory> serdeForgables = SerdeEventTypeUtility.plan(containedType, validationContext.getStatementRawInfo(), validationContext.getSerdeEventTypeRegistry(), validationContext.getSerdeResolver());
        validationContext.getAdditionalForgeables().addAll(serdeForgables);

        return new AggregationForgeFactoryAccessSorted(this, accessor, accessorResultType, containedType, null, stateDesc, null);
    }

    private Pair<ExprNode[], boolean[]> getCriteriaExpressions() {
        // determine ordering ascending/descending and build criteria expression without "asc" marker
        ExprNode[] criteriaExpressions = new ExprNode[this.positionalParams.length];
        boolean[] sortDescending = new boolean[positionalParams.length];
        for (int i = 0; i < positionalParams.length; i++) {
            ExprNode parameter = positionalParams[i];
            criteriaExpressions[i] = parameter;
            if (parameter instanceof ExprOrderedExpr) {
                ExprOrderedExpr ordered = (ExprOrderedExpr) parameter;
                sortDescending[i] = ordered.isDescending();
                if (!ordered.isDescending()) {
                    criteriaExpressions[i] = ordered.getChildNodes()[0];
                }
            }
        }
        return new Pair<>(criteriaExpressions, sortDescending);
    }

    public String getAggregationFunctionName() {
        if (sortedwin) {
            return "sorted";
        }
        if (ever) {
            return max ? "maxbyever" : "minbyever";
        }
        return max ? "maxby" : "minby";
    }

    @Override
    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(getAggregationFunctionName());
        ExprNodeUtilityPrint.toExpressionStringParams(writer, this.positionalParams);
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        if (!sortedwin) {
            return null;
        }
        return containedType;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return null;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        if (sortedwin) {
            return null;
        }
        return containedType;
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getEventBean", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public boolean isMax() {
        return max;
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }

    protected boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprAggMultiFunctionSortedMinMaxByNode)) {
            return false;
        }
        ExprAggMultiFunctionSortedMinMaxByNode other = (ExprAggMultiFunctionSortedMinMaxByNode) node;
        return max == other.max && containedType == other.containedType && sortedwin == other.sortedwin && ever == other.ever;
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public AggregationForgeFactory getAggregationForgeFactory() {
        return aggregationForgeFactory;
    }

    private String getErrorPrefix() {
        return "The '" + getAggregationFunctionName() + "' aggregation function";
    }
}