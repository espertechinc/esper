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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import com.espertech.esper.support.SupportEventTypeAssertionEnum;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecTableOnMerge implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionMergeWhereWithMethodRead(epService);
        runAssertionMergeSelectWithAggReadAndEnum(epService);
        runAssertionOnMergePlainPropsAnyKeyed(epService);
    }

    private void runAssertionMergeWhereWithMethodRead(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table varaggMMR (keyOne string primary key, cnt count(*))");
        epService.getEPAdministrator().createEPL("into table varaggMMR select count(*) as cnt " +
                "from SupportBean#lastevent group by theString");

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select varaggMMR[p00].keyOne as c0 from SupportBean_S0").addListener(listener);
        epService.getEPAdministrator().createEPL("on SupportBean_S1 merge varaggMMR where cnt = 0 when matched then delete");

        epService.getEPRuntime().sendEvent(new SupportBean("G1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("G2", 0));
        assertKeyFound(epService, listener, "G1,G2,G3", new boolean[]{true, true, false});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0)); // delete
        assertKeyFound(epService, listener, "G1,G2,G3", new boolean[]{false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("G3", 0));
        assertKeyFound(epService, listener, "G1,G2,G3", new boolean[]{false, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));  // delete
        assertKeyFound(epService, listener, "G1,G2,G3", new boolean[]{false, false, true});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionMergeSelectWithAggReadAndEnum(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create table varaggMS (" +
                "eventset window(*) @type(SupportBean), total sum(int))");
        epService.getEPAdministrator().createEPL("into table varaggMS select window(*) as eventset, " +
                "sum(intPrimitive) as total from SupportBean#length(2)");
        epService.getEPAdministrator().createEPL("on SupportBean_S0 merge varaggMS " +
                "when matched then insert into ResultStream select eventset, total, eventset.takeLast(1) as c0");
        epService.getEPAdministrator().createEPL("select * from ResultStream").addListener(listener);

        SupportBean e1 = new SupportBean("E1", 15);
        epService.getEPRuntime().sendEvent(e1);

        assertResultAggRead(epService, listener, new Object[]{e1}, 15);

        SupportBean e2 = new SupportBean("E2", 20);
        epService.getEPRuntime().sendEvent(e2);

        assertResultAggRead(epService, listener, new Object[]{e1, e2}, 35);

        SupportBean e3 = new SupportBean("E3", 30);
        epService.getEPRuntime().sendEvent(e3);

        assertResultAggRead(epService, listener, new Object[]{e2, e3}, 50);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertResultAggRead(EPServiceProvider epService, SupportUpdateListener listener, Object[] objects, int total) {
        String[] fields = "eventset,total".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EventBean event = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(event, fields, new Object[]{objects, total});
        EPAssertionUtil.assertEqualsExactOrder(new Object[]{objects[objects.length - 1]}, ((Collection) event.get("c0")).toArray());
    }

    private void assertKeyFound(EPServiceProvider epService, SupportUpdateListener listener, String keyCsv, boolean[] expected) {
        String[] split = keyCsv.split(",");
        for (int i = 0; i < split.length; i++) {
            String key = split[i];
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, key));
            String expectedString = expected[i] ? key : null;
            assertEquals("failed for key '" + key + "'", expectedString, listener.assertOneGetNewAndReset().get("c0"));
        }
    }

    private void runAssertionOnMergePlainPropsAnyKeyed(EPServiceProvider epService) {
        runOnMergeInsertUpdDeleteSingleKey(epService, true);
        runOnMergeInsertUpdDeleteSingleKey(epService, false);

        runOnMergeInsertUpdDeleteTwoKey(epService, true);
        runOnMergeInsertUpdDeleteTwoKey(epService, false);

        runOnMergeInsertUpdDeleteUngrouped(epService, true);
        runOnMergeInsertUpdDeleteUngrouped(epService, false);
    }

    private void runOnMergeInsertUpdDeleteUngrouped(EPServiceProvider epService, boolean soda) {
        String eplDeclare = "create table varaggIUD (p0 string, sumint sum(int))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String[] fields = "c0,c1".split(",");
        String eplRead = "select varaggIUD.p0 as c0, varaggIUD.sumint as c1, varaggIUD as c2 from SupportBean_S0";
        EPStatement stmtRead = SupportModelHelper.createByCompileOrParse(epService, soda, eplRead);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtRead.addListener(listener);

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", String.class}, {"c1", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtRead.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        // create merge
        String eplMerge = "on SupportBean merge varaggIUD" +
                " when not matched then" +
                " insert select theString as p0" +
                " when matched and theString like \"U%\" then" +
                " update set p0=\"updated\"" +
                " when matched and theString like \"D%\" then" +
                " delete";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);

        // merge for varagg
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));

        // assert
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", null});

        // also aggregate-into the same key
        SupportModelHelper.createByCompileOrParse(epService, soda, "into table varaggIUD select sum(50) as sumint from SupportBean_S1");
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 50});

        // update for varagg
        epService.getEPRuntime().sendEvent(new SupportBean("U2", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EventBean received = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(received, fields, new Object[]{"updated", 50});
        EPAssertionUtil.assertPropsMap((Map) received.get("c2"), "p0,sumint".split(","), new Object[]{"updated", 50});

        // delete for varagg
        epService.getEPRuntime().sendEvent(new SupportBean("D3", 0));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void runOnMergeInsertUpdDeleteSingleKey(EPServiceProvider epService, boolean soda) {
        String[] fieldsTable = "key,p0,p1,p2,sumint".split(",");
        String eplDeclare = "create table varaggMIU (key int primary key, p0 string, p1 int, p2 int[], sumint sum(int))";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String[] fields = "c0,c1,c2,c3".split(",");
        String eplRead = "select varaggMIU[id].p0 as c0, varaggMIU[id].p1 as c1, varaggMIU[id].p2 as c2, varaggMIU[id].sumint as c3 from SupportBean_S0";
        EPStatement stmtRead = SupportModelHelper.createByCompileOrParse(epService, soda, eplRead);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtRead.addListener(listener);

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", String.class}, {"c1", Integer.class}, {"c2", Integer[].class}, {"c3", Integer.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtRead.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        // create merge
        String eplMerge = "on SupportBean merge varaggMIU" +
                " where intPrimitive=key" +
                " when not matched then" +
                " insert select intPrimitive as key, \"v1\" as p0, 1000 as p1, {1,2} as p2" +
                " when matched and theString like \"U%\" then" +
                " update set p0=\"v2\", p1=2000, p2={3,4}" +
                " when matched and theString like \"D%\" then" +
                " delete";
        EPStatement stmtMerge = SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);
        SupportUpdateListener listenerMerge = new SupportUpdateListener();
        stmtMerge.addListener(listenerMerge);

        // merge for varagg[10]
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listenerMerge.assertOneGetNewAndReset(), fieldsTable, new Object[]{10, "v1", 1000, new int[]{1, 2}, null});

        // assert key "10"
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"v1", 1000, new Integer[]{1, 2}, null});

        // also aggregate-into the same key
        SupportModelHelper.createByCompileOrParse(epService, soda, "into table varaggMIU select sum(50) as sumint from SupportBean_S1 group by id");
        epService.getEPRuntime().sendEvent(new SupportBean_S1(10));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"v1", 1000, new Integer[]{1, 2}, 50});

        // update for varagg[10]
        epService.getEPRuntime().sendEvent(new SupportBean("U2", 10));
        EPAssertionUtil.assertProps(listenerMerge.getLastNewData()[0], fieldsTable, new Object[]{10, "v2", 2000, new int[]{3, 4}, 50});
        EPAssertionUtil.assertProps(listenerMerge.getAndResetLastOldData()[0], fieldsTable, new Object[]{10, "v1", 1000, new int[]{1, 2}, 50});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"v2", 2000, new Integer[]{3, 4}, 50});

        // delete for varagg[10]
        epService.getEPRuntime().sendEvent(new SupportBean("D3", 10));
        EPAssertionUtil.assertProps(listenerMerge.assertOneGetOldAndReset(), fieldsTable, new Object[]{10, "v2", 2000, new int[]{3, 4}, 50});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggMIU__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggMIU__public", false);
    }

    public void runOnMergeInsertUpdDeleteTwoKey(EPServiceProvider epService, boolean soda) {
        String eplDeclare = "create table varaggMIUD (keyOne int primary key, keyTwo string primary key, prop string)";
        SupportModelHelper.createByCompileOrParse(epService, soda, eplDeclare);

        String[] fields = "c0,c1,c2".split(",");
        String eplRead = "select varaggMIUD[id,p00].keyOne as c0, varaggMIUD[id,p00].keyTwo as c1, varaggMIUD[id,p00].prop as c2 from SupportBean_S0";
        EPStatement stmtRead = SupportModelHelper.createByCompileOrParse(epService, soda, eplRead);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtRead.addListener(listener);

        // assert selected column types
        Object[][] expectedAggType = new Object[][]{{"c0", Integer.class}, {"c1", String.class}, {"c2", String.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedAggType, stmtRead.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);

        // assert no row
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        // create merge
        String eplMerge = "on SupportBean merge varaggMIUD" +
                " where intPrimitive=keyOne and theString=keyTwo" +
                " when not matched then" +
                " insert select intPrimitive as keyOne, theString as keyTwo, \"inserted\" as prop" +
                " when matched and longPrimitive>0 then" +
                " update set prop=\"updated\"" +
                " when matched and longPrimitive<0 then" +
                " delete";
        EPStatement stmtMerge = SupportModelHelper.createByCompileOrParse(epService, soda, eplMerge);
        Object[][] expectedType = new Object[][]{{"keyOne", Integer.class}, {"keyTwo", String.class}, {"prop", String.class}};
        SupportEventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtMerge.getEventType(), SupportEventTypeAssertionEnum.NAME, SupportEventTypeAssertionEnum.TYPE);


        // merge for varagg[10, "A"]
        epService.getEPRuntime().sendEvent(new SupportBean("A", 10));

        // assert key {"10", "A"}
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, "A", "inserted"});

        // update for varagg[10, "A"]
        epService.getEPRuntime().sendEvent(makeSupportBean("A", 10, 1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{10, "A", "updated"});

        // test typable output
        epService.getEPAdministrator().getConfiguration().addEventType(LocalBean.class);
        EPStatement stmtConvert = epService.getEPAdministrator().createEPL("insert into LocalBean select varaggMIUD[10, 'A'] as val0 from SupportBean_S1");
        stmtConvert.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.keyOne".split(","), new Object[]{10});

        // delete for varagg[10, "A"]
        epService.getEPRuntime().sendEvent(makeSupportBean("A", 10, -1));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "A"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null});

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggMIUD__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_varaggMIUD__public", false);
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }

    public static class LocalSubBean {
        private int keyOne;
        private String keyTwo;
        private String prop;

        public int getKeyOne() {
            return keyOne;
        }

        public void setKeyOne(int keyOne) {
            this.keyOne = keyOne;
        }

        public String getKeyTwo() {
            return keyTwo;
        }

        public void setKeyTwo(String keyTwo) {
            this.keyTwo = keyTwo;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    public static class LocalBean {
        private LocalSubBean val0;

        public LocalSubBean getVal0() {
            return val0;
        }

        public void setVal0(LocalSubBean val0) {
            this.val0 = val0;
        }
    }
}
