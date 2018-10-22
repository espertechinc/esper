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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.dataflow.annotations.DataFlowContext;
import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignalFinalMarker;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpCloseContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpOpenContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowSourceOperator;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanManufacturer;
import com.espertech.esper.common.internal.event.core.EventPropertyWriter;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

public class BeaconSourceOp implements DataFlowSourceOperator {
    private static final Logger log = LoggerFactory.getLogger(BeaconSourceOp.class);

    private final BeaconSourceFactory factory;
    private final long iterations;
    private final long initialDelayMSec;
    private final long periodDelayMSec;

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private long lastSendTime;
    private long iterationNumber;
    private Pair<EventPropertyWriter, Object>[] additionalProperties;

    public BeaconSourceOp(BeaconSourceFactory factory, long iterations, long initialDelayMSec, long periodDelayMSec, Map<String, Object> additionalParameters) {
        this.factory = factory;
        this.iterations = iterations;
        this.initialDelayMSec = initialDelayMSec;
        this.periodDelayMSec = periodDelayMSec;

        if (additionalParameters != null) {
            additionalProperties = new Pair[additionalParameters.size()];
            int count = 0;
            for (Map.Entry<String, Object> param : additionalParameters.entrySet()) {
                EventPropertyWriter writer = ((EventTypeSPI) factory.getOutputEventType()).getWriter(param.getKey());
                if (writer == null) {
                    throw new EPException("Failed to find writer for property '" + param.getKey() + "' for event type '" + factory.getOutputEventType().getName() + "'");
                }
                additionalProperties[count++] = new Pair<>(writer, param.getValue());
            }
        }
    }

    public void next() {
        if (iterationNumber == 0 && initialDelayMSec > 0) {
            try {
                Thread.sleep(initialDelayMSec, 0);
            } catch (InterruptedException e) {
                graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
                });
            }
        }

        if (iterationNumber > 0 && periodDelayMSec > 0) {
            long nsecDelta = lastSendTime - System.nanoTime();
            long sleepTime = periodDelayMSec - nsecDelta / 1000000;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
                    });
                }
            }
        }

        if (iterations > 0 && iterationNumber >= iterations) {
            graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {
            });
        } else {
            iterationNumber++;
            ExprEvaluator[] evaluators = factory.getPropertyEvaluators();
            if (evaluators != null) {
                Object[] row = new Object[evaluators.length];
                for (int i = 0; i < row.length; i++) {
                    if (evaluators[i] != null) {
                        row[i] = evaluators[i].evaluate(null, true, null);
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("BeaconSource submitting row " + Arrays.toString(row));
                }

                EventBeanManufacturer manufacturer = factory.getManufacturer();
                if (manufacturer == null) {
                    submitAndDone(row);
                    return;
                }

                if (!factory.isProduceEventBean() && additionalProperties == null) {
                    Object outputEvent = manufacturer.makeUnderlying(row);
                    submitAndDone(outputEvent);
                    return;
                }

                EventBean event = manufacturer.make(row);
                if (additionalProperties != null) {
                    for (Pair<EventPropertyWriter, Object> pair : additionalProperties) {
                        pair.getFirst().write(pair.getSecond(), event);
                    }
                }

                if (!factory.isProduceEventBean()) {
                    submitAndDone(event.getUnderlying());
                    return;
                }

                submitAndDone(event);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("BeaconSource submitting empty row");
                }
                submitAndDone(new Object[0]);
            }
        }
    }

    private void submitAndDone(Object row) {
        graphContext.submit(row);
        if (periodDelayMSec > 0) {
            lastSendTime = System.nanoTime();
        }
    }

    public void open(DataFlowOpOpenContext openContext) {
        // no action
    }

    public void close(DataFlowOpCloseContext openContext) {
        // no action
    }
}
