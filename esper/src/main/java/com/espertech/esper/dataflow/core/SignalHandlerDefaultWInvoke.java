/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SignalHandlerDefaultWInvoke extends SignalHandlerDefault {

    private static final Log log = LogFactory.getLog(SignalHandlerDefaultWInvoke.class);

    protected final Object target;
    protected final FastMethod fastMethod;

    public SignalHandlerDefaultWInvoke(Object target, Method method) {
        this.target = target;

        FastClass fastClass = FastClass.create(Thread.currentThread().getContextClassLoader(), target.getClass());
        fastMethod = fastClass.getMethod(method);
    }

    @Override
    public void handleSignal(EPDataFlowSignal signal) {
        try {
            handleSignalInternal(signal);
        }
        catch (InvocationTargetException ex) {
            log.error("Failed to invoke signal handler: " + ex.getMessage(), ex);
        }
    }

    protected void handleSignalInternal(EPDataFlowSignal signal) throws InvocationTargetException {
        fastMethod.invoke(target, new Object[] {signal});
    }

}
