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
package com.espertech.esper.regressionlib.support.stage;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.runtime.client.stage.EPStage;
import com.espertech.esper.runtime.client.stage.EPStageException;

import java.util.Arrays;

public class SupportStageUtil {
    public static void stageIt(RegressionEnvironment env, String stageUri, String... deploymentIds) {
        EPStage stage = checkStage(env, stageUri);
        try {
            stage.stage(Arrays.asList(deploymentIds));
        } catch (EPStageException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void unstageIt(RegressionEnvironment env, String stageUri, String... deploymentIds) {
        EPStage stage = checkStage(env, stageUri);
        try {
            stage.unstage(Arrays.asList(deploymentIds));
        } catch (EPStageException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static EPStage checkStage(RegressionEnvironment env, String stageUri) {
        EPStage stage = env.stageService().getExistingStage(stageUri);
        if (stage == null) {
            throw new RuntimeException("Failed to find stage '" + stageUri + "'");
        }
        return stage;
    }
}
