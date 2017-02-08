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

public class LogicalChannelBinding {

    private final LogicalChannel logicalChannel;
    private final LogicalChannelBindingMethodDesc consumingBindingDesc;
    private final LogicalChannelBindingMethodDesc consumingSignalBindingDesc;

    public LogicalChannelBinding(LogicalChannel logicalChannel, LogicalChannelBindingMethodDesc consumingBindingDesc, LogicalChannelBindingMethodDesc consumingSignalBindingDesc) {
        this.logicalChannel = logicalChannel;
        this.consumingBindingDesc = consumingBindingDesc;
        this.consumingSignalBindingDesc = consumingSignalBindingDesc;
    }

    public LogicalChannel getLogicalChannel() {
        return logicalChannel;
    }

    public LogicalChannelBindingMethodDesc getConsumingBindingDesc() {
        return consumingBindingDesc;
    }

    public LogicalChannelBindingMethodDesc getConsumingSignalBindingDesc() {
        return consumingSignalBindingDesc;
    }
}
