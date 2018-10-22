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
package com.espertech.esper.runtime.internal.dataflow.op.epstatementsource;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowEPStatementFilter;
import com.espertech.esper.common.client.dataflow.core.EPDataFlowIRStreamCollector;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Map;

public class EPStatementSourceFactory implements DataFlowOperatorFactory {

    private ExprEvaluator statementDeploymentId;
    private ExprEvaluator statementName;
    private Map<String, Object> statementFilter;
    private Map<String, Object> collector;
    private boolean submitEventBean;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        String statementDeploymentIdParam = DataFlowParameterResolution.resolveStringOptional("statementDeploymentId", statementDeploymentId, context);
        String statementNameParam = DataFlowParameterResolution.resolveStringOptional("statementName", statementName, context);
        EPDataFlowEPStatementFilter statementFilterInstance = DataFlowParameterResolution.resolveOptionalInstance("statementFilter", statementFilter, EPDataFlowEPStatementFilter.class, context);
        EPDataFlowIRStreamCollector collectorInstance = DataFlowParameterResolution.resolveOptionalInstance("collector", collector, EPDataFlowIRStreamCollector.class, context);

        if (statementNameParam == null && statementFilterInstance == null) {
            throw new EPException("Failed to find required 'statementName' or 'statementFilter' parameter");
        }

        return new EPStatementSourceOp(this, context.getAgentInstanceContext(), statementDeploymentIdParam, statementNameParam, statementFilterInstance, collectorInstance);
    }

    public void setStatementName(ExprEvaluator statementName) {
        this.statementName = statementName;
    }

    public void setStatementFilter(Map<String, Object> statementFilter) {
        this.statementFilter = statementFilter;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public void setSubmitEventBean(boolean submitEventBean) {
        this.submitEventBean = submitEventBean;
    }

    public ExprEvaluator getStatementName() {
        return statementName;
    }

    public Map<String, Object> getStatementFilter() {
        return statementFilter;
    }

    public Map<String, Object> getCollector() {
        return collector;
    }

    public boolean isSubmitEventBean() {
        return submitEventBean;
    }

    public void setStatementDeploymentId(ExprEvaluator statementDeploymentId) {
        this.statementDeploymentId = statementDeploymentId;
    }
}
