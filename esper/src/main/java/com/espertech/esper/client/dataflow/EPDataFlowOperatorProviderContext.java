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

import com.espertech.esper.epl.spec.GraphOperatorSpec;

/**
 * Context for use with {@link EPDataFlowOperatorProvider}.
 */
public class EPDataFlowOperatorProviderContext {

    private final String dataFlowName;
    private final String operatorName;
    private final GraphOperatorSpec spec;

    /**
     * Ctor.
     *
     * @param dataFlowName data flow name
     * @param operatorName operator name
     * @param spec         specification
     */
    public EPDataFlowOperatorProviderContext(String dataFlowName, String operatorName, GraphOperatorSpec spec) {
        this.dataFlowName = dataFlowName;
        this.operatorName = operatorName;
        this.spec = spec;
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
     * Operator specification
     *
     * @return spec
     */
    public GraphOperatorSpec getSpec() {
        return spec;
    }
}
