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

import com.espertech.esper.common.internal.event.core.EventServiceSendEventCommon;
import com.espertech.esper.common.internal.event.util.EPRuntimeEventProcessWrapped;
import com.espertech.esper.common.internal.util.DeploymentIdNamePair;
import com.espertech.esper.runtime.client.EPEventService;

import java.util.Map;

public interface EPEventServiceSPI extends EPEventService, EPRuntimeEventProcessWrapped, EventServiceSendEventCommon {
    void initialize();

    Map<DeploymentIdNamePair, Long> getStatementNearestSchedules();

    void clearCaches();

    void destroy();

    long getRoutedInternal();

    long getRoutedExternal();
}
