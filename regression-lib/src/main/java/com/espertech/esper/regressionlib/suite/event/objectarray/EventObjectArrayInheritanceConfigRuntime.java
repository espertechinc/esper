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
package com.espertech.esper.regressionlib.suite.event.objectarray;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.EnumSet;

public class EventObjectArrayInheritanceConfigRuntime implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@buseventtype @public create objectarray schema RootEvent(base string);\n" +
            "@buseventtype @public create objectarray schema Sub1Event(sub1 string) inherits RootEvent;\n" +
            "@buseventtype @public create objectarray schema Sub2Event(sub2 string) inherits RootEvent;\n" +
            "@buseventtype @public create objectarray schema SubAEvent(suba string) inherits Sub1Event;\n" +
            "@buseventtype @public create objectarray schema SubBEvent(subb string) inherits SubAEvent;\n";
        RegressionPath path = new RegressionPath();
        env.compileDeploy(epl, path);

        EventObjectArrayInheritanceConfigInit.runObjectArrInheritanceAssertion(env, path);
    }

    public EnumSet<RegressionFlag> flags() {
        return EnumSet.of(RegressionFlag.OBSERVEROPS);
    }
}
