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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.EPStatementHandle;

public interface WorkQueue {
    void add(EventBean theEvent, EPStatementHandle epStatementHandle, boolean addToFront, int precedence);
    void add(EventBean theEvent);
    boolean isFrontEmpty();
    boolean processFront(EPEventServiceQueueProcessor epEventService);
    boolean processBack(EPEventServiceQueueProcessor epEventService);
}
