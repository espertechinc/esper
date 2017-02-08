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
package com.espertech.esper.dataflow.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogicalChannelUtil {

    public static List<LogicalChannelBinding> getBindingsConsuming(int producerOpNum, List<LogicalChannelBinding> bindings) {
        List<LogicalChannelBinding> result = new ArrayList<LogicalChannelBinding>();

        for (LogicalChannelBinding binding : bindings) {
            if (binding.getLogicalChannel().getOutputPort().getProducingOpNum() != producerOpNum) {
                continue;
            }
            result.add(binding);
        }

        return result;
    }

    public static String printChannels(List<LogicalChannel> channels) {
        StringWriter writer = new StringWriter();
        writer.write("\n");
        for (LogicalChannel channel : channels) {
            writer.write(channel.toString() + "\n");
        }
        return writer.toString();
    }

    public static List<LogicalChannelProducingPortCompiled> getOutputPortByStreamName(Set<Integer> incomingOpNums, String[] inputStreamNames, Map<Integer, List<LogicalChannelProducingPortCompiled>> compiledOutputPorts) {
        List<LogicalChannelProducingPortCompiled> ports = new ArrayList<LogicalChannelProducingPortCompiled>();
        for (int operator : incomingOpNums) {
            List<LogicalChannelProducingPortCompiled> opPorts = compiledOutputPorts.get(operator);
            if (opPorts != null) {  // Can be null if referring to itself
                for (LogicalChannelProducingPortCompiled opPort : opPorts) {
                    for (String streamName : inputStreamNames) {
                        if (opPort.getStreamName().equals(streamName)) {
                            ports.add(opPort);
                        }
                    }
                }
            }
        }
        return ports;
    }
}
