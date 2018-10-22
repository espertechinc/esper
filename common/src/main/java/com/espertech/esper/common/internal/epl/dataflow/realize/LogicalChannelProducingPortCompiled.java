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
import com.espertech.esper.common.internal.epl.dataflow.util.GraphTypeDesc;

public class LogicalChannelProducingPortCompiled {
    private int producingOpNum;
    private String producingOpPrettyPrint;
    private String streamName;
    private int streamNumber;
    private GraphTypeDesc graphTypeDesc;
    private boolean hasPunctuation;

    public LogicalChannelProducingPortCompiled() {
    }

    public LogicalChannelProducingPortCompiled(int producingOpNum, String producingOpPrettyPrint, String streamName, int streamNumber, GraphTypeDesc graphTypeDesc, boolean hasPunctuation) {
        this.producingOpNum = producingOpNum;
        this.producingOpPrettyPrint = producingOpPrettyPrint;
        this.streamName = streamName;
        this.streamNumber = streamNumber;
        this.graphTypeDesc = graphTypeDesc;
        this.hasPunctuation = hasPunctuation;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return new SAIFFInitializeBuilder(LogicalChannelProducingPortCompiled.class, this.getClass(), "c", parent, symbols, classScope)
                .constant("producingOpNum", producingOpNum)
                .constant("producingOpPrettyPrint", producingOpPrettyPrint)
                .constant("streamName", streamName)
                .constant("streamNumber", streamNumber)
                .method("graphTypeDesc", method -> graphTypeDesc.make(method, symbols, classScope))
                .constant("hasPunctuation", hasPunctuation)
                .build();
    }

    public String getProducingOpPrettyPrint() {
        return producingOpPrettyPrint;
    }

    public int getProducingOpNum() {
        return producingOpNum;
    }

    public String getStreamName() {
        return streamName;
    }

    public int getStreamNumber() {
        return streamNumber;
    }

    public boolean isHasPunctuation() {
        return hasPunctuation;
    }

    public GraphTypeDesc getGraphTypeDesc() {
        return graphTypeDesc;
    }

    public void setProducingOpNum(int producingOpNum) {
        this.producingOpNum = producingOpNum;
    }

    public void setProducingOpPrettyPrint(String producingOpPrettyPrint) {
        this.producingOpPrettyPrint = producingOpPrettyPrint;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber;
    }

    public void setGraphTypeDesc(GraphTypeDesc graphTypeDesc) {
        this.graphTypeDesc = graphTypeDesc;
    }

    public void setHasPunctuation(boolean hasPunctuation) {
        this.hasPunctuation = hasPunctuation;
    }

    public String toString() {
        return "LogicalChannelProducingPort{" +
                "op=" + producingOpPrettyPrint + '\'' +
                ", streamName='" + streamName + '\'' +
                ", portNumber=" + streamNumber +
                ", hasPunctuation=" + hasPunctuation +
                '}';
    }
}
