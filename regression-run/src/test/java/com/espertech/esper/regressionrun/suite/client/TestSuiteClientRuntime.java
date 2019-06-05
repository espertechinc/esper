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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.suite.client.runtime.*;
import com.espertech.esper.regressionlib.suite.event.infra.EventInfraPropertyUnderlyingSimple;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.client.MyAnnotationAPIEventType;
import com.espertech.esper.regressionlib.support.client.MyAnnotationNestableValues;
import com.espertech.esper.regressionlib.support.client.MyAnnotationValueEnum;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportInvalidAggregationFunctionForge;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;

import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_KEY;
import static com.espertech.esper.common.internal.avro.core.AvroConstant.PROP_JAVA_STRING_VALUE;

public class TestSuiteClientRuntime extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientRuntimeStatementAnnotation() {
        RegressionRunner.run(session, ClientRuntimeStatementAnnotation.executions());
    }

    public void testClientRuntimeStatementName() {
        RegressionRunner.run(session, ClientRuntimeStatementName.executions());
    }

    public void testClientRuntimeSubscriber() {
        RegressionRunner.run(session, ClientRuntimeSubscriber.executions());
    }

    public void testClientRuntimeItself() {
        RegressionRunner.run(session, ClientRuntimeItself.executions());
    }

    public void testClientRuntimeRuntimeProvider() {
        RegressionRunner.run(session, ClientRuntimeRuntimeProvider.executions());
    }

    public void testClientRuntimeEPStatement() {
        RegressionRunner.run(session, ClientRuntimeEPStatement.executions());
    }

    public void testClientRuntimeExceptionHandler() {
        RegressionRunner.run(session, ClientRuntimeExceptionHandler.executions());
    }

    public void testClientRuntimePriorityAndDropInstructions() {
        RegressionRunner.run(session, ClientRuntimePriorityAndDropInstructions.executions());
    }

    public void testClientRuntimeSolutionPatternPortScan() {
        RegressionRunner.run(session, ClientRuntimeSolutionPatternPortScan.executions());
    }

    public void testClientRuntimeUnmatchedListener() {
        RegressionRunner.run(session, ClientRuntimeUnmatchedListener.executions());
    }

    public void testClientRuntimeTimeControl() {
        RegressionRunner.run(session, ClientRuntimeTimeControl.executions());
    }

    public void testClientRuntimeListener() {
        RegressionRunner.run(session, ClientRuntimeListener.executions());
    }

    private void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanComplexProps.class, SupportBeanWithEnum.class, SupportMarketDataBean.class,
            SupportMarkerInterface.class, SupportBean_A.class, SupportBean_B.class, SupportBean_C.class, SupportBean_D.class, SupportBean_S0.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addEventType(ClientRuntimeListener.MAP_TYPENAME, Collections.singletonMap("ident", "string"));
        configuration.getCommon().addEventType(ClientRuntimeListener.OA_TYPENAME, new String[]{"ident"}, new Class[]{String.class});
        configuration.getCommon().addEventType(ClientRuntimeListener.BEAN_TYPENAME, ClientRuntimeListener.RoutedBeanEvent.class);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("myevent");
        String schema = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://www.espertech.com/schema/esper\" elementFormDefault=\"qualified\" xmlns:esper=\"http://www.espertech.com/schema/esper\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "\t<xs:element name=\"myevent\">\n" +
            "\t\t<xs:complexType>\n" +
            "\t\t\t<xs:attribute name=\"ident\" type=\"xs:string\" use=\"required\"/>\n" +
            "\t\t</xs:complexType>\n" +
            "\t</xs:element>\n" +
            "</xs:schema>\n";
        eventTypeMeta.setSchemaText(schema);
        configuration.getCommon().addEventType(ClientRuntimeListener.XML_TYPENAME, eventTypeMeta);

        Schema avroSchema = SchemaBuilder.record(EventInfraPropertyUnderlyingSimple.AVRO_TYPENAME)
            .fields()
            .name("ident").type().stringBuilder().prop(PROP_JAVA_STRING_KEY, PROP_JAVA_STRING_VALUE).endString().noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro(ClientRuntimeListener.AVRO_TYPENAME, new ConfigurationCommonEventTypeAvro(avroSchema));

        configuration.getCommon().addImport(MyAnnotationValueEnum.class);
        configuration.getCommon().addImport(MyAnnotationNestableValues.class.getPackage().getName() + ".*");
        configuration.getCommon().addAnnotationImport(SupportEnum.class);

        configuration.getCompiler().addPlugInAggregationFunctionForge("myinvalidagg", SupportInvalidAggregationFunctionForge.class.getName());

        // add service (not serializable, transient configuration)
        HashMap<String, Object> transients = new HashMap<String, Object>();
        transients.put(ClientRuntimeItself.TEST_SERVICE_NAME, new ClientRuntimeItself.MyLocalService(ClientRuntimeItself.TEST_SECRET_VALUE));
        configuration.getCommon().setTransientConfiguration(transients);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getRuntime().getExecution().setPrioritized(true);
    }
}
