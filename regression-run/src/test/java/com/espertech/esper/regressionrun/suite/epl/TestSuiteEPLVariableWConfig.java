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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.suite.epl.variable.EPLVariableEngineConfigXML;
import com.espertech.esper.regressionlib.suite.epl.variable.EPLVariablesTimer;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationCompiler;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationRuntime;

public class TestSuiteEPLVariableWConfig extends TestCase {

    public void testEPLVariablesTimer() {
        RegressionSession session = RegressionRunner.session();
        Configuration configuration = session.getConfiguration();
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getCommon().addVariable("var1", long.class, "12");
        configuration.getCommon().addVariable("var2", Long.class, "2");
        configuration.getCommon().addVariable("var3", Long.class, null);
        RegressionRunner.run(session, new EPLVariablesTimer());
        session.destroy();
    }

    public void testEPLVariableEngineConfigXML() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<esper-configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../esper-configuration-6-0.xsd\">" +
            "<common><variable name=\"p_1\" type=\"string\" />" +
            "<variable name=\"p_2\" type=\"bool\" initialization-value=\"true\"/>" +
            "<variable name=\"p_3\" type=\"long\" initialization-value=\"10\"/>" +
            "<variable name=\"p_4\" type=\"double\" initialization-value=\"11.1d\"/>" +
            "</common></esper-configuration>";
        Document doc = SupportXML.getDocument(xml);

        RegressionSession session = RegressionRunner.session();
        Configuration configuration = session.getConfiguration();
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.configure(doc);

        RegressionRunner.run(session, new EPLVariableEngineConfigXML());

        session.destroy();
    }

    public void testInvalidConfig() {
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), config -> config.getCommon().addVariable("invalidvar1", Integer.class, "abc"),
            "Failed compiler startup: Error configuring variable 'invalidvar1': Variable 'invalidvar1' of declared type java.lang.Integer cannot be initialized by value 'abc': java.lang.NumberFormatException: For input string: \"abc\"");
        tryInvalidConfigurationRuntime(SupportConfigFactory.getConfiguration(), config -> config.getCommon().addVariable("invalidvar1", Integer.class, "abc"),
            "Failed runtime startup: Error configuring variable 'invalidvar1': Variable 'invalidvar1' of declared type java.lang.Integer cannot be initialized by value 'abc': java.lang.NumberFormatException: For input string: \"abc\"");

        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), config -> config.getCommon().addVariable("invalidvar1", Integer.class, 1.1d),
            "Failed compiler startup: Error configuring variable 'invalidvar1': Variable 'invalidvar1' of declared type java.lang.Integer cannot be initialized by a value of type java.lang.Double");
        tryInvalidConfigurationRuntime(SupportConfigFactory.getConfiguration(), config -> config.getCommon().addVariable("invalidvar1", Integer.class, 1.1d),
            "Failed runtime startup: Error configuring variable 'invalidvar1': Variable 'invalidvar1' of declared type java.lang.Integer cannot be initialized by a value of type java.lang.Double");
    }
}
