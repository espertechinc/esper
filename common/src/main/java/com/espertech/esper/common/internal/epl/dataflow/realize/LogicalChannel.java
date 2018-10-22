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
package com.espertech.esper.common.internal.epl.dataflow.realize;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeBuilder;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;

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

    public LogicalChannel() {
    }

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

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(LogicalChannel.class, this.getClass(), "lc", parent, symbols, classScope)
                .constant("channelId", channelId)
                .constant("consumingOpName", consumingOpName)
                .constant("consumingOpNum", consumingOpNum)
                .constant("consumingOpStreamNum", consumingOpStreamNum)
                .constant("consumingOpStreamName", consumingOpStreamName)
                .constant("consumingOptStreamAliasName", consumingOptStreamAliasName)
                .constant("consumingOpPrettyPrint", consumingOpPrettyPrint)
                .method("outputPort", method -> outputPort.make(method, symbols, classScope))
                .build();
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

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public void setConsumingOpName(String consumingOpName) {
        this.consumingOpName = consumingOpName;
    }

    public void setConsumingOpNum(int consumingOpNum) {
        this.consumingOpNum = consumingOpNum;
    }

    public void setConsumingOpStreamNum(int consumingOpStreamNum) {
        this.consumingOpStreamNum = consumingOpStreamNum;
    }

    public void setConsumingOpStreamName(String consumingOpStreamName) {
        this.consumingOpStreamName = consumingOpStreamName;
    }

    public void setConsumingOptStreamAliasName(String consumingOptStreamAliasName) {
        this.consumingOptStreamAliasName = consumingOptStreamAliasName;
    }

    public void setConsumingOpPrettyPrint(String consumingOpPrettyPrint) {
        this.consumingOpPrettyPrint = consumingOpPrettyPrint;
    }

    public void setOutputPort(LogicalChannelProducingPortCompiled outputPort) {
        this.outputPort = outputPort;
    }

    public String toString() {
        return "LogicalChannel{" +
                "channelId=" + channelId +
                ", produced=" + outputPort +
                ", consumed='" + consumingOpPrettyPrint + '\'' +
                '}';
    }
}
