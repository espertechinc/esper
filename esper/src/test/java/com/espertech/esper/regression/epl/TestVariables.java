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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPRuntimeSPI;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.event.EventTypeAssertionEnum;
import com.espertech.esper.supportregression.event.EventTypeAssertionUtil;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestVariables extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;
    private SupportUpdateListener listenerSet;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setIterableUnbound(true);
        config.addVariable("MYCONST_THREE", "boolean", true, true);
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
        listenerSet = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        listenerSet = null;
    }

    public void testDotVariableSeparateThread() throws Exception {

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addVariable("mySimpleVariableService", MySimpleVariableService.class, null);
        epService.getEPRuntime().setVariableValue("mySimpleVariableService", new MySimpleVariableService());

        EPStatement epStatement = epService.getEPAdministrator().createEPL("select mySimpleVariableService.doSomething() as c0 from SupportBean");

        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> values = new ArrayList<String>();
        epStatement.setSubscriber(new Object() {
            public void update(final Map<?, ?> event) {
                String value = (String) event.get("c0");
                values.add(value);
                latch.countDown();
            }
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBean());
            }
        });
        latch.await();
        executorService.shutdownNow();

        assertEquals(1, values.size());
        assertEquals("hello", values.get(0));
    }

    public void testInvokeMethod() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addImport(MySimpleVariableServiceFactory.class);
        epService.getEPAdministrator().getConfiguration().addImport(MySimpleVariableService.class);

        // declared via EPL
        epService.getEPAdministrator().createEPL("create constant variable MySimpleVariableService myService = MySimpleVariableServiceFactory.makeService()");

        // added via runtime config
        epService.getEPAdministrator().getConfiguration().addVariable("myRuntimeInitService", MySimpleVariableService.class, MySimpleVariableServiceFactory.makeService());

        // exercise
        epService.getEPAdministrator().createEPL("select " +
                "myService.doSomething() as c0, " +
                "myRuntimeInitService.doSomething() as c1 " +
                "from SupportBean").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1".split(","), new Object[] {"hello", "hello"});
    }

    public void testConstantVariable() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().createEPL("create const variable int MYCONST = 10");

        tryOperator("MYCONST = intBoxed", new Object[][] {{10, true}, {9, false}, {null, false}});

        tryOperator("MYCONST > intBoxed", new Object[][] {{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator("MYCONST >= intBoxed", new Object[][] {{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator("MYCONST < intBoxed", new Object[][] {{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator("MYCONST <= intBoxed", new Object[][] {{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator("intBoxed < MYCONST", new Object[][] {{11, false}, {10, false}, {9, true}, {8, true}});
        tryOperator("intBoxed <= MYCONST", new Object[][] {{11, false}, {10, true}, {9, true}, {8, true}});
        tryOperator("intBoxed > MYCONST", new Object[][] {{11, true}, {10, false}, {9, false}, {8, false}});
        tryOperator("intBoxed >= MYCONST", new Object[][] {{11, true}, {10, true}, {9, false}, {8, false}});

        tryOperator("intBoxed in (MYCONST)", new Object[][] {{11, false}, {10, true}, {9, false}, {8, false}});
        tryOperator("intBoxed between MYCONST and MYCONST", new Object[][] {{11, false}, {10, true}, {9, false}, {8, false}});

        tryOperator("MYCONST != intBoxed", new Object[][] {{10, false}, {9, true}, {null, false}});
        tryOperator("intBoxed != MYCONST", new Object[][] {{10, false}, {9, true}, {null, false}});

        tryOperator("intBoxed not in (MYCONST)", new Object[][] {{11, true}, {10, false}, {9, true}, {8, true}});
        tryOperator("intBoxed not between MYCONST and MYCONST", new Object[][] {{11, true}, {10, false}, {9, true}, {8, true}});

        tryOperator("MYCONST is intBoxed", new Object[][] {{10, true}, {9, false}, {null, false}});
        tryOperator("intBoxed is MYCONST", new Object[][] {{10, true}, {9, false}, {null, false}});

        tryOperator("MYCONST is not intBoxed", new Object[][] {{10, false}, {9, true}, {null, true}});
        tryOperator("intBoxed is not MYCONST", new Object[][] {{10, false}, {9, true}, {null, true}});

        // try coercion
        tryOperator("MYCONST = shortBoxed", new Object[][] {{(short)10, true}, {(short)9, false}, {null, false}});
        tryOperator("shortBoxed = MYCONST", new Object[][] {{(short)10, true}, {(short)9, false}, {null, false}});

        tryOperator("MYCONST > shortBoxed", new Object[][] {{(short)11, false}, {(short)10, false}, {(short)9, true}, {(short)8, true}});
        tryOperator("shortBoxed < MYCONST", new Object[][] {{(short)11, false}, {(short)10, false}, {(short)9, true}, {(short)8, true}});

        tryOperator("shortBoxed in (MYCONST)", new Object[][] {{(short)11, false}, {(short)10, true}, {(short)9, false}, {(short)8, false}});

        // test SODA
        String epl = "create constant variable int MYCONST = 10";
        epService.getEPAdministrator().destroyAllStatements();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmt = epService.getEPAdministrator().create(model);
        assertEquals(epl, stmt.getText());

        // test invalid
        tryInvalidSet("on SupportBean set MYCONST = 10",
                "Error starting statement: Variable by name 'MYCONST' is declared constant and may not be set [on SupportBean set MYCONST = 10]");
        tryInvalidSet("select * from SupportBean output when true then set MYCONST=1",
                "Error starting statement: Error in the output rate limiting clause: Variable by name 'MYCONST' is declared constant and may not be set [select * from SupportBean output when true then set MYCONST=1]");

        // assure no update via API
        tryInvalidSetConstant("MYCONST", 1);

        // add constant variable via runtime API
        epService.getEPAdministrator().getConfiguration().addVariable("MYCONST_TWO", "string", null, true);
        tryInvalidSetConstant("MYCONST_TWO", "dummy");
        tryInvalidSetConstant("MYCONST_THREE", false);

        // try ESPER-653
        EPStatement stmtDate = epService.getEPAdministrator().createEPL("create constant variable java.util.Date START_TIME = java.util.Calendar.getInstance().getTime()");
        Object value = stmtDate.iterator().next().get("START_TIME");
        assertNotNull(value);

        // test array constant
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("create constant variable string[] var_strings = {'E1', 'E2'}");
        EPStatement stmtArrayVar = epService.getEPAdministrator().createEPL("select var_strings from SupportBean");
        assertEquals(String[].class, stmtArrayVar.getEventType().getPropertyType("var_strings"));
        runAssertionArrayVar("var_strings");
        epService.getEPAdministrator().getConfiguration().addVariable("varcoll", "String[]", new String[] {"E1", "E2"}, true);

        tryOperator("intBoxed in (10, 8)", new Object[][] {{11, false}, {10, true}, {9, false}, {8, true}});

        epService.getEPAdministrator().createEPL("create constant variable int [ ] var_ints = {8, 10}");
        tryOperator("intBoxed in (var_ints)", new Object[][] {{11, false}, {10, true}, {9, false}, {8, true}});

        epService.getEPAdministrator().createEPL("create constant variable int[]  var_intstwo = {9}");
        tryOperator("intBoxed in (var_ints, var_intstwo)", new Object[][] {{11, false}, {10, true}, {9, true}, {8, true}});

        tryInvalid("create constant variable SupportBean[] var_beans",
                "Error starting statement: Cannot create variable: Cannot create variable 'var_beans', type 'SupportBean' cannot be declared as an array type [create constant variable SupportBean[] var_beans]");

        // test array of primitives
        EPStatement stmtArrayOne = epService.getEPAdministrator().createEPL("create variable byte[] myBytesBoxed");
        Object[][] expectedType = new Object[][]{{"myBytesBoxed", Byte[].class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtArrayOne.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);
        EPStatement stmtArrayTwo = epService.getEPAdministrator().createEPL("create variable byte[primitive] myBytesPrimitive");
        expectedType = new Object[][]{{"myBytesPrimitive", byte[].class}};
        EventTypeAssertionUtil.assertEventTypeProperties(expectedType, stmtArrayTwo.getEventType(), EventTypeAssertionEnum.NAME, EventTypeAssertionEnum.TYPE);

        // test enum constant
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().addImport(SupportEnum.class);
        epService.getEPAdministrator().createEPL("create constant variable SupportEnum var_enumone = SupportEnum.ENUM_VALUE_2");
        tryOperator("var_enumone = enumValue", new Object[][] {{SupportEnum.ENUM_VALUE_3, false}, {SupportEnum.ENUM_VALUE_2, true}, {SupportEnum.ENUM_VALUE_1, false}});

        epService.getEPAdministrator().createEPL("create constant variable SupportEnum[] var_enumarr = {SupportEnum.ENUM_VALUE_2, SupportEnum.ENUM_VALUE_1}");
        tryOperator("enumValue in (var_enumarr, var_enumone)", new Object[][] {{SupportEnum.ENUM_VALUE_3, false}, {SupportEnum.ENUM_VALUE_2, true}, {SupportEnum.ENUM_VALUE_1, true}});

        epService.getEPAdministrator().createEPL("create variable SupportEnum var_enumtwo = SupportEnum.ENUM_VALUE_2");
        epService.getEPAdministrator().createEPL("on SupportBean set var_enumtwo = enumValue");
    }

    private void runAssertionArrayVar(String varName) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportBean(theString in (" + varName + "))");
        stmt.addListener(listener);
        sendBeanAssert("E1", true);
        sendBeanAssert("E2", true);
        sendBeanAssert("E3", false);
    }

    private void sendBeanAssert(String theString, boolean expected) {
        epService.getEPRuntime().sendEvent(new SupportBean(theString, 1));
        assertEquals(expected, listener.getAndClearIsInvoked());
    }

    public void testVariableEPRuntime() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", int.class, -1);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", String.class, "abc");
        EPRuntimeSPI runtimeSPI = (EPRuntimeSPI) epService.getEPRuntime();
        Map<String, Class> types = runtimeSPI.getVariableTypeAll();
        assertEquals(3, types.size());
        assertEquals(Integer.class, types.get("var1"));
        assertEquals(String.class, types.get("var2"));
        assertEquals(Integer.class, runtimeSPI.getVariableType("var1"));
        assertEquals(String.class, runtimeSPI.getVariableType("var2"));

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1 = intPrimitive, var2 = theString";
        epService.getEPAdministrator().createEPL(stmtTextSet);

        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {-1, "abc"});
        sendSupportBean(null, 99);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {99, null});

        epService.getEPRuntime().setVariableValue("var2", "def");
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {99, "def"});

        epService.getEPRuntime().setVariableValue("var1", 123);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {123, "def"});

        Map<String, Object> newValues = new HashMap<String, Object>();
        newValues.put("var1", 20);
        epService.getEPRuntime().setVariableValue(newValues);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {20, "def"});

        newValues.put("var1", (byte) 21);
        newValues.put("var2", "test");
        epService.getEPRuntime().setVariableValue(newValues);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {21, "test"});

        newValues.put("var1", null);
        newValues.put("var2", null);
        epService.getEPRuntime().setVariableValue(newValues);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {null, null});

        // try variable not found
        try
        {
            epService.getEPRuntime().setVariableValue("dummy", null);
            fail();
        }
        catch (VariableNotFoundException ex)
        {
            // expected
            assertEquals("Variable by name 'dummy' has not been declared", ex.getMessage());
        }

        // try variable not found
        try
        {
            newValues.put("dummy2", 20);
            epService.getEPRuntime().setVariableValue(newValues);
            fail();
        }
        catch (VariableNotFoundException ex)
        {
            // expected
            assertEquals("Variable by name 'dummy2' has not been declared", ex.getMessage());
        }

        // create new variable on the fly
        epService.getEPAdministrator().createEPL("create variable int dummy = 20 + 20");
        assertEquals(40, epService.getEPRuntime().getVariableValue("dummy"));

        // try type coercion
        try
        {
            epService.getEPRuntime().setVariableValue("dummy", "abc");
            fail();
        }
        catch (VariableValueException ex)
        {
            // expected
            assertEquals("Variable 'dummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.String", ex.getMessage());
        }
        try
        {
            epService.getEPRuntime().setVariableValue("dummy", 100L);
            fail();
        }
        catch (VariableValueException ex)
        {
            // expected
            assertEquals("Variable 'dummy' of declared type java.lang.Integer cannot be assigned a value of type java.lang.Long", ex.getMessage());
        }
        try
        {
            epService.getEPRuntime().setVariableValue("var2", 0);
            fail();
        }
        catch (VariableValueException ex)
        {
            // expected
            assertEquals("Variable 'var2' of declared type java.lang.String cannot be assigned a value of type java.lang.Integer", ex.getMessage());
        }

        // coercion
        epService.getEPRuntime().setVariableValue("var1", (short) -1);
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {-1, null});

        // rollback for coercion failed
        newValues = new LinkedHashMap<String, Object>();    // preserve order
        newValues.put("var2", "xyz");
        newValues.put("var1", 4.4d);
        try
        {
            epService.getEPRuntime().setVariableValue(newValues);
            fail();
        }
        catch (VariableValueException ex)
        {
            // expected
        }
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {-1, null});

        // rollback for variable not found
        newValues = new LinkedHashMap<String, Object>();    // preserve order
        newValues.put("var2", "xyz");
        newValues.put("var1", 1);
        newValues.put("notfoundvariable", null);
        try
        {
            epService.getEPRuntime().setVariableValue(newValues);
            fail();
        }
        catch (VariableNotFoundException ex)
        {
            // expected
        }
        assertVariableValues(new String[] {"var1", "var2"}, new Object[] {-1, null});
    }

    public void testSetSubquery() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addEventType("S1", SupportBean_S1.class);
        epService.getEPAdministrator().getConfiguration().addVariable("var1", String.class, "a");
        epService.getEPAdministrator().getConfiguration().addVariable("var2", String.class, "b");

        String stmtTextSet = "on " + SupportBean_S0.class.getName() + " as s0str set var1 = (select p10 from S1#lastevent), var2 = (select p11||s0str.p01 from S1#lastevent)";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"a", "b"}});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1));
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, null}});

        epService.getEPRuntime().sendEvent(new SupportBean_S1(0, "x", "y"));
        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "1", "2"));
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"x", "y2"}});
    }

    public void testVariableInFilterBoolean() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", String.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", String.class, null);

        String stmtTextSet = "on " + SupportBean_S0.class.getName() + " set var1 = p00, var2 = p01";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, null}});

        String stmtTextSelect = "select theString, intPrimitive from " + SupportBean.class.getName() + "(theString = var1 or theString = var2)";
        String[] fieldsSelect = new String[] {"theString", "intPrimitive"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listener);

        sendSupportBean(null, 1);
        assertFalse(listener.isInvoked());

        sendSupportBeanS0NewThread(100, "a", "b");
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"a", "b"});

        sendSupportBean("a", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"a", 2});

        sendSupportBean(null, 1);
        assertFalse(listener.isInvoked());

        sendSupportBean("b", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"b", 3});

        sendSupportBean("c", 4);
        assertFalse(listener.isInvoked());

        sendSupportBeanS0NewThread(100, "e", "c");
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"e", "c"});

        sendSupportBean("c", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"c", 5});

        sendSupportBean("e", 6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"e", 6});

        stmtSet.destroy();
    }

    public void testVariableInFilter() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", String.class, null);

        String stmtTextSet = "on " + SupportBean_S0.class.getName() + " set var1 = p00";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null}});

        String stmtTextSelect = "select theString, intPrimitive from " + SupportBean.class.getName() + "(theString = var1)";
        String[] fieldsSelect = new String[] {"theString", "intPrimitive"};
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listener);

        sendSupportBean(null, 1);
        assertFalse(listener.isInvoked());

        sendSupportBeanS0NewThread(100, "a", "b");
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"a"});

        sendSupportBean("a", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"a", 2});

        sendSupportBean(null, 1);
        assertFalse(listener.isInvoked());

        sendSupportBeanS0NewThread(100, "e", "c");
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"e"});

        sendSupportBean("c", 5);
        assertFalse(listener.isInvoked());

        sendSupportBean("e", 6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{"e", 6});

        stmtSet.destroy();
    }

    public void testAssignmentOrderNoDup()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Integer.class, "12");
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Integer.class, "2");
        epService.getEPAdministrator().getConfiguration().addVariable("var3", Integer.class, null);

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1 = intPrimitive, var2 = var1 + 1, var3 = var1 + var2";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2", "var3"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{12, 2, null}});

        sendSupportBean("S1", 3);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{3, 4, 7});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{3, 4, 7}});

        sendSupportBean("S1", -1);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{-1, 0, -1});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{-1, 0, -1}});

        sendSupportBean("S1", 90);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{90, 91, 181});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{90, 91, 181}});

        stmtSet.destroy();
    }

    public void testAssignmentOrderDup() throws Exception
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Integer.class, 0);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Integer.class, 1);
        epService.getEPAdministrator().getConfiguration().addVariable("var3", Integer.class, 2);

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1 = intPrimitive, var2 = var2, var1 = intBoxed, var3 = var3 + 1";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2", "var3"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{0, 1, 2}});

        sendSupportBean("S1", -1, 10);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{10, 1, 3});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{10, 1, 3}});

        sendSupportBean("S2", -2, 20);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{20, 1, 4});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{20, 1, 4}});

        sendSupportBeanNewThread("S3", -3, 30);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{30, 1, 5});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{30, 1, 5}});

        sendSupportBeanNewThread("S4", -4, 40);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{40, 1, 6});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{40, 1, 6}});

        stmtSet.destroy();
    }

    public void testObjectModel()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", double.class, 10d);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Long.class, 11L);

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("var1", "var2", "id"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean_A.class.getName())));

        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        String stmtText = "select var1, var2, id from " + SupportBean_A.class.getName();
        assertEquals(stmtText, model.toEPL());
        stmtSelect.addListener(listener);

        String[] fieldsSelect = new String[] {"var1", "var2", "id"};
        sendSupportBean_A("E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{10d, 11L, "E1"});

        model = new EPStatementObjectModel();
        model.setOnExpr(OnClause.createOnSet(Expressions.eq(Expressions.property("var1"), Expressions.property("intPrimitive"))).addAssignment(Expressions.eq(Expressions.property("var2"), Expressions.property("intBoxed"))));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1=intPrimitive, var2=intBoxed";
        EPStatement stmtSet = epService.getEPAdministrator().create(model);
        stmtSet.addListener(listenerSet);
        assertEquals(stmtTextSet, model.toEPL());

        EventType typeSet = stmtSet.getEventType();
        assertEquals(Double.class, typeSet.getPropertyType("var1"));
        assertEquals(Long.class, typeSet.getPropertyType("var2"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        String[] fieldsVar = new String[] {"var1", "var2"};
        EPAssertionUtil.assertEqualsAnyOrder(fieldsVar, typeSet.getPropertyNames());

        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{10d, 11L}});
        sendSupportBean("S1", 3, 4);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{3d, 4L});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{3d, 4L}});

        sendSupportBean_A("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{3d, 4L, "E2"});

        stmtSet.destroy();
        stmtSelect.destroy();
    }

    public void testCompile()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", double.class, 10d);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Long.class, 11L);

        String stmtText = "select var1, var2, id from " + SupportBean_A.class.getName();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        EPStatement stmtSelect = epService.getEPAdministrator().create(model);
        assertEquals(stmtText, model.toEPL());
        stmtSelect.addListener(listener);

        String[] fieldsSelect = new String[] {"var1", "var2", "id"};
        sendSupportBean_A("E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{10d, 11L, "E1"});

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1=intPrimitive, var2=intBoxed";
        model = epService.getEPAdministrator().compileEPL(stmtTextSet);
        EPStatement stmtSet = epService.getEPAdministrator().create(model);
        stmtSet.addListener(listenerSet);
        assertEquals(stmtTextSet, model.toEPL());

        EventType typeSet = stmtSet.getEventType();
        assertEquals(Double.class, typeSet.getPropertyType("var1"));
        assertEquals(Long.class, typeSet.getPropertyType("var2"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        String[] fieldsVar = new String[] {"var1", "var2"};
        EPAssertionUtil.assertEqualsAnyOrder(fieldsVar, typeSet.getPropertyNames());

        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{10d, 11L}});
        sendSupportBean("S1", 3, 4);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{3d, 4L});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{3d, 4L}});

        sendSupportBean_A("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{3d, 4L, "E2"});

        stmtSet.destroy();
        stmtSelect.destroy();

        // test prepared statement
        epService.getEPAdministrator().getConfiguration().addVariable("var_a", A.class, new A());
        epService.getEPAdministrator().getConfiguration().addEventType(B.class);
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL("select var_a.value from B");
        EPStatement statement = epService.getEPAdministrator().create(prepared);
        statement.setSubscriber(new Object() {
            public void update(String value) {
            }
        });
        epService.getEPRuntime().sendEvent(new B());
    }


    public void testRuntimeConfig()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Integer.class, 10);

        String stmtText = "select var1, theString from " + SupportBean.class.getName() + "(theString like 'E%')";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtText);
        stmtSelect.addListener(listener);

        String[] fieldsSelect = new String[] {"var1", "theString"};
        sendSupportBean("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{10, "E1"});

        sendSupportBean("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{10, "E2"});

        String stmtTextSet = "on " + SupportBean.class.getName() + "(theString like 'S%') set var1 = intPrimitive";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);

        EventType typeSet = stmtSet.getEventType();
        assertEquals(Integer.class, typeSet.getPropertyType("var1"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        assertTrue(Arrays.equals(typeSet.getPropertyNames(), new String[] {"var1"}));

        String[] fieldsVar = new String[] {"var1"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{10}});

        sendSupportBean("S1", 3);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{3});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{3}});

        sendSupportBean("E3", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{3, "E3"});

        sendSupportBean("S2", -1);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{-1});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{-1}});

        sendSupportBean("E4", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{-1, "E4"});

        try
        {
            epService.getEPAdministrator().getConfiguration().addVariable("var1", Integer.class, 10);
        }
        catch (ConfigurationException ex)
        {
            assertEquals("Error creating variable: Variable by name 'var1' has already been created", ex.getMessage());
        }

        stmtSet.destroy();
        stmtSelect.destroy();
    }

    public void testRuntimeOrderMultiple()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Integer.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Integer.class, 1);

        String stmtTextSet = "on " + SupportBean.class.getName() + "(theString like 'S%' or theString like 'B%') set var1 = intPrimitive, var2 = intBoxed";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, 1}});

        EventType typeSet = stmtSet.getEventType();
        assertEquals(Integer.class, typeSet.getPropertyType("var1"));
        assertEquals(Integer.class, typeSet.getPropertyType("var2"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        EPAssertionUtil.assertEqualsAnyOrder(new String[]{"var1", "var2"}, typeSet.getPropertyNames());

        sendSupportBean("S1", 3, null);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{3, null});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{3, null}});

        sendSupportBean("S1", -1, -2);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{-1, -2});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{-1, -2}});

        String stmtText = "select var1, var2, theString from " + SupportBean.class.getName() + "(theString like 'E%' or theString like 'B%')";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtText);
        stmtSelect.addListener(listener);
        String[] fieldsSelect = new String[] {"var1", "var2", "theString"};
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, null);

        sendSupportBean("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{-1, -2, "E1"});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{-1, -2}});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{-1, -2, "E1"}});

        sendSupportBean("S1", 11, 12);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{11, 12});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{11, 12}});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{11, 12, "E1"}});

        sendSupportBean("E2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{11, 12, "E2"});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{11, 12, "E2"}});

        stmtSelect.destroy();
        stmtSet.destroy();
    }

    public void testEngineConfigAPI()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addVariable("p_1", String.class, "begin");
        config.addVariable("p_2", boolean.class, true);
        config.addVariable("p_3", String.class, "value");

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String stmtTextSet = "on " + SupportBean.class.getName() + "(theString like 'S%') set p_1 = 'end', p_2 = false, p_3 = null";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"p_1", "p_2", "p_3"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"begin", true, "value"}});

        EventType typeSet = stmtSet.getEventType();
        assertEquals(String.class, typeSet.getPropertyType("p_1"));
        assertEquals(Boolean.class, typeSet.getPropertyType("p_2"));
        assertEquals(String.class, typeSet.getPropertyType("p_3"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        Arrays.sort(typeSet.getPropertyNames());
        assertTrue(Arrays.equals(typeSet.getPropertyNames(), fieldsVar));

        sendSupportBean("S1", 3);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"end", false, null});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"end", false, null}});

        sendSupportBean("S2", 4);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"end", false, null});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"end", false, null}});

        stmtSet.destroy();
    }

    public void testEngineConfigXML() throws Exception
    {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<esper-configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../esper-configuration-2-0.xsd\">" +
                "<variable name=\"p_1\" type=\"string\" />" +
                "<variable name=\"p_2\" type=\"bool\" initialization-value=\"true\"/>" +
                "<variable name=\"p_3\" type=\"long\" initialization-value=\"10\"/>" +
                "<variable name=\"p_4\" type=\"double\" initialization-value=\"11.1d\"/>" +
                "</esper-configuration>";

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document configDoc = builderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        Configuration config = SupportConfigFactory.getConfiguration();
        config.configure(configDoc);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String stmtTextSet = "on " + SupportBean.class.getName() + " set p_1 = theString, p_2 = boolBoxed, p_3 = intBoxed, p_4 = intBoxed";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"p_1", "p_2", "p_3", "p_4"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, true, 10L, 11.1d}});

        EventType typeSet = stmtSet.getEventType();
        assertEquals(String.class, typeSet.getPropertyType("p_1"));
        assertEquals(Boolean.class, typeSet.getPropertyType("p_2"));
        assertEquals(Long.class, typeSet.getPropertyType("p_3"));
        assertEquals(Double.class, typeSet.getPropertyType("p_4"));
        Arrays.sort(typeSet.getPropertyNames());
        assertTrue(Arrays.equals(typeSet.getPropertyNames(), fieldsVar));

        SupportBean bean = new SupportBean();
        bean.setTheString("text");
        bean.setBoolBoxed(false);
        bean.setIntBoxed(200);
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{"text", false, 200L, 200d});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{"text", false, 200L, 200d}});

        bean = new SupportBean();   // leave all fields null
        epService.getEPRuntime().sendEvent(bean);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{null, null, null, null});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, null, null, null}});

        stmtSet.destroy();
    }

    public void testCoercion()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", Float.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", Double.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("var3", Long.class, null);

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1 = intPrimitive, var2 = intPrimitive, var3=intBoxed";
        EPStatement stmtSet = epService.getEPAdministrator().createEPL(stmtTextSet);
        stmtSet.addListener(listenerSet);
        String[] fieldsVar = new String[] {"var1", "var2", "var3"};
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{null, null, null}});

        String stmtText = "select irstream var1, var2, var3, id from " + SupportBean_A.class.getName() + "#length(2)";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtText);
        stmtSelect.addListener(listener);
        String[] fieldsSelect = new String[] {"var1", "var2", "var3", "id"};
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, null);

        EventType typeSet = stmtSet.getEventType();
        assertEquals(Float.class, typeSet.getPropertyType("var1"));
        assertEquals(Double.class, typeSet.getPropertyType("var2"));
        assertEquals(Long.class, typeSet.getPropertyType("var3"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        EPAssertionUtil.assertEqualsAnyOrder(typeSet.getPropertyNames(), fieldsVar);

        sendSupportBean_A("A1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{null, null, null, "A1"});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{null, null, null, "A1"}});

        sendSupportBean("S1", 1, 2);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{1f, 1d, 2L});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{1f, 1d, 2L}});

        sendSupportBean_A("A2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsSelect, new Object[]{1f, 1d, 2L, "A2"});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{1f, 1d, 2L, "A1"}, {1f, 1d, 2L, "A2"}});

        sendSupportBean("S1", 10, 20);
        EPAssertionUtil.assertProps(listenerSet.assertOneGetNewAndReset(), fieldsVar, new Object[]{10f, 10d, 20L});
        EPAssertionUtil.assertPropsPerRow(stmtSet.iterator(), fieldsVar, new Object[][]{{10f, 10d, 20L}});

        sendSupportBean_A("A3");
        EPAssertionUtil.assertProps(listener.getLastNewData()[0], fieldsSelect, new Object[]{10f, 10d, 20L, "A3"});
        EPAssertionUtil.assertProps(listener.getLastOldData()[0], fieldsSelect, new Object[]{10f, 10d, 20L, "A1"});
        EPAssertionUtil.assertPropsPerRow(stmtSelect.iterator(), fieldsSelect, new Object[][]{{10f, 10d, 20L, "A2"}, {10f, 10d, 20L, "A3"}});

        stmtSelect.destroy();
        stmtSet.destroy();
    }

    public void testInvalidSet()
    {
        epService.getEPAdministrator().getConfiguration().addVariable("var1", String.class, null);
        epService.getEPAdministrator().getConfiguration().addVariable("var2", boolean.class, false);
        epService.getEPAdministrator().getConfiguration().addVariable("var3", int.class, 1);

        tryInvalidSet("on " + SupportBean.class.getName() + " set dummy = 100",
                      "Error starting statement: Variable by name 'dummy' has not been created or configured");

        tryInvalidSet("on " + SupportBean.class.getName() + " set var1 = 1",
                      "Error starting statement: Variable 'var1' of declared type java.lang.String cannot be assigned a value of type java.lang.Integer");

        tryInvalidSet("on " + SupportBean.class.getName() + " set var3 = 'abc'",
                      "Error starting statement: Variable 'var3' of declared type java.lang.Integer cannot be assigned a value of type java.lang.String");

        tryInvalidSet("on " + SupportBean.class.getName() + " set var3 = doublePrimitive",
                      "Error starting statement: Variable 'var3' of declared type java.lang.Integer cannot be assigned a value of type double");

        tryInvalidSet("on " + SupportBean.class.getName() + " set var2 = 'false'", null);
        tryInvalidSet("on " + SupportBean.class.getName() + " set var3 = 1.1", null);
        tryInvalidSet("on " + SupportBean.class.getName() + " set var3 = 22222222222222", null);
        tryInvalidSet("on " + SupportBean.class.getName() + " set var3", "Error starting statement: Missing variable assignment expression in assignment number 0 [");
    }

    private void tryInvalidSet(String stmtText, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            if (message != null)
            {
                SupportMessageAssertUtil.assertMessage(ex, message);
            }
        }
    }

    public void testInvalidInitialization()
    {
        tryInvalid(Integer.class, "abcdef",
                "Error creating variable: Variable 'var1' of declared type java.lang.Integer cannot be initialized by value 'abcdef': java.lang.NumberFormatException: For input string: \"abcdef\"");

        tryInvalid(Integer.class, new Double(11.1),
                "Error creating variable: Variable 'var1' of declared type java.lang.Integer cannot be initialized by a value of type java.lang.Double");

        tryInvalid(int.class, new Double(11.1), null);
        tryInvalid(String.class, true, null);
    }

    private void tryInvalid(Class type, Object value, String message)
    {
        try
        {
            epService.getEPAdministrator().getConfiguration().addVariable("var1", type, value);
            fail();
        }
        catch (ConfigurationException ex)
        {
            if (message != null)
            {
                assertEquals(message, ex.getMessage());
            }
        }
    }

    private void tryInvalid(String epl, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive, Integer intBoxed)
    {
        SupportBean bean = makeSupportBean(theString, intPrimitive, intBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private void sendSupportBeanNewThread(final String theString, final int intPrimitive, final Integer intBoxed) throws InterruptedException
    {
        Thread t = new Thread() {
            public void run()
            {
                SupportBean bean = makeSupportBean(theString, intPrimitive, intBoxed);
                epService.getEPRuntime().sendEvent(bean);
            }
        };
        t.start();
        t.join();
    }

    private void sendSupportBeanS0NewThread(final int id, final String p00, final String p01) throws InterruptedException
    {
        Thread t = new Thread() {
            public void run()
            {
                epService.getEPRuntime().sendEvent(new SupportBean_S0(id, p00, p01));
            }
        };
        t.start();
        t.join();
    }

    private SupportBean makeSupportBean(String theString, int intPrimitive, Integer intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        return bean;
    }

    private void assertVariableValues(String[] names, Object[] values)
    {
        assertEquals(names.length, values.length);

        // assert one-by-one
        for (int i = 0; i < names.length; i++)
        {
            assertEquals(values[i], epService.getEPRuntime().getVariableValue(names[i]));
        }

        // get and assert all
        Map<String, Object> all = epService.getEPRuntime().getVariableValueAll();
        for (int i = 0; i < names.length; i++)
        {
            assertEquals(values[i], all.get(names[i]));
        }

        // get by request
        Set<String> nameSet = new HashSet<String>();
        nameSet.addAll(Arrays.asList(names));
        Map<String, Object> valueSet = epService.getEPRuntime().getVariableValue(nameSet);
        for (int i = 0; i < names.length; i++)
        {
            assertEquals(values[i], valueSet.get(names[i]));
        }
    }

    public static class A implements Serializable {
        public String getValue() {
            return "";
        }
    }

    public static class B {
    }

    private void tryInvalidSetConstant(String variableName, Object newValue) {
        try {
            epService.getEPRuntime().setVariableValue(variableName, newValue);
            fail();
        }
        catch (VariableConstantValueException ex) {
            assertEquals(ex.getMessage(), "Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
        try {
            epService.getEPRuntime().setVariableValue(Collections.<String, Object>singletonMap(variableName, newValue));
            fail();
        }
        catch (VariableConstantValueException ex) {
            assertEquals(ex.getMessage(), "Variable by name '" + variableName + "' is declared as constant and may not be assigned a new value");
        }
    }

    private void tryOperator(String operator, Object[][] testdata) {
        EPServiceProviderSPI spi = (EPServiceProviderSPI) epService;
        FilterServiceSPI filterSpi = (FilterServiceSPI) spi.getFilterService();

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select theString as c0,intPrimitive as c1 from SupportBean(" + operator + ")");
        stmt.addListener(listener);

        // initiate
        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "S01"));

        for (int i = 0; i < testdata.length; i++) {
            SupportBean bean = new SupportBean();
            Object testValue = testdata[i][0];
            if (testValue instanceof Integer) {
                bean.setIntBoxed((Integer) testValue);
            }
            else if (testValue instanceof SupportEnum) {
                bean.setEnumValue((SupportEnum) testValue);
            }
            else {
                bean.setShortBoxed((Short) testValue);
            }
            boolean expected = (Boolean) testdata[i][1];

            epService.getEPRuntime().sendEvent(bean);
            assertEquals("Failed at " + i, expected, listener.getAndClearIsInvoked());
        }

        // assert type of expression
        if (filterSpi.isSupportsTakeApply()) {
            FilterSet set = filterSpi.take(Collections.singleton(stmt.getStatementId()));
            assertEquals(1, set.getFilters().size());
            FilterValueSet valueSet = set.getFilters().get(0).getFilterValueSet();
            assertEquals(1, valueSet.getParameters().length);
            FilterValueSetParam para = valueSet.getParameters()[0][0];
            assertTrue(para.getFilterOperator() != FilterOperator.BOOLEAN_EXPRESSION);
        }

        stmt.destroy();
    }

    public static class MySimpleVariableServiceFactory {
        public static MySimpleVariableService makeService() {
            return new MySimpleVariableService();
        }
    }

    public static class MySimpleVariableService {
        public String doSomething() {
            return "hello";
        }
    }
}
