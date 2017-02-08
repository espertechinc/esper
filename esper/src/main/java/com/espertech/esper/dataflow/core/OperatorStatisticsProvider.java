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

import com.espertech.esper.client.dataflow.EPDataFlowInstanceOperatorStat;
import com.espertech.esper.client.dataflow.EPDataFlowInstanceStatistics;
import com.espertech.esper.dataflow.util.OperatorMetadataDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OperatorStatisticsProvider implements EPDataFlowInstanceStatistics {
    private final long[][] submitCounts;
    private final long[][] cpuDelta;
    private final OperatorMetadataDescriptor[] desc;

    public OperatorStatisticsProvider(Map<Integer, OperatorMetadataDescriptor> operatorMetadata) {
        submitCounts = new long[operatorMetadata.size()][];
        cpuDelta = new long[operatorMetadata.size()][];
        desc = new OperatorMetadataDescriptor[operatorMetadata.size()];
        for (Map.Entry<Integer, OperatorMetadataDescriptor> entry : operatorMetadata.entrySet()) {
            int opNum = entry.getKey();
            desc[opNum] = entry.getValue();
            int numPorts = entry.getValue().getOperatorSpec().getOutput().getItems().size();
            submitCounts[opNum] = new long[numPorts];
            cpuDelta[opNum] = new long[numPorts];
        }
    }

    public List<EPDataFlowInstanceOperatorStat> getOperatorStatistics() {
        List<EPDataFlowInstanceOperatorStat> result = new ArrayList<EPDataFlowInstanceOperatorStat>(submitCounts.length);
        for (int i = 0; i < submitCounts.length; i++) {

            long[] submittedPerPort = submitCounts[i];
            long submittedOverall = 0;
            for (long port : submittedPerPort) {
                submittedOverall += port;
            }

            long[] timePerPort = cpuDelta[i];
            long timeOverall = 0;
            for (long port : timePerPort) {
                timeOverall += port;
            }

            OperatorMetadataDescriptor meta = desc[i];
            EPDataFlowInstanceOperatorStat stat = new EPDataFlowInstanceOperatorStat(meta.getOperatorName(), meta.getOperatorPrettyPrint(), i, submittedOverall, submittedPerPort, timeOverall, timePerPort);
            result.add(stat);
        }
        return result;
    }

    public void countSubmitPort(int producerOpNum, int portNumber) {
        submitCounts[producerOpNum][portNumber]++;
    }

    public void countSubmitPortWithTime(int producerOpNum, int portNumber, long nanoTimeDelta) {
        countSubmitPort(producerOpNum, portNumber);
        cpuDelta[producerOpNum][portNumber] += nanoTimeDelta;
    }
}
