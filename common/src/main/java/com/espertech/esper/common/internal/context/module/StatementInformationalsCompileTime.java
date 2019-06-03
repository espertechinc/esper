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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.annotation.Audit;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowState;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNode;
import com.espertech.esper.common.internal.filterspec.MatchedEventMapMinimal;
import com.espertech.esper.common.internal.metrics.audit.AuditPath;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.audit.AuditProviderDefault;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.schedule.ScheduleObjectType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.SerializerUtil;
import com.espertech.esper.common.internal.view.core.ViewFactory;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.annotation.AnnotationUtil.makeAnnotations;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.REF_EXPREVALCONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.NAME_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.resultset.codegen.ResultSetProcessorCodegenNames.MEMBER_AGENTINSTANCECONTEXT;

public class StatementInformationalsCompileTime {
    private final String statementNameCompileTime;
    private final boolean alwaysSynthesizeOutputEvents; // set when insert-into/for-clause/select-distinct
    private final String optionalContextName;
    private final String optionalContextModuleName;
    private final NameAccessModifier optionalContextVisibility;
    private final boolean canSelfJoin;
    private final boolean hasSubquery;
    private final boolean needDedup;
    private final Annotation[] annotations;
    private final boolean stateless;
    private final Serializable userObjectCompileTime;
    private final int numFilterCallbacks;
    private final int numScheduleCallbacks;
    private final int numNamedWindowCallbacks;
    private final StatementType statementType;
    private final int priority;
    private final boolean preemptive;
    private final boolean hasVariables;
    private final boolean writesToTables;
    private final boolean hasTableAccess;
    private final Class[] selectClauseTypes;
    private final String[] selectClauseColumnNames;
    private final boolean forClauseDelivery;
    private final ExprNode[] groupDelivery;
    private final MultiKeyClassRef groupDeliveryMultiKey;
    private final Map<StatementProperty, Object> properties;
    private final boolean hasMatchRecognize;
    private final boolean instrumented;
    private final CodegenPackageScope packageScope;
    private final String insertIntoLatchName;
    private final boolean allowSubscriber;
    private final ExpressionScriptProvided[] onScripts;

    public StatementInformationalsCompileTime(String statementNameCompileTime, boolean alwaysSynthesizeOutputEvents, String optionalContextName, String optionalContextModuleName, NameAccessModifier optionalContextVisibility, boolean canSelfJoin, boolean hasSubquery, boolean needDedup, Annotation[] annotations, boolean stateless, Serializable userObjectCompileTime, int numFilterCallbacks, int numScheduleCallbacks, int numNamedWindowCallbacks, StatementType statementType, int priority, boolean preemptive, boolean hasVariables, boolean writesToTables, boolean hasTableAccess, Class[] selectClauseTypes, String[] selectClauseColumnNames, boolean forClauseDelivery, ExprNode[] groupDelivery, MultiKeyClassRef groupDeliveryMultiKey, Map<StatementProperty, Object> properties, boolean hasMatchRecognize, boolean instrumented, CodegenPackageScope packageScope, String insertIntoLatchName, boolean allowSubscriber, ExpressionScriptProvided[] onScripts) {
        this.statementNameCompileTime = statementNameCompileTime;
        this.alwaysSynthesizeOutputEvents = alwaysSynthesizeOutputEvents;
        this.optionalContextName = optionalContextName;
        this.optionalContextModuleName = optionalContextModuleName;
        this.optionalContextVisibility = optionalContextVisibility;
        this.canSelfJoin = canSelfJoin;
        this.hasSubquery = hasSubquery;
        this.needDedup = needDedup;
        this.annotations = annotations;
        this.stateless = stateless;
        this.userObjectCompileTime = userObjectCompileTime;
        this.numFilterCallbacks = numFilterCallbacks;
        this.numScheduleCallbacks = numScheduleCallbacks;
        this.numNamedWindowCallbacks = numNamedWindowCallbacks;
        this.statementType = statementType;
        this.priority = priority;
        this.preemptive = preemptive;
        this.hasVariables = hasVariables;
        this.writesToTables = writesToTables;
        this.hasTableAccess = hasTableAccess;
        this.selectClauseTypes = selectClauseTypes;
        this.selectClauseColumnNames = selectClauseColumnNames;
        this.forClauseDelivery = forClauseDelivery;
        this.groupDelivery = groupDelivery;
        this.groupDeliveryMultiKey = groupDeliveryMultiKey;
        this.properties = properties;
        this.hasMatchRecognize = hasMatchRecognize;
        this.instrumented = instrumented;
        this.packageScope = packageScope;
        this.insertIntoLatchName = insertIntoLatchName;
        this.allowSubscriber = allowSubscriber;
        this.onScripts = onScripts;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StatementInformationalsRuntime.class, this.getClass(), classScope);

        CodegenExpressionRef info = ref("info");
        method.getBlock()
            .declareVar(StatementInformationalsRuntime.class, info.getRef(), newInstance(StatementInformationalsRuntime.class))
            .exprDotMethod(info, "setStatementNameCompileTime", constant(statementNameCompileTime))
            .exprDotMethod(info, "setAlwaysSynthesizeOutputEvents", constant(alwaysSynthesizeOutputEvents))
            .exprDotMethod(info, "setOptionalContextName", constant(optionalContextName))
            .exprDotMethod(info, "setOptionalContextModuleName", constant(optionalContextModuleName))
            .exprDotMethod(info, "setOptionalContextVisibility", constant(optionalContextVisibility))
            .exprDotMethod(info, "setCanSelfJoin", constant(canSelfJoin))
            .exprDotMethod(info, "setHasSubquery", constant(hasSubquery))
            .exprDotMethod(info, "setNeedDedup", constant(needDedup))
            .exprDotMethod(info, "setStateless", constant(stateless))
            .exprDotMethod(info, "setAnnotations", annotations == null ? constantNull() : localMethod(makeAnnotations(Annotation[].class, annotations, method, classScope)))
            .exprDotMethod(info, "setUserObjectCompileTime", SerializerUtil.expressionForUserObject(userObjectCompileTime))
            .exprDotMethod(info, "setNumFilterCallbacks", constant(numFilterCallbacks))
            .exprDotMethod(info, "setNumScheduleCallbacks", constant(numScheduleCallbacks))
            .exprDotMethod(info, "setNumNamedWindowCallbacks", constant(numNamedWindowCallbacks))
            .exprDotMethod(info, "setStatementType", constant(statementType))
            .exprDotMethod(info, "setPriority", constant(priority))
            .exprDotMethod(info, "setPreemptive", constant(preemptive))
            .exprDotMethod(info, "setHasVariables", constant(hasVariables))
            .exprDotMethod(info, "setWritesToTables", constant(writesToTables))
            .exprDotMethod(info, "setHasTableAccess", constant(hasTableAccess))
            .exprDotMethod(info, "setSelectClauseTypes", constant(selectClauseTypes))
            .exprDotMethod(info, "setSelectClauseColumnNames", constant(selectClauseColumnNames))
            .exprDotMethod(info, "setForClauseDelivery", constant(forClauseDelivery))
            .exprDotMethod(info, "setGroupDeliveryEval", MultiKeyCodegen.codegenExprEvaluatorMayMultikey(groupDelivery, null, groupDeliveryMultiKey, method, classScope))
            .exprDotMethod(info, "setProperties", makeProperties(properties, method, classScope))
            .exprDotMethod(info, "setHasMatchRecognize", constant(hasMatchRecognize))
            .exprDotMethod(info, "setAuditProvider", makeAuditProvider(method, classScope))
            .exprDotMethod(info, "setInstrumented", constant(instrumented))
            .exprDotMethod(info, "setInstrumentationProvider", makeInstrumentationProvider(method, classScope))
            .exprDotMethod(info, "setSubstitutionParamTypes", makeSubstitutionParamTypes())
            .exprDotMethod(info, "setSubstitutionParamNames", makeSubstitutionParamNames(method, classScope))
            .exprDotMethod(info, "setInsertIntoLatchName", constant(insertIntoLatchName))
            .exprDotMethod(info, "setAllowSubscriber", constant(allowSubscriber))
            .exprDotMethod(info, "setOnScripts", makeOnScripts(onScripts, method, classScope))
            .methodReturn(info);
        return localMethod(method);
    }

    public Map<StatementProperty, Object> getProperties() {
        return properties;
    }

    private CodegenExpression makeSubstitutionParamTypes() {
        List<CodegenSubstitutionParamEntry> numbered = packageScope.getSubstitutionParamsByNumber();
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = packageScope.getSubstitutionParamsByName();
        if (numbered.isEmpty() && named.isEmpty()) {
            return constantNull();
        }
        if (!numbered.isEmpty() && !named.isEmpty()) {
            throw new IllegalStateException("Both named and numbered substitution parameters are non-empty");
        }

        Class[] types;
        if (!numbered.isEmpty()) {
            types = new Class[numbered.size()];
            for (int i = 0; i < numbered.size(); i++) {
                types[i] = numbered.get(i).getType();
            }
        } else {
            types = new Class[named.size()];
            int count = 0;
            for (Map.Entry<String, CodegenSubstitutionParamEntry> entry : named.entrySet()) {
                types[count++] = entry.getValue().getType();
            }
        }
        return constant(types);
    }

    private CodegenExpression makeSubstitutionParamNames(CodegenMethodScope parent, CodegenClassScope classScope) {
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = packageScope.getSubstitutionParamsByName();
        if (named.isEmpty()) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(Map.class, this.getClass(), classScope);
        method.getBlock().declareVar(Map.class, "names", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(named.size()))));
        int count = 1;
        for (Map.Entry<String, CodegenSubstitutionParamEntry> entry : named.entrySet()) {
            method.getBlock().exprDotMethod(ref("names"), "put", constant(entry.getKey()), constant(count++));
        }
        method.getBlock().methodReturn(ref("names"));
        return localMethod(method);
    }

    private CodegenExpression makeInstrumentationProvider(CodegenMethod method, CodegenClassScope classScope) {
        if (!instrumented) {
            return constantNull();
        }

        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), InstrumentationCommon.class);

        CodegenMethod activated = CodegenMethod.makeParentNode(boolean.class, this.getClass(), classScope);
        anonymousClass.addMethod("activated", activated);
        activated.getBlock().methodReturn(constantTrue());

        for (Method forwarded : InstrumentationCommon.class.getMethods()) {
            if (forwarded.getDeclaringClass() == Object.class) {
                continue;
            }
            if (forwarded.getName().equals("activated")) {
                continue;
            }

            List<CodegenNamedParam> params = new ArrayList<>();
            CodegenExpression[] expressions = new CodegenExpression[forwarded.getParameterCount()];

            int num = 0;
            for (Parameter param : forwarded.getParameters()) {
                params.add(new CodegenNamedParam(param.getType(), param.getName()));
                expressions[num] = ref(param.getName());
                num++;
            }

            CodegenMethod m = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(params);
            anonymousClass.addMethod(forwarded.getName(), m);
            m.getBlock().apply(InstrumentationCode.instblock(classScope, forwarded.getName(), expressions));
        }

        return anonymousClass;
    }

    private CodegenExpression makeAuditProvider(CodegenMethod method, CodegenClassScope classScope) {
        if (!AnnotationUtil.hasAnnotation(annotations, Audit.class)) {
            return publicConstValue(AuditProviderDefault.class, "INSTANCE");
        }

        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), AuditProvider.class);

        CodegenMethod activated = CodegenMethod.makeParentNode(boolean.class, this.getClass(), classScope);
        anonymousClass.addMethod("activated", activated);
        activated.getBlock().methodReturn(constantTrue());

        CodegenMethod view = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EventBean[].class, "newData").addParam(EventBean[].class, "oldData").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ViewFactory.class, "viewFactory");
        anonymousClass.addMethod("view", view);
        if (AuditEnum.VIEW.getAudit(annotations) != null) {
            view.getBlock().staticMethod(AuditPath.class, "auditView", ref("newData"), ref("oldData"), MEMBER_AGENTINSTANCECONTEXT, ref("viewFactory"));
        }

        CodegenMethod streamOne = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef()).addParam(String.class, "filterText");
        anonymousClass.addMethod("stream", streamOne);
        CodegenMethod streamTwo = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EventBean[].class, "newData").addParam(EventBean[].class, "oldData").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef()).addParam(String.class, "filterText");
        anonymousClass.addMethod("stream", streamTwo);
        if (AuditEnum.STREAM.getAudit(annotations) != null) {
            streamOne.getBlock().staticMethod(AuditPath.class, "auditStream", ref("event"), REF_EXPREVALCONTEXT, ref("filterText"));
            streamTwo.getBlock().staticMethod(AuditPath.class, "auditStream", ref("newData"), ref("oldData"), REF_EXPREVALCONTEXT, ref("filterText"));
        }

        CodegenMethod scheduleAdd = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(long.class, "time").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleHandle.class, "scheduleHandle").addParam(ScheduleObjectType.class, "type").addParam(String.class, "name");
        CodegenMethod scheduleRemove = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleHandle.class, "scheduleHandle").addParam(ScheduleObjectType.class, "type").addParam(String.class, "name");
        CodegenMethod scheduleFire = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleObjectType.class, "type").addParam(String.class, "name");
        anonymousClass.addMethod("scheduleAdd", scheduleAdd);
        anonymousClass.addMethod("scheduleRemove", scheduleRemove);
        anonymousClass.addMethod("scheduleFire", scheduleFire);
        if (AuditEnum.SCHEDULE.getAudit(annotations) != null) {
            scheduleAdd.getBlock().staticMethod(AuditPath.class, "auditScheduleAdd", ref("time"), MEMBER_AGENTINSTANCECONTEXT, ref("scheduleHandle"), ref("type"), ref("name"));
            scheduleRemove.getBlock().staticMethod(AuditPath.class, "auditScheduleRemove", MEMBER_AGENTINSTANCECONTEXT, ref("scheduleHandle"), ref("type"), ref("name"));
            scheduleFire.getBlock().staticMethod(AuditPath.class, "auditScheduleFire", MEMBER_AGENTINSTANCECONTEXT, ref("type"), ref("name"));
        }

        CodegenMethod property = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "name").addParam(Object.class, "value").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("property", property);
        if (AuditEnum.PROPERTY.getAudit(annotations) != null) {
            property.getBlock().staticMethod(AuditPath.class, "auditProperty", ref("name"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod insert = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("insert", insert);
        if (AuditEnum.INSERT.getAudit(annotations) != null) {
            insert.getBlock().staticMethod(AuditPath.class, "auditInsert", ref("event"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod expression = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "text").addParam(Object.class, "value").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("expression", expression);
        if (AuditEnum.EXPRESSION.getAudit(annotations) != null || AuditEnum.EXPRESSION_NESTED.getAudit(annotations) != null) {
            expression.getBlock().staticMethod(AuditPath.class, "auditExpression", ref("text"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod patternTrue = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EvalFactoryNode.class, "factoryNode").addParam(Object.class, "from").addParam(MatchedEventMapMinimal.class, "matchEvent").addParam(boolean.class, "isQuitted").addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
        CodegenMethod patternFalse = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(EvalFactoryNode.class, "factoryNode").addParam(Object.class, "from").addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
        anonymousClass.addMethod("patternTrue", patternTrue);
        anonymousClass.addMethod("patternFalse", patternFalse);
        if (AuditEnum.PATTERN.getAudit(annotations) != null) {
            patternTrue.getBlock().staticMethod(AuditPath.class, "auditPatternTrue", ref("factoryNode"), ref("from"), ref("matchEvent"), ref("isQuitted"), MEMBER_AGENTINSTANCECONTEXT);
            patternFalse.getBlock().staticMethod(AuditPath.class, "auditPatternFalse", ref("factoryNode"), ref("from"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod patternInstance = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(boolean.class, "increase").addParam(EvalFactoryNode.class, "factoryNode").addParam(AgentInstanceContext.class, NAME_AGENTINSTANCECONTEXT);
        anonymousClass.addMethod("patternInstance", patternInstance);
        if (AuditEnum.PATTERNINSTANCES.getAudit(annotations) != null) {
            patternInstance.getBlock().staticMethod(AuditPath.class, "auditPatternInstance", ref("increase"), ref("factoryNode"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod exprdef = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "name").addParam(Object.class, "value").addParam(ExprEvaluatorContext.class, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("exprdef", exprdef);
        if (AuditEnum.EXPRDEF.getAudit(annotations) != null) {
            exprdef.getBlock().staticMethod(AuditPath.class, "auditExprDef", ref("name"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod dataflowTransition = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "name").addParam(String.class, "instance").addParam(EPDataFlowState.class, "state").addParam(EPDataFlowState.class, "newState").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowTransition", dataflowTransition);
        if (AuditEnum.DATAFLOW_TRANSITION.getAudit(annotations) != null) {
            dataflowTransition.getBlock().staticMethod(AuditPath.class, "auditDataflowTransition", ref("name"), ref("instance"), ref("state"), ref("newState"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod dataflowSource = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "name").addParam(String.class, "instance").addParam(String.class, "operatorName").addParam(int.class, "operatorNum").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowSource", dataflowSource);
        if (AuditEnum.DATAFLOW_SOURCE.getAudit(annotations) != null) {
            dataflowSource.getBlock().staticMethod(AuditPath.class, "auditDataflowSource", ref("name"), ref("instance"), ref("operatorName"), ref("operatorNum"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod dataflowOp = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(String.class, "name").addParam(String.class, "instance").addParam(String.class, "operatorName").addParam(int.class, "operatorNum").addParam(Object[].class, "params").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowOp", dataflowOp);
        if (AuditEnum.DATAFLOW_OP.getAudit(annotations) != null) {
            dataflowOp.getBlock().staticMethod(AuditPath.class, "auditDataflowOp", ref("name"), ref("instance"), ref("operatorName"), ref("operatorNum"), ref("params"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod contextPartition = CodegenMethod.makeParentNode(void.class, this.getClass(), classScope).addParam(boolean.class, "allocate").addParam(AgentInstanceContext.class, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("contextPartition", contextPartition);
        if (AuditEnum.CONTEXTPARTITION.getAudit(annotations) != null) {
            contextPartition.getBlock().staticMethod(AuditPath.class, "auditContextPartition", ref("allocate"), MEMBER_AGENTINSTANCECONTEXT);
        }

        return anonymousClass;
    }

    private CodegenExpression makeProperties(Map<StatementProperty, Object> properties, CodegenMethodScope parent, CodegenClassScope classScope) {
        if (properties.isEmpty()) {
            return staticMethod(Collections.class, "emptyMap");
        }
        Function<StatementProperty, CodegenExpression> field = x -> enumValue(StatementProperty.class, x.name());
        Function<Object, CodegenExpression> value = CodegenExpressionBuilder::constant;
        if (properties.size() == 1) {
            Map.Entry<StatementProperty, Object> first = properties.entrySet().iterator().next();
            return staticMethod(Collections.class, "singletonMap", field.apply(first.getKey()), value.apply(first.getValue()));
        }

        CodegenMethod method = parent.makeChild(Map.class, StatementInformationalsCompileTime.class, classScope);
        method.getBlock()
            .declareVar(Map.class, "properties", newInstance(HashMap.class, constant(CollectionUtil.capacityHashMap(properties.size()))));
        for (Map.Entry<StatementProperty, Object> entry : properties.entrySet()) {
            method.getBlock().exprDotMethod(ref("properties"), "put", field.apply(entry.getKey()), value.apply(entry.getValue()));
        }
        method.getBlock().methodReturn(ref("properties"));
        return localMethod(method);
    }

    private CodegenExpression makeOnScripts(ExpressionScriptProvided[] onScripts, CodegenMethodScope parent, CodegenClassScope classScope) {
        if (onScripts == null || onScripts.length == 0) {
            return constantNull();
        }
        CodegenExpression[] init = new CodegenExpression[onScripts.length];
        for (int i = 0; i < onScripts.length; i++) {
            init[i] = onScripts[i].make(parent, classScope);
        }
        return newArrayWithInit(ExpressionScriptProvided.class, init);
    }
}
