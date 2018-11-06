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

import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.metrics.audit.AuditProvider;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCommon;

import java.lang.annotation.Annotation;
import java.util.Map;

public class StatementInformationalsRuntime {
    private String statementNameCompileTime;
    private boolean alwaysSynthesizeOutputEvents; // set when insert-into/for-clause/select-distinct
    private String optionalContextName;
    private String optionalContextModuleName;
    private NameAccessModifier optionalContextVisibility;
    private boolean canSelfJoin;
    private boolean hasSubquery;
    private boolean needDedup;
    private Annotation[] annotations;
    private boolean stateless;
    private Object userObjectCompileTime;
    private int numFilterCallbacks;
    private int numScheduleCallbacks;
    private int numNamedWindowCallbacks;
    private StatementType statementType;
    private int priority;
    private boolean preemptive;
    private boolean hasVariables;
    private boolean writesToTables;
    private boolean hasTableAccess;
    private Class[] selectClauseTypes;
    private String[] selectClauseColumnNames;
    private boolean forClauseDelivery;
    private ExprEvaluator groupDeliveryEval;
    private Map<StatementProperty, Object> properties;
    private boolean hasMatchRecognize;
    private AuditProvider auditProvider;
    private boolean instrumented;
    private InstrumentationCommon instrumentationProvider;
    private Class[] substitutionParamTypes;
    private Map<String, Integer> substitutionParamNames;
    private String insertIntoLatchName;
    private boolean allowSubscriber;
    private ExpressionScriptProvided[] onScripts;

    public String getStatementNameCompileTime() {
        return statementNameCompileTime;
    }

    public void setStatementNameCompileTime(String statementNameCompileTime) {
        this.statementNameCompileTime = statementNameCompileTime;
    }

    public boolean isAlwaysSynthesizeOutputEvents() {
        return alwaysSynthesizeOutputEvents;
    }

    public void setAlwaysSynthesizeOutputEvents(boolean alwaysSynthesizeOutputEvents) {
        this.alwaysSynthesizeOutputEvents = alwaysSynthesizeOutputEvents;
    }

    public String getOptionalContextName() {
        return optionalContextName;
    }

    public void setOptionalContextName(String optionalContextName) {
        this.optionalContextName = optionalContextName;
    }

    public String getOptionalContextModuleName() {
        return optionalContextModuleName;
    }

    public void setOptionalContextModuleName(String optionalContextModuleName) {
        this.optionalContextModuleName = optionalContextModuleName;
    }

    public NameAccessModifier getOptionalContextVisibility() {
        return optionalContextVisibility;
    }

    public void setOptionalContextVisibility(NameAccessModifier optionalContextVisibility) {
        this.optionalContextVisibility = optionalContextVisibility;
    }

    public boolean isCanSelfJoin() {
        return canSelfJoin;
    }

    public void setCanSelfJoin(boolean canSelfJoin) {
        this.canSelfJoin = canSelfJoin;
    }

    public boolean isHasSubquery() {
        return hasSubquery;
    }

    public void setHasSubquery(boolean hasSubquery) {
        this.hasSubquery = hasSubquery;
    }

    public boolean isNeedDedup() {
        return needDedup;
    }

    public void setNeedDedup(boolean needDedup) {
        this.needDedup = needDedup;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
    }

    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }

    public Object getUserObjectCompileTime() {
        return userObjectCompileTime;
    }

    public void setUserObjectCompileTime(Object userObjectCompileTime) {
        this.userObjectCompileTime = userObjectCompileTime;
    }

    public int getNumFilterCallbacks() {
        return numFilterCallbacks;
    }

    public void setNumFilterCallbacks(int numFilterCallbacks) {
        this.numFilterCallbacks = numFilterCallbacks;
    }

    public int getNumScheduleCallbacks() {
        return numScheduleCallbacks;
    }

    public void setNumScheduleCallbacks(int numScheduleCallbacks) {
        this.numScheduleCallbacks = numScheduleCallbacks;
    }

    public int getNumNamedWindowCallbacks() {
        return numNamedWindowCallbacks;
    }

    public void setNumNamedWindowCallbacks(int numNamedWindowCallbacks) {
        this.numNamedWindowCallbacks = numNamedWindowCallbacks;
    }

    public StatementType getStatementType() {
        return statementType;
    }

    public void setStatementType(StatementType statementType) {
        this.statementType = statementType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isPreemptive() {
        return preemptive;
    }

    public void setPreemptive(boolean preemptive) {
        this.preemptive = preemptive;
    }

    public boolean isHasVariables() {
        return hasVariables;
    }

    public void setHasVariables(boolean hasVariables) {
        this.hasVariables = hasVariables;
    }

    public boolean isWritesToTables() {
        return writesToTables;
    }

    public void setWritesToTables(boolean writesToTables) {
        this.writesToTables = writesToTables;
    }

    public boolean isHasTableAccess() {
        return hasTableAccess;
    }

    public void setHasTableAccess(boolean hasTableAccess) {
        this.hasTableAccess = hasTableAccess;
    }

    public Class[] getSelectClauseTypes() {
        return selectClauseTypes;
    }

    public void setSelectClauseTypes(Class[] selectClauseTypes) {
        this.selectClauseTypes = selectClauseTypes;
    }

    public String[] getSelectClauseColumnNames() {
        return selectClauseColumnNames;
    }

    public void setSelectClauseColumnNames(String[] selectClauseColumnNames) {
        this.selectClauseColumnNames = selectClauseColumnNames;
    }

    public boolean isForClauseDelivery() {
        return forClauseDelivery;
    }

    public void setForClauseDelivery(boolean forClauseDelivery) {
        this.forClauseDelivery = forClauseDelivery;
    }

    public ExprEvaluator getGroupDeliveryEval() {
        return groupDeliveryEval;
    }

    public void setGroupDeliveryEval(ExprEvaluator groupDeliveryEval) {
        this.groupDeliveryEval = groupDeliveryEval;
    }

    public Map<StatementProperty, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<StatementProperty, Object> properties) {
        this.properties = properties;
    }

    public boolean isHasMatchRecognize() {
        return hasMatchRecognize;
    }

    public void setHasMatchRecognize(boolean hasMatchRecognize) {
        this.hasMatchRecognize = hasMatchRecognize;
    }

    public AuditProvider getAuditProvider() {
        return auditProvider;
    }

    public void setAuditProvider(AuditProvider auditProvider) {
        this.auditProvider = auditProvider;
    }

    public boolean isInstrumented() {
        return instrumented;
    }

    public void setInstrumented(boolean instrumented) {
        this.instrumented = instrumented;
    }

    public InstrumentationCommon getInstrumentationProvider() {
        return instrumentationProvider;
    }

    public void setInstrumentationProvider(InstrumentationCommon instrumentationProvider) {
        this.instrumentationProvider = instrumentationProvider;
    }

    public Class[] getSubstitutionParamTypes() {
        return substitutionParamTypes;
    }

    public void setSubstitutionParamTypes(Class[] substitutionParamTypes) {
        this.substitutionParamTypes = substitutionParamTypes;
    }

    public Map<String, Integer> getSubstitutionParamNames() {
        return substitutionParamNames;
    }

    public void setSubstitutionParamNames(Map<String, Integer> substitutionParamNames) {
        this.substitutionParamNames = substitutionParamNames;
    }

    public String getInsertIntoLatchName() {
        return insertIntoLatchName;
    }

    public void setInsertIntoLatchName(String insertIntoLatchName) {
        this.insertIntoLatchName = insertIntoLatchName;
    }

    public boolean isAllowSubscriber() {
        return allowSubscriber;
    }

    public void setAllowSubscriber(boolean allowSubscriber) {
        this.allowSubscriber = allowSubscriber;
    }

    public ExpressionScriptProvided[] getOnScripts() {
        return onScripts;
    }

    public void setOnScripts(ExpressionScriptProvided[] onScripts) {
        this.onScripts = onScripts;
    }
}
