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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.runtime.client.EPStageService;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public interface EPStageServiceSPI extends EPStageService {
    void clear();
    void recoverStage(String stageURI, int stageId, long stageCurrentTime);
    void recoverDeployment(String stageUri, DeploymentInternal deployment);
    void recoveredStageInitialize(Supplier<Collection<EventType>> availableTypes);
    boolean isEmpty();
    Map<String, EPStageImpl> getStages();
    void destroy();
}
