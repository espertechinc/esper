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
package com.espertech.esper.client.dataflow;

/**
 * Context for use with {@link EPDataFlowOperatorParameterProvider} describes the operator and parameters to provide.
 */
public class EPDataFlowOperatorParameterProviderContext {

    private final String operatorName;
    private final String parameterName;
    private final Object operatorInstance;
    private final int operatorNum;
    private final Object providedValue;
    private final String dataFlowName;

    /**
     * Ctor.
     *
     * @param operatorName     operator name
     * @param parameterName    parameter name
     * @param operatorInstance operator instance
     * @param operatorNum      operator number
     * @param providedValue    value if any was provided as part of the declaration
     * @param dataFlowName     data flow name
     */
    public EPDataFlowOperatorParameterProviderContext(String operatorName, String parameterName, Object operatorInstance, int operatorNum, Object providedValue, String dataFlowName) {
        this.operatorName = operatorName;
        this.parameterName = parameterName;
        this.operatorInstance = operatorInstance;
        this.operatorNum = operatorNum;
        this.providedValue = providedValue;
        this.dataFlowName = dataFlowName;
    }

    /**
     * Returns the operator name.
     *
     * @return operator name
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Returns the parameter name.
     *
     * @return parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Returns the operator instance.
     *
     * @return operator instance
     */
    public Object getOperatorInstance() {
        return operatorInstance;
    }

    /**
     * Returns the operator number
     *
     * @return operator num
     */
    public int getOperatorNum() {
        return operatorNum;
    }

    /**
     * Returns the parameters declared value, if any
     *
     * @return value
     */
    public Object getProvidedValue() {
        return providedValue;
    }

    /**
     * Returns the data flow name.
     *
     * @return data flow name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }
}
