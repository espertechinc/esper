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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterContext;
import com.espertech.esper.runtime.client.option.StatementSubstitutionParameterOption;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SupportPortableDeploySubstitutionParams implements StatementSubstitutionParameterOption, Serializable {
    private static final long serialVersionUID = -5640702703310083333L;
    private LinkedHashMap<Integer, Object> valuesByIndex;
    private Map<String, Object> valuesByName;

    public SupportPortableDeploySubstitutionParams() {
    }

    public SupportPortableDeploySubstitutionParams(Map<String, Object> valuesByName) {
        this.valuesByName = valuesByName;
    }

    public SupportPortableDeploySubstitutionParams(int index, Object value) {
        valuesByIndex = new LinkedHashMap<>();
        valuesByIndex.put(index, value);
    }

    public SupportPortableDeploySubstitutionParams(String name, Object value) {
        valuesByName = new LinkedHashMap<>();
        valuesByName.put(name, value);
    }

    public SupportPortableDeploySubstitutionParams(int indexOne, Object valueOne, int indexTwo, Object valueTwo) {
        valuesByIndex = new LinkedHashMap<>();
        valuesByIndex.put(indexOne, valueOne);
        valuesByIndex.put(indexTwo, valueTwo);
    }

    public void setStatementParameters(StatementSubstitutionParameterContext env) {
        if (valuesByIndex != null) {
            for (Map.Entry<Integer, Object> entry : valuesByIndex.entrySet()) {
                env.setObject(entry.getKey(), entry.getValue());
            }
        }
        if (valuesByName != null) {
            for (Map.Entry<String, Object> entry : valuesByName.entrySet()) {
                env.setObject(entry.getKey(), entry.getValue());
            }
        }
    }

    public SupportPortableDeploySubstitutionParams add(int index, Object value) {
        if (valuesByName != null) {
            throw new IllegalArgumentException("Values-by-name exists");
        }
        if (valuesByIndex == null) {
            valuesByIndex = new LinkedHashMap<>();
        }
        if (valuesByIndex.containsKey(index)) {
            throw new IllegalArgumentException("Index appears multiple times");
        }
        valuesByIndex.put(index, value);
        return this;
    }

    public SupportPortableDeploySubstitutionParams add(String name, Object value) {
        if (valuesByIndex != null) {
            throw new IllegalArgumentException("Values-by-name exists");
        }
        if (valuesByName == null) {
            valuesByName = new LinkedHashMap<>();
        }
        if (valuesByName.containsKey(name)) {
            throw new IllegalArgumentException("Name appears multiple times");
        }
        valuesByName.put(name, value);
        return this;
    }
}
