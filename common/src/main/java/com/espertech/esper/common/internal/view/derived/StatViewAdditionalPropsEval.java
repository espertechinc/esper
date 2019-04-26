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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Map;

public class StatViewAdditionalPropsEval {
    private final String[] additionalProps;
    private final ExprEvaluator[] additionalEvals;
    private final Class[] additionalTypes;
    private final DataInputOutputSerde[] additionalSerdes;

    public StatViewAdditionalPropsEval(String[] additionalProps, ExprEvaluator[] additionalEvals, Class[] additionalTypes, DataInputOutputSerde[] additionalSerdes) {
        this.additionalProps = additionalProps;
        this.additionalEvals = additionalEvals;
        this.additionalTypes = additionalTypes;
        this.additionalSerdes = additionalSerdes;
    }

    public String[] getAdditionalProps() {
        return additionalProps;
    }

    public ExprEvaluator[] getAdditionalEvals() {
        return additionalEvals;
    }

    public Class[] getAdditionalTypes() {
        return additionalTypes;
    }

    public DataInputOutputSerde[] getAdditionalSerdes() {
        return additionalSerdes;
    }

    public void addProperties(Map<String, Object> newDataMap, Object[] lastValuesEventNew) {
        if (lastValuesEventNew != null) {
            for (int i = 0; i < additionalProps.length; i++) {
                newDataMap.put(additionalProps[i], lastValuesEventNew[i]);
            }
        }
    }
}
