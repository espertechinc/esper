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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.util.List;

/**
 * Represents an output port of an operator.
 */
public class DataFlowOperatorOutput implements Serializable {

    private static final long serialVersionUID = 5502274822423382377L;
    private String streamName;
    private List<DataFlowOperatorOutputType> typeInfo;

    /**
     * Ctor.
     */
    public DataFlowOperatorOutput() {
    }

    /**
     * Ctor.
     *
     * @param streamName output stream name
     * @param typeInfo   type information
     */
    public DataFlowOperatorOutput(String streamName, List<DataFlowOperatorOutputType> typeInfo) {
        this.streamName = streamName;
        this.typeInfo = typeInfo;
    }

    /**
     * Returns the output stream name.
     *
     * @return stream name.
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Sets the output stream name.
     *
     * @param streamName stream name.
     */
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    /**
     * Returns output port type information
     *
     * @return type info
     */
    public List<DataFlowOperatorOutputType> getTypeInfo() {
        return typeInfo;
    }

    /**
     * Sets output port type information
     *
     * @param typeInfo type info to use
     */
    public void setTypeInfo(List<DataFlowOperatorOutputType> typeInfo) {
        this.typeInfo = typeInfo;
    }
}
