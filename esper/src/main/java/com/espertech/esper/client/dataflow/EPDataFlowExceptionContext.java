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
 * Context for use with {@link EPDataFlowExceptionHandler}.
 */
public class EPDataFlowExceptionContext {

    private final String dataFlowName;
    private final String operatorName;
    private final Object operatorNumber;
    private final Object operatorPrettyPrint;
    private final Throwable throwable;

    /**
     * Ctor.
     *
     * @param dataFlowName        data flow name
     * @param operatorName        operator name
     * @param operatorNumber      operator number
     * @param operatorPrettyPrint pretty-print of operator
     * @param throwable           cause
     */
    public EPDataFlowExceptionContext(String dataFlowName, String operatorName, Object operatorNumber, Object operatorPrettyPrint, Throwable throwable) {
        this.dataFlowName = dataFlowName;
        this.operatorName = operatorName;
        this.operatorNumber = operatorNumber;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.throwable = throwable;
    }

    /**
     * Returns the data flow name.
     *
     * @return data flow name
     */
    public String getDataFlowName() {
        return dataFlowName;
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
     * Returns the cause.
     *
     * @return cause
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Returns the operator number.
     *
     * @return operator num
     */
    public Object getOperatorNumber() {
        return operatorNumber;
    }

    /**
     * Returns the pretty-print for the operator.
     *
     * @return operator string
     */
    public Object getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }
}
