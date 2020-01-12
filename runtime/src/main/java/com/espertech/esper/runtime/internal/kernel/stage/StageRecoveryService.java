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

import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;

import java.util.Iterator;
import java.util.Map;

public interface StageRecoveryService {

    int stageAdd(String stageUri);
    Iterator<Map.Entry<String, Integer>> stagesIterate();
    void stageDestroy(String stageUri, int stageId);

    String deploymentGetStage(String deploymentId);
    void deploymentSetStage(String deploymentId, String stageUri);
    void deploymentRemoveFromStages(String deploymentId);

    StageSpecificServices makeSpecificServices(int stageId, String stageUri, EPServicesContext servicesContext);
    EPStageEventServiceSPI makeEventService(StageSpecificServices stageSpecificServices, int stageId, String stageUri, EPServicesContext servicesContext);
}
