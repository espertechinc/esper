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
package com.espertech.esper.runtime.internal.kernel.faf;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryInformationals;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethod;
import com.espertech.esper.common.internal.epl.fafquery.querymethod.FAFQueryMethodAssignerSetter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class EPFireAndForgetPreparedQueryParameterizedImpl implements EPFireAndForgetPreparedQueryParameterized {
    private final AtomicBoolean serviceProviderStatus;
    private final FAFQueryMethodAssignerSetter fields;
    private final FAFQueryMethod queryMethod;
    private final Class[] types;
    private final Map<String, Integer> names;

    private Set<Integer> unsatisfiedParamsOneOffset;

    public EPFireAndForgetPreparedQueryParameterizedImpl(AtomicBoolean serviceProviderStatus, FAFQueryMethodAssignerSetter fields, FAFQueryMethod queryMethod, FAFQueryInformationals queryInformationals) {
        this.serviceProviderStatus = serviceProviderStatus;
        this.fields = fields;
        this.queryMethod = queryMethod;
        this.types = queryInformationals.getSubstitutionParamsTypes();
        this.names = queryInformationals.getSubstitutionParamsNames();
        if (types != null && types.length > 0) {
            this.unsatisfiedParamsOneOffset = new LinkedHashSet<>();
            for (int i = 0; i < types.length; i++) {
                unsatisfiedParamsOneOffset.add(i + 1);
            }
        } else {
            unsatisfiedParamsOneOffset = Collections.emptySet();
        }
    }

    public void setObject(int parameterIndex, Object value) throws EPException {
        if (types == null || types.length == 0) {
            throw new EPException("The query has no substitution parameters");
        }
        if (names != null && !names.isEmpty()) {
            throw new EPException("Substitution parameter names have been provided for this query, please set the value by name");
        }
        if (parameterIndex > types.length || parameterIndex < 1) {
            throw new EPException("Invalid substitution parameter index, expected an index between 1 and " + types.length);
        }
        try {
            fields.setValue(parameterIndex, value);
            updateUnsatisfied(parameterIndex);
        } catch (Throwable t) {
            throw handleSetterException(Integer.toString(parameterIndex), parameterIndex, t);
        }
    }

    public void setObject(String parameterName, Object value) throws EPException {
        if (types == null || types.length == 0) {
            throw new EPException("The query has no substitution parameters");
        }
        if (names == null || names.isEmpty()) {
            throw new EPException("Substitution parameter names have not been provided for this query");
        }
        Integer index = names.get(parameterName);
        if (index == null) {
            throw new EPException("Failed to find substitution parameter named '" + parameterName + "', available parameters are " + names.keySet());
        }
        try {
            fields.setValue(index, value);
            updateUnsatisfied(index);
        } catch (Throwable t) {
            throw handleSetterException("'" + parameterName + "'", index, t);
        }
    }

    public FAFQueryMethodAssignerSetter getFields() {
        return fields;
    }

    public FAFQueryMethod getQueryMethod() {
        return queryMethod;
    }

    public AtomicBoolean getServiceProviderStatus() {
        return serviceProviderStatus;
    }

    public Set<Integer> getUnsatisfiedParamsOneOffset() {
        return unsatisfiedParamsOneOffset;
    }

    public Map<String, Integer> getNames() {
        return names;
    }

    private EPException handleSetterException(String parameterName, int parameterIndex, Throwable t) {
        String message = t.getMessage();
        if (t instanceof NullPointerException) {
            message = "Received a null-value for a primitive type";
        }
        return new EPException("Failed to set substitution parameter " + parameterName + ", expected a value of type '" + types[parameterIndex - 1].getName() + "': " + message, t);
    }

    private void updateUnsatisfied(Integer index) {
        if (unsatisfiedParamsOneOffset.isEmpty()) {
            return;
        }
        unsatisfiedParamsOneOffset.remove(index);
        if (unsatisfiedParamsOneOffset.isEmpty()) {
            unsatisfiedParamsOneOffset = Collections.emptySet();
        }
    }
}
