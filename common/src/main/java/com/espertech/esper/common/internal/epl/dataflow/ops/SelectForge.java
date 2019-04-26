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
package com.espertech.esper.common.internal.epl.dataflow.ops;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.EventRepresentation;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableType;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.select.StmtForgeMethodSelectResult;
import com.espertech.esper.common.internal.context.aifactory.select.StmtForgeMethodSelectUtil;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByExpressionHelper;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.util.StatementSpecRawWalkerSubselectAndDeclaredDot;
import com.espertech.esper.common.internal.filterspec.FilterSpecParam;
import com.espertech.esper.common.internal.type.AnnotationEventRepresentation;

import java.lang.annotation.Annotation;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.epl.dataflow.core.EPDataFlowServiceImpl.OP_PACKAGE_NAME;

public class SelectForge implements DataFlowOperatorForge {

    @DataFlowOpParameter
    private StatementSpecRaw select;

    @DataFlowOpParameter
    private boolean iterate;

    private EventType[] eventTypes;
    private boolean submitEventBean;
    private String classNameAIFactoryProvider;
    private int[] originatingStreamToViewableStream;

    public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
        if (context.getInputPorts().isEmpty()) {
            throw new IllegalArgumentException("Select operator requires at least one input stream");
        }
        if (context.getOutputPorts().size() != 1) {
            throw new IllegalArgumentException("Select operator requires one output stream but produces " + context.getOutputPorts().size() + " streams");
        }

        DataFlowOpOutputPort portZero = context.getOutputPorts().get(0);
        if (portZero.getOptionalDeclaredType() != null && !portZero.getOptionalDeclaredType().isUnderlying()) {
            submitEventBean = true;
        }

        // determine adapter factories for each type
        int numStreams = context.getInputPorts().size();
        eventTypes = new EventType[numStreams];
        for (int i = 0; i < numStreams; i++) {
            eventTypes[i] = context.getInputPorts().get(i).getTypeDesc().getEventType();
        }

        // validate
        if (select.getInsertIntoDesc() != null) {
            throw new ExprValidationException("Insert-into clause is not supported");
        }
        if (select.getSelectStreamSelectorEnum() != SelectClauseStreamSelectorEnum.ISTREAM_ONLY) {
            throw new ExprValidationException("Selecting remove-stream is not supported");
        }
        ExprNodeSubselectDeclaredDotVisitor visitor = StatementSpecRawWalkerSubselectAndDeclaredDot.walkSubselectAndDeclaredDotExpr(select);
        GroupByClauseExpressions groupByExpressions = GroupByExpressionHelper.getGroupByRollupExpressions(select.getGroupByExpressions(), select.getSelectClauseSpec(), select.getWhereClause(), select.getOrderByList(), null);
        if (!visitor.getSubselects().isEmpty()) {
            throw new ExprValidationException("Subselects are not supported");
        }

        Map<Integer, FilterStreamSpecRaw> streams = new HashMap<Integer, FilterStreamSpecRaw>();
        for (int streamNum = 0; streamNum < select.getStreamSpecs().size(); streamNum++) {
            StreamSpecRaw rawStreamSpec = select.getStreamSpecs().get(streamNum);
            if (!(rawStreamSpec instanceof FilterStreamSpecRaw)) {
                throw new ExprValidationException("From-clause must contain only streams and cannot contain patterns or other constructs");
            }
            streams.put(streamNum, (FilterStreamSpecRaw) rawStreamSpec);
        }

        // compile offered streams
        List<StreamSpecCompiled> streamSpecCompileds = new ArrayList<StreamSpecCompiled>();
        originatingStreamToViewableStream = new int[select.getStreamSpecs().size()];
        for (int streamNum = 0; streamNum < select.getStreamSpecs().size(); streamNum++) {
            FilterStreamSpecRaw filter = streams.get(streamNum);
            Map.Entry<Integer, DataFlowOpInputPort> inputPort = findInputPort(filter.getRawFilterSpec().getEventTypeName(), context.getInputPorts());
            if (inputPort == null) {
                throw new ExprValidationException("Failed to find stream '" + filter.getRawFilterSpec().getEventTypeName() + "' among input ports, input ports are " + Arrays.toString(getInputPortNames(context.getInputPorts())));
            }
            EventType eventType = inputPort.getValue().getTypeDesc().getEventType();
            originatingStreamToViewableStream[inputPort.getKey()] = streamNum;
            String streamAlias = filter.getOptionalStreamName();
            FilterSpecCompiled filterSpecCompiled = new FilterSpecCompiled(eventType, streamAlias, new List[]{Collections.<FilterSpecParam>emptyList()}, null);
            ViewSpec[] viewSpecs = select.getStreamSpecs().get(streamNum).getViewSpecs();
            FilterStreamSpecCompiled filterStreamSpecCompiled = new FilterStreamSpecCompiled(filterSpecCompiled, viewSpecs, streamAlias, StreamSpecOptions.DEFAULT);
            streamSpecCompileds.add(filterStreamSpecCompiled);
        }

        // create compiled statement spec
        SelectClauseSpecCompiled selectClauseCompiled = StatementLifecycleSvcUtil.compileSelectClause(select.getSelectClauseSpec());

        Annotation[] mergedAnnotations = AnnotationUtil.mergeAnnotations(context.getStatementRawInfo().getAnnotations(), context.getOperatorAnnotations());
        mergedAnnotations = addObjectArrayRepresentation(mergedAnnotations);
        StreamSpecCompiled[] streamSpecArray = streamSpecCompileds.toArray(new StreamSpecCompiled[streamSpecCompileds.size()]);

        // determine if snapshot output is needed
        OutputLimitSpec outputLimitSpec = select.getOutputLimitSpec();
        if (iterate) {
            if (outputLimitSpec != null) {
                throw new ExprValidationException("Output rate limiting is not supported with 'iterate'");
            }
            outputLimitSpec = new OutputLimitSpec(OutputLimitLimitType.SNAPSHOT, OutputLimitRateType.TERM);
            select.setOutputLimitSpec(outputLimitSpec);
        }

        // override the statement spec
        StatementSpecCompiled compiled = new StatementSpecCompiled(select, streamSpecArray, selectClauseCompiled, mergedAnnotations, groupByExpressions, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        String dataflowClassPostfix = context.getCodegenEnv().getClassPostfix() + "__dfo" + context.getOperatorNumber();
        StatementSpecCompiled containerStatement = context.getBase().getStatementSpec();
        context.getBase().setStatementSpec(compiled);

        // make forgeable
        StmtForgeMethodSelectResult forablesResult = StmtForgeMethodSelectUtil.make(true, context.getCodegenEnv().getPackageName(), dataflowClassPostfix, context.getBase(), context.getServices());

        // return the statement spec
        context.getBase().setStatementSpec(containerStatement);

        EventType outputEventType = forablesResult.getEventType();

        DataFlowOpForgeInitializeResult initializeResult = new DataFlowOpForgeInitializeResult();
        initializeResult.setTypeDescriptors(new GraphTypeDesc[]{new GraphTypeDesc(false, true, outputEventType)});
        initializeResult.setAdditionalForgeables(forablesResult.getForgeResult());

        for (StmtClassForgeable forgeable : forablesResult.getForgeResult().getForgeables()) {
            if (forgeable.getForgeableType() == StmtClassForgeableType.AIFACTORYPROVIDER) {
                classNameAIFactoryProvider = forgeable.getClassName();
            }
        }

        return initializeResult;
    }

    private Annotation[] addObjectArrayRepresentation(Annotation[] mergedAnnotations) {
        List<Annotation> annotations = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (!(annotation instanceof EventRepresentation)) {
                annotations.add(annotation);
                continue;
            }
        }
        annotations.add(new AnnotationEventRepresentation(EventUnderlyingType.OBJECTARRAY));
        return annotations.toArray(new Annotation[annotations.size()]);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        SAIFFInitializeBuilder builder = new SAIFFInitializeBuilder(OP_PACKAGE_NAME + ".select.SelectFactory", this.getClass(), "select", parent, symbols, classScope);
        return builder.eventtypesMayNull("eventTypes", eventTypes)
                .constant("submitEventBean", submitEventBean)
                .constant("iterate", iterate)
                .constant("originatingStreamToViewableStream", originatingStreamToViewableStream)
                .expression("factoryProvider", newInstance(classNameAIFactoryProvider, symbols.getAddInitSvc(builder.getMethod())))
                .build();
    }

    private String[] getInputPortNames(Map<Integer, DataFlowOpInputPort> inputPorts) {
        List<String> portNames = new ArrayList<String>();
        for (Map.Entry<Integer, DataFlowOpInputPort> entry : inputPorts.entrySet()) {
            if (entry.getValue().getOptionalAlias() != null) {
                portNames.add(entry.getValue().getOptionalAlias());
                continue;
            }
            if (entry.getValue().getStreamNames().size() == 1) {
                portNames.add(entry.getValue().getStreamNames().iterator().next());
            }
        }
        return portNames.toArray(new String[portNames.size()]);
    }

    private Map.Entry<Integer, DataFlowOpInputPort> findInputPort(String eventTypeName, Map<Integer, DataFlowOpInputPort> inputPorts) {
        for (Map.Entry<Integer, DataFlowOpInputPort> entry : inputPorts.entrySet()) {
            if (entry.getValue().getOptionalAlias() != null && entry.getValue().getOptionalAlias().equals(eventTypeName)) {
                return entry;
            }
            if (entry.getValue().getStreamNames().size() == 1 && entry.getValue().getStreamNames().iterator().next().equals(eventTypeName)) {
                return entry;
            }
        }
        return null;
    }
}
