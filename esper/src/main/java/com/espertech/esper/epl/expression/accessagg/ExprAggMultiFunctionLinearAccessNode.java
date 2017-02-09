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
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.AggregationMethodFactory;
import com.espertech.esper.epl.agg.service.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.AggregationStateKeyWStream;
import com.espertech.esper.epl.agg.service.AggregationStateTypeWStream;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.core.StreamTypeServiceImpl;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableMetadataColumnAggregation;
import com.espertech.esper.epl.table.mgmt.TableServiceUtil;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;

public class ExprAggMultiFunctionLinearAccessNode extends ExprAggregateNodeBase implements ExprEvaluatorEnumeration, ExprAggregateAccessMultiValueNode {
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
        ExprEvaluator evaluator;
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
            evaluator = ExprNodeUtility.makeUnderlyingEvaluator(0, resultType, tableMetadata);
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
            evaluator = ExprNodeUtility.makeUnderlyingEvaluator(streamNum, resultType, tableMetadata);
        } else {
            // validate when neither wildcard nor "stream.*"
            ExprNode child = childNodes[0];
            Set<Integer> streams = ExprNodeUtility.getIdentStreamNumbers(child);
            if (streams.isEmpty() || (streams.size() > 1)) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires that any child expressions evaluate properties of the same stream; Use 'firstever' or 'lastever' or 'nth' instead");
            }
            streamNum = streams.iterator().next();
            istreamOnly = getIstreamOnly(streamTypeService, streamNum);
            if ((stateType == AggregationStateType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
            resultType = childNodes[0].getExprEvaluator().getType();
            evaluator = childNodes[0].getExprEvaluator();
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
            if (evaluatorIndex.getExprEvaluator().getType() != Integer.class) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires an index expression that returns an integer value");
            }
        }

        // determine accessor
        AggregationAccessor accessor;
        if (evaluatorIndex != null) {
            boolean isFirst = stateType == AggregationStateType.FIRST;
            int constant = -1;
            if (evaluatorIndex.isConstantResult()) {
                constant = (Integer) evaluatorIndex.getExprEvaluator().evaluate(null, true, null);
            }
            accessor = new AggregationAccessorFirstLastIndexWEval(streamNum, evaluator, evaluatorIndex.getExprEvaluator(), constant, isFirst);
        } else {
            if (stateType == AggregationStateType.FIRST) {
                accessor = new AggregationAccessorFirstWEval(streamNum, evaluator);
            } else if (stateType == AggregationStateType.LAST) {
                accessor = new AggregationAccessorLastWEval(streamNum, evaluator);
            } else if (stateType == AggregationStateType.WINDOW) {
                accessor = new AggregationAccessorWindowWEval(streamNum, evaluator, resultType);
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
            AggregationMethodFactory factory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinearUnbounded(validationContext.getStatementExtensionSvcContext(), this, containedType, accessorResultType, streamNum);
            return new LinearAggregationFactoryDesc(factory, containedType, scalarCollectionComponentType);
        }

        AggregationStateKeyWStream stateKey = new AggregationStateKeyWStream(streamNum, containedType, AggregationStateTypeWStream.DATAWINDOWACCESS_LINEAR, new ExprNode[0]);

        AggregationStateFactory stateFactory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinear(validationContext.getStatementExtensionSvcContext(), this, streamNum);
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
        AggregationAccessor accessor = new AggregationAccessorWindowNoEval(componentType);
        AggregationStateFactory stateFactory = validationContext.getEngineImportService().getAggregationFactoryFactory().makeLinear(validationContext.getStatementExtensionSvcContext(), this, 0);
        ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, JavaClassHelper.getArrayType(componentType), containedType, null, stateFactory, null);
        return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
    }

    private LinearAggregationFactoryDesc handleIntoTable(ExprNode[] childNodes, AggregationStateType stateType, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "For into-table use 'window(*)' or ''window(stream.*)' instead";
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
        AggregationAccessor accessor = new AggregationAccessorWindowNoEval(componentType);
        AggregationAgent agent;
        if (streamNum == 0) {
            agent = AggregationAgentDefault.INSTANCE;
        } else {
            agent = new AggregationAgentRewriteStream(streamNum);
        }
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
        AggregationAccessor defaultAccessor = stateType == AggregationStateType.FIRST ?
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
            paramNode = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            ExprEvaluator paramNodeEval = paramNode.getExprEvaluator();
            AggregationAccessor accessor;
            if (stateType == AggregationStateType.FIRST) {
                accessor = new AggregationAccessorFirstWEval(0, paramNodeEval);
            } else {
                accessor = new AggregationAccessorLastWEval(0, paramNodeEval);
            }
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, paramNodeEval.getType(), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
        }
        if (childNodes.length == 2) {
            boolean isFirst = stateType == AggregationStateType.FIRST;
            int constant = -1;
            ExprNode indexEvalNode = childNodes[1];
            if (indexEvalNode.isConstantResult()) {
                constant = (Integer) indexEvalNode.getExprEvaluator().evaluate(null, true, null);
            }
            ExprEvaluator evaluatorIndex = indexEvalNode.getExprEvaluator();
            if (evaluatorIndex.getType() != Integer.class) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires a constant index expression that returns an integer value");
            }
            AggregationAccessor accessor = new AggregationAccessorFirstLastIndexNoEval(evaluatorIndex, constant, isFirst);
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
            AggregationAccessor accessor = new AggregationAccessorWindowNoEval(componentType);
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this, accessor, JavaClassHelper.getArrayType(componentType), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, factory.getContainedEventType(), null);
        }
        if (childNodes.length == 1) {
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableServiceUtil.streamTypeFromTableColumn(tableAccess, validationContext.getStreamTypeService().getEngineURIQualifier());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtility.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            ExprEvaluator paramNodeEval = paramNode.getExprEvaluator();
            ExprAggMultiFunctionLinearAccessNodeFactoryAccess factory = new ExprAggMultiFunctionLinearAccessNodeFactoryAccess(this,
                    new AggregationAccessorWindowWEval(0, paramNodeEval, paramNodeEval.getType()), JavaClassHelper.getArrayType(paramNodeEval.getType()), original.getContainedEventType(), null, null, null);
            return new LinearAggregationFactoryDesc(factory, null, paramNodeEval.getType());
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
        ExprNodeUtility.toExpressionStringParams(writer, this.getChildNodes());
    }

    public AggregationStateType getStateType() {
        return stateType;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return super.aggregationResultFuture.getCollectionOfEvents(column, eventsPerStream, isNewData, context);
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
        return false;
    }

    private static ExprValidationException makeUnboundValidationEx(AggregationStateType stateType) {
        return new ExprValidationException(getErrorPrefix(stateType) + " requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead");
    }

    private static String getErrorPrefix(AggregationStateType stateType) {
        return ExprAggMultiFunctionUtil.getErrorPrefix(stateType.toString().toLowerCase(Locale.ENGLISH));
    }
}