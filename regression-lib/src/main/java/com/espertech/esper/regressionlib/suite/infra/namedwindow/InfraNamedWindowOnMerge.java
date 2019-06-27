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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportBean_Container;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NOTE: More namedwindow-related tests in "nwtable"
 */
public class InfraNamedWindowOnMerge {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraUpdateNonPropertySet());
        execs.add(new InfraMergeTriggeredByAnotherWindow());
        execs.add(new InfraPropertyInsertBean());
        execs.add(new InfraSubselect());
        execs.add(new InfraDocExample());
        execs.add(new InfraOnMergeWhere1Eq2InsertSelectStar());
        execs.add(new InfraOnMergeNoWhereClauseInsertSelectStar());
        execs.add(new InfraOnMergeNoWhereClauseInsertTranspose());
        return execs;
    }

    private static class InfraOnMergeWhere1Eq2InsertSelectStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionInsertSelectStar(env, "on SBStream merge MyWindow where 1=2 when not matched then insert select *;\n");
        }
    }

    private static class InfraOnMergeNoWhereClauseInsertSelectStar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionInsertSelectStar(env, "on SBStream as sbs merge MyWindow insert select *;\n");
        }
    }

    private static class InfraOnMergeNoWhereClauseInsertTranspose implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertionInsertSelectStar(env, "on SBStream as sbs merge MyWindow insert select transpose(sbs);\n");
        }
    }

    private static class InfraUpdateNonPropertySet implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window MyWindowUNP#keepall as SupportBean", path);
            env.compileDeploy("insert into MyWindowUNP select * from SupportBean", path);
            env.compileDeploy("@name('merge') on SupportBean_S0 as sb " +
                "merge MyWindowUNP as mywin when matched then " +
                "update set mywin.setDoublePrimitive(id), increaseIntCopyDouble(initial, mywin)", path);
            env.addListener("merge");
            String[] fields = "intPrimitive,doublePrimitive,doubleBoxed".split(",");

            env.sendEventBean(makeSupportBean("E1", 10, 2));
            env.sendEventBean(new SupportBean_S0(5, "E1"));
            EPAssertionUtil.assertProps(env.listener("merge").getAndResetLastNewData()[0], fields, new Object[]{11, 5d, 5d});

            // try a case-statement
            String eplCase = "on SupportBean_S0 merge MyWindowUNP " +
                "when matched then update set theString = " +
                "case intPrimitive when 1 then 'a' else 'b' end";
            env.compileDeploy(eplCase, path);

            env.undeployAll();
        }
    }

    private static class InfraMergeTriggeredByAnotherWindow implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();

            // test dispatch between named windows
            env.compileDeploy("@Name('A') create window A#unique(id) as (id int)", path);
            env.compileDeploy("@Name('B') create window B#unique(id) as (id int)", path);
            env.compileDeploy("@Name('C') on A merge B when not matched then insert select 1 as id when matched then insert select 1 as id", path);

            env.compileDeploy("@Name('D') select * from B", path).addListener("D");
            env.compileDeploy("@Name('E') insert into A select intPrimitive as id from SupportBean", path);

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("D").isInvoked());
            env.undeployAll();

            // test insert-stream only, no remove stream
            String[] fields = "c0,c1".split(",");
            String epl = "create window W1#lastevent as SupportBean;\n" +
                "insert into W1 select * from SupportBean;\n" +
                "create window W2#lastevent as SupportBean;\n" +
                "on W1 as a merge W2 as b when not matched then insert into OutStream select a.theString as c0, istream() as c1;\n" +
                "@name('s0') select * from OutStream;\n";
            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E1", true});

            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{"E2", true});

            env.undeployAll();
        }
    }

    private static class InfraDocExample implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionDocExample(env, rep);
            }
        }
    }

    private static class InfraPropertyInsertBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@name('window') create window MergeWindow#unique(theString) as SupportBean", path);

            String epl = "@name('merge') on SupportBean as up merge MergeWindow as mv where mv.theString=up.theString when not matched then insert select intPrimitive";
            env.compileDeploy(epl, path);
            env.sendEventBean(new SupportBean("E1", 10));

            EventBean theEvent = env.iterator("window").next();
            EPAssertionUtil.assertProps(theEvent, "theString,intPrimitive".split(","), new Object[]{null, 10});
            env.undeployModuleContaining("merge");

            epl = "on SupportBean as up merge MergeWindow as mv where mv.theString=up.theString when not matched then insert select theString, intPrimitive";
            env.compileDeploy(epl, path);
            env.sendEventBean(new SupportBean("E2", 20));

            EPAssertionUtil.assertPropsPerRow(env.iterator("window"), "theString,intPrimitive".split(","), new Object[][]{{null, 10}, {"E2", 20}});

            env.undeployAll();
        }
    }

    private static class InfraSubselect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionSubselect(env, rep);
            }
        }
    }

    private static void tryAssertionSubselect(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        String[] fields = "col1,col2".split(",");
        String epl = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMyEvent.class) + " create schema MyEvent as (in1 string, in2 int);\n";
        epl += eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedMySchema.class) + " create schema MySchema as (col1 string, col2 int);\n";
        epl += "@name('create') create window MyWindowSS#lastevent as MySchema;\n";
        epl += "on SupportBean_A delete from MyWindowSS;\n";
        epl += "on MyEvent me " +
            "merge MyWindowSS mw " +
            "when not matched and (select intPrimitive>0 from SupportBean(theString like 'A%')#lastevent) then " +
            "insert(col1, col2) select (select theString from SupportBean(theString like 'A%')#lastevent), (select intPrimitive from SupportBean(theString like 'A%')#lastevent) " +
            "when matched and (select intPrimitive>0 from SupportBean(theString like 'B%')#lastevent) then " +
            "update set col1=(select theString from SupportBean(theString like 'B%')#lastevent), col2=(select intPrimitive from SupportBean(theString like 'B%')#lastevent) " +
            "when matched and (select intPrimitive>0 from SupportBean(theString like 'C%')#lastevent) then " +
            "delete;\n";
        env.compileDeployWBusPublicType(epl, new RegressionPath());

        // no action tests
        sendMyEvent(env, eventRepresentationEnum, "X1", 1);
        env.sendEventBean(new SupportBean("A1", 0));   // ignored
        sendMyEvent(env, eventRepresentationEnum, "X2", 2);
        env.sendEventBean(new SupportBean("A2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, null);

        sendMyEvent(env, eventRepresentationEnum, "X3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A2", 20}});

        env.sendEventBean(new SupportBean_A("Y1"));
        env.sendEventBean(new SupportBean("A3", 30));
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, null);

        sendMyEvent(env, eventRepresentationEnum, "X4", 4);
        env.sendEventBean(new SupportBean("A4", 40));
        sendMyEvent(env, eventRepresentationEnum, "X5", 5);   // ignored as matched (no where clause, no B event)
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A3", 30}});

        env.sendEventBean(new SupportBean("B1", 50));
        sendMyEvent(env, eventRepresentationEnum, "X6", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"B1", 50}});

        env.sendEventBean(new SupportBean("B2", 60));
        sendMyEvent(env, eventRepresentationEnum, "X7", 7);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"B2", 60}});

        env.sendEventBean(new SupportBean("B2", 0));
        sendMyEvent(env, eventRepresentationEnum, "X8", 8);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"B2", 60}});

        env.sendEventBean(new SupportBean("C1", 1));
        sendMyEvent(env, eventRepresentationEnum, "X9", 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, null);

        env.sendEventBean(new SupportBean("C1", 0));
        sendMyEvent(env, eventRepresentationEnum, "X10", 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.iterator("create"), fields, new Object[][]{{"A4", 40}});

        env.undeployAll();
    }

    private static SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private static void sendMyEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String in1, int in2) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{in1, in2}, "MyEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<>();
            theEvent.put("in1", in1);
            theEvent.put("in2", in2);
            env.sendEventMap(theEvent, "MyEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("MyEvent")));
            theEvent.put("in1", in1);
            theEvent.put("in2", in2);
            env.eventService().sendEventAvro(theEvent, "MyEvent");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            env.eventService().sendEventJson("{\"in1\": \"" + in1 + "\", \"in2\": " + in2 + "}", "MyEvent");
        } else {
            fail();
        }
    }

    private static void tryAssertionDocExample(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {
        RegressionPath path = new RegressionPath();
        String baseModuleEPL = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedOrderEvent.class) +
            " create schema OrderEvent as (orderId string, productId string, price double, quantity int, deletedFlag boolean)";
        env.compileDeployWBusPublicType(baseModuleEPL, path);

        String appModuleOne = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonProvidedProductTotalRec.class) + " create schema ProductTotalRec as (productId string, totalPrice double);" +
            "" +
            "@Name('nwProd') create window ProductWindow#unique(productId) as ProductTotalRec;" +
            "" +
            "on OrderEvent oe\n" +
            "merge ProductWindow pw\n" +
            "where pw.productId = oe.productId\n" +
            "when matched\n" +
            "then update set totalPrice = totalPrice + oe.price\n" +
            "when not matched\n" +
            "then insert select productId, price as totalPrice;";
        env.compileDeploy(appModuleOne, path);

        String appModuleTwo = "@Name('nwOrd') create window OrderWindow#keepall as OrderEvent;" +
            "" +
            "on OrderEvent oe\n" +
            "  merge OrderWindow pw\n" +
            "  where pw.orderId = oe.orderId\n" +
            "  when not matched \n" +
            "    then insert select *\n" +
            "  when matched and oe.deletedFlag=true\n" +
            "    then delete\n" +
            "  when matched\n" +
            "    then update set pw.quantity = oe.quantity, pw.price = oe.price";

        env.compileDeploy(appModuleTwo, path);

        sendOrderEvent(env, eventRepresentationEnum, "O1", "P1", 10, 100, false);
        sendOrderEvent(env, eventRepresentationEnum, "O1", "P1", 11, 200, false);
        sendOrderEvent(env, eventRepresentationEnum, "O2", "P2", 3, 300, false);
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("nwProd").iterator(), "productId,totalPrice".split(","), new Object[][]{{"P1", 21d}, {"P2", 3d}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.statement("nwOrd").iterator(), "orderId,quantity".split(","), new Object[][]{{"O1", 200}, {"O2", 300}});

        String module = "create schema StreetCarCountSchema (streetid string, carcount int);" +
            "    create schema StreetChangeEvent (streetid string, action string);" +
            "    create window StreetCarCountWindow#unique(streetid) as StreetCarCountSchema;" +
            "    on StreetChangeEvent ce merge StreetCarCountWindow w where ce.streetid = w.streetid\n" +
            "    when not matched and ce.action = 'ENTER' then insert select streetid, 1 as carcount\n" +
            "    when matched and ce.action = 'ENTER' then update set StreetCarCountWindow.carcount = carcount + 1\n" +
            "    when matched and ce.action = 'LEAVE' then update set StreetCarCountWindow.carcount = carcount - 1;" +
            "    select * from StreetCarCountWindow;";
        env.compileDeploy(module, path);

        env.undeployAll();
    }

    private static void sendOrderEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String orderId, String productId, double price, int quantity, boolean deletedFlag) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{orderId, productId, price, quantity, deletedFlag}, "OrderEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("orderId", orderId);
            theEvent.put("productId", productId);
            theEvent.put("price", price);
            theEvent.put("quantity", quantity);
            theEvent.put("deletedFlag", deletedFlag);
            env.sendEventMap(theEvent, "OrderEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            GenericData.Record theEvent = new GenericData.Record(SupportAvroUtil.getAvroSchema(env.runtime().getEventTypeService().getEventTypePreconfigured("OrderEvent")));
            theEvent.put("orderId", orderId);
            theEvent.put("productId", productId);
            theEvent.put("price", price);
            theEvent.put("quantity", quantity);
            theEvent.put("deletedFlag", deletedFlag);
            env.eventService().sendEventAvro(theEvent, "OrderEvent");
        } else {
            JsonObject object = new JsonObject();
            object.add("orderId", orderId);
            object.add("productId", productId);
            object.add("price", price);
            object.add("quantity", quantity);
            object.add("deletedFlag", deletedFlag);
            env.eventService().sendEventJson(object.toString(), "OrderEvent");
        }
    }

    private static void runAssertionInsertSelectStar(RegressionEnvironment env, String onInsert) {
        String epl = "insert into SBStream select * from SupportBean_Container[beans];\n" +
            "@name('window') create window MyWindow#keepall as SupportBean;\n" +
            onInsert;
        env.compileDeploy(epl);

        env.sendEventBean(new SupportBean_Container(Arrays.asList(new SupportBean("E1", 10), new SupportBean("E2", 20))));

        EPAssertionUtil.assertPropsPerRow(env.iterator("window"), "theString,intPrimitive".split(","), new Object[][]{{"E1", 10}, {"E2", 20}});

        env.undeployAll();
    }

    public static void increaseIntCopyDouble(SupportBean initialBean, SupportBean updatedBean) {
        updatedBean.setIntPrimitive(initialBean.getIntPrimitive() + 1);
        updatedBean.setDoubleBoxed(updatedBean.getDoublePrimitive());
    }

    public static class MyLocalJsonProvidedMyEvent implements Serializable {
        public String in1;
        public int in2;
    }

    public static class MyLocalJsonProvidedMySchema implements Serializable {
        public String col1;
        public int col2;
    }

    public static class MyLocalJsonProvidedOrderEvent implements Serializable {
        public String orderId;
        public String productId;
        public double price;
        public int quantity;
        public boolean deletedFlag;
    }

    public static class MyLocalJsonProvidedProductTotalRec implements Serializable {
        public String productId;
        public double totalPrice;
    }

}
