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
package com.espertech.esper.epl.core.resultset.grouped;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenNamedParam;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.agg.service.common.AggregationService;
import com.espertech.esper.codegen.core.CodegenInstanceAux;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorUtil;
import com.espertech.esper.epl.core.resultset.rowperevent.ResultSetProcessorRowPerEventImpl;
import com.espertech.esper.epl.core.resultset.rowpergroup.ResultSetProcessorRowPerGroup;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenNames.*;
import static com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames.REF_EPS;
import static com.espertech.esper.epl.expression.codegen.ExprForgeCodegenNames.NAME_EPS;

public class ResultSetProcessorGroupedUtil {
    public final static String METHOD_APPLYAGGVIEWRESULTKEYEDVIEW = "applyAggViewResultKeyedView";
    public final static String METHOD_APPLYAGGJOINRESULTKEYEDJOIN = "applyAggJoinResultKeyedJoin";

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param aggregationService aggs
     * @param agentInstanceContext ctx
     * @param newData new data
     * @param newDataMultiKey new data keys
     * @param oldData old data
     * @param oldDataMultiKey old data keys
     * @param eventsPerStream event buffer, transient buffer
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
     * @param aggregationService aggs
     * @param agentInstanceContext ctx
     * @param newEvents new data
     * @param newDataMultiKey new data keys
     * @param oldEvents old data
     * @param oldDataMultiKey old data keys
     */
    public static void applyAggJoinResultKeyedJoin(AggregationService aggregationService, AgentInstanceContext agentInstanceContext, Set<MultiKey<EventBean>> newEvents, Object[] newDataMultiKey, Set<MultiKey<EventBean>> oldEvents, Object[] oldDataMultiKey) {
        // update aggregates
        if (!newEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : newEvents) {
                aggregationService.applyEnter(eventsPerStream.getArray(), newDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }
        if (oldEvents != null && !oldEvents.isEmpty()) {
            // apply old data to aggregates
            int count = 0;
            for (MultiKey<EventBean> eventsPerStream : oldEvents) {
                aggregationService.applyLeave(eventsPerStream.getArray(), oldDataMultiKey[count], agentInstanceContext);
                count++;
            }
        }
    }

    public static CodegenMethodNode generateGroupKeySingleCodegen(ExprNode[] groupKeyExpressions, CodegenClassScope classScope, CodegenInstanceAux instance) {
        Consumer<CodegenMethodNode> code = methodNode -> {
            if (groupKeyExpressions.length == 1) {
                CodegenMethodNode expression = CodegenLegoMethodExpression.codegenExpression(groupKeyExpressions[0].getForge(), methodNode, classScope);
                methodNode.getBlock().methodReturn(localMethod(expression, REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
                return;
            }

            methodNode.getBlock().declareVar(Object[].class, "keys", newArrayByLength(Object.class, constant(groupKeyExpressions.length)));
            for (int i = 0; i < groupKeyExpressions.length; i++) {
                CodegenMethodNode expression = CodegenLegoMethodExpression.codegenExpression(groupKeyExpressions[i].getForge(), methodNode, classScope);
                methodNode.getBlock().assignArrayElement("keys", constant(i), localMethod(expression, REF_EPS, REF_ISNEWDATA, REF_AGENTINSTANCECONTEXT));
            }
            methodNode.getBlock().methodReturn(newInstance(MultiKeyUntyped.class, ref("keys")));
        };

        return instance.getMethods().addMethod(Object.class, "generateGroupKeySingle", CodegenNamedParam.from(EventBean[].class, NAME_EPS, boolean.class, NAME_ISNEWDATA), ResultSetProcessorUtil.class, classScope, code);
    }

    public static CodegenMethodNode generateGroupKeyArrayViewCodegen(ExprNode[] groupKeyExpressions, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(groupKeyExpressions, classScope, instance);

        Consumer<CodegenMethodNode> code = method -> {
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

    public static CodegenMethodNode generateGroupKeyArrayJoinCodegen(ExprNode[] groupKeyExpressions, CodegenClassScope classScope, CodegenInstanceAux instance) {
        CodegenMethodNode generateGroupKeySingle = generateGroupKeySingleCodegen(groupKeyExpressions, classScope, instance);
        Consumer<CodegenMethodNode> code = method -> {
            method.getBlock().ifCondition(exprDotMethod(ref("resultSet"), "isEmpty")).blockReturn(constantNull())
                    .declareVar(Object[].class, "keys", newArrayByLength(Object.class, exprDotMethod(ref("resultSet"), "size")))
                    .declareVar(int.class, "count", constant(0))
                    .forEach(MultiKey.class, "eventsPerStream", ref("resultSet"))
                    .assignArrayElement("keys", ref("count"), localMethod(generateGroupKeySingle, cast(EventBean[].class, exprDotMethod(ref("eventsPerStream"), "getArray")), REF_ISNEWDATA))
                    .increment("count")
                    .blockEnd()
                    .methodReturn(ref("keys"));
        };
        return instance.getMethods().addMethod(Object[].class, "generateGroupKeyArrayJoin", CodegenNamedParam.from(Set.class, "resultSet", boolean.class, "isNewData"), ResultSetProcessorRowPerEventImpl.class, classScope, code);
    }
}
