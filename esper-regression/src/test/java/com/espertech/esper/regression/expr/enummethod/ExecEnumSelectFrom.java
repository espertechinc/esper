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
import com.espertech.esper.supportregression.bean.SupportBean_ST0_Container;
import com.espertech.esper.supportregression.bean.SupportCollection;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collection;
import java.util.Map;

public class ExecEnumSelectFrom implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("Bean", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNew(epService);
        runAssertionSelect(epService);
    }

    private void runAssertionNew(EPServiceProvider epService) {

        String eplFragment = "select " +
                "contained.selectFrom(x => new {c0 = id||'x', c1 = key0||'y'}) as val0 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("E1,12,0", "E2,11,0", "E3,2,0"));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E1x", "12y"}, {"E2x", "11y"}, {"E3x", "2y"}});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("E4,0,1"));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[][]{{"E4x", "0y"}});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value(null));
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","), null);

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value());
        EPAssertionUtil.assertPropsPerRow(toMapArray(listener.assertOneGetNewAndReset().get("val0")), "c0,c1".split(","),
                new Object[0][]);

        stmtFragment.destroy();
    }

    private void runAssertionSelect(EPServiceProvider epService) {

        String eplFragment = "select " +
                "contained.selectFrom(x => id) as val0 " +
                "from Bean";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtFragment.getEventType(), "val0".split(","), new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value("E1,12", "E2,11", "E3,2"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1", "E2", "E3");
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value(null));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make2Value());
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", new String[0]);
        listener.reset();
        stmtFragment.destroy();

        // test scalar-coll with lambda
        String[] fields = "val0".split(",");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("extractNum", ExecEnumMinMax.MyService.class.getName(), "extractNum");
        String eplLambda = "select " +
                "strvals.selectFrom(v => extractNum(v)) as val0 " +
                "from SupportCollection";
        EPStatement stmtLambda = epService.getEPAdministrator().createEPL(eplLambda);
        stmtLambda.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtLambda.getEventType(), fields, new Class[]{Collection.class, Collection.class});

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E2,E1,E5,E4"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", 2, 1, 5, 4);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString("E1"));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", 1);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(null));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", null);
        listener.reset();

        epService.getEPRuntime().sendEvent(SupportCollection.makeString(""));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0");

        stmtLambda.destroy();
    }

    private Map[] toMapArray(Object result) {
        if (result == null) {
            return null;
        }
        Collection<Map> val = (Collection<Map>) result;
        return val.toArray(new Map[val.size()]);
    }
}
