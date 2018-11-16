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
package com.espertech.esper.example.matchmaker;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.DeploymentOptions;
import com.espertech.esper.runtime.client.EPRuntime;

public class MatchMakerEPL {
    public static void setup(EPRuntime runtime, MatchAlertListener listener) {

        // Allocate a partition (session) that lasts for a mobile user until they sent a new event with new location
        String eplContext = "create context PerUser " +
            "initiated by MobileUserBean as mub " +
            "terminated by MobileUserBean(userId = mub.userId);\n";

        // For each partition find a match within the location range
        String eplMatch = "@name('alert') context PerUser select mub as self, context.mub as other from MobileUserBean (" +
            "locationX in [(context.mub.locationX - 1) : (context.mub.locationX + 1)]," +
            "locationY in [(context.mub.locationY - 1) : (context.mub.locationY + 1)]," +
            "myGender = context.mub.preferredGender, " +
            "myAgeRange = context.mub.preferredAgeRange, " +
            "myHairColor = context.mub.preferredHairColor, " +
            "preferredGender = context.mub.myGender, " +
            "preferredAgeRange = context.mub.myAgeRange, " +
            "preferredHairColor = context.mub.myHairColor," +
            "userId != context.mub.userId" +
            ") as mub;\n";

        try {
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(eplContext + eplMatch, new CompilerArguments(runtime.getRuntimePath()));
            runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("matchmaker-deployment"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        runtime.getDeploymentService().getStatement("matchmaker-deployment", "alert").addListener(listener);
    }
}
