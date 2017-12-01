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
package com.espertech.esper.dataflow.ops;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.*;
import com.espertech.esper.dataflow.util.GraphTypeDesc;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeOrigin;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.util.EPLValidationUtil;
import com.espertech.esper.event.EventBeanSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@DataFlowOperator
public class Filter implements DataFlowOpLifecycle {

    private static final Logger log = LoggerFactory.getLogger(Filter.class);

    @DataFlowOpParameter
    private ExprNode filter;

    private ExprEvaluator evaluator;
    private EventBeanSPI theEvent;
    private EventBean[] eventsPerStream = new EventBean[1];
    private boolean singleOutputPort;

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext prepareContext) throws Exception {

        if (prepareContext.getInputPorts().size() != 1) {
            throw new ExprValidationException("Filter requires single input port");
        }
        if (filter == null) {
            throw new ExprValidationException("Required parameter 'filter' providing the filter expression is not provided");
        }
        if (prepareContext.getOutputPorts().isEmpty() || prepareContext.getOutputPorts().size() > 2) {
            throw new IllegalArgumentException("Filter operator requires one or two output stream(s) but produces " + prepareContext.getOutputPorts().size() + " streams");
        }

        EventType eventType = prepareContext.getInputPorts().get(0).getTypeDesc().getEventType();
        singleOutputPort = prepareContext.getOutputPorts().size() == 1;

        ExprNode validated = EPLValidationUtil.validateSimpleGetSubtree(ExprNodeOrigin.DATAFLOWFILTER, filter, prepareContext.getStatementContext(), eventType, false);
        evaluator = ExprNodeCompiler.allocateEvaluator(validated.getForge(), prepareContext.getServicesContext().getEngineImportService(), Filter.class, false, prepareContext.getStatementContext().getStatementName());
        theEvent = prepareContext.getServicesContext().getEventAdapterService().getShellForType(eventType);
        eventsPerStream[0] = theEvent;

        GraphTypeDesc[] typesPerPort = new GraphTypeDesc[prepareContext.getOutputPorts().size()];
        for (int i = 0; i < typesPerPort.length; i++) {
            typesPerPort[i] = new GraphTypeDesc(false, true, eventType);
        }
        return new DataFlowOpInitializeResult(typesPerPort);
    }

    public void onInput(Object row) {
        if (log.isDebugEnabled()) {
            log.debug("Received row for filtering: " + Arrays.toString((Object[]) row));
        }

        if (!(row instanceof EventBeanSPI)) {
            theEvent.setUnderlying(row);
        } else {
            theEvent = (EventBeanSPI) row;
        }

        Boolean pass = (Boolean) evaluator.evaluate(eventsPerStream, true, null);
        if (pass != null && pass) {
            if (log.isDebugEnabled()) {
                log.debug("Submitting row " + Arrays.toString((Object[]) row));
            }

            if (singleOutputPort) {
                graphContext.submit(row);
            } else {
                graphContext.submitPort(0, row);
            }
        } else {
            if (!singleOutputPort) {
                graphContext.submitPort(1, row);
            }
        }
    }

    public void open(DataFlowOpOpenContext openContext) {
        // no action
    }

    public void close(DataFlowOpCloseContext openContext) {
        // no action
    }
}
