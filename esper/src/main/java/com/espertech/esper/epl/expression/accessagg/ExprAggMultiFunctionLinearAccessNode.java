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
package com.espertech.esper.epl.expression.accessagg;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoEvaluateSelf;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.common.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.agg.service.common.AggregationStateKeyWStream;
import com.espertech.esper.epl.agg.service.common.AggregationStateTypeWStream;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public class ExprAggMultiFunctionLinearAccessNode extends ExprAggregateNodeBase implements ExprEnumerationForge, ExprEnumerationEval, ExprAggregateAccessMultiValueNode {
    private static final long serialVersionUID = -6088874732989061687L;

    private final AggregationStateType stateType;
    private transient EventType containedType;
    private transient Class scalarCollectionComponentType;

    public ExprAggMultiFunctionLinearAccessNode(AggregationStateType stateType) {
        super(false);
        this.stateType = stateType;
    }

    public AggregationMethodFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        return validateAggregationInternal(validationContext, null);
    }

    public AggregationMethodFactory validateAggregationParamsWBinding(ExprValidationContext validationContext, TableMetadataColumnAggregation tableAccessColumn) throws ExprValidationException {
        return validateAggregationInternal(validationContext, tableAccessColumn);
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        return this;
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionScalar(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetEventBean(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    private AggregationMethodFactory validateAggregationInternal(ExprValidationContext validationContext, TableMetadataColumnAggregation optionalBinding) throws ExprValidationException {

        LinearAggregationFactoryDesc desc;

        // handle table-access expression (state provided, accessor needed)
        if (optionalBinding != null) {
            desc = handleTableAccess(positionalParams, stateType, validationContext, optionalBinding);
        } else if (validationContext.getExprEvaluatorContext().getStatementType() == StatementType.CREATE_TABLE) {
            // handle create-table statements (state creator and default accessor, limited to certain options)
            desc = handleCreateTable(positionalParams, stateType, validationContext);
        } else if (validationContext.getIntoTableName() != null) {
            // handle into-table (state provided, accessor and agent needed, validation done by factory)
            desc = handleIntoTable(positionalParams, stateType, validationContext);
        } else {
            // handle standalone
            desc = handleNonIntoTable(positionalParams, stateType, validationContext);
        }

        containedType = desc.getEnumerationEventType();
        scalarCollectionComponentType = desc.getScalarCollectionType();

        return desc.getFactory();
    }

    private LinearAggregationFactoryDesc handleNonIntoTable(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext) throws ExprValidationException {

        StreamTypeService streamTypeService = validationContext.getStreamTypeService();
        int streamNum;
        Class resultType;
        ExprForge forge;
        ExprNode evaluatorIndex = null;
        boolean istreamOnly;
        EventType containedType;
        Class scalarCollectionComponentType = null;

        // validate wildcard use
        boolean isWildcard = childNodes.length == 0 || childNodes.length > 0 && childNodes[0] instanceof ExprWildcard;
        if (isWildcard) {
            ExprAggMultiFunctionUtil.validateWildcardStreamNumbers(validationContext.getStreamTypeService(), stateType.toString().toLowerCase(Locale.ENGLISH));
            streamNum = 0;
            containedType = streamTypeService.getEventTypes()[0];
            resultType = containedType.getUnderlyingType();
            TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(containedType);
            forge = ExprNodeUtilityRich.makeUnderlyingForge(0, resultType, tableMetadata);
            istreamOnly = getIstreamOnly(streamTypeService, 0);
            if ((stateType == AggregationStateType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
        } else if (childNodes.length > 0 && childNodes[0] instanceof ExprStreamUnderlyingNode) {
            // validate "stream.*"
            streamNum = ExprAggMultiFunctionUtil.validateStreamWildcardGetStreamNum(childNodes[0]);
            istreamOnly = getIstreamOnly(streamTypeService, streamNum);
            if ((stateType == AggregationStateType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
            EventType type = streamTypeService.getEventTypes()[streamNum];
            containedType = type;
            resultType = type.getUnderlyingType();
            TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(type);
            forge = ExprNodeUtilityRich.makeUnderlyingForge(streamNum, resultType, tableMetadata);
        } else {
            // validate when neither wildcard nor "stream.*"
            ExprNode child = childNodes[0];
            Set<Integer> streams = ExprNodeUtilityRich.getIdentStreamNumbers(child);
            if (streams.isEmpty() || (streams.size() > 1)) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires that any child expressions evaluate properties of the same stream; Use 'firstever' or 'lastever' or 'nth' instead");
            }
            streamNum = streams.iterator().next();
            istreamOnly = getIstreamOnly(streamTypeService, streamNum);
            if ((stateType == AggregationStateType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
            resultType = childNodes[0].getForge().getEvaluationType();
            forge = childNodes[0].getForge();
            if (streamNum >= streamTypeService.getEventTypes().length) {
                containedType = streamTypeService.getEventTypes()[0];
            } else {
                containedType = streamTypeService.getEventTypes()[streamNum];
            }
            scalarCollectionComponentType = resultType;
        }

        if (childNodes.length > 1) {
            if (stateType == AggregationStateType.WINDOW) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " does not accept an index expression; Use 'first' or 'last' instead");
            }
            evaluatorIndex = childNodes[1];
            Class indexResultType = evaluatorIndex.getForge().getEvaluationType();
            if (indexResultType != Integer.class && indexResultType != int.class) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires an index expression that returns an integer value");
            }
        }

        // determine accessor
        AggregationAccessorForge accessor;
        if (evaluatorIndex != null) {
            boolean isFirst = stateType == AggregationStateType.FIRST;
            int constant = -1;
            ExprForge forgeIndex;
            if (evaluatorIndex.isConstantResult()) {
                constant = (Integer) evaluatorIndex.getForge().getExprEvaluator().evaluate(null, true, null);
                forgeIndex = null;
            } else {
                forgeIndex = evaluatorIndex.getForge();
            }
            accessor = new AggregationAccessorFirstLastIndexWEvalForge(streamNum, forge, forgeIndex, constant, isFirst);
        } else {
            if (stateType == AggregationStateType.FIRST) {
                accessor = new AggregationAccessorFirstWEvalForge(streamNum, forge);
            } else if (stateType == AggregationStateType.LAST) {
                accessor = new AggregationAccessorLastWEvalForge(streamNum, forge);
            } else if (stateType == AggregationStateType.WINDOW) {
                accessor = new AggregationAccessorWindowWEvalForge(streamNum, forge, resultType);
            } else {
                throw new IllegalStateException("Access type is undefined or not known as code '" + stateType + "'");
            }
        }

        Class accessorResultType = resultType;
        if (stateType == AggregationStateType.WINDOW) {
            accessorResultType = JavaClassHelper.getArrayType(resultType);
        }

        boolean isFafWindow = streamTypeService.isOnDemandStreams() && stateType == AggregationStateType.WINDOW;
        TableMetadata tableMetadata = validationContext.getTableService().getTableMetadataFromEventType(containedType);

        if (tableMetadata == null && !isFafWindow && (istreamOnly || streamTypeService.isOnDemandStreams())) {
            if (optionalFilter != null) {
                positionalParams = ExprNodeUtilityCore.addExpression(positionalParams, optionalFilter);
            }
            AggregationMethodFactory factory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinearUnbounded(validationContext.getStatementExtensionSvcContext(), this, containedType, accessorResultType, streamNum, optionalFilter != null);
            return new LinearAggregationFactoryDesc(factory, containedType, scalarCollectionComponentType);
        }

        AggregationStateKeyWStream stateKey = new AggregationStateKeyWStream(streamNum, containedType, AggregationStateTypeWStream.DATAWINDOWACCESS_LINEAR, ExprNodeUtilityCore.EMPTY_EXPR_ARRAY, optionalFilter);

        ExprForge optionalFilterForge = optionalFilter == null ? null : optionalFilter.getForge();
        boolean join = validationContext.getStreamTypeService().getEventTypes().length > 1;
        AggregationStateFactoryForge stateFactory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinear(validationContext.getStatementExtensionSvcContext(), this, streamNum, optionalFilterForge, join);
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, accessorResultType, containedType,
                stateKey, stateFactory, AggregationAgentDefault.INSTANCE);
        EventType enumerationType = scalarCollectionComponentType == null ? containedType : null;
        return new LinearAggregationFactoryDesc(factory, enumerationType, scalarCollectionComponentType);
    }

    private LinearAggregationFactoryDesc handleCreateTable(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "For tables columns, the " + stateType.name().toLowerCase(Locale.ENGLISH) + " aggregation function requires the 'window(*)' declaration";
        if (stateType != AggregationStateType.WINDOW) {
            throw new ExprValidationException(message);
        }
        if (childNodes.length == 0 || childNodes.length > 1 || !(childNodes[0] instanceof ExprWildcard)) {
            throw new ExprValidationException(message);
        }
        if (validationContext.getStreamTypeService().getStreamNames().length == 0) {
            throw new ExprValidationException(getErrorPrefix(stateType) + " requires that the event type is provided");
        }
        EventType containedType = validationContext.getStreamTypeService().getEventTypes()[0];
        Class componentType = containedType.getUnderlyingType();
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEval(componentType);
        boolean join = validationContext.getStreamTypeService().getEventTypes().length > 1;
        AggregationStateFactoryForge stateFactory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinear(validationContext.getStatementExtensionSvcContext(), this, 0, null, join);
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, JavaClassHelper.getArrayType(componentType), containedType, null, stateFactory, null);
        return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
    }

    private LinearAggregationFactoryDesc handleIntoTable(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "For into-table use 'window(*)' or 'window(stream.*)' instead";
        if (stateType != AggregationStateType.WINDOW) {
            throw new ExprValidationException(message);
        }
        if (childNodes.length == 0 || childNodes.length > 1) {
            throw new ExprValidationException(message);
        }
        if (validationContext.getStreamTypeService().getStreamNames().length == 0) {
            throw new ExprValidationException(getErrorPrefix(stateType) + " requires that at least one stream is provided");
        }
        int streamNum;
        if (childNodes[0] instanceof ExprWildcard) {
            if (validationContext.getStreamTypeService().getStreamNames().length != 1) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " with wildcard requires a single stream");
            }
            streamNum = 0;
        } else if (childNodes[0] instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode und = (ExprStreamUnderlyingNode) childNodes[0];
            streamNum = und.getStreamId();
        } else {
            throw new ExprValidationException(message);
        }
        EventType containedType = validationContext.getStreamTypeService().getEventTypes()[streamNum];
        Class componentType = containedType.getUnderlyingType();
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEval(componentType);
        AggregationAgentForge agent = ExprAggAggregationAgentFactory.make(streamNum, optionalFilter, validationContext.getEngineImportService(), validationContext.getStreamTypeService().isOnDemandStreams(), validationContext.getStatementName());
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, JavaClassHelper.getArrayType(componentType), containedType, null, null, agent);
        return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
    }

    private LinearAggregationFactoryDesc handleTableAccess(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext, TableMetadataColumnAggregation tableAccess)
            throws ExprValidationException {
        if (stateType == AggregationStateType.FIRST || stateType == AggregationStateType.LAST) {
            return handleTableAccessFirstLast(childNodes, stateType, validationContext, tableAccess);
        } else if (stateType == AggregationStateType.WINDOW) {
            return handleTableAccessWindow(childNodes, stateType, validationContext, tableAccess);
        }
        throw new IllegalStateException("Unrecognized type " + stateType);
    }

    private LinearAggregationFactoryDesc handleTableAccessFirstLast(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext, TableMetadataColumnAggregation tableAccess)
            throws ExprValidationException {
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess original = (ExprAggMultiFunctionLinearAccessNodeFactoryAccess) tableAccess.getFactory();
        Class resultType = original.getContainedEventType().getUnderlyingType();
        AggregationAccessorForge defaultAccessor = stateType == AggregationStateType.FIRST ?
                AggregationAccessorFirstNoEval.INSTANCE : AggregationAccessorLastNoEval.INSTANCE;
        if (childNodes.length == 0) {
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factoryAccess = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, defaultAccessor, resultType, original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factoryAccess, factoryAccess.getContainedEventType(), null);
        }
        if (childNodes.length == 1) {
            if (childNodes[0] instanceof ExprWildcard) {
                ExprAggMultiFunctionLinearAccessNodeFactoryAccess factoryAccess = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, defaultAccessor, resultType, original.getContainedEventType(), null, null, null);
                return new LinearAggregationFactoryDesc(factoryAccess, factoryAccess.getContainedEventType(), null);
            }
            if (childNodes[0] instanceof ExprStreamUnderlyingNode) {
                throw new ExprValidationException("Stream-wildcard is not allowed for table column access");
            }
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableServiceUtil.streamTypeFromTableColumn(tableAccess, validationContext.getStreamTypeService().getEngineURIQualifier());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            ExprForge paramNodeForge = paramNode.getForge();
            AggregationAccessorForge accessor;
            if (stateType == AggregationStateType.FIRST) {
                accessor = new AggregationAccessorFirstWEvalForge(0, paramNodeForge);
            } else {
                accessor = new AggregationAccessorLastWEvalForge(0, paramNodeForge);
            }
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, paramNode.getForge().getEvaluationType(), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
        }
        if (childNodes.length == 2) {
            boolean isFirst = stateType == AggregationStateType.FIRST;
            int constant = -1;
            ExprNode indexEvalNode = childNodes[1];
            Class indexEvalType = indexEvalNode.getForge().getEvaluationType();
            if (indexEvalType != Integer.class && indexEvalType != int.class) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires a constant index expression that returns an integer value");
            }

            ExprForge forgeIndex;
            if (indexEvalNode.isConstantResult()) {
                constant = (Integer) indexEvalNode.getForge().getExprEvaluator().evaluate(null, true, null);
                forgeIndex = null;
            } else {
                forgeIndex = indexEvalNode.getForge();
            }
            AggregationAccessorForge accessor = new AggregationAccessorFirstLastIndexNoEvalForge(forgeIndex, constant, isFirst);
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, resultType, original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
        }
        throw new ExprValidationException("Invalid number of parameters");
    }

    private LinearAggregationFactoryDesc handleTableAccessWindow(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext, TableMetadataColumnAggregation tableAccess)
            throws ExprValidationException {
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess original = (ExprAggMultiFunctionLinearAccessNodeFactoryAccess) tableAccess.getFactory();
        if (childNodes.length == 0 ||
                (childNodes.length == 1 && childNodes[0] instanceof ExprWildcard)) {
            Class componentType = original.getContainedEventType().getUnderlyingType();
            AggregationAccessorForge accessor = new AggregationAccessorWindowNoEval(componentType);
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, JavaClassHelper.getArrayType(componentType), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
        }
        if (childNodes.length == 1) {
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableServiceUtil.streamTypeFromTableColumn(tableAccess, validationContext.getStreamTypeService().getEngineURIQualifier());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            Class paramNodeType = paramNode.getForge().getEvaluationType();
            ExprForge paramNodeEval = paramNode.getForge();
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this,
                    new AggregationAccessorWindowWEvalForge(0, paramNodeEval, paramNodeType), JavaClassHelper.getArrayType(paramNodeType), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, null, paramNodeType);
        }
        throw new ExprValidationException("Invalid number of parameters");
    }

    protected static boolean getIstreamOnly(StreamTypeService streamTypeService, int streamNum) {
        if (streamNum < streamTypeService.getEventTypes().length) {
            return streamTypeService.getIStreamOnly()[streamNum];
        }
        // this could happen for match-recognize which has different stream types for selection and for aggregation
        return streamTypeService.getIStreamOnly()[0];
    }

    @Override
    public String getAggregationFunctionName() {
        return stateType.toString().toLowerCase(Locale.ENGLISH);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(stateType.toString().toLowerCase(Locale.ENGLISH));
        ExprNodeUtilityCore.toExpressionStringParams(writer, this.getChildNodes());
    }

    public AggregationStateType getStateType() {
        return stateType;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return super.aggregationResultFuture.getCollectionOfEvents(column, eventsPerStream, isNewData, context);
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        return CodegenLegoEvaluateSelf.evaluateSelfGetROCollectionEvents(this, codegenMethodScope, exprSymbol, codegenClassScope);
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return super.aggregationResultFuture.getCollectionScalar(column, eventsPerStream, isNewData, context);
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) {
        if (stateType == AggregationStateType.FIRST || stateType == AggregationStateType.LAST) {
            return null;
        }
        return containedType;
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return scalarCollectionComponentType;
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        if (stateType == AggregationStateType.FIRST || stateType == AggregationStateType.LAST) {
            return containedType;
        }
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return super.aggregationResultFuture.getEventBean(column, eventsPerStream, isNewData, context);
    }

    protected boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprAggMultiFunctionLinearAccessNode)) {
            return false;
        }
        ExprAggMultiFunctionLinearAccessNode other = (ExprAggMultiFunctionLinearAccessNode) node;
        return stateType == other.stateType && containedType == other.containedType && scalarCollectionComponentType == other.scalarCollectionComponentType;
    }

    private static ExprValidationException makeUnboundValidationEx(AggregationStateType stateType) {
        return new ExprValidationException(getErrorPrefix(stateType) + " requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead");
    }

    private static String getErrorPrefix(AggregationStateType stateType) {
        return ExprAggMultiFunctionUtil.getErrorPrefix(stateType.toString().toLowerCase(Locale.ENGLISH));
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }
}