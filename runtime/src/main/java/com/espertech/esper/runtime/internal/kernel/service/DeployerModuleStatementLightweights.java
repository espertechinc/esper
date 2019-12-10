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

import com.espertech.esper.common.internal.context.module.StatementLightweight;

import java.util.List;
import java.util.Map;

public class DeployerModuleStatementLightweights {
    private final int statementIdFirstStatement;
    private final List<StatementLightweight> lightweights;
    private final Map<Integer, Map<Integer, Object>> substitutionParameters;

    public DeployerModuleStatementLightweights(int statementIdFirstStatement, List<StatementLightweight> lightweights, Map<Integer, Map<Integer, Object>> substitutionParameters) {
        this.statementIdFirstStatement = statementIdFirstStatement;
        this.lightweights = lightweights;
        this.substitutionParameters = substitutionParameters;
    }

    public List<StatementLightweight> getLightweights() {
        return lightweights;
    }

    public Map<Integer, Map<Integer, Object>> getSubstitutionParameters() {
        return substitutionParameters;
    }

    public int getStatementIdFirstStatement() {
        return statementIdFirstStatement;
    }
}
