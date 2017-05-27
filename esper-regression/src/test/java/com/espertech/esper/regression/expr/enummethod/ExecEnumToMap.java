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
package com.espertech.esper.regression.expr.enummethod;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_ST0;
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ExecEnumToMap implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // - duplicate value allowed, latest value wins
        // - null key & value allowed

        String eplFragment = "select contained.toMap(c => id, c=> p00) as val from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val".split(","), new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,5"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{1, 5, 12});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,1", "E3,12", "E2,12", "E1,2"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{2, 12, 12});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0_Container(Collections.singletonList(new SupportBean_ST0(null, null))));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val"), "E1,E2,E3".split(","), new Object[]{null, null, null});
        stmtFragment.destroy();

        // test scalar-coll with lambda
        String[] fields = "val0".split(",");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.toMap(c => c, c => extractNum(c)) as val0 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Map.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E3"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), "E1,E2,E3".split(","), new Object[]{1, 2, 3});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), "E1".split(","), new Object[]{1});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        assertNull(listener.assertOneGetNewAndReset().get("val0"));

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        assertEquals(0, ((Map) listener.assertOneGetNewAndReset().get("val0")).size());
    }
}
