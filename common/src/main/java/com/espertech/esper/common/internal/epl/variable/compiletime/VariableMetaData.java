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
package com.espertech.esper.common.internal.epl.variable.compiletime;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class VariableMetaData {
    private final String variableName;
    private final String variableModuleName;
    private final NameAccessModifier variableVisibility;
    private final String optionalContextName;
    private final NameAccessModifier optionalContextVisibility;
    private final String optionalContextModule;
    private final Class type;
    private final EventType eventType;
    private final boolean preconfigured;
    private final boolean constant;
    private final boolean compileTimeConstant;
    private final Object valueWhenAvailable;
    private final boolean createdByCurrentModule;

    public VariableMetaData(String variableName, String variableModuleName, NameAccessModifier variableVisibility, String optionalContextName, NameAccessModifier optionalContextVisibility, String optionalContextModule, Class type, EventType eventType, boolean preconfigured, boolean constant, boolean compileTimeConstant, Object valueWhenAvailable, boolean createdByCurrentModule) {
        this.variableName = variableName;
        this.variableModuleName = variableModuleName;
        this.variableVisibility = variableVisibility;
        this.optionalContextName = optionalContextName;
        this.optionalContextVisibility = optionalContextVisibility;
        this.optionalContextModule = optionalContextModule;
        this.type = type;
        this.eventType = eventType;
        this.preconfigured = preconfigured;
        this.constant = constant;
        this.compileTimeConstant = compileTimeConstant;
        this.valueWhenAvailable = valueWhenAvailable;
        this.createdByCurrentModule = createdByCurrentModule;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getVariableModuleName() {
        return variableModuleName;
    }

    public String getOptionalContextName() {
        return optionalContextName;
    }

    public Class getType() {
        return type;
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isConstant() {
        return constant;
    }

    public Object getValueWhenAvailable() {
        return valueWhenAvailable;
    }

    public boolean isPreconfigured() {
        return preconfigured;
    }

    public NameAccessModifier getOptionalContextVisibility() {
        return optionalContextVisibility;
    }

    public String getOptionalContextModule() {
        return optionalContextModule;
    }

    public boolean isCompileTimeConstant() {
        return compileTimeConstant;
    }

    public NameAccessModifier getVariableVisibility() {
        return variableVisibility;
    }

    public CodegenExpression make(CodegenExpressionRef addInitSvc) {
        return newInstance(VariableMetaData.class, constant(variableName), constant(variableModuleName), constant(variableVisibility),
                constant(optionalContextName), constant(optionalContextVisibility), constant(optionalContextModule), constant(type),
                eventType == null ? constantNull() : EventTypeUtility.resolveTypeCodegen(eventType, addInitSvc),
                constant(preconfigured), constant(constant), constant(compileTimeConstant), constant(valueWhenAvailable), constant(false));
    }

    public boolean isCreatedByCurrentModule() {
        return createdByCurrentModule;
    }
}
