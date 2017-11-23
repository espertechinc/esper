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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.bean.bookexample.BookDesc;
import com.espertech.esper.supportregression.bean.lambda.LambdaAssertionUtil;
import com.espertech.esper.supportregression.bean.lrreport.LocationReportFactory;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.CollectionUtil;

import java.util.*;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecEnumDataSources implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("SupportBean", SupportBean.class);
        configuration.addEventType("SupportBean_A", SupportBean_A.class);
        configuration.addEventType("SupportBean_ST0", SupportBean_ST0.class);
        configuration.addEventType("SupportBean_ST0_Container", SupportBean_ST0_Container.class);
        configuration.addEventType("SupportCollection", SupportCollection.class);
        configuration.addEventType(MyEvent.class);
        configuration.addImport(LocationReportFactory.class);
        configuration.getEngineDefaults().getExpression().setUdfCache(false);
        configuration.addPlugInSingleRowFunction("makeSampleList", SupportBean_ST0_Container.class.getName(), "makeSampleList");
        configuration.addPlugInSingleRowFunction("makeSampleArray", SupportBean_ST0_Container.class.getName(), "makeSampleArray");
        configuration.addPlugInSingleRowFunction("makeSampleListString", SupportCollection.class.getName(), "makeSampleListString");
        configuration.addPlugInSingleRowFunction("makeSampleArrayString", SupportCollection.class.getName(), "makeSampleArrayString");
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionSubstitutionParameter(epService);
        runAssertionTableRow(epService);
        runAssertionPatternFilter(epService);
        runAssertionMatchRecognizeDefine(epService);
        runAssertionMatchRecognizeMeasures(epService, false);
        runAssertionMatchRecognizeMeasures(epService, true);
        runAssertionEnumObject(epService);
        runAssertionSortedMaxMinBy(epService);
        runAssertionJoin(epService);
        runAssertionPrevWindowSorted(epService);
        runAssertionNamedWindow(epService);
        runAssertionSubselect(epService);
        runAssertionVariable(epService);
        runAssertionAccessAggregation(epService);
        runAssertionProperty(epService);
        runAssertionPrevFuncs(epService);
        runAssertionUDFStaticMethod(epService);
        runAssertionPropertySchema(epService);
        runAssertionPropertyInsertIntoAtEventBean(epService);
    }

    private void runAssertionPropertySchema(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema OrderDetail(itemId string)");
        epService.getEPAdministrator().createEPL("create schema OrderEvent(details OrderDetail[])");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select details.where(i => i.itemId = '001') as c0 from OrderEvent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> detailOne = CollectionUtil.populateNameValueMap("itemId", "002");
        Map<String, Object> detailTwo = CollectionUtil.populateNameValueMap("itemId", "001");
        epService.getEPRuntime().sendEvent(CollectionUtil.populateNameValueMap("details", new Map[] {detailOne, detailTwo}), "OrderEvent");

        Collection c = (Collection) listener.assertOneGetNewAndReset().get("c0");
        EPAssertionUtil.assertEqualsExactOrder(c.toArray(), new Map[] {detailTwo});

        stmt.destroy();
    }

    private void runAssertionPropertyInsertIntoAtEventBean(EPServiceProvider epService) throws Exception {
        String epl = "create objectarray schema StockTick(id string, price int);\n" +
                "insert into TicksLarge select window(*).where(e => e.price > 100) @eventbean as ticksLargePrice\n" +
                "from StockTick#time(10) having count(*) > 2;\n" +
                "@name('out') select ticksLargePrice.where(e => e.price < 200) as ticksLargeLess200 from TicksLarge;\n";
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl).getDeploymentId();
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("out").addListener(listener);

        epService.getEPRuntime().sendEvent(new Object[] {"E1", 90}, "StockTick");
        epService.getEPRuntime().sendEvent(new Object[] {"E2", 120}, "StockTick");
        epService.getEPRuntime().sendEvent(new Object[] {"E3", 95}, "StockTick");

        assertEquals(1, ((Collection) listener.assertOneGetNewAndReset().get("ticksLargeLess200")).size());

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
    }

    private void runAssertionMatchRecognizeMeasures(EPServiceProvider epService, boolean select) {
        String epl;
        if (!select) {
            epl = "select ids from SupportBean match_recognize ( " +
                    "  measures A.selectFrom(o -> o.theString) as ids ";
        }
        else {
            epl = "select a.selectFrom(o -> o.theString) as ids from SupportBean match_recognize (measures A as a ";
        }
        epl = epl + " pattern (A{3}) define A as A.intPrimitive = 1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 1));
        assertColl("E1,E2,E3", listener.assertOneGetNewAndReset().get("ids"));

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 1));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 1));
        assertColl("E4,E5,E6", listener.assertOneGetNewAndReset().get("ids"));

        stmt.destroy();
    }

    private void runAssertionSubstitutionParameter(EPServiceProvider epService) {
        trySubstitutionParameter(epService, new Integer[]{1, 10, 100});
        trySubstitutionParameter(epService, new Object[]{1, 10, 100});
        trySubstitutionParameter(epService, new int[]{1, 10, 100});
    }

    private void runAssertionTableRow(EPServiceProvider epService) {
        // test table access expression
        epService.getEPAdministrator().createEPL("create table MyTableUnkeyed(theWindow window(*) @type(SupportBean))");
        epService.getEPAdministrator().createEPL("into table MyTableUnkeyed select window(*) as theWindow from SupportBean#time(30)");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));

        EPStatement stmt = epService.getEPAdministrator().createEPL("select MyTableUnkeyed.theWindow.anyOf(v=>intPrimitive=10) as c0 from SupportBean_A");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A0"));
        assertEquals(true, listener.assertOneGetNewAndReset().get("c0"));
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionPatternFilter(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from pattern [ ([2]a=SupportBean_ST0) -> b=SupportBean(intPrimitive > a.max(i -> p00))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2", 15));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 15));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 16));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a[0].id,a[1].id,b.theString".split(","), new Object[]{"E1", "E2", "E4"});
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL("select * from pattern [ a=SupportBean_ST0 until b=SupportBean -> c=SupportBean(intPrimitive > a.sumOf(i => p00))]");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E10", 10));
        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E11", 15));
        epService.getEPRuntime().sendEvent(new SupportBean("E12", -1));
        epService.getEPRuntime().sendEvent(new SupportBean("E13", 25));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E14", 26));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a[0].id,a[1].id,b.theString,c.theString".split(","), new Object[]{"E10", "E11", "E12", "E14"});

        stmt.destroy();
    }

    private void runAssertionMatchRecognizeDefine(EPServiceProvider epService) {

        // try define-clause
        String[] fieldsOne = "a_array[0].theString,a_array[1].theString,b.theString".split(",");
        String textOne = "select * from SupportBean " +
                "match_recognize (" +
                " measures A as a_array, B as b " +
                " pattern (A* B)" +
                " define" +
                " B as A.anyOf(v=> v.intPrimitive = B.intPrimitive)" +
                ")";

        EPStatement stmtOne = epService.getEPAdministrator().createEPL(textOne);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"A1", "A2", "A3"});

        epService.getEPRuntime().sendEvent(new SupportBean("A4", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A5", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("A6", 3));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("A7", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsOne, new Object[]{"A4", "A5", "A7"});
        stmtOne.destroy();

        // try measures-clause
        String[] fieldsTwo = "c0".split(",");
        String textTwo = "select * from SupportBean " +
                "match_recognize (" +
                " measures A.anyOf(v=> v.intPrimitive = B.intPrimitive) as c0 " +
                " pattern (A* B)" +
                " define" +
                " A as A.theString like 'A%'," +
                " B as B.theString like 'B%'" +
                ")";

        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(textTwo);
        stmtTwo.addListener(listener);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 20));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{true});

        epService.getEPRuntime().sendEvent(new SupportBean("A1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("B1", 15));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsTwo, new Object[]{false});

        stmtTwo.destroy();
    }

    private void runAssertionEnumObject(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addImport(SupportEnumTwo.class);
        epService.getEPAdministrator().getConfiguration().addEventType(SupportEnumTwoEvent.class);

        String[] fields = "c0,c1".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "SupportEnumTwo.ENUM_VALUE_1.getMystrings().anyOf(v => v = id) as c0, " +
                "value.getMystrings().anyOf(v => v = '2') as c1 " +
                "from SupportEnumTwoEvent");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportEnumTwoEvent("0", SupportEnumTwo.ENUM_VALUE_1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportEnumTwoEvent("2", SupportEnumTwo.ENUM_VALUE_2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        stmt.destroy();
    }

    private void runAssertionSortedMaxMinBy(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3,c4".split(",");

        String eplWindowAgg = "select " +
                "sorted(theString).allOf(x => x.intPrimitive < 5) as c0," +
                "maxby(theString).allOf(x => x.intPrimitive < 5) as c1," +
                "minby(theString).allOf(x => x.intPrimitive < 5) as c2," +
                "maxbyever(theString).allOf(x => x.intPrimitive < 5) as c3," +
                "minbyever(theString).allOf(x => x.intPrimitive < 5) as c4" +
                " from SupportBean#length(5)";
        EPStatement stmtWindowAgg = epService.getEPAdministrator().createEPL(eplWindowAgg);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtWindowAgg.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, true});

        stmtWindowAgg.destroy();
    }

    private void runAssertionJoin(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SelectorEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(ContainerEvent.class);

        EPStatement stmt = epService.getEPAdministrator().createEPL("" +
                "select * from SelectorEvent#keepall as sel, ContainerEvent#keepall as cont " +
                "where cont.items.anyOf(i => sel.selector = i.selected)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SelectorEvent("S1", "sel1"));
        epService.getEPRuntime().sendEvent(new ContainerEvent("C1", new ContainedItem("I1", "sel1")));
        assertTrue(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionPrevWindowSorted(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select prevwindow(st0) as val0, prevwindow(st0).esperInternalNoop() as val1 " +
                "from SupportBean_ST0#sort(3, p00 asc) as st0");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmt.getEventType(), "val0,val1".split(","), new Class[]{SupportBean_ST0[].class, Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 5));
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2", 6));
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E1,E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E3", 4));
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E3,E1,E2");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E5", 3));
        LambdaAssertionUtil.assertST0Id(listener, "val1", "E5,E3,E1");
        listener.reset();
        stmt.destroy();

        // Scalar version
        String[] fields = new String[]{"val0"};
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL("select prevwindow(id).where(x => x not like '%ignore%') as val0 " +
                "from SupportBean_ST0#keepall as st0");
        stmtScalar.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtScalar.getEventType(), fields, new Class[]{Collection.class});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E1", 5));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2ignore", 6));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E3", 4));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E3", "E1");
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ignoreE5", 3));
        LambdaAssertionUtil.assertValuesArrayScalar(listener, "val0", "E3", "E1");
        listener.reset();

        stmtScalar.destroy();
    }

    private void runAssertionNamedWindow(EPServiceProvider epService) {

        // test named window
        epService.getEPAdministrator().createEPL("create window MyWindow#keepall as SupportBean_ST0");
        epService.getEPAdministrator().createEPL("on SupportBean_A delete from MyWindow");
        epService.getEPAdministrator().createEPL("insert into MyWindow select * from SupportBean_ST0");
        String eplNamedWindow = "select MyWindow.allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall";
        EPStatement stmtNamedWindow = epService.getEPAdministrator().createEPL(eplNamedWindow);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtNamedWindow.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtNamedWindow.getEventType(), "allOfX".split(","), new Class[]{Boolean.class});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        assertEquals(false, listener.assertOneGetNewAndReset().get("allOfX"));

        stmtNamedWindow.destroy();
        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));

        // test named window correlated
        String eplNamedWindowCorrelated = "select MyWindow(key0 = sb.theString).allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall sb";
        EPStatement stmtNamedWindowCorrelated = epService.getEPAdministrator().createEPL(eplNamedWindowCorrelated);
        stmtNamedWindowCorrelated.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("E2", "KEY1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertEquals(null, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean("KEY1", 0));
        assertEquals(true, listener.assertOneGetNewAndReset().get("allOfX"));
        stmtNamedWindowCorrelated.destroy();
    }

    private void runAssertionSubselect(EPServiceProvider epService) {

        // test subselect-wildcard
        String eplSubselect = "select (select * from SupportBean_ST0#keepall).allOf(x => x.p00 < 5) as allOfX from SupportBean#keepall";
        EPStatement stmtSubselect = epService.getEPAdministrator().createEPL(eplSubselect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSubselect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(true, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("ST0", "1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("allOfX"));
        stmtSubselect.destroy();

        // test subselect scalar return
        String eplSubselectScalar = "select (select id from SupportBean_ST0#keepall).allOf(x => x  like '%B%') as allOfX from SupportBean#keepall";
        EPStatement stmtSubselectScalar = epService.getEPAdministrator().createEPL(eplSubselectScalar);
        stmtSubselectScalar.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("B1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(true, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("A1", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertEquals(false, listener.assertOneGetNewAndReset().get("allOfX"));
        stmtSubselectScalar.destroy();

        // test subselect-correlated scalar return
        String eplSubselectScalarCorrelated = "select (select key0 from SupportBean_ST0#keepall st0 where st0.id = sb.theString).allOf(x => x  like '%hello%') as allOfX from SupportBean#keepall sb";
        EPStatement stmtSubselectScalarCorrelated = epService.getEPAdministrator().createEPL(eplSubselectScalarCorrelated);
        stmtSubselectScalarCorrelated.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("A1", "hello", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertEquals(null, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("A2", "hello", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A2", 1));
        assertEquals(true, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("A3", "test", 0));
        epService.getEPRuntime().sendEvent(new SupportBean("A3", 1));
        assertEquals(false, listener.assertOneGetNewAndReset().get("allOfX"));
        stmtSubselectScalarCorrelated.destroy();

        // test subselect multivalue return
        String[] fields = new String[]{"id", "p00"};
        String eplSubselectMultivalue = "select (select id, p00 from SupportBean_ST0#keepall).take(10) as c0 from SupportBean";
        EPStatement stmtSubselectMultivalue = epService.getEPAdministrator().createEPL(eplSubselectMultivalue);
        stmtSubselectMultivalue.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("B1", 10));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));
        assertPropsMapRows((Collection) listener.assertOneGetNewAndReset().get("c0"), fields, new Object[][]{{"B1", 10}});

        epService.getEPRuntime().sendEvent(new SupportBean_ST0("B2", 20));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 0));
        assertPropsMapRows((Collection) listener.assertOneGetNewAndReset().get("c0"), fields, new Object[][]{{"B1", 10}, {"B2", 20}});
        stmtSubselectMultivalue.destroy();

        // test subselect that delivers events
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("create schema AEvent (symbol string)");
        epService.getEPAdministrator().createEPL("create schema BEvent (a AEvent)");
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select (select a from BEvent#keepall).anyOf(v => symbol = 'GE') as flag from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(makeBEvent("XX"), "BEvent");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(false, listener.assertOneGetNewAndReset().get("flag"));

        epService.getEPRuntime().sendEvent(makeBEvent("GE"), "BEvent");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(true, listener.assertOneGetNewAndReset().get("flag"));

        stmt.destroy();
    }

    private void runAssertionVariable(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create variable string[] myvar = { 'E1', 'E3' }");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("select * from SupportBean(myvar.anyOf(v => v = theString))").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listener.getAndClearIsInvoked());
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 1));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionAccessAggregation(EPServiceProvider epService) {
        String[] fields = new String[]{"val0", "val1", "val2", "val3", "val4"};

        // test window(*) and first(*)
        String eplWindowAgg = "select " +
                "window(*).allOf(x => x.intPrimitive < 5) as val0," +
                "first(*).allOf(x => x.intPrimitive < 5) as val1," +
                "first(*, 1).allOf(x => x.intPrimitive < 5) as val2," +
                "last(*).allOf(x => x.intPrimitive < 5) as val3," +
                "last(*, 1).allOf(x => x.intPrimitive < 5) as val4" +
                " from SupportBean#length(2)";
        EPStatement stmtWindowAgg = epService.getEPAdministrator().createEPL(eplWindowAgg);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtWindowAgg.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, null, true, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, false, false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true, false});

        stmtWindowAgg.destroy();

        // test scalar: window(*) and first(*)
        String eplWindowAggScalar = "select " +
                "window(intPrimitive).allOf(x => x < 5) as val0," +
                "first(intPrimitive).allOf(x => x < 5) as val1," +
                "first(intPrimitive, 1).allOf(x => x < 5) as val2," +
                "last(intPrimitive).allOf(x => x < 5) as val3," +
                "last(intPrimitive, 1).allOf(x => x < 5) as val4" +
                " from SupportBean#length(2)";
        EPStatement stmtWindowAggScalar = epService.getEPAdministrator().createEPL(eplWindowAggScalar);
        stmtWindowAggScalar.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, null, true, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, false, false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, true, true, false});

        stmtWindowAggScalar.destroy();
    }

    private void runAssertionProperty(EPServiceProvider epService) {

        // test fragment type - collection inside
        String eplFragment = "select contained.allOf(x => x.p00 < 5) as allOfX from SupportBean_ST0_Container#keepall";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("ID1,KEY1,1"));
        assertEquals(true, listener.assertOneGetNewAndReset().get("allOfX"));

        epService.getEPRuntime().sendEvent(SupportBean_ST0_Container.make3Value("ID1,KEY1,10"));
        assertEquals(false, listener.assertOneGetNewAndReset().get("allOfX"));
        stmtFragment.destroy();

        // test array and iterable
        String[] fields = "val0,val1".split(",");
        eplFragment = "select intarray.sumof() as val0, " +
                "intiterable.sumOf() as val1 " +
                " from SupportCollection#keepall";
        stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportCollection.makeNumeric("5,6,7"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{5 + 6 + 7, 5 + 6 + 7});

        // test map event type with object-array prop
        epService.getEPAdministrator().getConfiguration().addEventType(BookDesc.class);
        epService.getEPAdministrator().createEPL("create schema MySchema (books BookDesc[])");

        EPStatement stmt = epService.getEPAdministrator().createEPL("select books.max(i => i.price) as mymax from MySchema");
        stmt.addListener(listener);

        Map<String, Object> event = Collections.singletonMap("books", new BookDesc[]{new BookDesc("1", "book1", "dave", 1.00, null)});
        epService.getEPRuntime().sendEvent(event, "MySchema");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "mymax".split(","), new Object[]{1.0});

        // test method invocation variations returning list/array of string and test UDF +property as well
        runAssertionMethodInvoke(epService, "select e.getTheList().anyOf(v => v = selector) as flag from MyEvent e");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("convertToArray", MyEvent.class.getName(), "convertToArray");
        runAssertionMethodInvoke(epService, "select convertToArray(theList).anyOf(v => v = selector) as flag from MyEvent e");
        runAssertionMethodInvoke(epService, "select theArray.anyOf(v => v = selector) as flag from MyEvent e");
        runAssertionMethodInvoke(epService, "select e.getTheArray().anyOf(v => v = selector) as flag from MyEvent e");
        runAssertionMethodInvoke(epService, "select e.theList.anyOf(v => v = e.selector) as flag from pattern[every e=MyEvent]");
        runAssertionMethodInvoke(epService, "select e.nestedMyEvent.myNestedList.anyOf(v => v = e.selector) as flag from pattern[every e=MyEvent]");
        runAssertionMethodInvoke(epService, "select " + MyEvent.class.getName() + ".convertToArray(theList).anyOf(v => v = selector) as flag from MyEvent e");

        stmt.destroy();
    }

    private void runAssertionMethodInvoke(EPServiceProvider epService, String epl) {
        String[] fields = "flag".split(",");
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmtMethodAnyOf = epService.getEPAdministrator().createEPL(epl);
        stmtMethodAnyOf.addListener(listener);

        epService.getEPRuntime().sendEvent(new MyEvent("1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(new MyEvent("4"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        stmtMethodAnyOf.destroy();
    }

    private void runAssertionPrevFuncs(EPServiceProvider epService) {
        // test prevwindow(*) etc
        String[] fields = new String[]{"val0", "val1", "val2"};
        String epl = "select " +
                "prevwindow(sb).allOf(x => x.intPrimitive < 5) as val0," +
                "prev(sb,1).allOf(x => x.intPrimitive < 5) as val1," +
                "prevtail(sb,1).allOf(x => x.intPrimitive < 5) as val2" +
                " from SupportBean#length(2) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, true});
        stmt.destroy();

        // test scalar prevwindow(property) etc
        String eplScalar = "select " +
                "prevwindow(intPrimitive).allOf(x => x < 5) as val0," +
                "prev(intPrimitive,1).allOf(x => x < 5) as val1," +
                "prevtail(intPrimitive,1).allOf(x => x < 5) as val2" +
                " from SupportBean#length(2) as sb";
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL(eplScalar);
        stmtScalar.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, true});

        stmtScalar.destroy();
    }

    private void runAssertionUDFStaticMethod(EPServiceProvider epService) {

        String[] fields = "val1,val2,val3,val4".split(",");
        epService.getEPAdministrator().getConfiguration().addImport(SupportBean_ST0_Container.class);
        String epl = "select " +
                "SupportBean_ST0_Container.makeSampleList().where(x => x.p00 < 5) as val1, " +
                "SupportBean_ST0_Container.makeSampleArray().where(x => x.p00 < 5) as val2, " +
                "makeSampleList().where(x => x.p00 < 5) as val3, " +
                "makeSampleArray().where(x => x.p00 < 5) as val4 " +
                "from SupportBean#length(2) as sb";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_ST0_Container.setSamples(new String[]{"E1,1", "E2,20", "E3,3"});
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            SupportBean_ST0[] result = toArray((Collection) listener.assertOneGetNew().get(field));
            assertEquals("Failed for field " + field, 2, result.length);
        }
        listener.reset();

        SupportBean_ST0_Container.setSamples(null);
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            assertNull(listener.assertOneGetNew().get(field));
        }
        listener.reset();

        SupportBean_ST0_Container.setSamples(new String[0]);
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            SupportBean_ST0[] result = toArray((Collection) listener.assertOneGetNew().get(field));
            assertEquals(0, result.length);
        }
        listener.reset();
        stmt.destroy();

        // test UDF returning scalar values collection
        fields = "val0,val1,val2,val3".split(",");
        epService.getEPAdministrator().getConfiguration().addImport(SupportCollection.class);
        String eplScalar = "select " +
                "SupportCollection.makeSampleListString().where(x => x != 'E1') as val0, " +
                "SupportCollection.makeSampleArrayString().where(x => x != 'E1') as val1, " +
                "makeSampleListString().where(x => x != 'E1') as val2, " +
                "makeSampleArrayString().where(x => x != 'E1') as val3 " +
                "from SupportBean#length(2) as sb";
        EPStatement stmtScalar = epService.getEPAdministrator().createEPL(eplScalar);
        stmtScalar.addListener(listener);
        LambdaAssertionUtil.assertTypes(stmtScalar.getEventType(), fields, new Class[]{Collection.class, Collection.class, Collection.class, Collection.class});

        SupportCollection.setSampleCSV("E1,E2,E3");
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field, "E2", "E3");
        }
        listener.reset();

        SupportCollection.setSampleCSV(null);
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field, null);
        }
        listener.reset();

        SupportCollection.setSampleCSV("");
        epService.getEPRuntime().sendEvent(new SupportBean());
        for (String field : fields) {
            LambdaAssertionUtil.assertValuesArrayScalar(listener, field);
        }
        listener.reset();

        stmtScalar.destroy();
    }

    private void trySubstitutionParameter(EPServiceProvider epService, Object parameter) {
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL("select * from SupportBean(?.sequenceEqual({1, intPrimitive, 100}))");
        prepared.setObject(1, parameter);
        epService.getEPAdministrator().create(prepared).addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 20));
        assertFalse(listener.getAndClearIsInvoked());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean_ST0[] toArray(Collection<SupportBean_ST0> it) {
        if (!it.isEmpty() && it.iterator().next() instanceof EventBean) {
            fail("Iterator provides EventBean instances");
        }
        return it.toArray(new SupportBean_ST0[it.size()]);
    }

    private Map<String, Object> makeBEvent(String symbol) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("a", Collections.singletonMap("symbol", symbol));
        return map;
    }

    private void assertPropsMapRows(Collection rows, String[] fields, Object[][] objects) {
        Collection<Map> mapsColl = (Collection<Map>) rows;
        Map[] maps = mapsColl.toArray(new Map[mapsColl.size()]);
        EPAssertionUtil.assertPropsPerRow(maps, fields, objects);
    }

    private void assertColl(String expected, Object value) {
        EPAssertionUtil.assertEqualsExactOrder(expected.split(","), ((Collection)value).toArray());
    }

    public static class SelectorEvent {
        private final String selectorId;
        private final String selector;

        public SelectorEvent(String selectorId, String selector) {
            this.selectorId = selectorId;
            this.selector = selector;
        }

        public String getSelectorId() {
            return selectorId;
        }

        public String getSelector() {
            return selector;
        }
    }

    public static class ContainerEvent {
        private final String containerId;
        private final ContainedItem[] items;

        public ContainerEvent(String containerId, ContainedItem... items) {
            this.containerId = containerId;
            this.items = items;
        }

        public String getContainerId() {
            return containerId;
        }

        public ContainedItem[] getItems() {
            return items;
        }
    }

    public static class ContainedItem {
        private final String itemId;
        private final String selected;

        public ContainedItem(String itemId, String selected) {
            this.itemId = itemId;
            this.selected = selected;
        }

        public String getItemId() {
            return itemId;
        }

        public String getSelected() {
            return selected;
        }
    }

    public static class MyEvent {
        private final String selector;
        private final List<String> myList;

        public MyEvent(String selector) {
            this.selector = selector;

            myList = new ArrayList<String>();
            myList.add("1");
            myList.add("2");
            myList.add("3");
        }

        public String getSelector() {
            return selector;
        }

        public List<String> getTheList() {
            return myList;
        }

        public String[] getTheArray() {
            return myList.toArray(new String[myList.size()]);
        }

        public NestedMyEvent getNestedMyEvent() {
            return new NestedMyEvent(myList);
        }

        public static String[] convertToArray(List<String> list) {
            return list.toArray(new String[list.size()]);
        }
    }

    public static class NestedMyEvent {
        private final List<String> myNestedList;

        public NestedMyEvent(List<String> myList) {
            this.myNestedList = myList;
        }

        public List<String> getMyNestedList() {
            return myNestedList;
        }
    }

    public static class SupportEnumTwoEvent {
        private final String id;
        private final SupportEnumTwo value;

        public SupportEnumTwoEvent(String id, SupportEnumTwo value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public SupportEnumTwo getValue() {
            return value;
        }
    }
}
