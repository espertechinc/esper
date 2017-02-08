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

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.interfaces.EPDataFlowSignalHandler;

import java.util.ArrayList;
import java.util.List;

@DataFlowOperator
public class DefaultSupportCaptureOpStatic<T> implements EPDataFlowSignalHandler {

    private static List<DefaultSupportCaptureOpStatic> instances = new ArrayList<DefaultSupportCaptureOpStatic>();

    private List<Object> current = new ArrayList<Object>();

    public DefaultSupportCaptureOpStatic() {
        instances.add(this);
    }

    public synchronized void onInput(T event) {
        current.add(event);
    }

    public void onSignal(EPDataFlowSignal signal) {
        current.add(signal);
    }

    public static List<DefaultSupportCaptureOpStatic> getInstances() {
        return instances;
    }

    public List<Object> getCurrent() {
        return current;
    }
}

