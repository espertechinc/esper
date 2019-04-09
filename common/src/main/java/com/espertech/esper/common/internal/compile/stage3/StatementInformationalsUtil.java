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
package com.espertech.esper.common.internal.compile.stage3;

import com.espertech.esper.common.client.annotation.Drop;
import com.espertech.esper.common.client.annotation.Hint;
import com.espertech.esper.common.client.annotation.Priority;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementLifecycleSvcUtil;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecWalkUtil;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.context.module.StatementInformationalsCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSummaryVisitor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectSubscriberDescriptor;
import com.espertech.esper.common.internal.epl.util.StatementSpecRawWalkerExpr;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatementInformationalsUtil {

    public final static String EPL_ONSTART_SCRIPT_NAME = "on_statement_start";
    public final static String EPL_ONSTOP_SCRIPT_NAME = "on_statement_stop";
    public final static String EPL_ONLISTENERUPDATE_SCRIPT_NAME = "on_statement_listener_update";

    public static StatementInformationalsCompileTime getInformationals(StatementBaseInfo base,
                                                                       List<FilterSpecCompiled> filterSpecCompileds,
                                                                       List<ScheduleHandleCallbackProvider> schedules,
                                                                       List<NamedWindowConsumerStreamSpec> namedWindowConsumers,
                                                                       boolean allowContext,
                                                                       SelectSubscriberDescriptor selectSubscriberDescriptor,
                                                                       CodegenPackageScope packageScope,
                                                                       StatementCompileTimeServices services) {
        StatementSpecCompiled specCompiled = base.getStatementSpec();

        boolean alwaysSynthesizeOutputEvents = specCompiled.getRaw().getInsertIntoDesc() != null | specCompiled.getRaw().getForClauseSpec() != null || specCompiled.getSelectClauseCompiled().isDistinct()
            || specCompiled.getRaw().getCreateDataFlowDesc() != null;
        boolean needDedup = isNeedDedup(filterSpecCompileds);
        boolean hasSubquery = !base.getStatementSpec().getSubselectNodes().isEmpty();
        boolean canSelfJoin = StatementSpecWalkUtil.isPotentialSelfJoin(specCompiled) || needDedup;

        // Determine stateless statement
        boolean stateless = determineStatelessSelect(base.getStatementRawInfo().getStatementType(), base.getStatementSpec().getRaw(), !base.getStatementSpec().getSubselectNodes().isEmpty());

        String contextName = null;
        String contextModuleName = null;
        NameAccessModifier contextVisibility = null;
        if (allowContext) {
            ContextCompileTimeDescriptor descriptor = base.getStatementRawInfo().getOptionalContextDescriptor();
            if (descriptor != null) {
                contextName = descriptor.getContextName();
                contextModuleName = descriptor.getContextModuleName();
                contextVisibility = descriptor.getContextVisibility();
            }
        }

        AnnotationAnalysisResult annotationData = AnnotationAnalysisResult.analyzeAnnotations(base.getStatementSpec().getAnnotations());
        // Hint annotations are often driven by variables
        boolean hasHint = false;
        if (base.getStatementSpec().getRaw().getAnnotations() != null) {
            for (Annotation annotation : base.getStatementRawInfo().getAnnotations()) {
                if (annotation instanceof Hint) {
                    hasHint = true;
                }
            }
        }

        boolean hasVariables = hasHint || !base.getStatementSpec().getRaw().getReferencedVariables().isEmpty() || base.getStatementSpec().getRaw().getCreateContextDesc() != null;
        boolean writesToTables = StatementLifecycleSvcUtil.isWritesToTables(base.getStatementSpec().getRaw(), services.getTableCompileTimeResolver());
        boolean hasTableAccess = StatementLifecycleSvcUtil.determineHasTableAccess(base.getStatementSpec().getSubselectNodes(), base.getStatementSpec().getRaw(), services.getTableCompileTimeResolver());

        Map<StatementProperty, Object> properties = new HashMap<>();
        if (services.getConfiguration().getCompiler().getByteCode().isAttachEPL()) {
            properties.put(StatementProperty.EPL, base.getCompilable().toEPL());
        }

        String insertIntoLatchName = null;
        if (base.getStatementSpec().getRaw().getInsertIntoDesc() != null || base.getStatementSpec().getRaw().getOnTriggerDesc() instanceof OnTriggerMergeDesc) {
            if (base.getStatementSpec().getRaw().getInsertIntoDesc() != null) {
                insertIntoLatchName = base.getStatementSpec().getRaw().getInsertIntoDesc().getEventTypeName();
            } else {
                insertIntoLatchName = "merge";
            }
        }

        boolean allowSubscriber = services.getConfiguration().getCompiler().getByteCode().isAllowSubscriber();

        List<ExpressionScriptProvided> statementScripts = base.getStatementSpec().getRaw().getScriptExpressions();
        List<ExpressionScriptProvided> onScripts = new ArrayList<>(2);
        if (statementScripts != null) {
            for (ExpressionScriptProvided script : statementScripts) {
                if (script.getName().equals(EPL_ONLISTENERUPDATE_SCRIPT_NAME) ||
                    script.getName().equals(EPL_ONSTART_SCRIPT_NAME) ||
                    script.getName().equals(EPL_ONSTOP_SCRIPT_NAME)) {
                    onScripts.add(script);
                }
            }
        }

        return new StatementInformationalsCompileTime(base.getStatementName(), alwaysSynthesizeOutputEvents,
            contextName, contextModuleName, contextVisibility, canSelfJoin, hasSubquery,
            needDedup, specCompiled.getAnnotations(), stateless, base.getUserObjectCompileTime(),
            filterSpecCompileds.size(), schedules.size(), namedWindowConsumers.size(), base.getStatementRawInfo().getStatementType(),
            annotationData.getPriority(), annotationData.isPremptive(), hasVariables, writesToTables, hasTableAccess,
            selectSubscriberDescriptor.getSelectClauseTypes(), selectSubscriberDescriptor.getSelectClauseColumnNames(),
            selectSubscriberDescriptor.isForClauseDelivery(), selectSubscriberDescriptor.getGroupDelivery(),
            selectSubscriberDescriptor.getGroupDeliveryMultiKey(), properties,
            base.getStatementSpec().getRaw().getMatchRecognizeSpec() != null, services.isInstrumented(),
            packageScope, insertIntoLatchName, allowSubscriber, onScripts.toArray(new ExpressionScriptProvided[0]));
    }

    private static boolean isNeedDedup(List<FilterSpecCompiled> filterSpecCompileds) {
        for (FilterSpecCompiled provider : filterSpecCompileds) {
            if (provider.getParameters().length > 1) {
                return true;
            }
        }
        return false;
    }

    private static boolean determineStatelessSelect(StatementType type, StatementSpecRaw spec, boolean hasSubselects) {

        if (hasSubselects) {
            return false;
        }
        if (type != StatementType.SELECT) {
            return false;
        }
        if (spec.getStreamSpecs() == null || spec.getStreamSpecs().size() > 1 || spec.getStreamSpecs().isEmpty()) {
            return false;
        }
        StreamSpecRaw singleStream = spec.getStreamSpecs().get(0);
        if (!(singleStream instanceof FilterStreamSpecRaw) && !(singleStream instanceof NamedWindowConsumerStreamSpec)) {
            return false;
        }
        if (singleStream.getViewSpecs() != null && singleStream.getViewSpecs().length > 0) {
            return false;
        }
        if (spec.getOutputLimitSpec() != null) {
            return false;
        }
        if (spec.getMatchRecognizeSpec() != null) {
            return false;
        }

        List<ExprNode> expressions = StatementSpecRawWalkerExpr.collectExpressionsShallow(spec);
        if (expressions.isEmpty()) {
            return true;
        }

        ExprNodeSummaryVisitor visitor = new ExprNodeSummaryVisitor();
        for (ExprNode expr : expressions) {
            if (expr == null) {
                continue;
            }
            expr.accept(visitor);
        }

        return !visitor.isHasAggregation() && !visitor.isHasPreviousPrior() && !visitor.isHasSubselect();
    }

    /**
     * Analysis result of analysing annotations for a statement.
     */
    public static class AnnotationAnalysisResult {
        private int priority;
        private boolean isPremptive;

        /**
         * Ctor.
         *
         * @param priority  priority
         * @param premptive preemptive indicator
         */
        private AnnotationAnalysisResult(int priority, boolean premptive) {
            this.priority = priority;
            isPremptive = premptive;
        }

        /**
         * Returns execution priority.
         *
         * @return priority.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Returns preemptive indicator (drop or normal).
         *
         * @return true for drop
         */
        public boolean isPremptive() {
            return isPremptive;
        }

        /**
         * Analyze the annotations and return priority and drop settings.
         *
         * @param annotations to analyze
         * @return analysis result
         */
        public static AnnotationAnalysisResult analyzeAnnotations(Annotation[] annotations) {
            boolean preemptive = false;
            int priority = 0;
            boolean hasPrioritySetting = false;
            for (Annotation annotation : annotations) {
                if (annotation instanceof Priority) {
                    priority = ((Priority) annotation).value();
                    hasPrioritySetting = true;
                }
                if (annotation instanceof Drop) {
                    preemptive = true;
                }
            }
            if (!hasPrioritySetting && preemptive) {
                priority = 1;
            }
            return new AnnotationAnalysisResult(priority, preemptive);
        }
    }
}
