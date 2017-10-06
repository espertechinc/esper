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
package com.espertech.esper.epl.agg.service.groupby;

import com.espertech.esper.epl.variable.VariableReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for handling aggregation with grouping by group-keys.
 */
public class AggSvcGroupByReclaimAgedEvalFuncVariable implements AggSvcGroupByReclaimAgedEvalFunc {
    private static final Logger log = LoggerFactory.getLogger(AggSvcGroupByReclaimAgedEvalFuncVariable.class);

    private VariableReader variableReader;

    public AggSvcGroupByReclaimAgedEvalFuncVariable(VariableReader variableReader) {
        this.variableReader = variableReader;
    }

    public Double getLongValue() {
        Object val = variableReader.getValue();
        if ((val != null) && (val instanceof Number)) {
            return ((Number) val).doubleValue();
        }
        log.warn("Variable '" + variableReader.getVariableMetaData().getVariableName() + " returned a null value, using last valid value");
        return null;
    }
}
