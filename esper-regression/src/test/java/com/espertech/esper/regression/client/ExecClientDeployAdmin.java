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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.deploy.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.io.InputStream;
import java.util.*;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExecClientDeployAdmin implements RegressionExecution {
    private final static String NEWLINE = System.getProperty("line.separator");

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionUserObjectAndStatementNameResolver(epService);
        runAssertionExplicitDeploymentId(epService);
        runAssertionTransition(epService);
        runAssertionTransitionInvalid(epService);
        runAssertionDeployImports(epService);
        runAssertionDeploySingle(epService);
        runAssertionLineNumberAndComments(epService);
        runAssertionShortcutReadDeploy(epService);
        runAssertionDeployUndeploy(epService);
        runAssertionInvalidExceptionList(epService);
        runAssertionFlagRollbackFailfastCompile(epService);
        runAssertionFlagCompileOnly(epService);
        runAssertionFlagValidateOnly(epService);
        runAssertionFlagIsolated(epService);
        runAssertionFlagUndeployNoDestroy(epService);
    }

    private void runAssertionUserObjectAndStatementNameResolver(EPServiceProvider epService) throws Exception {
        Module module = epService.getEPAdministrator().getDeploymentAdmin().parse("select * from java.lang.Object where 1=2; select * from java.lang.Object where 3=4;");
        DeploymentOptions options = new DeploymentOptions();
        options.setStatementNameResolver(new StatementNameResolver() {
            public String getStatementName(StatementDeploymentContext context) {
                return context.getEpl().contains("1=2") ? "StmtOne" : "StmtTwo";
            }
        });
        options.setStatementUserObjectResolver(new StatementUserObjectResolver() {
            public Object getUserObject(StatementDeploymentContext context) {
                return context.getEpl().contains("1=2") ? 100 : 200;
            }
        });

        epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);

        assertEquals(100, epService.getEPAdministrator().getStatement("StmtOne").getUserObject());
        assertEquals(200, epService.getEPAdministrator().getStatement("StmtTwo").getUserObject());

        undeployRemoveAll(epService);
    }

    private void runAssertionExplicitDeploymentId(EPServiceProvider epService) throws Exception {
        // try module-add
        Module module = epService.getEPAdministrator().getDeploymentAdmin().parse("select * from java.lang.Object");
        epService.getEPAdministrator().getDeploymentAdmin().add(module, "ABC01");
        assertEquals(DeploymentState.UNDEPLOYED, epService.getEPAdministrator().getDeploymentAdmin().getDeployment("ABC01").getState());
        assertEquals(1, epService.getEPAdministrator().getDeploymentAdmin().getDeployments().length);

        epService.getEPAdministrator().getDeploymentAdmin().deploy("ABC01", null);
        assertEquals(DeploymentState.DEPLOYED, epService.getEPAdministrator().getDeploymentAdmin().getDeployment("ABC01").getState());

        try {
            epService.getEPAdministrator().getDeploymentAdmin().add(module, "ABC01");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Assigned deployment id 'ABC01' is already in use", ex.getMessage());
        }
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove("ABC01");
        assertEquals(0, epService.getEPAdministrator().getDeploymentAdmin().getDeployments().length);

        // try module-deploy
        Module moduleTwo = epService.getEPAdministrator().getDeploymentAdmin().parse("select * from java.lang.Object");
        epService.getEPAdministrator().getDeploymentAdmin().deploy(moduleTwo, null, "ABC02");
        assertEquals(DeploymentState.DEPLOYED, epService.getEPAdministrator().getDeploymentAdmin().getDeployment("ABC02").getState());
        assertEquals(1, epService.getEPAdministrator().getDeploymentAdmin().getDeployments().length);

        try {
            epService.getEPAdministrator().getDeploymentAdmin().add(module, "ABC02");
            fail();
        } catch (IllegalArgumentException ex) {
            assertEquals("Assigned deployment id 'ABC02' is already in use", ex.getMessage());
        }
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove("ABC02");
        assertEquals(0, epService.getEPAdministrator().getDeploymentAdmin().getDeployments().length);
    }

    private void runAssertionTransition(EPServiceProvider epService) throws Exception {

        // add module
        Module module = makeModule("com.testit", "create schema S1 as (col1 int)");
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().add(module);
        DeploymentInformation originalInfo = epService.getEPAdministrator().getDeploymentAdmin().getDeployment(deploymentId);
        Calendar addedDate = originalInfo.getAddedDate();
        Calendar lastUpdDate = originalInfo.getLastUpdateDate();
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());
        assertEquals("com.testit", originalInfo.getModule().getName());
        assertEquals(0, originalInfo.getItems().length);

        // deploy added module
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().deploy(deploymentId, null);
        assertEquals(deploymentId, result.getDeploymentId());
        DeploymentInformation info = epService.getEPAdministrator().getDeploymentAdmin().getDeployment(deploymentId);
        assertEquals(DeploymentState.DEPLOYED, info.getState());
        assertEquals("com.testit", info.getModule().getName());
        assertEquals(addedDate, info.getAddedDate());
        assertTrue(info.getLastUpdateDate().getTimeInMillis() - lastUpdDate.getTimeInMillis() < 5000);
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());

        // undeploy module
        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
        assertEquals(deploymentId, result.getDeploymentId());
        info = epService.getEPAdministrator().getDeploymentAdmin().getDeployment(deploymentId);
        assertEquals(DeploymentState.UNDEPLOYED, info.getState());
        assertEquals("com.testit", info.getModule().getName());
        assertEquals(addedDate, info.getAddedDate());
        assertTrue(info.getLastUpdateDate().getTimeInMillis() - lastUpdDate.getTimeInMillis() < 5000);
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());

        // remove module
        epService.getEPAdministrator().getDeploymentAdmin().remove(deploymentId);
        assertNull(epService.getEPAdministrator().getDeploymentAdmin().getDeployment(deploymentId));
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());

        undeployRemoveAll(epService);
    }

    private void runAssertionTransitionInvalid(EPServiceProvider epService) throws Exception {

        // invalid from deployed state
        Module module = makeModule("com.testit", "create schema S1 as (col1 int)");
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().deploy(module, null);
        try {
            epService.getEPAdministrator().getDeploymentAdmin().deploy(deploymentResult.getDeploymentId(), null);
            fail();
        } catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is already in deployed state"));
        }

        try {
            epService.getEPAdministrator().getDeploymentAdmin().remove(deploymentResult.getDeploymentId());
            fail();
        } catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is in deployed state, please undeploy first"));
        }

        // invalid from undeployed state
        module = makeModule("com.testit", "create schema S1 as (col1 int)");
        String deploymentId = epService.getEPAdministrator().getDeploymentAdmin().add(module);
        try {
            epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentId);
            fail();
        } catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is already in undeployed state"));
        }
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deploymentId);
        assertNull(epService.getEPAdministrator().getDeploymentAdmin().getDeployment(deploymentId));

        // not found
        assertNull(epService.getEPAdministrator().getDeploymentAdmin().getDeployment("123"));
        try {
            epService.getEPAdministrator().getDeploymentAdmin().deploy("123", null);
            fail();
        } catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().getDeploymentAdmin().undeploy("123");
            fail();
        } catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().getDeploymentAdmin().remove("123");
            fail();
        } catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            epService.getEPAdministrator().getDeploymentAdmin().undeployRemove("123");
            fail();
        } catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        undeployRemoveAll(epService);
    }

    private void runAssertionDeployImports(EPServiceProvider epService) throws Exception {
        Module module = makeModule("com.testit", "create schema S1 as SupportBean", "@Name('A') select SupportStaticMethodLib.plusOne(intPrimitive) as val from S1");
        module.getImports().add(SupportBean.class.getName());
        module.getImports().add(SupportStaticMethodLib.class.getPackage().getName() + ".*");
        assertFalse(epService.getEPAdministrator().getDeploymentAdmin().isDeployed("com.testit"));
        epService.getEPAdministrator().getDeploymentAdmin().deploy(module, null);
        assertTrue(epService.getEPAdministrator().getDeploymentAdmin().isDeployed("com.testit"));
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("A").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        assertEquals(5, listener.assertOneGetNewAndReset().get("val"));

        undeployRemoveAll(epService);
    }

    private void runAssertionDeploySingle(EPServiceProvider epService) throws Exception {
        Module module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_9.epl");
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().deploy(module, new DeploymentOptions());

        assertNotNull(result.getDeploymentId());
        assertEquals(2, result.getStatements().size());
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals("@Name(\"StmtOne\")" + NEWLINE +
                "create schema MyEvent(id String, val1 int, val2 int)", epService.getEPAdministrator().getStatement("StmtOne").getText());
        assertEquals("@Name(\"StmtTwo\")" + NEWLINE +
                "select * from MyEvent", epService.getEPAdministrator().getStatement("StmtTwo").getText());

        assertEquals(1, epService.getEPAdministrator().getDeploymentAdmin().getDeployments().length);
        assertEquals(result.getDeploymentId(), epService.getEPAdministrator().getDeploymentAdmin().getDeployments()[0]);

        // test deploy with variable
        String moduleStr = "create variable integer snapshotOutputSecs = 10; " +
                "create schema foo as (bar string); " +
                "select bar from foo output snapshot every snapshotOutputSecs seconds;";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(moduleStr);

        undeployRemoveAll(epService);
    }

    private void runAssertionLineNumberAndComments(EPServiceProvider epService) throws Exception {
        String moduleText = NEWLINE + NEWLINE + "select * from ABC;" +
                NEWLINE + "select * from DEF";

        Module module = epService.getEPAdministrator().getDeploymentAdmin().parse(moduleText);
        assertEquals(2, module.getItems().size());
        assertEquals(3, module.getItems().get(0).getLineNumber());
        assertEquals(4, module.getItems().get(1).getLineNumber());

        module = epService.getEPAdministrator().getDeploymentAdmin().parse("/* abc */");
        epService.getEPAdministrator().getDeploymentAdmin().deploy(module, new DeploymentOptions());

        module = epService.getEPAdministrator().getDeploymentAdmin().parse("select * from java.lang.Object; \r\n/* abc */\r\n");
        epService.getEPAdministrator().getDeploymentAdmin().deploy(module, new DeploymentOptions());

        undeployRemoveAll(epService);
    }

    private void runAssertionShortcutReadDeploy(EPServiceProvider epService) throws Exception {

        String resource = "regression/test_module_12.epl";
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertNotNull(input);
        DeploymentResult resultOne = epService.getEPAdministrator().getDeploymentAdmin().readDeploy(input, null, null, null);
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultOne.getDeploymentId());
        assertNull(epService.getEPAdministrator().getDeploymentAdmin().getDeployment(resultOne.getDeploymentId()));

        resultOne = epService.getEPAdministrator().getDeploymentAdmin().readDeploy(resource, "uri1", "archive1", "obj1");
        assertEquals("regression.test", epService.getEPAdministrator().getDeploymentAdmin().getDeployment(resultOne.getDeploymentId()).getModule().getName());
        assertEquals(2, resultOne.getStatements().size());
        assertEquals("create schema MyType(col1 integer)", resultOne.getStatements().get(0).getText());
        assertTrue(epService.getEPAdministrator().getDeploymentAdmin().isDeployed("regression.test"));
        assertEquals("module regression.test;" + NEWLINE + NEWLINE +
                "create schema MyType(col1 integer);" + NEWLINE +
                "select * from MyType;" + NEWLINE, epService.getEPAdministrator().getDeploymentAdmin().getDeployment(resultOne.getDeploymentId()).getModule().getModuleText());

        String moduleText = "module regression.test.two;" +
                "uses regression.test;" +
                "create schema MyTypeTwo(col1 integer, col2.col3 string);" +
                "select * from MyTypeTwo;";
        DeploymentResult resultTwo = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(moduleText, "uri2", "archive2", "obj2");
        DeploymentInformation[] infos = epService.getEPAdministrator().getDeploymentAdmin().getDeploymentInformation();
        assertEquals(2, infos.length);

        List<DeploymentInformation> infoList = new ArrayList<DeploymentInformation>(Arrays.asList(infos));
        Collections.sort(infoList, new Comparator<DeploymentInformation>() {
            public int compare(DeploymentInformation o1, DeploymentInformation o2) {
                return o1.getModule().getName().compareTo(o2.getModule().getName());
            }
        });
        DeploymentInformation infoOne = infoList.get(0);
        DeploymentInformation infoTwo = infoList.get(1);
        assertEquals("regression.test", infoOne.getModule().getName());
        assertEquals("uri1", infoOne.getModule().getUri());
        assertEquals("archive1", infoOne.getModule().getArchiveName());
        assertEquals("obj1", infoOne.getModule().getUserObject());
        assertNotNull(infoOne.getAddedDate());
        assertNotNull(infoOne.getLastUpdateDate());
        assertEquals(DeploymentState.DEPLOYED, infoOne.getState());
        assertEquals("regression.test.two", infoTwo.getModule().getName());
        assertEquals("uri2", infoTwo.getModule().getUri());
        assertEquals("archive2", infoTwo.getModule().getArchiveName());
        assertEquals("obj2", infoTwo.getModule().getUserObject());
        assertNotNull(infoTwo.getAddedDate());
        assertNotNull(infoTwo.getLastUpdateDate());
        assertEquals(DeploymentState.DEPLOYED, infoTwo.getState());

        undeployRemoveAll(epService);
    }

    private void runAssertionDeployUndeploy(EPServiceProvider epService) throws Exception {
        Module moduleOne = makeModule("mymodule.one", "@Name('A1') create schema MySchemaOne (col1 int)", "@Name('B1') select * from MySchemaOne");
        DeploymentResult resultOne = epService.getEPAdministrator().getDeploymentAdmin().deploy(moduleOne, new DeploymentOptions());
        assertEquals(2, resultOne.getStatements().size());
        assertTrue(epService.getEPAdministrator().getDeploymentAdmin().isDeployed("mymodule.one"));

        Module moduleTwo = makeModule("mymodule.two", "@Name('A2') create schema MySchemaTwo (col1 int)", "@Name('B2') select * from MySchemaTwo");
        moduleTwo.setUserObject(100L);
        moduleTwo.setArchiveName("archive");
        DeploymentResult resultTwo = epService.getEPAdministrator().getDeploymentAdmin().deploy(moduleTwo, new DeploymentOptions());
        assertEquals(2, resultTwo.getStatements().size());

        DeploymentInformation[] info = epService.getEPAdministrator().getDeploymentAdmin().getDeploymentInformation();
        List<DeploymentInformation> infoList = new ArrayList<DeploymentInformation>(Arrays.asList(info));
        Collections.sort(infoList, new Comparator<DeploymentInformation>() {
            public int compare(DeploymentInformation o1, DeploymentInformation o2) {
                return o1.getModule().getName().compareTo(o2.getModule().getName());
            }
        });
        assertEquals(2, info.length);
        assertEquals(resultOne.getDeploymentId(), infoList.get(0).getDeploymentId());
        assertNotNull(infoList.get(0).getLastUpdateDate());
        assertEquals("mymodule.one", infoList.get(0).getModule().getName());
        assertEquals(null, infoList.get(0).getModule().getUri());
        assertEquals(0, infoList.get(0).getModule().getUses().size());
        assertEquals(resultTwo.getDeploymentId(), infoList.get(1).getDeploymentId());
        assertEquals(100L, infoList.get(1).getModule().getUserObject());
        assertEquals("archive", infoList.get(1).getModule().getArchiveName());
        assertEquals(2, infoList.get(1).getItems().length);
        assertEquals("A2", infoList.get(1).getItems()[0].getStatementName());
        assertEquals("@Name('A2') create schema MySchemaTwo (col1 int)", infoList.get(1).getItems()[0].getExpression());
        assertEquals("B2", infoList.get(1).getItems()[1].getStatementName());
        assertEquals("@Name('B2') select * from MySchemaTwo", infoList.get(1).getItems()[1].getExpression());
        assertEquals(4, epService.getEPAdministrator().getStatementNames().length);

        UndeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultTwo.getDeploymentId());
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals(2, result.getStatementInfo().size());
        assertEquals("A2", result.getStatementInfo().get(0).getStatementName());
        assertEquals("@Name('A2') create schema MySchemaTwo (col1 int)", result.getStatementInfo().get(0).getExpression());
        assertEquals("B2", result.getStatementInfo().get(1).getStatementName());
        assertEquals("@Name('B2') select * from MySchemaTwo", result.getStatementInfo().get(1).getExpression());

        result = epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultOne.getDeploymentId());
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);
        assertEquals(2, result.getStatementInfo().size());
        assertEquals("A1", result.getStatementInfo().get(0).getStatementName());

        undeployRemoveAll(epService);
    }

    private void runAssertionInvalidExceptionList(EPServiceProvider epService) throws Exception {
        Module moduleOne = makeModule("mymodule.one", "create schema MySchemaOne (col1 Wrong)", "create schema MySchemaOne (col2 WrongTwo)");
        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setFailFast(false);
            epService.getEPAdministrator().getDeploymentAdmin().deploy(moduleOne, options);
            fail();
        } catch (DeploymentActionException ex) {
            assertEquals("Deployment failed in module 'mymodule.one' in expression 'create schema MySchemaOne (col1 Wrong)' : Error starting statement: Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col1 Wrong)]", ex.getMessage());
            assertEquals(2, ex.getExceptions().size());
            assertEquals("create schema MySchemaOne (col1 Wrong)", ex.getExceptions().get(0).getExpression());
            assertEquals("Error starting statement: Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col1 Wrong)]", ex.getExceptions().get(0).getInner().getMessage());
            assertEquals("create schema MySchemaOne (col2 WrongTwo)", ex.getExceptions().get(1).getExpression());
            assertEquals("Error starting statement: Nestable type configuration encountered an unexpected property type name 'WrongTwo' for property 'col2', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col2 WrongTwo)]", ex.getExceptions().get(1).getInner().getMessage());
        }

        // test NEWLINE as part of the failing expression - replaced by space
        try {
            epService.getEPAdministrator().getDeploymentAdmin().parseDeploy("XX\nX");
            fail();
        } catch (DeploymentException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Compilation failed in expression 'XX X' : Incorrect syntax near 'XX' [");
        }
    }

    private void runAssertionFlagRollbackFailfastCompile(EPServiceProvider epService) throws Exception {

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') create schema MySchemaTwo (col1 not_existing_type)";
        String errorTextTwo = "Error starting statement: Nestable type configuration encountered an unexpected property type name 'not_existing_type' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [@Name('B') create schema MySchemaTwo (col1 not_existing_type)]";
        String textThree = "@Name('C') create schema MySchemaTwo (col1 int)";
        Module module = makeModule("mymodule.two", textOne, textTwo, textThree);

        try {
            DeploymentOptions options = new DeploymentOptions();
            epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);
            fail();
        } catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(textTwo, first.getExpression());
            assertEquals(errorTextTwo, first.getInner().getMessage());
        }
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);

        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setRollbackOnFail(false);
            epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);
            fail();
        } catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(textTwo, first.getExpression());
            assertEquals(errorTextTwo, first.getInner().getMessage());
            EPAssertionUtil.assertEqualsExactOrder(epService.getEPAdministrator().getStatementNames(), new String[]{"A"});
            epService.getEPAdministrator().getStatement("A").destroy();
        }

        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setRollbackOnFail(false);
            options.setFailFast(false);
            epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);
            fail();
        } catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(textTwo, first.getExpression());
            assertEquals(errorTextTwo, first.getInner().getMessage());
            EPAssertionUtil.assertEqualsExactOrder(new String[]{"A", "C"}, epService.getEPAdministrator().getStatementNames());
        }
    }

    private void runAssertionFlagCompileOnly(EPServiceProvider epService) throws Exception {
        String text = "create schema SomeSchema (col1 NotExists)";
        String error = "Error starting statement: Nestable type configuration encountered an unexpected property type name 'NotExists' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema SomeSchema (col1 NotExists)]";

        try {
            epService.getEPAdministrator().getDeploymentAdmin().deploy(makeModule("test", text), null);
            fail();
        } catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(error, first.getInner().getMessage());
        }

        DeploymentOptions options = new DeploymentOptions();
        options.setCompileOnly(true);
        assertNull(epService.getEPAdministrator().getDeploymentAdmin().deploy(makeModule("test", text), options));
    }

    private void runAssertionFlagValidateOnly(EPServiceProvider epService) throws Exception {
        undeployRemoveAll(epService);
        epService.getEPAdministrator().destroyAllStatements();

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') select * from MySchemaTwo";
        Module module = makeModule("mymodule.two", textOne, textTwo);

        DeploymentOptions options = new DeploymentOptions();
        options.setValidateOnly(true);
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);
        assertNull(result);
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);

        undeployRemoveAll(epService);
    }

    private void runAssertionFlagIsolated(EPServiceProvider epService) throws Exception {

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') select * from MySchemaTwo";
        Module module = makeModule("mymodule.two", textOne, textTwo);

        DeploymentOptions options = new DeploymentOptions();
        options.setIsolatedServiceProvider("iso1");
        DeploymentResult result = epService.getEPAdministrator().getDeploymentAdmin().deploy(module, options);
        assertNotNull(result);
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals("iso1", epService.getEPAdministrator().getStatement("A").getServiceIsolated());
        assertEquals("iso1", epService.getEPAdministrator().getStatement("B").getServiceIsolated());

        undeployRemoveAll(epService);
    }

    private void runAssertionFlagUndeployNoDestroy(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        DeploymentResult resultOne = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy("@Name('S0') select * from SupportBean");
        DeploymentResult resultTwo = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy("@Name('S1') select * from SupportBean");

        UndeploymentOptions options = new UndeploymentOptions();
        options.setDestroyStatements(false);
        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(resultOne.getDeploymentId(), options);
        assertNotNull(epService.getEPAdministrator().getStatement("S0"));

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(resultTwo.getDeploymentId(), options);
        assertNotNull(epService.getEPAdministrator().getStatement("S1"));
    }

    private Module makeModule(String name, String... statements) {

        ModuleItem[] items = new ModuleItem[statements.length];
        for (int i = 0; i < statements.length; i++) {
            items[i] = new ModuleItem(statements[i], false, 0, 0, 0);
        }
        return new Module(name, null, new HashSet<>(), new HashSet<>(), Arrays.asList(items), null);
    }

    private void undeployRemoveAll(EPServiceProvider epService) throws DeploymentNotFoundException {
        DeploymentInformation[] deployments = epService.getEPAdministrator().getDeploymentAdmin().getDeploymentInformation();
        for (DeploymentInformation deployment : deployments) {
            epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deployment.getDeploymentId());
        }
    }
}
