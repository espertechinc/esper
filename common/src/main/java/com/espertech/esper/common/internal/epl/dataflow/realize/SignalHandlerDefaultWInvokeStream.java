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

import com.espertech.esper.common.client.dataflow.util.EPDataFlowSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SignalHandlerDefaultWInvokeStream extends SignalHandlerDefaultWInvoke {

    private static final Logger log = LoggerFactory.getLogger(SignalHandlerDefaultWInvokeStream.class);

    private final int streamNum;

    public SignalHandlerDefaultWInvokeStream(Object target, Method method, int streamNum) {
        super(target, method);
        this.streamNum = streamNum;
    }

    @Override
    public void handleSignalInternal(EPDataFlowSignal signal) throws InvocationTargetException {
        try {
            method.invoke(target, new Object[]{streamNum, signal});
        } catch (IllegalAccessException e) {
            log.error("Failed to invoke signal handler: " + e.getMessage(), e);
        }
    }
}
