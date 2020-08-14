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
import com.espertech.esper.common.client.dataflow.core.EPDataFlowService;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.*;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
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
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;
import com.espertech.esper.common.internal.schedule.ScheduleHandle;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
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
import static com.espertech.esper.common.internal.epl.output.core.OutputProcessViewCodegenNames.MEMBER_AGENTINSTANCECONTEXT;
import static com.espertech.esper.common.internal.epl.output.core.OutputProcessViewCodegenNames.NAME_AGENTINSTANCECONTEXT;

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
        CodegenMethod method = parent.makeChild(StatementInformationalsRuntime.EPTYPE, this.getClass(), classScope);
        CodegenSetterBuilder builder = new CodegenSetterBuilder(StatementInformationalsRuntime.EPTYPE, StatementInformationalsCompileTime.class, "info", classScope, method);
        builder.constantDefaultCheckedObj("statementNameCompileTime", statementNameCompileTime)
            .constantDefaultChecked("alwaysSynthesizeOutputEvents", alwaysSynthesizeOutputEvents)
            .constantDefaultCheckedObj("optionalContextName", optionalContextName)
            .constantDefaultCheckedObj("optionalContextModuleName", optionalContextModuleName)
            .constantDefaultCheckedObj("optionalContextVisibility", optionalContextVisibility)
            .constantDefaultChecked("canSelfJoin", canSelfJoin)
            .constantDefaultChecked("hasSubquery", hasSubquery)
            .constantDefaultChecked("needDedup", needDedup)
            .constantDefaultChecked("stateless", stateless)
            .constantDefaultChecked("numFilterCallbacks", numFilterCallbacks)
            .constantDefaultChecked("numScheduleCallbacks", numScheduleCallbacks)
            .constantDefaultChecked("numNamedWindowCallbacks", numNamedWindowCallbacks)
            .constantDefaultCheckedObj("statementType", statementType)
            .constantDefaultChecked("priority", priority)
            .constantDefaultChecked("preemptive", preemptive)
            .constantDefaultChecked("hasVariables", hasVariables)
            .constantDefaultChecked("writesToTables", writesToTables)
            .constantDefaultChecked("hasTableAccess", hasTableAccess)
            .constantDefaultCheckedObj("selectClauseTypes", selectClauseTypes)
            .constantDefaultCheckedObj("selectClauseColumnNames", selectClauseColumnNames)
            .constantDefaultChecked("forClauseDelivery", forClauseDelivery)
            .constantDefaultChecked("hasMatchRecognize", hasMatchRecognize)
            .constantDefaultChecked("instrumented", instrumented)
            .constantDefaultCheckedObj("insertIntoLatchName", insertIntoLatchName)
            .constantDefaultChecked("allowSubscriber", allowSubscriber)
            .expressionDefaultChecked("annotations", annotations == null ? constantNull() : makeAnnotations(EPTypePremade.ANNOTATIONARRAY.getEPType(), annotations, method, classScope))
            .expressionDefaultChecked("userObjectCompileTime", SerializerUtil.expressionForUserObject(userObjectCompileTime))
            .expressionDefaultChecked("groupDeliveryEval", MultiKeyCodegen.codegenExprEvaluatorMayMultikey(groupDelivery, null, groupDeliveryMultiKey, method, classScope))
            .expressionDefaultChecked("properties", makeProperties(properties, method, classScope))
            .expressionDefaultChecked("auditProvider", makeAuditProvider(method, classScope))
            .expressionDefaultChecked("instrumentationProvider", makeInstrumentationProvider(method, classScope))
            .expressionDefaultChecked("substitutionParamTypes", makeSubstitutionParamTypes())
            .expressionDefaultChecked("substitutionParamNames", makeSubstitutionParamNames(method, classScope))
            .expressionDefaultChecked("onScripts", makeOnScripts(onScripts, method, classScope));
        method.getBlock().methodReturn(builder.getRefName());
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
                types[i] = numbered.get(i).getType().getType();
            }
        } else {
            types = new Class[named.size()];
            int count = 0;
            for (Map.Entry<String, CodegenSubstitutionParamEntry> entry : named.entrySet()) {
                types[count++] = entry.getValue().getType().getType();
            }
        }
        return constant(types);
    }

    private CodegenExpression makeSubstitutionParamNames(CodegenMethodScope parent, CodegenClassScope classScope) {
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = packageScope.getSubstitutionParamsByName();
        if (named.isEmpty()) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), this.getClass(), classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "names", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(named.size()))));
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

        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), InstrumentationCommon.EPTYPE);

        CodegenMethod activated = CodegenMethod.makeParentNode(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), this.getClass(), classScope);
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
                EPTypeClass paramType = ClassHelperGenericType.getParameterType(param);
                params.add(new CodegenNamedParam(paramType, param.getName()));
                expressions[num] = ref(param.getName());
                num++;
            }

            CodegenMethod m = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(params);
            anonymousClass.addMethod(forwarded.getName(), m);
            m.getBlock().apply(InstrumentationCode.instblock(classScope, forwarded.getName(), expressions));
        }

        return anonymousClass;
    }

    private CodegenExpression makeAuditProvider(CodegenMethod method, CodegenClassScope classScope) {
        if (!AnnotationUtil.hasAnnotation(annotations, Audit.class)) {
            return constantNull();
        }

        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), AuditProvider.EPTYPE);

        CodegenMethod activated = CodegenMethod.makeParentNode(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), this.getClass(), classScope);
        anonymousClass.addMethod("activated", activated);
        activated.getBlock().methodReturn(constantTrue());

        CodegenMethod view = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPEARRAY, "newData").addParam(EventBean.EPTYPEARRAY, "oldData").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ViewFactory.EPTYPE, "viewFactory");
        anonymousClass.addMethod("view", view);
        if (AuditEnum.VIEW.getAudit(annotations) != null) {
            view.getBlock().staticMethod(AuditPath.class, "auditView", ref("newData"), ref("oldData"), MEMBER_AGENTINSTANCECONTEXT, ref("viewFactory"));
        }

        CodegenMethod streamOne = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPE, "event").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef()).addParam(EPTypePremade.STRING.getEPType(), "filterText");
        anonymousClass.addMethod("stream", streamOne);
        CodegenMethod streamTwo = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPEARRAY, "newData").addParam(EventBean.EPTYPEARRAY, "oldData").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef()).addParam(EPTypePremade.STRING.getEPType(), "filterText");
        anonymousClass.addMethod("stream", streamTwo);
        if (AuditEnum.STREAM.getAudit(annotations) != null) {
            streamOne.getBlock().staticMethod(AuditPath.class, "auditStream", ref("event"), REF_EXPREVALCONTEXT, ref("filterText"));
            streamTwo.getBlock().staticMethod(AuditPath.class, "auditStream", ref("newData"), ref("oldData"), REF_EXPREVALCONTEXT, ref("filterText"));
        }

        CodegenMethod scheduleAdd = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.LONGPRIMITIVE.getEPType(), "time").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleHandle.EPTYPE, "scheduleHandle").addParam(ScheduleHandle.EPTYPE_SCHEDULEOBJECTTYPE, "type").addParam(EPTypePremade.STRING.getEPType(), "name");
        CodegenMethod scheduleRemove = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleHandle.EPTYPE, "scheduleHandle").addParam(ScheduleHandle.EPTYPE_SCHEDULEOBJECTTYPE, "type").addParam(EPTypePremade.STRING.getEPType(), "name");
        CodegenMethod scheduleFire = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef()).addParam(ScheduleHandle.EPTYPE_SCHEDULEOBJECTTYPE, "type").addParam(EPTypePremade.STRING.getEPType(), "name");
        anonymousClass.addMethod("scheduleAdd", scheduleAdd);
        anonymousClass.addMethod("scheduleRemove", scheduleRemove);
        anonymousClass.addMethod("scheduleFire", scheduleFire);
        if (AuditEnum.SCHEDULE.getAudit(annotations) != null) {
            scheduleAdd.getBlock().staticMethod(AuditPath.class, "auditScheduleAdd", ref("time"), MEMBER_AGENTINSTANCECONTEXT, ref("scheduleHandle"), ref("type"), ref("name"));
            scheduleRemove.getBlock().staticMethod(AuditPath.class, "auditScheduleRemove", MEMBER_AGENTINSTANCECONTEXT, ref("scheduleHandle"), ref("type"), ref("name"));
            scheduleFire.getBlock().staticMethod(AuditPath.class, "auditScheduleFire", MEMBER_AGENTINSTANCECONTEXT, ref("type"), ref("name"));
        }

        CodegenMethod property = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.OBJECT.getEPType(), "value").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("property", property);
        if (AuditEnum.PROPERTY.getAudit(annotations) != null) {
            property.getBlock().staticMethod(AuditPath.class, "auditProperty", ref("name"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod insert = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPE, "event").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("insert", insert);
        if (AuditEnum.INSERT.getAudit(annotations) != null) {
            insert.getBlock().staticMethod(AuditPath.class, "auditInsert", ref("event"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod expression = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "text").addParam(EPTypePremade.OBJECT.getEPType(), "value").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("expression", expression);
        if (AuditEnum.EXPRESSION.getAudit(annotations) != null || AuditEnum.EXPRESSION_NESTED.getAudit(annotations) != null) {
            expression.getBlock().staticMethod(AuditPath.class, "auditExpression", ref("text"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod patternTrue = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EvalFactoryNode.EPTYPE, "factoryNode").addParam(EPTypePremade.OBJECT.getEPType(), "from").addParam(MatchedEventMapMinimal.EPTYPE, "matchEvent").addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "isQuitted").addParam(AgentInstanceContext.EPTYPE, NAME_AGENTINSTANCECONTEXT);
        CodegenMethod patternFalse = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EvalFactoryNode.EPTYPE, "factoryNode").addParam(EPTypePremade.OBJECT.getEPType(), "from").addParam(AgentInstanceContext.EPTYPE, NAME_AGENTINSTANCECONTEXT);
        anonymousClass.addMethod("patternTrue", patternTrue);
        anonymousClass.addMethod("patternFalse", patternFalse);
        if (AuditEnum.PATTERN.getAudit(annotations) != null) {
            patternTrue.getBlock().staticMethod(AuditPath.class, "auditPatternTrue", ref("factoryNode"), ref("from"), ref("matchEvent"), ref("isQuitted"), MEMBER_AGENTINSTANCECONTEXT);
            patternFalse.getBlock().staticMethod(AuditPath.class, "auditPatternFalse", ref("factoryNode"), ref("from"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod patternInstance = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "increase").addParam(EvalFactoryNode.EPTYPE, "factoryNode").addParam(AgentInstanceContext.EPTYPE, NAME_AGENTINSTANCECONTEXT);
        anonymousClass.addMethod("patternInstance", patternInstance);
        if (AuditEnum.PATTERNINSTANCES.getAudit(annotations) != null) {
            patternInstance.getBlock().staticMethod(AuditPath.class, "auditPatternInstance", ref("increase"), ref("factoryNode"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod exprdef = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.OBJECT.getEPType(), "value").addParam(ExprEvaluatorContext.EPTYPE, REF_EXPREVALCONTEXT.getRef());
        anonymousClass.addMethod("exprdef", exprdef);
        if (AuditEnum.EXPRDEF.getAudit(annotations) != null) {
            exprdef.getBlock().staticMethod(AuditPath.class, "auditExprDef", ref("name"), ref("value"), REF_EXPREVALCONTEXT);
        }

        CodegenMethod dataflowTransition = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.STRING.getEPType(), "instance").addParam(EPDataFlowService.EPTYPE_DATAFLOWSTATE, "state").addParam(EPDataFlowService.EPTYPE_DATAFLOWSTATE, "newState").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowTransition", dataflowTransition);
        if (AuditEnum.DATAFLOW_TRANSITION.getAudit(annotations) != null) {
            dataflowTransition.getBlock().staticMethod(AuditPath.class, "auditDataflowTransition", ref("name"), ref("instance"), ref("state"), ref("newState"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod dataflowSource = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.STRING.getEPType(), "instance").addParam(EPTypePremade.STRING.getEPType(), "operatorName").addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "operatorNum").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowSource", dataflowSource);
        if (AuditEnum.DATAFLOW_SOURCE.getAudit(annotations) != null) {
            dataflowSource.getBlock().staticMethod(AuditPath.class, "auditDataflowSource", ref("name"), ref("instance"), ref("operatorName"), ref("operatorNum"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod dataflowOp = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.STRING.getEPType(), "name").addParam(EPTypePremade.STRING.getEPType(), "instance").addParam(EPTypePremade.STRING.getEPType(), "operatorName").addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "operatorNum").addParam(EPTypePremade.OBJECTARRAY.getEPType(), "params").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef());
        anonymousClass.addMethod("dataflowOp", dataflowOp);
        if (AuditEnum.DATAFLOW_OP.getAudit(annotations) != null) {
            dataflowOp.getBlock().staticMethod(AuditPath.class, "auditDataflowOp", ref("name"), ref("instance"), ref("operatorName"), ref("operatorNum"), ref("params"), MEMBER_AGENTINSTANCECONTEXT);
        }

        CodegenMethod contextPartition = CodegenMethod.makeParentNode(EPTypePremade.VOID.getEPType(), this.getClass(), classScope).addParam(EPTypePremade.BOOLEANPRIMITIVE.getEPType(), "allocate").addParam(AgentInstanceContext.EPTYPE, MEMBER_AGENTINSTANCECONTEXT.getRef());
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

        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), StatementInformationalsCompileTime.class, classScope);
        method.getBlock()
            .declareVar(EPTypePremade.MAP.getEPType(), "properties", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(properties.size()))));
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
        return newArrayWithInit(ExpressionScriptProvided.EPTYPE, init);
    }
}
