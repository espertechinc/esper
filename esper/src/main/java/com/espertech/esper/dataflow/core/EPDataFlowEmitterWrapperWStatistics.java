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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;

public class EPDataFlowEmitterWrapperWStatistics implements EPDataFlowEmitter {

    private final EPDataFlowEmitter facility;
    private final int producerOpNum;
    private final OperatorStatisticsProvider statisticsProvider;
    private final boolean cpuStatistics;

    public EPDataFlowEmitterWrapperWStatistics(EPDataFlowEmitter facility, int producerOpNum, OperatorStatisticsProvider statisticsProvider, boolean cpuStatistics) {
        this.facility = facility;
        this.producerOpNum = producerOpNum;
        this.statisticsProvider = statisticsProvider;
        this.cpuStatistics = cpuStatistics;
    }

    public void submit(Object object) {
        submitPort(0, object);
    }

    public void submitSignal(EPDataFlowSignal signal) {
        facility.submitSignal(signal);
    }

    public void submitPort(int portNumber, Object object) {
        if (!cpuStatistics) {
            facility.submitPort(portNumber, object);
            statisticsProvider.countSubmitPort(producerOpNum, portNumber);
        } else {
            long nanoTime = System.nanoTime();
            facility.submitPort(portNumber, object);
            long nanoTimDelta = System.nanoTime() - nanoTime;
            statisticsProvider.countSubmitPortWithTime(producerOpNum, portNumber, nanoTimDelta);
        }
    }
}
