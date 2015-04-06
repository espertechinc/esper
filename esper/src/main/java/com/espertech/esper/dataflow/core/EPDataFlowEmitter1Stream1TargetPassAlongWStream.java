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

import com.espertech.esper.dataflow.util.DataFlowSignalManager;

import java.lang.reflect.InvocationTargetException;

public class EPDataFlowEmitter1Stream1TargetPassAlongWStream extends EPDataFlowEmitter1Stream1TargetPassAlong {

    private final int streamNum;

    public EPDataFlowEmitter1Stream1TargetPassAlongWStream(int operatorNum, DataFlowSignalManager signalManager, SignalHandler signalHandler, EPDataFlowEmitterExceptionHandler exceptionHandler, ObjectBindingPair target, int streamNum) {
        super(operatorNum, signalManager, signalHandler, exceptionHandler, target);
        this.streamNum = streamNum;
    }

    @Override
    public void submitInternal(Object object) {
        Object[] parameters = new Object[] {streamNum, object};
        try {
            exceptionHandler.handleAudit(targetObject, parameters);
            fastMethod.invoke(targetObject, parameters);
        }
        catch (InvocationTargetException e) {
            exceptionHandler.handleException(targetObject, fastMethod, e, parameters);
        }
    }
}
