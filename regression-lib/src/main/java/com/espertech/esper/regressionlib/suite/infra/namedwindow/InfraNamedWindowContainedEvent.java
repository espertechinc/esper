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
package com.espertech.esper.regressionlib.suite.infra.namedwindow;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowContainedEvent implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl;
        RegressionPath path = new RegressionPath();
        env.compileDeploy("create window BookWindow#time(30) as BookDesc", path);

        epl = "select * from SupportBean unidirectional, BookWindow[reviews]";
        tryInvalidCompile(env, path, epl, "Failed to validate named window use in join, contained-event is only allowed for named windows when marked as unidirectional");

        epl = "select *, (select * from BookWindow[reviews] where sb.theString = comment) " +
            "from SupportBean sb";
        tryInvalidCompile(env, path, epl, "Failed to plan subquery number 1 querying BookWindow: Failed to validate named window use in subquery, contained-event is only allowed for named windows when not correlated ");

        env.undeployAll();
    }
}
