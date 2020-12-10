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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.access.core.*;
import com.espertech.esper.common.internal.epl.agg.access.linear.*;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAccessorForge;
import com.espertech.esper.common.internal.epl.agg.core.AggregationForgeFactory;
import com.espertech.esper.common.internal.epl.agg.core.AggregationStateFactoryForge;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethod;
import static com.espertech.esper.common.internal.util.JavaClassHelper.isTypeInteger;

public class ExprAggMultiFunctionLinearAccessNode extends ExprAggregateNodeBase implements ExprEnumerationForge, ExprAggMultiFunctionNode {
    private final AggregationAccessorLinearType stateType;
    private AggregationForgeFactory aggregationForgeFactory;
    private EventType containedType;
    private EPTypeClass scalarCollectionComponentType;
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

        aggregationForgeFactory = desc.getFactory();
        return aggregationForgeFactory;
    }

    public AggregationForgeFactory getAggregationForgeFactory() {
        return aggregationForgeFactory;
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
        EPTypeClass resultType;
        ExprForge forge;
        ExprNode evaluatorIndex = null;
        boolean istreamOnly;
        EventType containedType;
        EPTypeClass scalarCollectionComponentType = null;

        // validate wildcard use
        boolean isWildcard = childNodes.length == 0 || childNodes.length > 0 && childNodes[0] instanceof ExprWildcard;
        if (isWildcard) {
            ExprAggMultiFunctionUtil.validateWildcardStreamNumbers(validationContext.getStreamTypeService(), stateType.toString().toLowerCase(Locale.ENGLISH));
            streamNum = 0;
            containedType = streamTypeService.getEventTypes()[0];
            EPTypeClass resultTypeStream = containedType.getUnderlyingEPType();
            resultType = resultTypeStream;
            TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(containedType);
            forge = ExprNodeUtilityMake.makeUnderlyingForge(0, resultTypeStream, tableMetadata);
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
            resultType = type.getUnderlyingEPType();
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
            EPType childType = childNodes[0].getForge().getEvaluationType();
            if (childType == EPTypeNull.INSTANCE) {
                throw new ExprValidationException("Null-type is not allowed");
            }
            resultType = (EPTypeClass) childType;
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
            if (!isTypeInteger(evaluatorIndex.getForge().getEvaluationType())) {
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

        EPTypeClass accessorResultType = resultType;
        if (stateType == AggregationAccessorLinearType.WINDOW) {
            accessorResultType = JavaClassHelper.getArrayType(resultType);
        }

        boolean isFafWindow = streamTypeService.isOnDemandStreams() && stateType == AggregationAccessorLinearType.WINDOW;
        TableMetaData tableMetadata = validationContext.getTableCompileTimeResolver().resolveTableFromEventType(containedType);

        if (tableMetadata == null && !isFafWindow && (istreamOnly || streamTypeService.isOnDemandStreams())) {
            if (optionalFilter != null) {
                positionalParams = ExprNodeUtilityMake.addExpression(positionalParams, optionalFilter);
            }
            DataInputOutputSerdeForge serde = validationContext.getSerdeResolver().serdeForAggregation(accessorResultType, validationContext.getStatementRawInfo());
            AggregationForgeFactory factory = new AggregationForgeFactoryFirstLastUnbound(this, accessorResultType, optionalFilter != null, serde);
            return new AggregationLinearFactoryDesc(factory, containedType, scalarCollectionComponentType, streamNum);
        }

        AggregationStateKeyWStream stateKey = new AggregationStateKeyWStream(streamNum, containedType, AggregationStateTypeWStream.DATAWINDOWACCESS_LINEAR, ExprNodeUtilityQuery.EMPTY_EXPR_ARRAY, optionalFilter);

        ExprForge optionalFilterForge = optionalFilter == null ? null : optionalFilter.getForge();
        boolean join = validationContext.getStreamTypeService().getEventTypes().length > 1;
        AggregationStateFactoryForge stateFactory = new AggregationStateLinearForge(this, streamNum, optionalFilterForge, join);

        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, accessorResultType,
                stateKey, stateFactory, AggregationAgentDefault.INSTANCE, containedType);
        EventType enumerationType = scalarCollectionComponentType == null ? containedType : null;

        List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(containedType, validationContext.getStatementRawInfo(), validationContext.getSerdeEventTypeRegistry(), validationContext.getSerdeResolver(), validationContext.getStateMgmtSettingsProvider());
        validationContext.getAdditionalForgeables().addAll(serdeForgeables);

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
        EPTypeClass componentType = containedType.getUnderlyingEPType();
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEvalForge(componentType);
        AggregationStateFactoryForge stateFactory = new AggregationStateLinearForge(this, 0, null, false);
        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, JavaClassHelper.getArrayType(componentType), null, stateFactory, null, containedType);

        List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(containedType, validationContext.getStatementRawInfo(), validationContext.getSerdeEventTypeRegistry(), validationContext.getSerdeResolver(), validationContext.getStateMgmtSettingsProvider());
        validationContext.getAdditionalForgeables().addAll(serdeForgeables);

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
        EPTypeClass componentType = containedType.getUnderlyingEPType();
        AggregationAccessorForge accessor = new AggregationAccessorWindowNoEvalForge(componentType);
        AggregationAgentForge agent = AggregationAgentForgeFactory.make(streamNum, optionalFilter, validationContext.getClasspathImportService(), validationContext.getStreamTypeService().isOnDemandStreams(), validationContext.getStatementName());
        AggregationForgeFactoryAccessLinear factory = new AggregationForgeFactoryAccessLinear(this, accessor, JavaClassHelper.getArrayType(componentType), null, null, agent, containedType);
        return new AggregationLinearFactoryDesc(factory, containedType, null, 0);
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

    public void toPrecedenceFreeEPL(StringWriter writer, ExprNodeRenderableFlags flags) {
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

    public EPTypeClass getComponentTypeCollection() {
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