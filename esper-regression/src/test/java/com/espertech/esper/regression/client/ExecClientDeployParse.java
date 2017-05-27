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
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ParseException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecClientDeployParse implements RegressionExecution {
    private final static String NEWLINE = System.getProperty("line.separator");

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionParse(epService);
        runAssertionParseFail(epService);
    }

    private void runAssertionParse(EPServiceProvider epService) throws Exception {
        Module module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_4.epl");
        assertModule(module, null, "abd", null, new String[]{
            "select * from ABC",
            "/* Final comment */"
            }, new boolean[]{false, true},
                new int[]{3, 8},
                new int[]{12, 0},
                new int[]{37, 0}
        );

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_1.epl");
        assertModule(module, "abc", "def,jlk", null, new String[]{
            "select * from A",
            "select * from B" + NEWLINE +
                    "where C=d",
            "/* Test ; Comment */" + NEWLINE +
                    "update ';' where B=C",
            "update D"
            }
        );

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_2.epl");
        assertModule(module, "abc.def.hij", "def.hik,jlk.aja", null, new String[]{
            "// Note 4 white spaces after * and before from" + NEWLINE + "select * from A",
            "select * from B",
            "select *    " + NEWLINE + "    from C",
            }
        );

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_3.epl");
        assertModule(module, null, null, null, new String[]{
            "create window ABC",
            "select * from ABC"
            }
        );

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_5.epl");
        assertModule(module, "abd.def", null, null, new String[0]);

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_6.epl");
        assertModule(module, null, null, null, new String[0]);

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_7.epl");
        assertModule(module, null, null, null, new String[0]);

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_8.epl");
        assertModule(module, "def.jfk", null, null, new String[0]);

        module = epService.getEPAdministrator().getDeploymentAdmin().parse("module mymodule; uses mymodule2; import abc; select * from MyEvent;");
        assertModule(module, "mymodule", "mymodule2", "abc", new String[]{
            "select * from MyEvent"
        });

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_11.epl");
        assertModule(module, null, null, "com.mycompany.pck1", new String[0]);

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_10.epl");
        assertModule(module, "abd.def", "one.use,two.use", "com.mycompany.pck1,com.mycompany.*", new String[]{
            "select * from A",
            }
        );

        assertEquals("org.mycompany.events", epService.getEPAdministrator().getDeploymentAdmin().parse("module org.mycompany.events; select * from java.lang.Object;").getName());
        assertEquals("glob.update.me", epService.getEPAdministrator().getDeploymentAdmin().parse("module glob.update.me; select * from java.lang.Object;").getName());
        assertEquals("seconds.until.every.where", epService.getEPAdministrator().getDeploymentAdmin().parse("uses seconds.until.every.where; select * from java.lang.Object;").getUses().toArray()[0]);
        assertEquals("seconds.until.every.where", epService.getEPAdministrator().getDeploymentAdmin().parse("import seconds.until.every.where; select * from java.lang.Object;").getImports().toArray()[0]);

        // Test script square brackets
        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_13.epl");
        assertEquals(1, module.getItems().size());

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_14.epl");
        assertEquals(4, module.getItems().size());

        module = epService.getEPAdministrator().getDeploymentAdmin().read("regression/test_module_15.epl");
        assertEquals(1, module.getItems().size());
    }

    private void runAssertionParseFail(EPServiceProvider epService) throws Exception {
        tryInvalidIO(epService, "regression/dummy_not_there.epl",
                "Failed to find resource 'regression/dummy_not_there.epl' in classpath");

        tryInvalidParse(epService, "regression/test_module_1_fail.epl",
                "Keyword 'module' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_1_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_2_fail.epl",
                "Keyword 'uses' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_2_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_3_fail.epl",
                "Keyword 'module' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_3_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_4_fail.epl",
                "Keyword 'uses' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_4_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_5_fail.epl",
                "Keyword 'import' must be followed by a name or package name (set of names separated by dots) for resource 'regression/test_module_5_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_6_fail.epl",
                "The 'module' keyword must be the first declaration in the module file for resource 'regression/test_module_6_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_7_fail.epl",
                "Duplicate use of the 'module' keyword for resource 'regression/test_module_7_fail.epl'");

        tryInvalidParse(epService, "regression/test_module_8_fail.epl",
                "The 'uses' and 'import' keywords must be the first declaration in the module file or follow the 'module' declaration");

        tryInvalidParse(epService, "regression/test_module_9_fail.epl",
                "The 'uses' and 'import' keywords must be the first declaration in the module file or follow the 'module' declaration");

        // try control chars
        tryInvalidControlCharacters(epService);
    }

    private void tryInvalidControlCharacters(EPServiceProvider epService) throws Exception {
        String epl = "select * \u008F from " + SupportBean.class.getName();
        try {
            epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
            fail();
        } catch (ParseException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Unrecognized control characters found in text, failed to parse text ");
        }
    }

    private void tryInvalidIO(EPServiceProvider epService, String resource, String message) throws ParseException {
        try {
            epService.getEPAdministrator().getDeploymentAdmin().read(resource);
            fail();
        } catch (IOException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void tryInvalidParse(EPServiceProvider epService, String resource, String message) throws IOException {
        try {
            epService.getEPAdministrator().getDeploymentAdmin().read(resource);
            fail();
        } catch (ParseException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    private void assertModule(Module module, String name, String usesCSV, String importsCSV, String[] statements) {
        assertModule(module, name, usesCSV, importsCSV, statements, new boolean[statements.length], new int[statements.length], new int[statements.length], new int[statements.length]);
    }

    private void assertModule(Module module, String name, String usesCSV, String importsCSV, String[] statementsExpected,
                              boolean[] commentsExpected,
                              int[] lineNumsExpected,
                              int[] charStartsExpected,
                              int[] charEndsExpected) {
        assertEquals(name, module.getName());

        String[] expectedUses = usesCSV == null ? new String[0] : usesCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedUses, module.getUses().toArray());

        String[] expectedImports = importsCSV == null ? new String[0] : importsCSV.split(",");
        EPAssertionUtil.assertEqualsExactOrder(expectedImports, module.getImports().toArray());

        String[] stmtsFound = new String[module.getItems().size()];
        boolean[] comments = new boolean[module.getItems().size()];
        int[] lineNumsFound = new int[module.getItems().size()];
        int[] charStartsFound = new int[module.getItems().size()];
        int[] charEndsFound = new int[module.getItems().size()];

        for (int i = 0; i < module.getItems().size(); i++) {
            stmtsFound[i] = module.getItems().get(i).getExpression();
            comments[i] = module.getItems().get(i).isCommentOnly();
            lineNumsFound[i] = module.getItems().get(i).getLineNumber();
            charStartsFound[i] = module.getItems().get(i).getCharPosStart();
            charEndsFound[i] = module.getItems().get(i).getCharPosEnd();
        }

        EPAssertionUtil.assertEqualsExactOrder(statementsExpected, stmtsFound);
        EPAssertionUtil.assertEqualsExactOrder(commentsExpected, comments);

        boolean isCompareLineNums = false;
        for (int l : lineNumsExpected) {
            if (l > 0) {
                isCompareLineNums = true;
            }
        }
        if (isCompareLineNums) {
            EPAssertionUtil.assertEqualsExactOrder(lineNumsExpected, lineNumsFound);
            // Start and end character position can be platform-dependent
            // commented-out: EPAssertionUtil.assertEqualsExactOrder(charStartsExpected, charStartsFound);
            // commented-out: EPAssertionUtil.assertEqualsExactOrder(charEndsExpected, charEndsFound);
        }
    }
}
