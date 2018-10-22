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
package com.espertech.esper.common.internal.metrics.audit;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowState;
import com.espertech.esper.common.internal.collection.LRUCache;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackSchedule;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMinimal;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.util.EventBeanSummarizer;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Arrays;

/**
 * Global boolean for enabling and disable audit path reporting.
 */
public class AuditPath {

    private static final Logger AUDIT_LOG_DESTINATION = LoggerFactory.getLogger(AuditPath.AUDIT_LOG);
    private static LRUCache<AuditPatternInstanceKey, Integer> patternInstanceCounts;

    private volatile static AuditCallback auditCallback;

    /**
     * Logger destination for the query plan logging.
     */
    public static final String QUERYPLAN_LOG = "com.espertech.esper.queryplan";

    /**
     * Logger destination for the JDBC logging.
     */
    public static final String JDBC_LOG = "com.espertech.esper.jdbc";

    /**
     * Logger destination for the audit logging.
     */
    public static final String AUDIT_LOG = "com.espertech.esper.audit";

    /**
     * Public access.
     */
    public static boolean isAuditEnabled = false;

    private static String auditPattern;

    public static void setAuditPattern(String auditPattern) {
        AuditPath.auditPattern = auditPattern;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param theEvent             event
     * @param exprEvaluatorContext ctx
     */
    public static void auditInsert(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        auditLog(exprEvaluatorContext, AuditEnum.INSERT, EventBeanSummarizer.summarize(theEvent));
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param newData     new events
     * @param oldData     old events
     * @param context     context
     * @param viewFactory view factory
     */
    public static void auditView(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext context, ViewFactory viewFactory) {
        if (AuditPath.isInfoEnabled()) {
            auditLog(context, AuditEnum.VIEW, viewFactory.getViewName() + " insert {" + EventBeanSummarizer.summarize(newData) + "} remove {" + EventBeanSummarizer.summarize(oldData) + "}");
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param event      event
     * @param context    context
     * @param filterText text for filter
     */
    public static void auditStream(EventBean event, ExprEvaluatorContext context, String filterText) {
        if (AuditPath.isInfoEnabled()) {
            String eventText = EventBeanSummarizer.summarize(event);
            auditLog(context, AuditEnum.STREAM, filterText + " inserted " + eventText);
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param newData    new data
     * @param oldData    old data
     * @param context    context
     * @param filterText text for filter
     */
    public static void auditStream(EventBean[] newData, EventBean[] oldData, ExprEvaluatorContext context, String filterText) {
        if (AuditPath.isInfoEnabled()) {
            String inserted = EventBeanSummarizer.summarize(newData);
            String removed = EventBeanSummarizer.summarize(oldData);
            auditLog(context, AuditEnum.STREAM, filterText + " insert {" + inserted + "} remove {" + removed + "}");
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param nextScheduledTime    time
     * @param agentInstanceContext ctx
     * @param scheduleHandle       handle
     * @param name                 name
     * @param objectType           object type
     */
    public static void auditScheduleAdd(long nextScheduledTime, AgentInstanceContext agentInstanceContext, ScheduleHandle scheduleHandle, ScheduleObjectType objectType, String name) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.write("add after ");
            message.write(Long.toString(nextScheduledTime));
            printScheduleObjectType(message, objectType, name, scheduleHandle);
            AuditPath.auditLog(agentInstanceContext, AuditEnum.SCHEDULE, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param agentInstanceContext ctx
     * @param scheduleHandle       handle
     * @param name                 name
     * @param objectType           object type
     */
    public static void auditScheduleRemove(AgentInstanceContext agentInstanceContext, ScheduleHandle scheduleHandle, ScheduleObjectType objectType, String name) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.write("remove");
            printScheduleObjectType(message, objectType, name, scheduleHandle);
            AuditPath.auditLog(agentInstanceContext, AuditEnum.SCHEDULE, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param agentInstanceContext ctx
     * @param objectType           object type
     * @param name                 name
     */
    public static void auditScheduleFire(AgentInstanceContext agentInstanceContext, ScheduleObjectType objectType, String name) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.write("fire");
            printScheduleObjectType(message, objectType, name);
            AuditPath.auditLog(agentInstanceContext, AuditEnum.SCHEDULE, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param name                 name
     * @param value                value
     * @param exprEvaluatorContext ctx
     */
    public static void auditProperty(String name, Object value, ExprEvaluatorContext exprEvaluatorContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.append(name).append(" value ");
            renderNonParameterValue(message, value);
            AuditPath.auditLog(exprEvaluatorContext, AuditEnum.PROPERTY, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param text                 name
     * @param value                value
     * @param exprEvaluatorContext ctx
     */
    public static void auditExpression(String text, Object value, ExprEvaluatorContext exprEvaluatorContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.append(text).append(" value ");
            renderNonParameterValue(message, value);
            AuditPath.auditLog(exprEvaluatorContext, AuditEnum.EXPRESSION, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param factoryNode          factory
     * @param from                 from
     * @param matchEvent           state
     * @param isQuitted            quitted-flag
     * @param agentInstanceContext ctx
     */
    public static void auditPatternTrue(EvalFactoryNode factoryNode, Object from, MatchedEventMapMinimal matchEvent, boolean isQuitted, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            String message = patternToStringEvaluateTrue(factoryNode, matchEvent, from, isQuitted);
            AuditPath.auditLog(agentInstanceContext, AuditEnum.PATTERN, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param factoryNode          factory
     * @param from                 from
     * @param agentInstanceContext ctx
     */
    public static void auditPatternFalse(EvalFactoryNode factoryNode, Object from, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            String message = patternToStringEvaluateFalse(factoryNode, from);
            AuditPath.auditLog(agentInstanceContext, AuditEnum.PATTERN, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param name                 name
     * @param value                value
     * @param exprEvaluatorContext ctx
     */
    public static void auditExprDef(String name, Object value, ExprEvaluatorContext exprEvaluatorContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            message.append(name).append(" value ");
            renderNonParameterValue(message, value);
            AuditPath.auditLog(exprEvaluatorContext, AuditEnum.EXPRDEF, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param increase             flag whether plus one or minus one
     * @param factoryNode          factory
     * @param agentInstanceContext ctx
     */
    public synchronized static void auditPatternInstance(boolean increase, EvalFactoryNode factoryNode, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            if (patternInstanceCounts == null) {
                patternInstanceCounts = new LRUCache<>(100);
            }
            AuditPatternInstanceKey key = new AuditPatternInstanceKey(agentInstanceContext.getRuntimeURI(), agentInstanceContext.getStatementId(), agentInstanceContext.getAgentInstanceId(), factoryNode.getTextForAudit());
            Integer existing = patternInstanceCounts.get(key);
            int count;
            if (existing == null) {
                count = increase ? 1 : 0;
            } else {
                count = existing + (increase ? 1 : -1);
            }
            StringWriter writer = new StringWriter();
            patternInstanceCounts.put(key, count);
            writePatternExpr(factoryNode, writer);

            if (increase) {
                writer.write(" increased to " + count);
            } else {
                writer.write(" decreased to " + count);
            }

            auditLog(agentInstanceContext, AuditEnum.PATTERNINSTANCES, writer.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param dataflowName         name
     * @param dataFlowInstanceId   id
     * @param state                old state
     * @param newState             new state
     * @param agentInstanceContext ctx
     */
    public static void auditDataflowTransition(String dataflowName, String dataFlowInstanceId, EPDataFlowState state, EPDataFlowState newState, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            writeDataflow(message, dataflowName, dataFlowInstanceId);
            message.append(" from state ").append(state == null ? "(none)" : state.name()).append(" to state ").append(newState.toString());
            auditLog(agentInstanceContext, AuditEnum.DATAFLOW_TRANSITION, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param dataflowName         name
     * @param dataFlowInstanceId   id
     * @param operatorName         name of op
     * @param operatorNumber       num of op
     * @param agentInstanceContext ctx
     */
    public static void auditDataflowSource(String dataflowName, String dataFlowInstanceId, String operatorName, int operatorNumber, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            writeDataflow(message, dataflowName, dataFlowInstanceId);
            writeDataflowOp(message, operatorName, operatorNumber);
            message.append(" invoking source.next()");
            auditLog(agentInstanceContext, AuditEnum.DATAFLOW_SOURCE, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param dataflowName         name
     * @param dataFlowInstanceId   id
     * @param operatorName         name of op
     * @param operatorNumber       num of op
     * @param agentInstanceContext ctx
     * @param params               params
     */
    public static void auditDataflowOp(String dataflowName, String dataFlowInstanceId, String operatorName, int operatorNumber, Object[] params, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter message = new StringWriter();
            writeDataflow(message, dataflowName, dataFlowInstanceId);
            writeDataflowOp(message, operatorName, operatorNumber);
            message.append(" parameters ").append(Arrays.toString(params));
            auditLog(agentInstanceContext, AuditEnum.DATAFLOW_OP, message.toString());
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param allocate             allocate
     * @param agentInstanceContext ctx
     */
    public static void auditContextPartition(boolean allocate, AgentInstanceContext agentInstanceContext) {
        if (AuditPath.isInfoEnabled()) {
            StringWriter writer = new StringWriter();
            writer.write(allocate ? "Allocate" : "Destroy");
            writer.write(" cpid ");
            writer.write(Integer.toString(agentInstanceContext.getAgentInstanceId()));
            auditLog(agentInstanceContext, AuditEnum.CONTEXTPARTITION, writer.toString());
        }
    }

    private static void auditLog(ExprEvaluatorContext ctx, AuditEnum category, String message) {
        if (auditPattern == null) {
            String text = AuditContext.defaultFormat(ctx.getStatementName(), ctx.getAgentInstanceId(), category, message);
            AUDIT_LOG_DESTINATION.info(text);
        } else {
            String result = auditPattern
                    .replace("%s", ctx.getStatementName())
                    .replace("%d", ctx.getDeploymentId())
                    .replace("%u", ctx.getRuntimeURI())
                    .replace("%i", Integer.toString(ctx.getAgentInstanceId()))
                    .replace("%c", category.getValue())
                    .replace("%m", message);
            AUDIT_LOG_DESTINATION.info(result);
        }
        if (auditCallback != null) {
            auditCallback.audit(new AuditContext(ctx.getRuntimeURI(), ctx.getDeploymentId(), ctx.getStatementName(), ctx.getAgentInstanceId(), category, message));
        }
    }

    public static boolean isInfoEnabled() {
        return AUDIT_LOG_DESTINATION.isInfoEnabled() || auditCallback != null;
    }

    public static void setAuditCallback(AuditCallback auditCallback) {
        AuditPath.auditCallback = auditCallback;
    }

    public static AuditCallback getAuditCallback() {
        return auditCallback;
    }

    private static void printScheduleObjectType(StringWriter message, ScheduleObjectType objectType, String name) {
        message.append(" ").append(objectType.name()).append(" '").append(name).append("'");
    }

    private static void printScheduleObjectType(StringWriter message, ScheduleObjectType objectType, String name, ScheduleHandle scheduleHandle) {
        printScheduleObjectType(message, objectType, name);
        message.append(" handle '");
        printHandle(message, scheduleHandle);
        message.append("'");
    }

    private static void printHandle(StringWriter message, ScheduleHandle handle) {
        if (handle instanceof EPStatementHandleCallbackSchedule) {
            EPStatementHandleCallbackSchedule callback = (EPStatementHandleCallbackSchedule) handle;
            JavaClassHelper.writeInstance(message, callback.getScheduleCallback(), false);
        } else {
            JavaClassHelper.writeInstance(message, handle, false);
        }
    }

    private static String patternToStringEvaluateTrue(EvalFactoryNode factoryNode, MatchedEventMapMinimal matchEvent, Object fromNode, boolean isQuitted) {

        StringWriter writer = new StringWriter();

        writePatternExpr(factoryNode, writer);
        writer.write(" evaluate-true {");

        writer.write(" from: ");
        JavaClassHelper.writeInstance(writer, fromNode, false);

        writer.write(" map: {");
        String delimiter = "";
        Object[] data = matchEvent.getMatchingEvents();
        for (int i = 0; i < data.length; i++) {
            String name = matchEvent.getMeta().getTagsPerIndex()[i];
            Object value = data[i];
            writer.write(delimiter);
            writer.write(name);
            writer.write("=");
            if (value instanceof EventBean) {
                writer.write(((EventBean) value).getUnderlying().toString());
            } else if (value instanceof EventBean[]) {
                writer.write(EventBeanSummarizer.summarize((EventBean[]) value));
            }
            delimiter = ", ";
        }

        writer.write("} quitted: ");
        writer.write(Boolean.toString(isQuitted));

        writer.write("}");
        return writer.toString();
    }

    private static String patternToStringEvaluateFalse(EvalFactoryNode factoryNode, Object fromNode) {

        StringWriter writer = new StringWriter();
        writePatternExpr(factoryNode, writer);
        writer.write(" evaluate-false {");

        writer.write(" from ");
        JavaClassHelper.writeInstance(writer, fromNode, false);

        writer.write("}");
        return writer.toString();
    }

    private static void renderNonParameterValue(StringWriter message, Object value) {
        JavaClassHelper.getObjectValuePretty(value, message);
    }

    private static void writeDataflow(StringWriter message, String dataflowName, String dataFlowInstanceId) {
        message.append("dataflow ").append(dataflowName).append(" instance ").append(dataFlowInstanceId == null ? "(unnamed)" : dataFlowInstanceId);
    }

    private static void writeDataflowOp(StringWriter message, String operatorName, int operatorNumber) {
        message.append(" operator ").append(operatorName).append("(").append(Integer.toString(operatorNumber)).append(")");
    }

    private static void writePatternExpr(EvalFactoryNode factoryNode, StringWriter writer) {
        if (factoryNode.getTextForAudit() != null) {
            writer.write('(');
            writer.write(factoryNode.getTextForAudit());
            writer.write(')');
        } else {
            JavaClassHelper.writeInstance(writer, "subexr", factoryNode);
        }
    }
}
