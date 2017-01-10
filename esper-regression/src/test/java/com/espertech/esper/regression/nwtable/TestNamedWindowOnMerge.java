/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.bookexample.OrderBean;
import com.espertech.esper.supportregression.bean.bookexample.OrderBeanFactory;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestNamedWindowOnMerge extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener nwListener;
    private SupportUpdateListener mergeListener;

    protected void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_A", SupportBean_A.class);
        config.addEventType("SupportBean_S0", SupportBean_S0.class);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        nwListener = new SupportUpdateListener();
        mergeListener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        nwListener = null;
        mergeListener = null;
    }

    public void testUpdateNonPropertySet() {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("increaseIntCopyDouble", this.getClass().getName(), "increaseIntCopyDouble");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean");
        EPStatement stmt = epService.getEPAdministrator().createEPL("on SupportBean_S0 as sb " +
                "merge MyWindow as mywin when matched then " +
                "update set mywin.setDoublePrimitive(id), increaseIntCopyDouble(initial, mywin)");
        stmt.addListener(mergeListener);
        String[] fields = "intPrimitive,doublePrimitive,doubleBoxed".split(",");

        epService.getEPRuntime().sendEvent(makeSupportBean("E1", 10, 2));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(5, "E1"));
        EPAssertionUtil.assertProps(mergeListener.getAndResetLastNewData()[0], fields, new Object[]{11, 5d, 5d});

        // try a case-statement
        String eplCase = "on SupportBean_S0 merge MyWindow " +
                "when matched then update set theString = " +
                "case intPrimitive when 1 then 'a' else 'b' end";
        epService.getEPAdministrator().createEPL(eplCase);
    }

    public void testMergeTriggeredByAnotherWindow() {

        // test dispatch between named windows
        epService.getEPAdministrator().createEPL("@Name('A') create window A#unique(id) as (id int)");
        epService.getEPAdministrator().createEPL("@Name('B') create window B#unique(id) as (id int)");
        epService.getEPAdministrator().createEPL("@Name('C') on A merge B when not matched then insert select 1 as id when matched then insert select 1 as id");

        epService.getEPAdministrator().createEPL("@Name('D') select * from B").addListener(nwListener);
        epService.getEPAdministrator().createEPL("@Name('E') insert into A select intPrimitive as id FROM SupportBean");

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(nwListener.isInvoked());
        epService.getEPAdministrator().destroyAllStatements();

        // test insert-stream only, no remove stream
        String[] fields = "c0,c1".split(",");
        epService.getEPAdministrator().createEPL("create window W1#lastevent as SupportBean");
        epService.getEPAdministrator().createEPL("insert into W1 select * from SupportBean");
        epService.getEPAdministrator().createEPL("create window W2#lastevent as SupportBean");
        epService.getEPAdministrator().createEPL("on W1 as a merge W2 as b when not matched then insert into OutStream " +
                "select a.theString as c0, istream() as c1");
        epService.getEPAdministrator().createEPL("select * from OutStream").addListener(mergeListener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[] {"E1", true});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(mergeListener.assertOneGetNewAndReset(), fields, new Object[] {"E2", true});
    }

    public void testDocExample() throws Exception {
        for (EventRepresentationEnum rep : EventRepresentationEnum.values()) {
            runAssertionDocExample(rep);
        }
    }

    public void runAssertionDocExample(EventRepresentationEnum eventRepresentationEnum) throws Exception {

        String baseModule = eventRepresentationEnum.getAnnotationText() + " create schema OrderEvent as (orderId string, productId string, price double, quantity int, deletedFlag boolean)";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(baseModule, null, null, null);

        String appModuleOne = eventRepresentationEnum.getAnnotationText() + " create schema ProductTotalRec as (productId string, totalPrice double);" +
                "" +
                eventRepresentationEnum.getAnnotationText() + " @Name('nwProd') create window ProductWindow#unique(productId) as ProductTotalRec;" +
                "" +
                "on OrderEvent oe\n" +
                "merge ProductWindow pw\n" +
                "where pw.productId = oe.productId\n" +
                "when matched\n" +
                "then update set totalPrice = totalPrice + oe.price\n" +
                "when not matched\n" +
                "then insert select productId, price as totalPrice;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(appModuleOne, null, null, null);

        String appModuleTwo = eventRepresentationEnum.getAnnotationText() + " @Name('nwOrd') create window OrderWindow#keepall as OrderEvent;" +
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

        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(appModuleTwo, null, null, null);

        sendOrderEvent(eventRepresentationEnum, "O1", "P1", 10, 100, false);
        sendOrderEvent(eventRepresentationEnum, "O1", "P1", 11, 200, false);
        sendOrderEvent(eventRepresentationEnum, "O2", "P2", 3, 300, false);
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("nwProd").iterator(), "productId,totalPrice".split(","), new Object[][]{{"P1", 21d}, {"P2", 3d}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(epService.getEPAdministrator().getStatement("nwOrd").iterator(), "orderId,quantity".split(","), new Object[][]{{"O1", 200}, {"O2", 300}});

        String module = "create schema StreetCarCountSchema (streetid string, carcount int);" +
                "    create schema StreetChangeEvent (streetid string, action string);" +
                "    create window StreetCarCountWindow#unique(streetid) as StreetCarCountSchema;" +
                "    on StreetChangeEvent ce merge StreetCarCountWindow w where ce.streetid = w.streetid\n" +
                "    when not matched and ce.action = 'ENTER' then insert select streetid, 1 as carcount\n" +
                "    when matched and ce.action = 'ENTER' then update set StreetCarCountWindow.carcount = carcount + 1\n" +
                "    when matched and ce.action = 'LEAVE' then update set StreetCarCountWindow.carcount = carcount - 1;" +
                "    select * from StreetCarCountWindow;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(module, null, null, null);

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "OrderEvent,OrderWindow,StreetCarCountSchema,StreetCarCountWindow,StreetChangeEvent,ProductWindow,ProductTotalRec".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private void sendOrderEvent(EventRepresentationEnum eventRepresentationEnum, String orderId, String productId, double price, int quantity, boolean deletedFlag) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {orderId, productId, price, quantity, deletedFlag}, "OrderEvent");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("orderId", orderId);
            theEvent.put("productId", productId);
            theEvent.put("price", price);
            theEvent.put("quantity", quantity);
            theEvent.put("deletedFlag", deletedFlag);
            epService.getEPRuntime().sendEvent(theEvent, "OrderEvent");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType("OrderEvent")).getSchemaAvro();
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("orderId", orderId);
            theEvent.put("productId", productId);
            theEvent.put("price", price);
            theEvent.put("quantity", quantity);
            theEvent.put("deletedFlag", deletedFlag);
            epService.getEPRuntime().sendEventAvro(theEvent, "OrderEvent");
        }
        else {
            fail();
        }
    }

    public void testTypeReference() {
        ConfigurationOperations configOps = epService.getEPAdministrator().getConfiguration();

        epService.getEPAdministrator().createEPL("@Name('ces') create schema EventSchema(in1 string, in2 int)");
        epService.getEPAdministrator().createEPL("@Name('cnws') create schema WindowSchema(in1 string, in2 int)");

        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"ces"}, configOps.getEventTypeNameUsedBy("EventSchema").toArray());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"cnws"}, configOps.getEventTypeNameUsedBy("WindowSchema").toArray());

        epService.getEPAdministrator().createEPL("@Name('cnw') create window MyWindow#keepall as WindowSchema");
        EPAssertionUtil.assertEqualsAnyOrder("cnws,cnw".split(","), configOps.getEventTypeNameUsedBy("WindowSchema").toArray());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"cnw"}, configOps.getEventTypeNameUsedBy("MyWindow").toArray());

        epService.getEPAdministrator().createEPL("@Name('om') on EventSchema merge into MyWindow " +
                "when not matched then insert select in1, in2");
        EPAssertionUtil.assertEqualsAnyOrder("ces,om".split(","), configOps.getEventTypeNameUsedBy("EventSchema").toArray());
        EPAssertionUtil.assertEqualsAnyOrder("cnws,cnw".split(","), configOps.getEventTypeNameUsedBy("WindowSchema").toArray());
    }

    public void testPropertyEval() {
        epService.getEPAdministrator().getConfiguration().addEventType("OrderBean", OrderBean.class);

        String[] fields = "c1,c2".split(",");
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as (c1 string, c2 string)");

        String epl =  "on OrderBean[books] " +
                      "merge MyWindow mw " +
                      "when not matched then " +
                      "insert select bookId as c1, title as c2 ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(mergeListener);

        epService.getEPRuntime().sendEvent(OrderBeanFactory.makeEventOne());
        EPAssertionUtil.assertPropsPerRow(mergeListener.getLastNewData(), fields, new Object[][]{{"10020", "Enders Game"},
                {"10021", "Foundation 1"}, {"10022", "Stranger in a Strange Land"}});
    }

    public void testPropertyInsertBean() {
        EPStatement stmtWindow = epService.getEPAdministrator().createEPL("create window MergeWindow#unique(theString) as SupportBean");

        String epl = "on SupportBean as up merge MergeWindow as mv where mv.theString=up.theString when not matched then insert select intPrimitive";
        EPStatement stmtMerge = epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));

        EventBean theEvent = stmtWindow.iterator().next();
        EPAssertionUtil.assertProps(theEvent, "theString,intPrimitive".split(","), new Object[]{null, 10});
        stmtMerge.destroy();

        epl = "on SupportBean as up merge MergeWindow as mv where mv.theString=up.theString when not matched then insert select theString, intPrimitive";
        epService.getEPAdministrator().createEPL(epl);
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        EPAssertionUtil.assertPropsPerRow(stmtWindow.iterator(), "theString,intPrimitive".split(","), new Object[][]{{null, 10}, {"E2", 20}});
    }

    public void testSubselect() {
        for (EventRepresentationEnum rep : EventRepresentationEnum.values()) {
            runAssertionSubselect(rep);
        }
    }

    private void runAssertionSubselect(EventRepresentationEnum eventRepresentationEnum) {
        String[] fields = "col1,col2".split(",");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MyEvent as (in1 string, in2 int)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema MySchema as (col1 string, col2 int)");
        EPStatement namedWindowStmt = epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create window MyWindow#lastevent as MySchema");
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyWindow");

        String epl =  "on MyEvent me " +
                      "merge MyWindow mw " +
                      "when not matched and (select intPrimitive>0 from SupportBean(theString like 'A%')#lastevent) then " +
                      "insert(col1, col2) select (select theString from SupportBean(theString like 'A%')#lastevent), (select intPrimitive from SupportBean(theString like 'A%')#lastevent) " +
                      "when matched and (select intPrimitive>0 from SupportBean(theString like 'B%')#lastevent) then " +
                      "update set col1=(select theString from SupportBean(theString like 'B%')#lastevent), col2=(select intPrimitive from SupportBean(theString like 'B%')#lastevent) " +
                      "when matched and (select intPrimitive>0 from SupportBean(theString like 'C%')#lastevent) then " +
                      "delete";
        epService.getEPAdministrator().createEPL(epl);

        // no action tests
        sendMyEvent(eventRepresentationEnum, "X1", 1);
        epService.getEPRuntime().sendEvent(new SupportBean("A1", 0));   // ignored
        sendMyEvent(eventRepresentationEnum, "X2", 2);
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, null);

        sendMyEvent(eventRepresentationEnum, "X3", 3);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A2", 20}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("Y1"));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 30));
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, null);

        sendMyEvent(eventRepresentationEnum, "X4", 4);
        epService.getEPRuntime().sendEvent(new SupportBean("A4", 40));
        sendMyEvent(eventRepresentationEnum, "X5", 5);   // ignored as matched (no where clause, no B event)
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A3", 30}});

        epService.getEPRuntime().sendEvent(new SupportBean("B1", 50));
        sendMyEvent(eventRepresentationEnum, "X6", 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"B1", 50}});

        epService.getEPRuntime().sendEvent(new SupportBean("B2", 60));
        sendMyEvent(eventRepresentationEnum, "X7", 7);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"B2", 60}});

        epService.getEPRuntime().sendEvent(new SupportBean("B2", 0));
        sendMyEvent(eventRepresentationEnum, "X8", 8);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"B2", 60}});

        epService.getEPRuntime().sendEvent(new SupportBean("C1", 1));
        sendMyEvent(eventRepresentationEnum, "X9", 9);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, null);

        epService.getEPRuntime().sendEvent(new SupportBean("C1", 0));
        sendMyEvent(eventRepresentationEnum, "X10", 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(namedWindowStmt.iterator(), fields, new Object[][]{{"A4", 40}});

        epService.getEPAdministrator().destroyAllStatements();
        for (String name : "MyEvent,MySchema,MyWindow".split(",")) {
            epService.getEPAdministrator().getConfiguration().removeEventType(name, true);
        }
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        return sb;
    }

    private void sendMyEvent(EventRepresentationEnum eventRepresentationEnum, String in1, int in2) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {in1, in2}, "MyEvent");
        }
        else if (eventRepresentationEnum.isMapEvent()) {
            Map<String, Object> theEvent = new LinkedHashMap<String, Object>();
            theEvent.put("in1", in1);
            theEvent.put("in2", in2);
            epService.getEPRuntime().sendEvent(theEvent, "MyEvent");
        }
        else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = ((AvroEventType) epService.getEPAdministrator().getConfiguration().getEventType("MyEvent")).getSchemaAvro();
            GenericData.Record theEvent = new GenericData.Record(schema);
            theEvent.put("in1", in1);
            theEvent.put("in2", in2);
            epService.getEPRuntime().sendEventAvro(theEvent, "MyEvent");
        }
        else {
            fail();
        }
    }

    public static void increaseIntCopyDouble(SupportBean initialBean, SupportBean updatedBean) {
        updatedBean.setIntPrimitive(initialBean.getIntPrimitive() + 1);
        updatedBean.setDoubleBoxed(updatedBean.getDoublePrimitive());
    }
}
