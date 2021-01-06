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
package com.espertech.esper.regressionlib.suite.expr.datetime;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.client.type.EPTypePremade.*;

public class ExprDTWithTime implements RegressionExecution {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        return executions;
    }

    public void run(RegressionEnvironment env) {
        RegressionPath path = new RegressionPath();
        String epl = "@name('variables') create variable int varhour;\n" +
            "create variable int varmin;\n" +
            "create variable int varsec;\n" +
            "create variable int varmsec;\n";
        env.compileDeploy(epl, path);
        String variablesDepId = env.deploymentId("variables");

        String startTime = "2002-05-30T09:00:00.000";
        env.advanceTime(DateTime.parseDefaultMSec(startTime));

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        epl = "@name('s0') select " +
            "current_timestamp.withTime(varhour, varmin, varsec, varmsec) as val0," +
            "utildate.withTime(varhour, varmin, varsec, varmsec) as val1," +
            "longdate.withTime(varhour, varmin, varsec, varmsec) as val2," +
            "caldate.withTime(varhour, varmin, varsec, varmsec) as val3," +
            "localdate.withTime(varhour, varmin, varsec, varmsec) as val4," +
            "zoneddate.withTime(varhour, varmin, varsec, varmsec) as val5" +
            " from SupportDateTime";
        env.compileDeploy(epl, path).addListener("s0");
        SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), fields, new EPTypeClass[]{LONGBOXED.getEPType(), DATE.getEPType(), LONGBOXED.getEPType(), CALENDAR.getEPType(), LOCALDATETIME.getEPType(), ZONEDDATETIME.getEPType()});

        env.sendEventBean(SupportDateTime.make(null));
        env.assertPropsListenerNew("s0", fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null});

        String expectedTime = "2002-05-30T09:00:00.000";
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varhour", null); // variable is null
        env.sendEventBean(SupportDateTime.make(startTime));
        env.assertPropsListenerNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "long", "util", "long", "cal", "ldt", "zdt"));

        expectedTime = "2002-05-30T01:02:03.004";
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varhour", 1);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varmin", 2);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varsec", 3);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varmsec", 4);
        env.sendEventBean(SupportDateTime.make(startTime));
        env.assertPropsListenerNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "long", "util", "long", "cal", "ldt", "zdt"));

        expectedTime = "2002-05-30T00:00:00.006";
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varhour", 0);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varmin", null);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varsec", null);
        env.runtime().getVariableService().setVariableValue(variablesDepId, "varmsec", 6);
        env.sendEventBean(SupportDateTime.make(startTime));
        env.assertPropsListenerNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "long", "util", "long", "cal", "ldt", "zdt"));

        env.undeployAll();
    }
}
