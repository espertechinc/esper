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

import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchSpin;
import com.espertech.esper.common.internal.statement.insertintolatch.InsertIntoLatchWait;

public interface EPEventServiceQueueProcessor {
    void processThreadWorkQueueLatchedWait(InsertIntoLatchWait insertIntoLatch);
    void processThreadWorkQueueLatchedSpin(InsertIntoLatchSpin insertIntoLatch);
    void processThreadWorkQueueUnlatched(Object item);
}
