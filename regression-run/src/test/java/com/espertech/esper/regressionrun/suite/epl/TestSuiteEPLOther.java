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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.epl.other.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.OrderBean;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TestSuiteEPLOther extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLOtherCreateSchema() {
        RegressionRunner.run(session, EPLOtherCreateSchema.executions());
    }

    public void testEPLOtherCreateIndex() {
        RegressionRunner.run(session, EPLOtherCreateIndex.executions());
    }

    public void testEPLOtherSplitStream() {
        RegressionRunner.run(session, EPLOtherSplitStream.executions());
    }

    public void testEPLOtherUpdateIStream() {
        RegressionRunner.run(session, EPLOtherUpdateIStream.executions());
    }

    public void testEPLOtherAsKeywordBacktick() {
        RegressionRunner.run(session, EPLOtherAsKeywordBacktick.executions());
    }

    public void testEPLOtherComments() {
        RegressionRunner.run(session, new EPLOtherComments());
    }

    public void testEPLOtherCreateExpression() {
        RegressionRunner.run(session, EPLOtherCreateExpression.executions());
    }

    public void testEPLOtherDistinct() {
        RegressionRunner.run(session, EPLOtherDistinct.executions());
    }

    public void testEPLOtherForGroupDelivery() {
        RegressionRunner.run(session, EPLOtherForGroupDelivery.executions());
    }

    public void testEPLOtherInvalid() {
        RegressionRunner.run(session, EPLOtherInvalid.executions());
    }

    public void testEPLOtherIStreamRStreamKeywords() {
        RegressionRunner.run(session, EPLOtherIStreamRStreamKeywords.executions());
    }

    public void testEPLOtherLiteralConstants() {
        RegressionRunner.run(session, new EPLOtherLiteralConstants());
    }

    public void testEPLOtherUnaryMinus() {
        RegressionRunner.run(session, new EPLOtherUnaryMinus());
    }

    public void testEPLOtherStaticFunctions() {
        RegressionRunner.run(session, EPLOtherStaticFunctions.executions());
    }

    public void testEPLOtherSelectExpr() {
        RegressionRunner.run(session, EPLOtherSelectExpr.executions());
    }

    public void testEPLOtherSelectWildcardWAdditional() {
        RegressionRunner.run(session, EPLOtherSelectWildcardWAdditional.executions());
    }

    public void testEPLOtherSelectExprSQLCompat() {
        RegressionRunner.run(session, EPLOtherSelectExprSQLCompat.executions());
    }

    public void testEPLOtherSelectExprEventBeanAnnotation() {
        RegressionRunner.run(session, EPLOtherSelectExprEventBeanAnnotation.executions());
    }

    public void testEPLOtherSelectExprStreamSelector() {
        RegressionRunner.run(session, EPLOtherSelectExprStreamSelector.executions());
    }

    public void testEPLOtherPlanExcludeHint() {
        RegressionRunner.run(session, EPLOtherPlanExcludeHint.executions());
    }

    public void testEPLOtherPlanInKeywordQuery() {
        RegressionRunner.run(session, EPLOtherPlanInKeywordQuery.executions());
    }

    public void testEPLOtherStreamExpr() {
        RegressionRunner.run(session, EPLOtherStreamExpr.executions());
    }

    public void testEPLOtherSelectJoin() {
        RegressionRunner.run(session, EPLOtherSelectJoin.executions());
    }

    public void testEPLOtherPatternEventProperties() {
        RegressionRunner.run(session, EPLOtherPatternEventProperties.executions());
    }

    public void testEPLOtherPatternQueries() {
        RegressionRunner.run(session, EPLOtherPatternQueries.executions());
    }

    public void testEPLOtherNestedClass() {
        RegressionRunner.run(session, EPLOtherNestedClass.executions());
    }

    private static void configure(Configuration configuration) {
        configuration.getCommon().addEventType("ObjectEvent", Object.class);

        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBean_S2.class, SupportBeanSourceEvent.class, OrderBean.class,
            SupportBeanReadOnly.class, SupportBeanErrorTestingOne.class, OrderBean.class,
            SupportCollection.class, SupportBean_A.class, SupportBean_B.class, SupportBean_N.class,
            SupportChainTop.class, SupportTemperatureBean.class, SupportBeanKeywords.class,
            SupportBeanSimple.class, SupportBeanStaticOuter.class, SupportMarketDataBean.class,
            SupportBeanComplexProps.class, SupportBeanCombinedProps.class, SupportEventWithIntArray.class,
            SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> myMapTypeInv = new HashMap<>();
        myMapTypeInv.put("p0", long.class);
        myMapTypeInv.put("p1", long.class);
        myMapTypeInv.put("p2", long.class);
        myMapTypeInv.put("p3", String.class);
        configuration.getCommon().addEventType("MyMapTypeInv", myMapTypeInv);

        Map<String, Object> myMapTypeII = new HashMap<>();
        myMapTypeII.put("p0", long.class);
        myMapTypeII.put("p1", long.class);
        myMapTypeII.put("p2", long.class);
        configuration.getCommon().addEventType("MyMapTypeII", myMapTypeII);

        Map<String, Object> myMapTypeIDB = new HashMap<>();
        myMapTypeIDB.put("p0", String.class);
        myMapTypeIDB.put("p1", String.class);
        configuration.getCommon().addEventType("MyMapTypeIDB", myMapTypeIDB);

        Map<String, Object> myMapTypeNW = new HashMap<>();
        myMapTypeNW.put("p0", String.class);
        myMapTypeNW.put("p1", String.class);
        configuration.getCommon().addEventType("MyMapTypeNW", myMapTypeNW);

        Map<String, Object> myMapTypeSR = new HashMap<String, Object>();
        myMapTypeSR.put("p0", String.class);
        myMapTypeSR.put("p1", String.class);
        configuration.getCommon().addEventType("MyMapTypeSR", myMapTypeSR);

        Map<String, Object> myMapTypeSODA = new HashMap<>();
        myMapTypeSODA.put("p0", String.class);
        myMapTypeSODA.put("p1", String.class);
        configuration.getCommon().addEventType("MyMapTypeSODA", myMapTypeSODA);

        ConfigurationCommonEventTypeXMLDOM configXML = new ConfigurationCommonEventTypeXMLDOM();
        configXML.setRootElementName("MyXMLEvent");
        configuration.getCommon().addEventType("MyXmlEvent", configXML);

        ConfigurationCommonEventTypeXMLDOM config = new ConfigurationCommonEventTypeXMLDOM();
        config.setRootElementName("simpleEvent");
        configuration.getCommon().addEventType("MyXMLEvent", config);

        Map<String, Object> myMapTypeSelect = new HashMap<>();
        myMapTypeSelect.put("s0", String.class);
        myMapTypeSelect.put("s1", int.class);
        configuration.getCommon().addEventType("MyMapTypeSelect", myMapTypeSelect);

        Map<String, Object> myMapTypeWhere = new HashMap<>();
        myMapTypeWhere.put("w0", int.class);
        configuration.getCommon().addEventType("MyMapTypeWhere", myMapTypeWhere);

        Map<String, Object> myMapTypeUO = new HashMap<>();
        myMapTypeUO.put("s0", String.class);
        myMapTypeUO.put("s1", int.class);
        configuration.getCommon().addEventType("MyMapTypeUO", myMapTypeUO);

        ConfigurationCommonEventTypeBean legacy = new ConfigurationCommonEventTypeBean();
        legacy.setCopyMethod("myCopyMethod");
        configuration.getCommon().addEventType("SupportBeanCopyMethod", SupportBeanCopyMethod.class.getName(), legacy);

        Map<String, Object> defMapTypeKVDistinct = new HashMap<>();
        defMapTypeKVDistinct.put("k1", String.class);
        defMapTypeKVDistinct.put("v1", int.class);
        configuration.getCommon().addEventType("MyMapTypeKVDistinct", defMapTypeKVDistinct);

        Map<String, Object> typeMap = new HashMap<>();
        typeMap.put("int", Integer.class);
        typeMap.put("theString", String.class);
        configuration.getCommon().addEventType("MyMapEventIntString", typeMap);

        configuration.getCommon().addEventType("MapTypeEmpty", new HashMap<>());

        ConfigurationCommonEventTypeXMLDOM testXMLNoSchemaType = new ConfigurationCommonEventTypeXMLDOM();
        testXMLNoSchemaType.setRootElementName("myevent");
        configuration.getCommon().addEventType("TestXMLNoSchemaType", testXMLNoSchemaType);

        Map<String, Object> myConfiguredMape = new HashMap<>();
        myConfiguredMape.put("bean", "SupportBean");
        myConfiguredMape.put("beanarray", "SupportBean_S0[]");
        configuration.getCommon().addEventType("MyConfiguredMap", myConfiguredMape);

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
        configuration.getRuntime().getExecution().setPrioritized(true);

        configuration.getCommon().addVariable("myvar", Integer.class, 10);

        configuration.getCommon().addImport("java.beans.EventHandler");
        configuration.getCommon().addImport("java.sql.*");
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());
        configuration.getCommon().addImport(EPLOtherStaticFunctions.LevelZero.class);
        configuration.getCommon().addImport(SupportChainTop.class);
        configuration.getCommon().addImport(EPLOtherStaticFunctions.NullPrimitive.class);
        configuration.getCommon().addImport(EPLOtherStaticFunctions.PrimitiveConversionLib.class);
        configuration.getCommon().addImport(Rectangle.class.getPackage().getName() + ".*");

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getCompiler().addPlugInSingleRowFunction("sleepme", SupportStaticMethodLib.class.getName(), "sleep", ConfigurationCompilerPlugInSingleRowFunction.ValueCache.ENABLED);
    }
}
