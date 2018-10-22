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
package com.espertech.esper.common.client.dataflow.core;

import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;

/**
 * Context for use with {@link EPDataFlowOperatorParameterProvider} describes the operator and parameters to provide.
 */
public class EPDataFlowOperatorParameterProviderContext {

    private final String operatorName;
    private final String parameterName;
    private final DataFlowOperatorFactory factory;
    private final int operatorNum;
    private final String dataFlowName;

    /**
     * Ctor.
     *
     * @param initializeContext context
     * @param parameterName     parameter name
     */
    public EPDataFlowOperatorParameterProviderContext(DataFlowOpInitializeContext initializeContext, String parameterName) {
        this.operatorName = initializeContext.getOperatorName();
        this.parameterName = parameterName;
        this.factory = initializeContext.getDataFlowOperatorFactory();
        this.operatorNum = initializeContext.getOperatorNumber();
        this.dataFlowName = initializeContext.getDataFlowName();
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
    public Object getFactory() {
        return factory;
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
     * Returns the data flow name.
     *
     * @return data flow name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }
}
