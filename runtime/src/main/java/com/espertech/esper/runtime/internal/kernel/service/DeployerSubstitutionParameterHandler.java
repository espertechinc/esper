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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.internal.context.module.StatementAIFactoryProvider;
import com.espertech.esper.common.internal.context.module.StatementLightweight;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterContext;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class DeployerSubstitutionParameterHandler implements StatementSubstitutionParameterContext {
    private final String deploymentId;
    private final StatementLightweight lightweight;
    private final Map<Integer, Map<Integer, Object>> provided;
    private final Class[] types;
    private final Map<String, Integer> names;
    private final StatementAIFactoryProvider aiFactoryProvider;

    public DeployerSubstitutionParameterHandler(String deploymentId, StatementLightweight lightweight, Map<Integer, Map<Integer, Object>> provided, Class[] types, Map<String, Integer> names) {
        this.deploymentId = deploymentId;
        this.lightweight = lightweight;
        this.provided = provided;
        this.types = types;
        this.names = names;
        aiFactoryProvider = lightweight.getStatementProvider().getStatementAIFactoryProvider();
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getStatementName() {
        return lightweight.getStatementContext().getStatementName();
    }

    public int getStatementId() {
        return lightweight.getStatementContext().getStatementId();
    }

    public String getEpl() {
        return (String) lightweight.getStatementInformationals().getProperties().get(StatementProperty.EPL);
    }

    public Annotation[] getAnnotations() {
        return lightweight.getStatementContext().getAnnotations();
    }

    public void setObject(int parameterIndex, Object value) throws EPException {
        if (types == null || types.length == 0) {
            throw new EPException("The statement has no substitution parameters");
        }
        if (names != null && !names.isEmpty()) {
            throw new EPException("Substitution parameter names have been provided for this statement, please set the value by name");
        }
        if (parameterIndex > types.length || parameterIndex < 1) {
            throw new EPException("Invalid substitution parameter index, expected an index between 1 and " + types.length);
        }
        try {
            aiFactoryProvider.setValue(parameterIndex, value);
            addValue(parameterIndex, value);
        } catch (Throwable t) {
            throw handleSetterException(Integer.toString(parameterIndex), parameterIndex, t);
        }
    }

    public Class[] getSubstitutionParameterTypes() {
        return types;
    }

    public Map<String, Integer> getSubstitutionParameterNames() {
        return names;
    }

    public void setObject(String parameterName, Object value) throws EPException {
        if (types == null || types.length == 0) {
            throw new EPException("The statement has no substitution parameters");
        }
        if (names == null || names.isEmpty()) {
            throw new EPException("Substitution parameter names have not been provided for this statement");
        }
        Integer index = names.get(parameterName);
        if (index == null) {
            throw new EPException("Failed to find substitution parameter named '" + parameterName + "', available parameters are " + names.keySet());
        }
        try {
            aiFactoryProvider.setValue(index, value);
            addValue(index, value);
        } catch (Throwable t) {
            throw handleSetterException("'" + parameterName + "'", index, t);
        }
    }

    private void addValue(int index, Object value) {
        int statementId = lightweight.getStatementContext().getStatementId();
        Map<Integer, Object> existing = provided.get(statementId);
        if (existing == null) {
            existing = new HashMap<>(8);
            provided.put(statementId, existing);
        }
        existing.put(index, value);
    }

    private EPException handleSetterException(String parameterName, int parameterIndex, Throwable t) {
        String message = t.getMessage();
        if (t instanceof NullPointerException) {
            message = "Received a null-value for a primitive type";
        }
        return new EPException("Failed to set substitution parameter " + parameterName + ", expected a value of type '" + types[parameterIndex - 1].getName() + "': " + message, t);
    }
}
