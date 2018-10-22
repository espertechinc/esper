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

import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;

/**
 * Context for use with {@link EPDataFlowOperatorProvider}.
 */
public class EPDataFlowOperatorProviderContext {

    private final String dataFlowName;
    private final String operatorName;
    private final DataFlowOperatorFactory factory;

    /**
     * Ctor.
     *
     * @param dataFlowName data flow name
     * @param operatorName operator name
     * @param factory      factory
     */
    public EPDataFlowOperatorProviderContext(String dataFlowName, String operatorName, DataFlowOperatorFactory factory) {
        this.dataFlowName = dataFlowName;
        this.operatorName = operatorName;
        this.factory = factory;
    }

    /**
     * Operator name.
     *
     * @return name
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Data flow name
     *
     * @return name
     */
    public String getDataFlowName() {
        return dataFlowName;
    }

    /**
     * Returns the factory
     *
     * @return factory
     */
    public DataFlowOperatorFactory getFactory() {
        return factory;
    }
}
