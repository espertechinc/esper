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

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.client.soda.StreamSelector;
import com.espertech.esper.regressionlib.suite.epl.other.EPLOtherIStreamRStreamConfigSelectorIRStream;
import com.espertech.esper.regressionlib.suite.epl.other.EPLOtherIStreamRStreamConfigSelectorRStream;
import com.espertech.esper.regressionlib.suite.epl.other.EPLOtherStaticFunctionsNoUDFCache;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportTemperatureBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLOtherWConfig extends TestCase {

    public void testEPLOtherIStreamRStreamConfigSelectorRStream() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getStreamSelection().setDefaultStreamSelector(StreamSelector.RSTREAM_ONLY);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, new EPLOtherIStreamRStreamConfigSelectorRStream());
        session.destroy();
    }

    public void testEPLOtherIStreamRStreamConfigSelectorIRStream() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getStreamSelection().setDefaultStreamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, new EPLOtherIStreamRStreamConfigSelectorIRStream());
        session.destroy();
    }

    public void testEPLOtherStaticFunctionsNoUDFCache() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addImport(SupportStaticMethodLib.class.getName());
        session.getConfiguration().getCompiler().addPlugInSingleRowFunction("sleepme", SupportStaticMethodLib.class.getName(), "sleep", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.ENABLED);
        session.getConfiguration().getCompiler().getExpression().setUdfCache(false);
        session.getConfiguration().getCommon().addEventType(SupportTemperatureBean.class);
        RegressionRunner.run(session, new EPLOtherStaticFunctionsNoUDFCache());
        session.destroy();
    }
}
