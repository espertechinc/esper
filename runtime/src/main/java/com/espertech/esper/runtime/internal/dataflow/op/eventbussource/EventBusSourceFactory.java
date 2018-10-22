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
package com.espertech.esper.runtime.internal.dataflow.op.eventbussource;

import com.espertech.esper.common.client.dataflow.core.EPDataFlowEventBeanCollector;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.Map;

public class EventBusSourceFactory implements DataFlowOperatorFactory {
    private Map<String, Object> collector;
    private FilterSpecActivatable filterSpecActivatable;
    private boolean submitEventBean;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        EPDataFlowEventBeanCollector collectorInstance = DataFlowParameterResolution.resolveOptionalInstance("collector", collector, EPDataFlowEventBeanCollector.class, context);
        return new EventBusSourceOp(this, context.getAgentInstanceContext(), collectorInstance);
    }

    public void setSubmitEventBean(boolean submitEventBean) {
        this.submitEventBean = submitEventBean;
    }

    public boolean isSubmitEventBean() {
        return submitEventBean;
    }

    public void setCollector(Map<String, Object> collector) {
        this.collector = collector;
    }

    public FilterSpecActivatable getFilterSpecActivatable() {
        return filterSpecActivatable;
    }

    public void setFilterSpecActivatable(FilterSpecActivatable filterSpecActivatable) {
        this.filterSpecActivatable = filterSpecActivatable;
    }
}
