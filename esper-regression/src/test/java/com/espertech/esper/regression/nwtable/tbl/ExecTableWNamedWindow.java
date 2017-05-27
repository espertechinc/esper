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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecTableWNamedWindow implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@Name('var') create table varagg (key string primary key, total sum(int))");
        epService.getEPAdministrator().createEPL("@Name('win') create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("@Name('insert') insert into MyWindow select * from SupportBean");
        epService.getEPAdministrator().createEPL("@Name('populate') into table varagg select sum(intPrimitive) as total from MyWindow group by theString");
        epService.getEPAdministrator().createEPL("@Name('select') on SupportBean_S0 select theString, varagg[p00].total as c0 from MyWindow where theString = p00").addListener(listener);
        String[] fields = "theString,c0".split(",");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 10});
    }
}
