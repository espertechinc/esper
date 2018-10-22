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
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.agg.access.core.*;
import com.espertech.esper.common.internal.epl.agg.access.linear.*;
import com.espertech.esper.common.internal.epl.agg.core.*;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeUtil;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetadataColumnAggregation;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.Locale;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;

public class ExprAggMultiFunctionLinearAccessNode extends ExprAggregateNodeBase implements ExprEnumerationForge, ExprAggMultiFunctionNode {
    private final AggregationAccessorLinearType stateType;
    private EventType containedType;
    private Class scalarCollectionComponentType;
    private EventType streamType;

    public ExprAggMultiFunctionLinearAccessNode(AggregationAccessorLinearType stateType) {
        super(false);
        this.stateType = stateType;
    }

    public AggregationForgeFactory validateAggregationChild(ExprValidationContext validationContext) throws ExprValidationException {
        AggregationLinearFactoryDesc desc;

        // handle table-access expression (state provided, accessor needed)
        if (validationContext.getStatementRawInfo().getStatementType() == StatementType.CREATE_TABLE) {
            // handle create-table statements (state creator and default accessor, limited to certain options)
            desc = handleCreateTable(positionalParams, stateType, validationContext);
        } else if (validationContext.getStatementRawInfo().getIntoTableName() != null) {
            // handle into-table (state provided, accessor and agent needed, validation done by factory)
            desc = handleIntoTable(positionalParams, stateType, validationContext);
        } else {
            // handle standalone
            desc = handleNonIntoTable(positionalParams, stateType, validationContext);
        }

        containedType = desc.getEnumerationEventType();
        scalarCollectionComponentType = desc.getScalarCollectionType();

        EventType[] streamTypes = validationContext.getStreamTypeService().getEventTypes();
        streamType = desc.getStreamNum() >= streamTypes.length ? streamTypes[0] : streamTypes[desc.getStreamNum()];

        return desc.getFactory();
    }

    public AggregationTableReadDesc validateAggregationTableRead(ExprValidationContext validationContext, TableMetadataColumnAggregation tableAccessColumn, TableMetaData table) throws ExprValidationException {
        AggregationPortableValidation validation = tableAccessColumn.getAggregationPortableValidation();
        if (!(validation instanceof AggregationPortableValidationLinear)) {
            throw new ExprValidationException("Invalid aggregation column type for column '" + tableAccessColumn.getColumnName() + "'");
        }
        AggregationPortableValidationLinear validationLinear = (AggregationPortableValidationLinear) validation;

        if (stateType == AggregationAccessorLinearType.FIRST || stateType == AggregationAccessorLinearType.LAST) {
            return handleTableAccessFirstLast(getChildNodes(), stateType, validationContext, validationLinear);
        } else if (stateType == AggregationAccessorLinearType.WINDOW) {
            return handleTableAccessWindow(getChildNodes(), validationContext, validationLinear);
        }
        throw new IllegalStateException("Unrecognized type " + stateType);
    }

    public ExprEnumerationEval getExprEvaluatorEnumeration() {
        throw ExprNodeUtilityMake.makeUnsupportedCompileTime();
    }

    public CodegenExpression evaluateGetROCollectionScalarCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getCollectionScalar", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public CodegenExpression evaluateGetEventBeanCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getEventBean", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    public CodegenExpression evaluateGetROCollectionEventsCodegen(CodegenMethodScope parent, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression future = getAggFuture(codegenClassScope);
        return exprDotMethod(future, "getCollectionOfEvents", constant(column), exprSymbol.getAddEPS(parent), exprSymbol.getAddIsNewData(parent), exprSymbol.getAddExprEvalCtx(parent));
    }

    private AggregationLinearFactoryDesc handleNonIntoTable(ExprNode[] childNodes, AggregationAccessorLinearType stateType, ExprValidationContext validationContext) throws ExprValidationException {

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
            TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(containedType);
            forge = ExprNodeUtilityMake.makeUnderlyingForge(0, resultType, tableMetadata);
            istreamOnly = getIstreamOnly(streamTypeService, 0);
            if ((stateType == AggregationAccessorLinearType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
        } else if (childNodes.length > 0 && childNodes[0] instanceof ExprStreamUnderlyingNode) {
            // validate "stream.*"
            streamNum = ExprAggMultiFunctionUtil.validateStreamWildcardGetStreamNum(childNodes[0]);
            istreamOnly = getIstreamOnly(streamTypeService, streamNum);
            if ((stateType == AggregationAccessorLinearType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
                throw makeUnboundValidationEx(stateType);
            }
            EventType type = streamTypeService.getEventTypes()[streamNum];
            containedType = type;
            resultType = type.getUnderlyingType();
            TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(type);
            forge = ExprNodeUtilityMake.makeUnderlyingForge(streamNum, resultType, tableMetadata);
        } else {
            // validate when neither wildcard nor "stream.*"
            ExprNode child = childNodes[0];
            Set<Integer> streams = ExprNodeUtilityQuery.getIdentStreamNumbers(child);
            if (streams.isEmpty() || (streams.size() > 1)) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires that any child expressions evaluate properties of the same stream; Use 'firstever' or 'lastever' or 'nth' instead");
            }
            streamNum = streams.iterator().next();
            istreamOnly = getIstreamOnly(streamTypeService, streamNum);
            if ((stateType == AggregationAccessorLinearType.WINDOW) && istreamOnly && !streamTypeService.isOnDemandStreams()) {
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
            if (stateType == AggregationAccessorLinearType.WINDOW) {
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
            boolean isFirst = stateType == AggregationAccessorLinearType.FIRST;
            int constant = -1;
            ExprForge forgeIndex;
            if (evaluatorIndex.getForge().getForgeConstantType().isCompileTimeConstant()) {
                constant = (Integer) evaluatorIndex.getForge().getExprEvaluator().evaluate(null, true, null);
                forgeIndex = null;
            } else {
                forgeIndex = evaluatorIndex.getForge();
            }
            accessor = new AggregationAccessorFirstLastIndexWEvalForge(streamNum, forge, forgeIndex, constant, isFirst);
        } else {
            if (stateType == AggregationAccessorLinearType.FIRST) {
                accessor = new AggregationAccessorFirstWEvalForge(streamNum, forge);
            } else if (stateType == AggregationAccessorLinearType.LAST) {
                accessor = new AggregationAccessorLastWEvalForge(streamNum, forge);
            } else if (stateType == AggregationAccessorLinearType.WINDOW) {
                accessor = new AggregationAccessorWindowWEvalForge(streamNum, forge, resultType);
            } else {
                throw new IllegalStateException("Access type is undefined or not known as code '" + stateType + "'");
            }
        }

        Class accessorResultType = resultType;
        if (stateType == AggregationAccessorLinearType.WINDOW) {
            accessorResultType = JavaClassHelper.getArrayType(resultType);
        }

        boolean isFafWindow = streamTypeService.isOnDemandStreams() && stateType == AggregationAccessorLinearType.WINDOW;
        TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(containedType);

        if (tableMetadata == null && !isFafWindow && (istreamOnly || streamTypeService.isOnDemandStreams())) {
            if (optionalFilter != null) {
                positionalParams = ExprNodeUtilityMake.addExpression(positionalParams, optionalFilter);
            }
            AggregationForgeFactory factory = new AggregationFactoryMethodFirstLastUnbound(this, containedType, accessorResultType, streamNum, optionalFilter != null);
            return new AggregationLinearFactoryDesc(factory, containedType, scalarCollectionComponentType, streamNum);
        }

        AggregationStateKeyWStream stateKey = new AggregationStateKeyWStream(streamNum, containedType, AggregationStateTypeWStream.DATAWINDOWACCESS_LINEAR, ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY, optionalFilter);

        ExprForge optionalFilterForge = optionalFilter == null ? null : optionalFilter.getForge();
        AggregationStateFactoryForge stateFactory = new AggregationStateLinearForge(this, streamNum, optionalFilterForge);

        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, accessorResultType,
                stateKey, stateFactory, AggregationAgentDefault.INSTANCE, containedType);
        EventType enumerationType = scalarCollectionComponentType == null ? containedType : null;
        return new AggregationLinearFactoryDesc(factory, enumerationType, scalarCollectionComponentType, streamNum);
    }

    private AggregationLinearFactoryDesc handleCreateTable(ExprNode[] childNodes, AggregationAccessorLinearType stateType, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "For tables columns, the " + stateType.name().toLowerCase(Locale.ENGLISH) + " aggregation function requires the 'window(*)' declaration";
        if (stateType != AggregationAccessorLinearType.WINDOW) {
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
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEvalForge(componentType);
        AggregationStateFactoryForge stateFactory = new AggregationStateLinearForge(this, 0, null);
        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, JavaClassHelper.getArrayType(componentType), null, stateFactory, null, containedType);
        return new AggregationLinearFactoryDesc(factory, containedType, null, 0);
    }

    private AggregationLinearFactoryDesc handleIntoTable(ExprNode[] childNodes, AggregationAccessorLinearType stateType, ExprValidationContext validationContext) throws ExprValidationException {
        String message = "For into-table use 'window(*)' or 'window(stream.*)' instead";
        if (stateType != AggregationAccessorLinearType.WINDOW) {
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
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEvalForge(componentType);
        AggregationAgentForge agent = AggregationAgentForgeFactory.make(streamNum, optionalFilter, validationContext.getClasspathImportService(), validationContext.getStreamTypeService().isOnDemandStreams(), validationContext.getStatementName());
        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, JavaClassHelper.getArrayType(componentType), null, null, agent, containedType);
        return new AggregationLinearFactoryDesc(factory, containedType, null, 0);
    }

    private AggregationTableReadDesc handleTableAccessFirstLast(ExprNode[] childNodes, AggregationAccessorLinearType stateType, ExprValidationContext validationContext, AggregationPortableValidationLinear validationLinear)
            throws ExprValidationException {
        Class underlyingType = validationLinear.getContainedEventType().getUnderlyingType();
        if (childNodes.length == 0) {
            AggregationTAAReaderLinearFirstLastForge forge = new AggregationTAAReaderLinearFirstLastForge(underlyingType, stateType, null);
            return new AggregationTableReadDesc(forge, null, null, validationLinear.getContainedEventType());
        }
        if (childNodes.length == 1) {
            if (childNodes[0] instanceof ExprWildcard) {
                AggregationTAAReaderLinearFirstLastForge forge = new AggregationTAAReaderLinearFirstLastForge(underlyingType, stateType, null);
                return new AggregationTableReadDesc(forge, null, null, validationLinear.getContainedEventType());
            }
            if (childNodes[0] instanceof ExprStreamUnderlyingNode) {
                throw new ExprValidationException("Stream-wildcard is not allowed for table column access");
            }
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableCompileTimeUtil.streamTypeFromTableColumn(validationLinear.getContainedEventType());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            AggregationTAAReaderLinearFirstLastForge forge = new AggregationTAAReaderLinearFirstLastForge(paramNode.getForge().getEvaluationType(), stateType, paramNode);
            return new AggregationTableReadDesc(forge, null, null, null);
        }
        if (childNodes.length == 2) {
            Integer constant = null;
            ExprNode indexEvalNode = childNodes[1];
            Class indexEvalType = indexEvalNode.getForge().getEvaluationType();
            if (indexEvalType != Integer.class && indexEvalType != int.class) {
                throw new ExprValidationException(getErrorPrefix(stateType) + " requires a constant index expression that returns an integer value");
            }

            ExprNode indexExpr;
            if (indexEvalNode.getForge().getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
                constant = (Integer) indexEvalNode.getForge().getExprEvaluator().evaluate(null, true, null);
                indexExpr = null;
            } else {
                indexExpr = indexEvalNode;
            }
            AggregationTAAReaderLinearFirstLastIndexForge forge = new AggregationTAAReaderLinearFirstLastIndexForge(underlyingType, stateType, constant, indexExpr);
            return new AggregationTableReadDesc(forge, null, null, validationLinear.getContainedEventType());
        }
        throw new ExprValidationException("Invalid number of parameters");
    }

    private AggregationTableReadDesc handleTableAccessWindow(ExprNode[] childNodes, ExprValidationContext validationContext, AggregationPortableValidationLinear validationLinear)
            throws ExprValidationException {

        if (childNodes.length == 0 || (childNodes.length == 1 && childNodes[0] instanceof ExprWildcard)) {
            Class componentType = validationLinear.getContainedEventType().getUnderlyingType();
            AggregationTAAReaderLinearWindowForge forge = new AggregationTAAReaderLinearWindowForge(JavaClassHelper.getArrayType(componentType), null);
            return new AggregationTableReadDesc(forge, validationLinear.getContainedEventType(), null, null);
        }
        if (childNodes.length == 1) {
            // Expressions apply to events held, thereby validate in terms of event value expressions
            ExprNode paramNode = childNodes[0];
            StreamTypeServiceImpl streams = TableCompileTimeUtil.streamTypeFromTableColumn(validationLinear.getContainedEventType());
            ExprValidationContext localValidationContext = new ExprValidationContext(streams, validationContext);
            paramNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.AGGPARAM, paramNode, localValidationContext);
            Class paramNodeType = JavaClassHelper.getBoxedType(paramNode.getForge().getEvaluationType());
            AggregationTAAReaderLinearWindowForge forge = new AggregationTAAReaderLinearWindowForge(JavaClassHelper.getArrayType(paramNodeType), paramNode);
            return new AggregationTableReadDesc(forge, null, paramNodeType, null);
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
        ExprNodeUtilityPrint.toExpressionStringParams(writer, this.getChildNodes());
    }

    public AggregationAccessorLinearType getStateType() {
        return stateType;
    }

    public EventType getEventTypeCollection(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        if (stateType == AggregationAccessorLinearType.FIRST || stateType == AggregationAccessorLinearType.LAST) {
            return null;
        }
        return containedType;
    }

    public Class getComponentTypeCollection() {
        return scalarCollectionComponentType;
    }

    public EventType getEventTypeSingle(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        if (stateType == AggregationAccessorLinearType.FIRST || stateType == AggregationAccessorLinearType.LAST) {
            return containedType;
        }
        return null;
    }

    protected boolean equalsNodeAggregateMethodOnly(ExprAggregateNode node) {
        if (!(node instanceof ExprAggMultiFunctionLinearAccessNode)) {
            return false;
        }
        ExprAggMultiFunctionLinearAccessNode other = (ExprAggMultiFunctionLinearAccessNode) node;
        return stateType == other.stateType && containedType == other.containedType && scalarCollectionComponentType == other.scalarCollectionComponentType;
    }

    private static ExprValidationException makeUnboundValidationEx(AggregationAccessorLinearType stateType) {
        return new ExprValidationException(getErrorPrefix(stateType) + " requires that the aggregated events provide a remove stream; Please define a data window onto the stream or use 'firstever', 'lastever' or 'nth' instead");
    }

    private static String getErrorPrefix(AggregationAccessorLinearType stateType) {
        return ExprAggMultiFunctionUtil.getErrorPrefix(stateType.toString().toLowerCase(Locale.ENGLISH));
    }

    protected boolean isFilterExpressionAsLastParameter() {
        return false;
    }

    public EventType getStreamType() {
        return streamType;
    }
}