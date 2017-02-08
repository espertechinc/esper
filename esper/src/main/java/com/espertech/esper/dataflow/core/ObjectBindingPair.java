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

import com.espertech.esper.dataflow.util.LogicalChannelBinding;

public class ObjectBindingPair {
    private Object target;
    private String operatorPrettyPrint;
    private LogicalChannelBinding binding;

    public ObjectBindingPair(Object target, String operatorPrettyPrint, LogicalChannelBinding binding) {
        this.target = target;
        this.operatorPrettyPrint = operatorPrettyPrint;
        this.binding = binding;
    }

    public String getOperatorPrettyPrint() {
        return operatorPrettyPrint;
    }

    public Object getTarget() {
        return target;
    }

    public LogicalChannelBinding getBinding() {
        return binding;
    }
}
