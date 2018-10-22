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

package com.espertech.esper.runtime.internal.dataflow.op.epstatementsource;

public class PortAndMessagePair {
    private final int port;
    private final Object message;

    public PortAndMessagePair(int port, Object message) {
        this.port = port;
        this.message = message;
    }

    public int getPort() {
        return port;
    }

    public Object getMessage() {
        return message;
    }
}
