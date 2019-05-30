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
package com.espertech.esper.common.internal.epl.resultset.grouped;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationService;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.common.internal.epl.resultset.rowperevent.ResultSetProcessorRowPerEventImpl;
import com.espertech.esper.common.internal.epl.resultset.rowpergroup.ResultSetProcessorRowPerGroup;

import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_ISNEWDATA;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode.instblock;

public class ResultSetProcessorGroupedUtil {
    public final static String METHOD_APPLYAGGVIEWRESULTKEYEDVIEW = "applyAggViewResultKeyedView";
    public final static String METHOD_APPLYAGGJOINRESULTKEYEDJOIN = "applyAggJoinResultKeyedJoin";

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param aggregationService   aggs
     * @param agentInstanceContext ctx
     * @param newData              new data
     * @param newDataMultiKey      new data keys
     * @param oldData              old data
     * @param oldDataMultiKey      old data keys
     * @param eventsPerStream      event buffer, transient buffer
     */
    public static void applyAggViewResultKeyedView(AggregationService aggregationService, AgentInstanceContext agentInstanceContext, EventBean[] newData, Object[] newDataMultiKey, EventBean[] oldData, Object[] oldDataMultiKey, EventBean[] eventsPerStream) {
        // update aggregates
        if (newData != null) {
            // apply new data to aggregates
            for (int i = 0; i < newData.length; i++) {
                eventsPerStream[0] = newData[i];
                aggregationService.applyEnter(eventsPerStream, newDataMultiKey[i], agentInstanceContext);
            }
        }
        if (oldData != null) {
            // apply old data to aggregates
            for (int i = 0; i < oldData.length; i++) {
                eventsPerStream[0] = oldData[i];
                aggregationService.applyLeave(eventsPerStream, oldDataMultiKey[i], agentInstanceContext);
            }
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param aggregationService   aggs
     * @param agentInstanceContext ctx
     * @param newEvents            new data
     * @param newDataMultiKey      new data keys
     * @param oldEvents            old data
     * @param oldDataMultiKey      old data keys
     */
    public static void applyAggJoinResultKeyedJoin(AggregationService aggregationService, AgentInstanceContext agentInstanceContext, Set<MultiKeyArrayOfKeys<EventBean>> newEvents, Object[] newDataMultiKey, Set<MultiKeyArrayOfKeys<EventBean>> oldEvents, Object[] oldDataMultiKey) {
        // update aggregates
        if (!newEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKeyArrayOfKeys<EventBean> eventsPerStream : newEvents) {
                aggregationService.applyEnter(eventsPerStream.getArray(), newDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKeyArrayOfKeys<EventBean> eventsPerStream : oldEvents) {
                aggregationService.applyLeave(eventsPerStream.getArray(), oldDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }
    }

    public static CodegenMethod generateGroupKeySingleCodegen(ExprNode[] groupKeyExpressions, MultiKeyClassRef optionalMultiKeyClasses, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = methodNode -> {
            String[] expressions = null;
            if (classScope.isInstrumented()) {
                expressions = ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(groupKeyExpressions);
            }
            methodNode.getBlock().apply(instblock(classScope, "qResultSetProcessComputeGroupKeys", REF_ISNEWDATA, constant(expressions), REF_EPS));

            if (optionalMultiKeyClasses != null && optionalMultiKeyClasses.getClassNameMK() != null) {
                CodegenMethod method = MultiKeyCodegen.codegenMethod(groupKeyExpressions, optionalMultiKeyClasses, methodNode, classScope);
                methodNode.getBlock()
                    .declareVar(Object.class, "key", localMethod(method, REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))
                    .apply(instblock(classScope, "aResultSetProcessComputeGroupKeys", REF_ISNEWDATA, ref("key")))
                    .methodReturn(ref("key"));
                return;
            }

            if (groupKeyExpressions.length > 1) {
                throw new IllegalStateException("Multiple group-by expression and no multikey");
            }

            CodegenMethod expression = CodegenLegoMethodExpression.codegenExpression(groupKeyExpressions[0].getForge(), methodNode, classScope);
            methodNode.getBlock()
                    .declareVar(Object.class, "key", localMethod(expression, REF_EPS, REF_ISNEWDATA, MEMBER_AGENTINSTANCECONTEXT))
                    .apply(instblock(classScope, "aResultSetProcessComputeGroupKeys", REF_ISNEWDATA, ref("key")))
                    .methodReturn(ref("key"));
        };

        return instance.getMethods().addMethod(Object.class, "generateGroupKeySingle", CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA), ResultSetProcessorUtil.class, classScope, code);
    }

    public static CodegenMethod generateGroupKeyArrayViewCodegen(CodegenMethod generateGroupKeySingle, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().ifRefNullReturnNull("events")
                    .declareVar(EventBean[].class, "eventsPerStream", newArrayByLength(EventBean.class, constant(1)))
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, arrayLength(ref("events"))));
            {
                CodegenBlock forLoop = method.getBlock().forLoopIntSimple("i", arrayLength(ref("events")));
                forLoop.assignArrayElement("eventsPerStream", constant(0), arrayAtIndex(ref("events"), ref("i")))
                        .assignArrayElement("keys", ref("i"), localMethod(generateGroupKeySingle, ref("eventsPerStream"), REF_ISNEWDATA));
            }
            method.getBlock().methodReturn(ref("keys"));
        };
        return instance.getMethods().addMethod(Object[].class, "generateGroupKeyArrayView", CodegenNamedParam.from(EventBean[].class, "events", boolean.class, NAME_ISNEWDATA), ResultSetProcessorRowPerGroup.class, classScope, code);
    }

    public static CodegenMethod generateGroupKeyArrayJoinCodegen(CodegenMethod generateGroupKeySingle, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethod> code = method -> {
            method.getBlock().ifCondition(exprDotMethod(ref("resultSet"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(int.class, "count", constant(0))
                    .forEach(MultiKeyArrayOfKeys.class, "eventsPerStream", ref("resultSet"))
                    .assignArrayElement("keys", ref("count"), localMethod(generateGroupKeySingle, cast(EventBean[].class, exprDotMethod(ref("eventsPerStream"), "getArray")), REF_ISNEWDATA))
                    .incrementRef("count")
                    .blockEnd()
                    .methodReturn(ref("keys"));
        };
        return instance.getMethods().addMethod(Object[].class, "generateGroupKeyArrayJoin", CodegenNamedParam.from(Set.class, "resultSet", boolean.class, "isNewData"), ResultSetProcessorRowPerEventImpl.class, classScope, code);
    }
}
