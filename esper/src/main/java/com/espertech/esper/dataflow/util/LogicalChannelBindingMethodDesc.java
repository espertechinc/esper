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

import java.lang.reflect.Method;

public class LogicalChannelBindingMethodDesc {

    private Method method;
    private LogicalChannelBindingType bindingType;

    public LogicalChannelBindingMethodDesc(Method method, LogicalChannelBindingType bindingType) {
        this.method = method;
        this.bindingType = bindingType;
    }

    public Method getMethod() {
        return method;
    }

    public LogicalChannelBindingType getBindingType() {
        return bindingType;
    }
}
