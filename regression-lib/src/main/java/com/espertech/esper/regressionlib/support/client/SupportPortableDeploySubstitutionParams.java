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
    private final LinkedHashMap<Integer, Object> valuesByIndex;

    public SupportPortableDeploySubstitutionParams(int index, Object value) {
        valuesByIndex = new LinkedHashMap<>();
        valuesByIndex.put(index, value);
    }

    public void setStatementParameters(StatementSubstitutionParameterContext env) {
        for (Map.Entry<Integer, Object> entry : valuesByIndex.entrySet()) {
            env.setObject(entry.getKey(), entry.getValue());
        }
    }
}
