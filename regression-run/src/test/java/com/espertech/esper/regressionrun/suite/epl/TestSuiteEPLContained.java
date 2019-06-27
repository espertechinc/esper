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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.avro.support.SupportAvroArrayEvent;
import com.espertech.esper.regressionlib.support.bean.SupportJsonArrayEvent;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.epl.contained.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bookexample.BookDesc;
import com.espertech.esper.regressionlib.support.bookexample.OrderBean;
import com.espertech.esper.regressionlib.support.wordexample.SentenceEvent;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.Map;

public class TestSuiteEPLContained extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLContainedEventSimple() {
        RegressionRunner.run(session, EPLContainedEventSimple.executions());
    }

    public void testEPLContainedEventArray() {
        RegressionRunner.run(session, EPLContainedEventArray.executions());
    }

    public void testEPLContainedEventExample() {
        RegressionRunner.run(session, EPLContainedEventExample.executions());
    }

    public void testEPLContainedEventNested() {
        RegressionRunner.run(session, EPLContainedEventNested.executions());
    }

    public void testEPLContainedEventSplitExpr() {
        RegressionRunner.run(session, EPLContainedEventSplitExpr.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, OrderBean.class, BookDesc.class, SentenceEvent.class,
            SupportStringBeanWithArray.class, SupportBeanArrayCollMap.class, SupportObjectArrayEvent.class,
            SupportCollectionEvent.class, SupportResponseEvent.class, SupportAvroArrayEvent.class, SupportJsonArrayEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> innerMapDef = Collections.singletonMap("p", String.class);
        configuration.getCommon().addEventType("MyInnerMap", innerMapDef);
        Map<String, Object> outerMapDef = Collections.singletonMap("i", "MyInnerMap[]");
        configuration.getCommon().addEventType("MyOuterMap", outerMapDef);

        String[] funcs = "splitSentence,splitSentenceBean,splitWord".split(",");
        for (int i = 0; i < funcs.length; i++) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                String[] methods;
                if (rep.isObjectArrayEvent()) {
                    methods = "splitSentenceMethodReturnObjectArray,splitSentenceBeanMethodReturnObjectArray,splitWordMethodReturnObjectArray".split(",");
                } else if (rep.isMapEvent()) {
                    methods = "splitSentenceMethodReturnMap,splitSentenceBeanMethodReturnMap,splitWordMethodReturnMap".split(",");
                } else if (rep.isAvroEvent()) {
                    methods = "splitSentenceMethodReturnAvro,splitSentenceBeanMethodReturnAvro,splitWordMethodReturnAvro".split(",");
                } else if (rep.isJsonEvent() || rep.isJsonProvidedClassEvent()) {
                    methods = "splitSentenceMethodReturnJson,splitSentenceBeanMethodReturnJson,splitWordMethodReturnJson".split(",");
                } else {
                    throw new IllegalStateException("Unrecognized enum " + rep);
                }

                configuration.getCompiler().addPlugInSingleRowFunction(funcs[i] + "_" + rep.name(), EPLContainedEventSplitExpr.class.getName(), methods[i]);
            }
        }

        ConfigurationCommonEventTypeXMLDOM config = new ConfigurationCommonEventTypeXMLDOM();
        String schemaUri = TestSuiteEPLContained.class.getClassLoader().getResource("regression/mediaOrderSchema.xsd").toString();
        config.setSchemaResource(schemaUri);
        config.setRootElementName("mediaorder");
        configuration.getCommon().addEventType("MediaOrder", config);
        configuration.getCommon().addEventType("Cancel", config);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getCompiler().addPlugInSingleRowFunction("invalidSentence", EPLContainedEventSplitExpr.class.getName(), "invalidSentenceMethod");
        configuration.getCompiler().addPlugInSingleRowFunction("mySplitUDFReturnEventBeanArray", EPLContainedEventSplitExpr.class.getName(), "mySplitUDFReturnEventBeanArray");
    }
}
