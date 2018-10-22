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
import com.espertech.esper.common.internal.epl.dataflow.interfaces.EPDataFlowEmitter;

import java.lang.reflect.Method;

public interface SubmitHandler extends EPDataFlowEmitter {
    public void submitInternal(Object object);

    public void handleSignal(EPDataFlowSignal signal);

    public Method getFastMethod();
}
