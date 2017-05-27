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
package com.espertech.esper.regression.epl.variable;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.CreateVariableClause;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.soda.Expressions;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecVariablesCreate implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionOM(epService);
        runAssertionCompileStartStop(epService);
        runAssertionSubscribeAndIterate(epService);
        runAssertionDeclarationAndSelect(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionOM(EPServiceProvider epService) {
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setCreateVariable(CreateVariableClause.create("long", "var1OM", null));
        epService.getEPAdministrator().create(model);
        assertEquals("create variable long var1OM", model.toEPL());

        model = new EPStatementObjectModel();
        model.setCreateVariable(CreateVariableClause.create("string", "var2OM", Expressions.constant("abc")));
        epService.getEPAdministrator().create(model);
        assertEquals("create variable string var2OM = \"abc\"", model.toEPL());

        String stmtTextSelect = "select var1OM, var2OM from " + SupportBean.class.getName();
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        String[] fieldsVar = new String[]{"var1OM", "var2OM"};
        sendSupportBean(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsVar, new Object[]{null, "abc"});

        SupportModelHelper.compileCreate(epService, "create variable double[] arrdouble = {1.0d,2.0d}");

        stmtSelect.destroy();
    }

    private void runAssertionCompileStartStop(EPServiceProvider epService) {
        String text = "create variable long var1CSS";
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(text);
        epService.getEPAdministrator().create(model);
        assertEquals(text, model.toEPL());

        text = "create variable string var2CSS = \"abc\"";
        model = epService.getEPAdministrator().compileEPL(text);
        epService.getEPAdministrator().create(model);
        assertEquals(text, model.toEPL());

        String stmtTextSelect = "select var1CSS, var2CSS from " + SupportBean.class.getName();
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        String[] fieldsVar = new String[]{"var1CSS", "var2CSS"};
        sendSupportBean(epService, "E1", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fieldsVar, new Object[]{null, "abc"});

        // ESPER-545
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String createText = "create variable int FOO = 0";
        epService.getEPAdministrator().createEPL(createText);
        epService.getEPAdministrator().createEPL("on pattern [every SupportBean] set FOO = FOO + 1");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(1, epService.getEPRuntime().getVariableValue("FOO"));

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL(createText);
        assertEquals(0, epService.getEPRuntime().getVariableValue("FOO"));

        // cleanup of variable when statement exception occurs
        epService.getEPAdministrator().createEPL("create variable int x = 123");
        try {
            epService.getEPAdministrator().createEPL("select missingScript(x) from SupportBean");
        } catch (Exception ex) {
            for (String statementName : epService.getEPAdministrator().getStatementNames()) {
                epService.getEPAdministrator().getStatement(statementName).destroy();
            }
        }
        epService.getEPAdministrator().createEPL("create variable int x = 123");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionSubscribeAndIterate(EPServiceProvider epService) {
        String stmtCreateTextOne = "create variable long var1SAI = null";
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtCreateTextOne);
        assertEquals(StatementType.CREATE_VARIABLE, ((EPStatementSPI) stmtCreateOne).getStatementMetadata().getStatementType());
        SupportUpdateListener listenerCreateOne = new SupportUpdateListener();
        stmtCreateOne.addListener(listenerCreateOne);
        String[] fieldsVar1 = new String[]{"var1SAI"};
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsVar1, new Object[][]{{null}});
        assertFalse(listenerCreateOne.isInvoked());

        EventType typeSet = stmtCreateOne.getEventType();
        assertEquals(Long.class, typeSet.getPropertyType("var1SAI"));
        assertEquals(Map.class, typeSet.getUnderlyingType());
        assertTrue(Arrays.equals(typeSet.getPropertyNames(), new String[]{"var1SAI"}));

        String stmtCreateTextTwo = "create variable long var2SAI = 20";
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtCreateTextTwo);
        SupportUpdateListener listenerCreateTwo = new SupportUpdateListener();
        stmtCreateTwo.addListener(listenerCreateTwo);
        String[] fieldsVar2 = new String[]{"var2SAI"};
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsVar2, new Object[][]{{20L}});
        assertFalse(listenerCreateTwo.isInvoked());

        String stmtTextSet = "on " + SupportBean.class.getName() + " set var1SAI = intPrimitive * 2, var2SAI = var1SAI + 1";
        epService.getEPAdministrator().createEPL(stmtTextSet);

        sendSupportBean(epService, "E1", 100);
        EPAssertionUtil.assertProps(listenerCreateOne.getLastNewData()[0], fieldsVar1, new Object[]{200L});
        EPAssertionUtil.assertProps(listenerCreateOne.getLastOldData()[0], fieldsVar1, new Object[]{null});
        listenerCreateOne.reset();
        EPAssertionUtil.assertProps(listenerCreateTwo.getLastNewData()[0], fieldsVar2, new Object[]{201L});
        EPAssertionUtil.assertProps(listenerCreateTwo.getLastOldData()[0], fieldsVar2, new Object[]{20L});
        listenerCreateOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsVar1, new Object[][]{{200L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsVar2, new Object[][]{{201L}});

        sendSupportBean(epService, "E2", 200);
        EPAssertionUtil.assertProps(listenerCreateOne.getLastNewData()[0], fieldsVar1, new Object[]{400L});
        EPAssertionUtil.assertProps(listenerCreateOne.getLastOldData()[0], fieldsVar1, new Object[]{200L});
        listenerCreateOne.reset();
        EPAssertionUtil.assertProps(listenerCreateTwo.getLastNewData()[0], fieldsVar2, new Object[]{401L});
        EPAssertionUtil.assertProps(listenerCreateTwo.getLastOldData()[0], fieldsVar2, new Object[]{201L});
        listenerCreateOne.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsVar1, new Object[][]{{400L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsVar2, new Object[][]{{401L}});

        stmtCreateTwo.stop();
        stmtCreateTwo.start();

        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsVar1, new Object[][]{{400L}});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsVar2, new Object[][]{{20L}});
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionDeclarationAndSelect(EPServiceProvider epService) throws Exception {
        Object[][] variables = new Object[][]{
                {"var1", "int", "1", 1},
                {"var2", "int", "'2'", 2},
                {"var3", "INTEGER", " 3+2 ", 5},
                {"var4", "bool", " true|false ", true},
                {"var5", "boolean", " var1=1 ", true},
                {"var6", "double", " 1.11 ", 1.11d},
                {"var7", "double", " 1.20d ", 1.20d},
                {"var8", "Double", " ' 1.12 ' ", 1.12d},
                {"var9", "float", " 1.13f*2f ", 2.26f},
                {"var10", "FLOAT", " -1.14f ", -1.14f},
                {"var11", "string", " ' XXXX ' ", " XXXX "},
                {"var12", "string", " \"a\" ", "a"},
                {"var13", "character", "'a'", 'a'},
                {"var14", "char", "'x'", 'x'},
                {"var15", "short", " 20 ", (short) 20},
                {"var16", "SHORT", " ' 9 ' ", (short) 9},
                {"var17", "long", " 20*2 ", (long) 40},
                {"var18", "LONG", " ' 9 ' ", (long) 9},
                {"var19", "byte", " 20*2 ", (byte) 40},
                {"var20", "BYTE", "9+1", (byte) 10},
                {"var21", "int", null, null},
                {"var22", "bool", null, null},
                {"var23", "double", null, null},
                {"var24", "float", null, null},
                {"var25", "string", null, null},
                {"var26", "char", null, null},
                {"var27", "short", null, null},
                {"var28", "long", null, null},
                {"var29", "BYTE", null, null},
        };

        for (int i = 0; i < variables.length; i++) {
            String text = "create variable " + variables[i][1] + " " + variables[i][0];
            if (variables[i][2] != null) {
                text += " = " + variables[i][2];
            }

            epService.getEPAdministrator().createEPL(text);
        }

        // select all variables
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        buf.append("select ");
        for (int i = 0; i < variables.length; i++) {
            buf.append(delimiter);
            buf.append(variables[i][0]);
            delimiter = ",";
        }
        buf.append(" from ");
        buf.append(SupportBean.class.getName());
        EPStatement stmt = epService.getEPAdministrator().createEPL(buf.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        // assert initialization values
        sendSupportBean(epService, "E1", 1);
        EventBean received = listener.assertOneGetNewAndReset();
        for (int i = 0; i < variables.length; i++) {
            assertEquals(variables[i][3], received.get((String) variables[i][0]));
        }

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String stmt = "create variable somedummy myvar = 10";
        SupportMessageAssertUtil.tryInvalid(epService, stmt, "Error starting statement: Cannot create variable: Cannot create variable 'myvar', type 'somedummy' is not a recognized type [create variable somedummy myvar = 10]");

        stmt = "create variable string myvar = 5";
        SupportMessageAssertUtil.tryInvalid(epService, stmt, "Error starting statement: Cannot create variable: Variable 'myvar' of declared type java.lang.String cannot be initialized by a value of type java.lang.Integer [create variable string myvar = 5]");

        stmt = "create variable string myvar = 'a'";
        epService.getEPAdministrator().createEPL("create variable string myvar = 'a'");
        SupportMessageAssertUtil.tryInvalid(epService, stmt, "Error starting statement: Cannot create variable: Variable by name 'myvar' has already been created [create variable string myvar = 'a']");

        SupportMessageAssertUtil.tryInvalid(epService, "select * from " + SupportBean.class.getName() + " output every somevar events",
                "Error starting statement: Error in the output rate limiting clause: Variable named 'somevar' has not been declared [");
    }

    private void sendSupportBean(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
