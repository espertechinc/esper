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
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportDateTime;

public class ExprDTWithDate implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String startTime = "2002-05-30T09:00:00.000";
        env.advanceTime(DateTime.parseDefaultMSec(startTime));

        String[] fields = "val0,val1,val2,val3,val4,val5".split(",");
        String epl = "" +
            "create variable int varyear;\n" +
            "create variable int varmonth;\n" +
            "create variable int varday;\n" +
            "@name('s0') select " +
            "current_timestamp.withDate(varyear, varmonth, varday) as val0," +
            "utildate.withDate(varyear, varmonth, varday) as val1," +
            "longdate.withDate(varyear, varmonth, varday) as val2," +
            "caldate.withDate(varyear, varmonth, varday) as val3," +
            "localdate.withDate(varyear, varmonth+1, varday) as val4," +
            "zoneddate.withDate(varyear, varmonth+1, varday) as val5" +
            " from SupportDateTime";
        env.compileDeploy(epl).addListener("s0");
        String deployId = env.deploymentId("s0");
        SupportEventPropUtil.assertTypes(env.statement("s0").getEventType(), fields, new EPTypeClass[]{EPTypePremade.LONGBOXED.getEPType(), EPTypePremade.DATE.getEPType(), EPTypePremade.LONGBOXED.getEPType(), EPTypePremade.CALENDAR.getEPType(), EPTypePremade.LOCALDATETIME.getEPType(), EPTypePremade.ZONEDDATETIME.getEPType()});

        env.sendEventBean(SupportDateTime.make(null));
        env.assertPropsNew("s0", fields, new Object[]{SupportDateTime.getValueCoerced(startTime, "long"), null, null, null, null, null});

        String expectedTime = "2004-09-03T09:00:00.000";
        env.runtime().getVariableService().setVariableValue(deployId, "varyear", 2004);
        env.runtime().getVariableService().setVariableValue(deployId, "varmonth", 8);
        env.runtime().getVariableService().setVariableValue(deployId, "varday", 3);
        env.sendEventBean(SupportDateTime.make(startTime));
        env.assertPropsNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "long", "util", "long", "cal", "ldt", "zdt"));

        expectedTime = "2002-09-30T09:00:00.000";
        env.runtime().getVariableService().setVariableValue(deployId, "varyear", null);
        env.runtime().getVariableService().setVariableValue(deployId, "varmonth", 8);
        env.runtime().getVariableService().setVariableValue(deployId, "varday", null);
        env.sendEventBean(SupportDateTime.make(startTime));
        env.assertPropsNew("s0", fields, SupportDateTime.getArrayCoerced(expectedTime, "long", "util", "long", "cal", "ldt", "zdt"));

        env.undeployAll();
    }
}
