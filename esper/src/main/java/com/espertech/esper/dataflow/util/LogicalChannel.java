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

/**
 * Models a pipe between two operators.
 */
public class LogicalChannel {
    private int channelId;
    private String consumingOpName;
    private int consumingOpNum;
    private int consumingOpStreamNum;
    private String consumingOpStreamName;
    private String consumingOptStreamAliasName;
    private String consumingOpPrettyPrint;
    private LogicalChannelProducingPortCompiled outputPort;

    public LogicalChannel(int channelId, String consumingOpName, int consumingOpNum, int consumingOpStreamNum, String consumingOpStreamName, String consumingOptStreamAliasName, String consumingOpPrettyPrint, LogicalChannelProducingPortCompiled outputPort) {
        this.channelId = channelId;
        this.consumingOpName = consumingOpName;
        this.consumingOpNum = consumingOpNum;
        this.consumingOpStreamNum = consumingOpStreamNum;
        this.consumingOpStreamName = consumingOpStreamName;
        this.consumingOptStreamAliasName = consumingOptStreamAliasName;
        this.consumingOpPrettyPrint = consumingOpPrettyPrint;
        this.outputPort = outputPort;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getConsumingOpName() {
        return consumingOpName;
    }

    public String getConsumingOpStreamName() {
        return consumingOpStreamName;
    }

    public String getConsumingOptStreamAliasName() {
        return consumingOptStreamAliasName;
    }

    public int getConsumingOpStreamNum() {
        return consumingOpStreamNum;
    }

    public int getConsumingOpNum() {
        return consumingOpNum;
    }

    public LogicalChannelProducingPortCompiled getOutputPort() {
        return outputPort;
    }

    public String getConsumingOpPrettyPrint() {
        return consumingOpPrettyPrint;
    }

    public String toString() {
        return "LogicalChannel{" +
                "channelId=" + channelId +
                ", produced=" + outputPort +
                ", consumed='" + consumingOpPrettyPrint + '\'' +
                '}';
    }
}
