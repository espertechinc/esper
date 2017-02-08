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
package com.espertech.esper.dataflow.interfaces;

import com.espertech.esper.dataflow.util.GraphTypeDesc;

import java.util.Set;

public class DataFlowOpInputPort {
    private final GraphTypeDesc typeDesc;
    private final Set<String> streamNames;
    private final String optionalAlias;
    private final boolean hasPunctuationSignal;

    public DataFlowOpInputPort(GraphTypeDesc typeDesc, Set<String> streamNames, String optionalAlias, boolean hasPunctuationSignal) {
        this.typeDesc = typeDesc;
        this.streamNames = streamNames;
        this.optionalAlias = optionalAlias;
        this.hasPunctuationSignal = hasPunctuationSignal;
    }

    public GraphTypeDesc getTypeDesc() {
        return typeDesc;
    }

    public Set<String> getStreamNames() {
        return streamNames;
    }

    public String getOptionalAlias() {
        return optionalAlias;
    }

    public boolean isHasPunctuationSignal() {
        return hasPunctuationSignal;
    }
}
