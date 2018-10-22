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

public class SignalHandlerDefaultWInvoke extends SignalHandlerDefault {

    private static final Logger log = LoggerFactory.getLogger(SignalHandlerDefaultWInvoke.class);

    protected final Object target;
    protected final Method method;

    public SignalHandlerDefaultWInvoke(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    @Override
    public void handleSignal(EPDataFlowSignal signal) {
        try {
            handleSignalInternal(signal);
        } catch (InvocationTargetException ex) {
            log.error("Failed to invoke signal handler: " + ex.getMessage(), ex);
        }
    }

    protected void handleSignalInternal(EPDataFlowSignal signal) throws InvocationTargetException {
        try {
            method.invoke(target, new Object[]{signal});
        } catch (IllegalAccessException e) {
            log.error("Failed to invoke signal handler: " + e.getMessage(), e);
        }
    }
}
