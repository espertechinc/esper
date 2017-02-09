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
 * Handles setting or overriding properties for operators in a data flow.
 */
public interface EPDataFlowOperatorParameterProvider {

    /**
     * Return new value for operator
     *
     * @param context operator and parameter information
     * @return value
     */
    public Object provide(EPDataFlowOperatorParameterProviderContext context);
}
