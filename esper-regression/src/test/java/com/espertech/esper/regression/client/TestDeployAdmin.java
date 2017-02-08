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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.deploy.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.io.InputStream;
import java.util.*;

public class TestDeployAdmin extends TestCase
{
    private static String newline = System.getProperty("line.separator");

    private EPServiceProvider epService;
    private EPDeploymentAdmin deploymentAdmin;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        deploymentAdmin = epService.getEPAdministrator().getDeploymentAdmin();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
        deploymentAdmin = null;
    }

    public void testUserObjectAndStatementNameResolver() throws Exception {
        Module module = deploymentAdmin.parse("select * from java.lang.Object where 1=2; select * from java.lang.Object where 3=4;");
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

        deploymentAdmin.deploy(module, options);

        assertEquals(100, epService.getEPAdministrator().getStatement("StmtOne").getUserObject());
        assertEquals(200, epService.getEPAdministrator().getStatement("StmtTwo").getUserObject());
    }

    public void testExplicitDeploymentId() throws Exception {
        // try module-add
        Module module = deploymentAdmin.parse("select * from java.lang.Object");
        deploymentAdmin.add(module, "ABC01");
        assertEquals(DeploymentState.UNDEPLOYED, deploymentAdmin.getDeployment("ABC01").getState());
        assertEquals(1, deploymentAdmin.getDeployments().length);

        deploymentAdmin.deploy("ABC01", null);
        assertEquals(DeploymentState.DEPLOYED, deploymentAdmin.getDeployment("ABC01").getState());

        try {
            deploymentAdmin.add(module, "ABC01");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Assigned deployment id 'ABC01' is already in use", ex.getMessage());
        }
        deploymentAdmin.undeployRemove("ABC01");
        assertEquals(0, deploymentAdmin.getDeployments().length);

        // try module-deploy
        Module moduleTwo = deploymentAdmin.parse("select * from java.lang.Object");
        deploymentAdmin.deploy(moduleTwo, null, "ABC02");
        assertEquals(DeploymentState.DEPLOYED, deploymentAdmin.getDeployment("ABC02").getState());
        assertEquals(1, deploymentAdmin.getDeployments().length);

        try {
            deploymentAdmin.add(module, "ABC02");
            fail();
        }
        catch (IllegalArgumentException ex) {
            assertEquals("Assigned deployment id 'ABC02' is already in use", ex.getMessage());
        }
        deploymentAdmin.undeployRemove("ABC02");
        assertEquals(0, deploymentAdmin.getDeployments().length);
    }

    public void testTransition() throws Exception {

        // add module
        Module module = makeModule("com.testit", "create schema S1 as (col1 int)");
        String deploymentId = deploymentAdmin.add(module);
        DeploymentInformation originalInfo = deploymentAdmin.getDeployment(deploymentId);
        Calendar addedDate = originalInfo.getAddedDate();
        Calendar lastUpdDate = originalInfo.getLastUpdateDate();
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());
        assertEquals("com.testit", originalInfo.getModule().getName());
        assertEquals(0, originalInfo.getItems().length);

        // deploy added module
        DeploymentResult result = deploymentAdmin.deploy(deploymentId, null);
        assertEquals(deploymentId, result.getDeploymentId());
        DeploymentInformation info = deploymentAdmin.getDeployment(deploymentId);
        assertEquals(DeploymentState.DEPLOYED, info.getState());
        assertEquals("com.testit", info.getModule().getName());
        assertEquals(addedDate, info.getAddedDate());
        assertTrue(info.getLastUpdateDate().getTimeInMillis() - lastUpdDate.getTimeInMillis() < 5000);
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());

        // undeploy module
        deploymentAdmin.undeploy(deploymentId);
        assertEquals(deploymentId, result.getDeploymentId());
        info = deploymentAdmin.getDeployment(deploymentId);
        assertEquals(DeploymentState.UNDEPLOYED, info.getState());
        assertEquals("com.testit", info.getModule().getName());
        assertEquals(addedDate, info.getAddedDate());
        assertTrue(info.getLastUpdateDate().getTimeInMillis() - lastUpdDate.getTimeInMillis() < 5000);
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());

        // remove module
        deploymentAdmin.remove(deploymentId);
        assertNull(deploymentAdmin.getDeployment(deploymentId));
        assertEquals(DeploymentState.UNDEPLOYED, originalInfo.getState());
    }

    public void testTransitionInvalid() throws Exception {

        // invalid from deployed state
        Module module = makeModule("com.testit", "create schema S1 as (col1 int)");
        DeploymentResult deploymentResult = deploymentAdmin.deploy(module, null);
        try {
            deploymentAdmin.deploy(deploymentResult.getDeploymentId(), null);
            fail();
        }
        catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is already in deployed state"));
        }

        try {
            deploymentAdmin.remove(deploymentResult.getDeploymentId());
            fail();
        }
        catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is in deployed state, please undeploy first"));
        }

        // invalid from undeployed state
        module = makeModule("com.testit", "create schema S1 as (col1 int)");
        String deploymentId = deploymentAdmin.add(module);
        try {
            deploymentAdmin.undeploy(deploymentId);
            fail();
        }
        catch (DeploymentStateException ex) {
            assertTrue(ex.getMessage().contains("is already in undeployed state"));
        }
        deploymentAdmin.undeployRemove(deploymentId);
        assertNull(deploymentAdmin.getDeployment(deploymentId));

        // not found
        assertNull(deploymentAdmin.getDeployment("123"));
        try {
            deploymentAdmin.deploy("123", null);
            fail();
        }
        catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            deploymentAdmin.undeploy("123");
            fail();
        }
        catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            deploymentAdmin.remove("123");
            fail();
        }
        catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }

        try {
            deploymentAdmin.undeployRemove("123");
            fail();
        }
        catch (DeploymentNotFoundException ex) {
            assertEquals("Deployment by id '123' could not be found", ex.getMessage());
        }
    }

    public void testDeployImports() throws Exception {
        Module module = makeModule("com.testit", "create schema S1 as SupportBean", "@Name('A') select SupportStaticMethodLib.plusOne(intPrimitive) as val from S1");
        module.getImports().add(SupportBean.class.getName());
        module.getImports().add(SupportStaticMethodLib.class.getPackage().getName() + ".*");
        assertFalse(deploymentAdmin.isDeployed("com.testit"));
        deploymentAdmin.deploy(module, null);
        assertTrue(deploymentAdmin.isDeployed("com.testit"));
        epService.getEPAdministrator().getStatement("A").addListener(listener);
        
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        assertEquals(5, listener.assertOneGetNewAndReset().get("val"));
    }

    public void testDeploySingle() throws Exception {
        Module module = deploymentAdmin.read("regression/test_module_9.epl");
        DeploymentResult result = deploymentAdmin.deploy(module, new DeploymentOptions());

        assertNotNull(result.getDeploymentId());
        assertEquals(2, result.getStatements().size());
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals("@Name(\"StmtOne\")" + newline +
                "create schema MyEvent(id String, val1 int, val2 int)", epService.getEPAdministrator().getStatement("StmtOne").getText());
        assertEquals("@Name(\"StmtTwo\")" + newline +
                "select * from MyEvent", epService.getEPAdministrator().getStatement("StmtTwo").getText());

        assertEquals(1, deploymentAdmin.getDeployments().length);
        assertEquals(result.getDeploymentId(), deploymentAdmin.getDeployments()[0]);

        // test deploy with variable
        String moduleStr = "create variable integer snapshotOutputSecs = 10; " +
                "create schema foo as (bar string); " +
                "select bar from foo output snapshot every snapshotOutputSecs seconds;";
        deploymentAdmin.parseDeploy(moduleStr);
    }

    public void testLineNumberAndComments() throws Exception {
        String moduleText = newline + newline + "select * from ABC;" +
                            newline + "select * from DEF";
        
        Module module = deploymentAdmin.parse(moduleText);
        assertEquals(2, module.getItems().size());
        assertEquals(3, module.getItems().get(0).getLineNumber());
        assertEquals(4, module.getItems().get(1).getLineNumber());

        module = deploymentAdmin.parse("/* abc */");
		deploymentAdmin.deploy(module, new DeploymentOptions());

        module = deploymentAdmin.parse("select * from java.lang.Object; \r\n/* abc */\r\n");
		deploymentAdmin.deploy(module, new DeploymentOptions());
    }

    public void testShortcutReadDeploy() throws Exception {

        String resource = "regression/test_module_12.epl";
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        assertNotNull(input);
        DeploymentResult resultOne = deploymentAdmin.readDeploy(input, null, null, null);
        deploymentAdmin.undeployRemove(resultOne.getDeploymentId());
        assertNull(deploymentAdmin.getDeployment(resultOne.getDeploymentId()));

        resultOne = deploymentAdmin.readDeploy(resource, "uri1", "archive1", "obj1");
        assertEquals("regression.test", deploymentAdmin.getDeployment(resultOne.getDeploymentId()).getModule().getName());
        assertEquals(2, resultOne.getStatements().size());
        assertEquals("create schema MyType(col1 integer)", resultOne.getStatements().get(0).getText());
        assertTrue(deploymentAdmin.isDeployed("regression.test"));
        assertEquals("module regression.test;" + newline + newline +
                "create schema MyType(col1 integer);" + newline +
                "select * from MyType;" + newline, deploymentAdmin.getDeployment(resultOne.getDeploymentId()).getModule().getModuleText());

        String moduleText = "module regression.test.two;" +
                "uses regression.test;" +
                "create schema MyTypeTwo(col1 integer, col2.col3 string);" +
                "select * from MyTypeTwo;";
        DeploymentResult resultTwo = deploymentAdmin.parseDeploy(moduleText, "uri2", "archive2", "obj2");
        DeploymentInformation[] infos = deploymentAdmin.getDeploymentInformation();
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
    }

    public void testDeployUndeploy() throws Exception {
        Module moduleOne = makeModule("mymodule.one", "@Name('A1') create schema MySchemaOne (col1 int)", "@Name('B1') select * from MySchemaOne");
        DeploymentResult resultOne = deploymentAdmin.deploy(moduleOne, new DeploymentOptions());
        assertEquals(2, resultOne.getStatements().size());
        assertTrue(deploymentAdmin.isDeployed("mymodule.one"));

        Module moduleTwo = makeModule("mymodule.two", "@Name('A2') create schema MySchemaTwo (col1 int)", "@Name('B2') select * from MySchemaTwo");
        moduleTwo.setUserObject(100L);
        moduleTwo.setArchiveName("archive");
        DeploymentResult resultTwo = deploymentAdmin.deploy(moduleTwo, new DeploymentOptions());
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
        
        UndeploymentResult result = deploymentAdmin.undeployRemove(resultTwo.getDeploymentId());
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals(2, result.getStatementInfo().size());
        assertEquals("A2", result.getStatementInfo().get(0).getStatementName());
        assertEquals("@Name('A2') create schema MySchemaTwo (col1 int)", result.getStatementInfo().get(0).getExpression());
        assertEquals("B2", result.getStatementInfo().get(1).getStatementName());
        assertEquals("@Name('B2') select * from MySchemaTwo", result.getStatementInfo().get(1).getExpression());

        result = deploymentAdmin.undeployRemove(resultOne.getDeploymentId());
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);
        assertEquals(2, result.getStatementInfo().size());
        assertEquals("A1", result.getStatementInfo().get(0).getStatementName());
    }

    public void testInvalidExceptionList() throws Exception {
        Module moduleOne = makeModule("mymodule.one", "create schema MySchemaOne (col1 Wrong)", "create schema MySchemaOne (col2 WrongTwo)");
        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setFailFast(false);
            deploymentAdmin.deploy(moduleOne, options);
            fail();
        }
        catch (DeploymentActionException ex) {
            assertEquals("Deployment failed in module 'mymodule.one' in expression 'create schema MySchemaOne (col1 Wrong)' : Error starting statement: Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col1 Wrong)]", ex.getMessage());
            assertEquals(2,  ex.getExceptions().size());
            assertEquals("create schema MySchemaOne (col1 Wrong)", ex.getExceptions().get(0).getExpression());
            assertEquals("Error starting statement: Nestable type configuration encountered an unexpected property type name 'Wrong' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col1 Wrong)]", ex.getExceptions().get(0).getInner().getMessage());
            assertEquals("create schema MySchemaOne (col2 WrongTwo)", ex.getExceptions().get(1).getExpression());
            assertEquals("Error starting statement: Nestable type configuration encountered an unexpected property type name 'WrongTwo' for property 'col2', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema MySchemaOne (col2 WrongTwo)]", ex.getExceptions().get(1).getInner().getMessage());
        }

        // test newline as part of the failing expression - replaced by space
        try {
            deploymentAdmin.parseDeploy("XX\nX");
            fail();
        }
        catch (DeploymentException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Compilation failed in expression 'XX X' : Incorrect syntax near 'XX' [");
        }
    }

    public void testFlagRollbackFailfastCompile() throws Exception {

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') create schema MySchemaTwo (col1 not_existing_type)";
        String errorTextTwo = "Error starting statement: Nestable type configuration encountered an unexpected property type name 'not_existing_type' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [@Name('B') create schema MySchemaTwo (col1 not_existing_type)]";
        String textThree = "@Name('C') create schema MySchemaTwo (col1 int)";
        Module module = makeModule("mymodule.two", textOne, textTwo, textThree);

        try {
            DeploymentOptions options = new DeploymentOptions();
            deploymentAdmin.deploy(module, options);
            fail();
        }
        catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(textTwo, first.getExpression());
            assertEquals(errorTextTwo, first.getInner().getMessage());
        }
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);

        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setRollbackOnFail(false);
            deploymentAdmin.deploy(module, options);
            fail();
        }
        catch (DeploymentActionException ex) {
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
            deploymentAdmin.deploy(module, options);
            fail();
        }
        catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(textTwo, first.getExpression());
            assertEquals(errorTextTwo, first.getInner().getMessage());
            EPAssertionUtil.assertEqualsExactOrder(new String[]{"A", "C"}, epService.getEPAdministrator().getStatementNames());
        }
    }

    public void testFlagCompileOnly() throws Exception {
        String text = "create schema SomeSchema (col1 NotExists)";
        String error = "Error starting statement: Nestable type configuration encountered an unexpected property type name 'NotExists' for property 'col1', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type [create schema SomeSchema (col1 NotExists)]";

        try {
            deploymentAdmin.deploy(makeModule("test", text), null);
            fail();
        }
        catch (DeploymentActionException ex) {
            assertEquals(1, ex.getExceptions().size());
            DeploymentItemException first = ex.getExceptions().get(0);
            assertEquals(error, first.getInner().getMessage());
        }

        DeploymentOptions options = new DeploymentOptions();
        options.setCompileOnly(true);
        assertNull(deploymentAdmin.deploy(makeModule("test", text), options));
    }

    public void testFlagValidateOnly() throws Exception {

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') select * from MySchemaTwo";
        Module module = makeModule("mymodule.two", textOne, textTwo);

        DeploymentOptions options = new DeploymentOptions();
        options.setValidateOnly(true);
        DeploymentResult result = deploymentAdmin.deploy(module, options);
        assertNull(result);
        assertEquals(0, epService.getEPAdministrator().getStatementNames().length);
    }

    public void testFlagIsolated() throws Exception {

        String textOne = "@Name('A') create schema MySchemaTwo (col1 int)";
        String textTwo = "@Name('B') select * from MySchemaTwo";
        Module module = makeModule("mymodule.two", textOne, textTwo);

        DeploymentOptions options = new DeploymentOptions();
        options.setIsolatedServiceProvider("iso1");
        DeploymentResult result = deploymentAdmin.deploy(module, options);
        assertNotNull(result);
        assertEquals(2, epService.getEPAdministrator().getStatementNames().length);
        assertEquals("iso1", epService.getEPAdministrator().getStatement("A").getServiceIsolated());
        assertEquals("iso1", epService.getEPAdministrator().getStatement("B").getServiceIsolated());
    }

    public void testFlagUndeployNoDestroy() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        DeploymentResult resultOne = deploymentAdmin.parseDeploy("@Name('S0') select * from SupportBean");
        DeploymentResult resultTwo = deploymentAdmin.parseDeploy("@Name('S1') select * from SupportBean");

        UndeploymentOptions options = new UndeploymentOptions();
        options.setDestroyStatements(false);
        deploymentAdmin.undeployRemove(resultOne.getDeploymentId(), options);
        assertNotNull(epService.getEPAdministrator().getStatement("S0"));

        deploymentAdmin.undeploy(resultTwo.getDeploymentId(), options);
        assertNotNull(epService.getEPAdministrator().getStatement("S1"));
    }

    private Module makeModule(String name, String... statements) {

        ModuleItem[] items = new ModuleItem[statements.length];
        for (int i = 0; i < statements.length; i++) {
            items[i] = new ModuleItem(statements[i], false, 0, 0, 0);
        }
        return new Module(name, null, new HashSet<String>(), new HashSet<String>(), Arrays.asList(items), null);
    }
}
