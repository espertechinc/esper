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
package com.espertech.esper.runtime.internal.dataflow.op.beaconsource;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.dataflow.util.DataFlowParameterResolution;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpFactoryInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;

public class BeaconSourceFactory implements DataFlowOperatorFactory {
    private ExprEvaluator iterations;
    private ExprEvaluator initialDelay;
    private ExprEvaluator interval;
    private EventType outputEventType;
    private ExprEvaluator[] propertyEvaluators;
    private EventBeanManufacturer manufacturer;
    private boolean produceEventBean;

    public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        // no action
    }

    public DataFlowOperator operator(DataFlowOpInitializeContext context) {
        long iterationsCount = DataFlowParameterResolution.resolveNumber("iterations", iterations, 0, context).longValue();
        double initialDelaySec = DataFlowParameterResolution.resolveNumber("initialDelay", initialDelay, 0, context).doubleValue();
        long initialDelayMSec = (long) (initialDelaySec * 1000);
        double intervalSec = DataFlowParameterResolution.resolveNumber("interval", interval, 0, context).doubleValue();
        long intervalMSec = (long) (intervalSec * 1000);
        return new BeaconSourceOp(this, iterationsCount, initialDelayMSec, intervalMSec, context.getAdditionalParameters());
    }

    public ExprEvaluator[] getPropertyEvaluators() {
        return propertyEvaluators;
    }

    public void setPropertyEvaluators(ExprEvaluator[] propertyEvaluators) {
        this.propertyEvaluators = propertyEvaluators;
    }

    public EventBeanManufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(EventBeanManufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public boolean isProduceEventBean() {
        return produceEventBean;
    }

    public void setProduceEventBean(boolean produceEventBean) {
        this.produceEventBean = produceEventBean;
    }

    public ExprEvaluator getIterations() {
        return iterations;
    }

    public void setIterations(ExprEvaluator iterations) {
        this.iterations = iterations;
    }

    public ExprEvaluator getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(ExprEvaluator initialDelay) {
        this.initialDelay = initialDelay;
    }

    public ExprEvaluator getInterval() {
        return interval;
    }

    public void setInterval(ExprEvaluator interval) {
        this.interval = interval;
    }

    public EventType getOutputEventType() {
        return outputEventType;
    }

    public void setOutputEventType(EventType outputEventType) {
        this.outputEventType = outputEventType;
    }
}
