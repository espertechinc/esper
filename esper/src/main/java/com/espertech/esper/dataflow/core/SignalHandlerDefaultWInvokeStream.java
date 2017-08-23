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
package com.espertech.esper.dataflow.core;

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.epl.core.engineimport.EngineImportService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SignalHandlerDefaultWInvokeStream extends SignalHandlerDefaultWInvoke {

    private final int streamNum;

    public SignalHandlerDefaultWInvokeStream(Object target, Method method, EngineImportService engineImportService, int streamNum) {
        super(target, method, engineImportService);
        this.streamNum = streamNum;
    }

    @Override
    public void handleSignalInternal(EPDataFlowSignal signal) throws InvocationTargetException {
        fastMethod.invoke(target, new Object[]{streamNum, signal});
    }
}
