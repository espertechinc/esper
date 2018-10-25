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
package com.espertech.esper.regressionrun.suite.event;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.util.FileUtil;
import com.espertech.esper.regressionlib.suite.event.xml.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.util.SupportXPathFunctionResolver;
import com.espertech.esper.regressionlib.support.util.SupportXPathVariableResolver;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import javax.xml.xpath.XPathConstants;
import java.io.InputStream;

public class TestSuiteEventXML extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEventXMLNoSchemaEventTransposeXPathConfigured() {
        RegressionRunner.run(session, new EventXMLNoSchemaEventTransposeXPathConfigured());
    }

    public void testEventXMLNoSchemaEventTransposeXPathGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaEventTransposeXPathGetter());
    }

    public void testEventXMLNoSchemaEventTransposeDOM() {
        RegressionRunner.run(session, new EventXMLNoSchemaEventTransposeDOM());
    }

    public void testEventXMLSchemaPropertyDynamicXPathGetter() {
        RegressionRunner.run(session, new EventXMLSchemaPropertyDynamicXPathGetter());
    }

    public void testEventXMLSchemaEventObservationDOM() {
        RegressionRunner.run(session, new EventXMLSchemaEventObservationDOM());
    }

    public void testEventXMLSchemaEventObservationXPath() {
        RegressionRunner.run(session, new EventXMLSchemaEventObservationXPath());
    }

    public void testEventXMLSchemaEventSender() {
        RegressionRunner.run(session, new EventXMLSchemaEventSender());
    }

    public void testEventXMLSchemaEventTransposeDOMGetter() {
        RegressionRunner.run(session, new EventXMLSchemaEventTransposeDOMGetter());
    }

    public void testEventXMLSchemaEventTransposeXPathConfigured() {
        RegressionRunner.run(session, new EventXMLSchemaEventTransposeXPathConfigured());
    }

    public void testEventXMLSchemaEventTransposeXPathGetter() {
        RegressionRunner.run(session, new EventXMLSchemaEventTransposeXPathGetter());
    }

    public void testEventXMLSchemaEventTransposePrimitiveArray() {
        RegressionRunner.run(session, new EventXMLSchemaEventTransposePrimitiveArray());
    }

    public void testEventXMLSchemaEventTransposeNodeArray() {
        RegressionRunner.run(session, new EventXMLSchemaEventTransposeNodeArray());
    }

    public void testEventXMLSchemaEventTypes() {
        RegressionRunner.run(session, new EventXMLSchemaEventTypes());
    }

    public void testEventXMLSchemaWithRestriction() {
        RegressionRunner.run(session, new EventXMLSchemaWithRestriction());
    }

    public void testEventXMLSchemaWithAll() {
        RegressionRunner.run(session, new EventXMLSchemaWithAll());
    }

    public void testEventXMLSchemaDOMGetterBacked() {
        RegressionRunner.run(session, new EventXMLSchemaDOMGetterBacked());
    }

    public void testEventXMLSchemaXPathBacked() {
        RegressionRunner.run(session, new EventXMLSchemaXPathBacked());
    }

    public void testEventXMLSchemaInvalid() {
        RegressionRunner.run(session, new EventXMLSchemaInvalid());
    }

    public void testEventXMLNoSchemaVariableAndDotMethodResolution() {
        RegressionRunner.run(session, new EventXMLNoSchemaVariableAndDotMethodResolution());
    }

    public void testEventXMLNoSchemaSimpleXMLXPathProperties() {
        RegressionRunner.run(session, new EventXMLNoSchemaSimpleXMLXPathProperties());
    }

    public void testEventXMLNoSchemaSimpleXMLDOMGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaSimpleXMLDOMGetter());
    }

    public void testEventXMLNoSchemaSimpleXMLXPathGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaSimpleXMLXPathGetter());
    }

    public void testEventXMLNoSchemaNestedXMLDOMGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaNestedXMLDOMGetter());
    }

    public void testEventXMLNoSchemaNestedXMLXPathGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaNestedXMLXPathGetter());
    }

    public void testEventXMLNoSchemaDotEscape() {
        RegressionRunner.run(session, new EventXMLNoSchemaDotEscape());
    }

    public void testEventXMLNoSchemaEventXML() {
        RegressionRunner.run(session, new EventXMLNoSchemaEventXML());
    }

    public void testEventXMLNoSchemaElementNode() {
        RegressionRunner.run(session, new EventXMLNoSchemaElementNode());
    }

    public void testEventXMLNoSchemaNamespaceXPathRelative() {
        RegressionRunner.run(session, new EventXMLNoSchemaNamespaceXPathRelative());
    }

    public void testEventXMLNoSchemaNamespaceXPathAbsolute() {
        RegressionRunner.run(session, new EventXMLNoSchemaNamespaceXPathAbsolute());
    }

    public void testEventXMLNoSchemaXPathArray() {
        RegressionRunner.run(session, new EventXMLNoSchemaXPathArray());
    }

    public void testEventXMLNoSchemaPropertyDynamicDOMGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaPropertyDynamicDOMGetter());
    }

    public void testEventXMLNoSchemaPropertyDynamicXPathGetter() {
        RegressionRunner.run(session, new EventXMLNoSchemaPropertyDynamicXPathGetter());
    }

    public void testEventXMLSchemaPropertyDynamicDOMGetter() {
        RegressionRunner.run(session, new EventXMLSchemaPropertyDynamicDOMGetter());
    }

    private static void configure(Configuration configuration) {

        configuration.getCommon().addVariable("var", int.class, 0);

        for (Class clazz : new Class[]{SupportBean.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        String schemaUriSimpleSchema = TestSuiteEventXML.class.getClassLoader().getResource("regression/simpleSchema.xsd").toString();
        String schemaUriTypeTestSchema = TestSuiteEventXML.class.getClassLoader().getResource("regression/typeTestSchema.xsd").toString();
        String schemaUriSimpleSchemaWithAll = TestSuiteEventXML.class.getClassLoader().getResource("regression/simpleSchemaWithAll.xsd").toString();
        String schemaUriSensorEvent = TestSuiteEventXML.class.getClassLoader().getResource("regression/sensorSchema.xsd").toString();

        InputStream schemaStream = TestSuiteEventXML.class.getClassLoader().getResourceAsStream("regression/simpleSchemaWithRestriction.xsd");
        assertNotNull(schemaStream);
        String schemaTextSimpleSchemaWithRestriction = FileUtil.linesToText(FileUtil.readFile(schemaStream));

        ConfigurationCommonEventTypeXMLDOM aEventConfig = new ConfigurationCommonEventTypeXMLDOM();
        aEventConfig.setRootElementName("myroot");
        configuration.getCommon().addEventType("AEvent", aEventConfig);

        ConfigurationCommonEventTypeXMLDOM aEventWithXPath = new ConfigurationCommonEventTypeXMLDOM();
        aEventWithXPath.setRootElementName("a");
        aEventWithXPath.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.getCommon().addEventType("AEventWithXPath", aEventWithXPath);

        ConfigurationCommonEventTypeXMLDOM aEventMoreXPath = new ConfigurationCommonEventTypeXMLDOM();
        aEventMoreXPath.setRootElementName("a");
        aEventMoreXPath.setXPathPropertyExpr(true);
        aEventMoreXPath.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.getCommon().addEventType("AEventMoreXPath", aEventMoreXPath);

        ConfigurationCommonEventTypeXMLDOM desc = new ConfigurationCommonEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "//event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "//event/@uid", XPathConstants.STRING);
        desc.setRootElementName("batch-event");
        configuration.getCommon().addEventType("MyEvent", desc);

        ConfigurationCommonEventTypeXMLDOM myEventSimpleEvent = new ConfigurationCommonEventTypeXMLDOM();
        myEventSimpleEvent.setRootElementName("simpleEvent");
        configuration.getCommon().addEventType("MyEventSimpleEvent", myEventSimpleEvent);

        ConfigurationCommonEventTypeXMLDOM mwEventWXPathExprTrue = new ConfigurationCommonEventTypeXMLDOM();
        mwEventWXPathExprTrue.setRootElementName("simpleEvent");
        mwEventWXPathExprTrue.setXPathPropertyExpr(true);
        configuration.getCommon().addEventType("MyEventWXPathExprTrue", mwEventWXPathExprTrue);

        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        configuration.getCommon().addEventType("TestXMLSchemaType", eventTypeMeta);

        ConfigurationCommonEventTypeXMLDOM rootMeta = new ConfigurationCommonEventTypeXMLDOM();
        rootMeta.setRootElementName("simpleEvent");
        rootMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        rootMeta.addXPathPropertyFragment("nested1simple", "/ss:simpleEvent/ss:nested1", XPathConstants.NODE, "MyNestedEvent");
        rootMeta.addXPathPropertyFragment("nested4array", "//ss:nested4", XPathConstants.NODESET, "MyNestedArrayEvent");
        configuration.getCommon().addEventType("MyXMLEvent", rootMeta);

        ConfigurationCommonEventTypeXMLDOM metaNested = new ConfigurationCommonEventTypeXMLDOM();
        metaNested.setRootElementName("nested1");
        configuration.getCommon().addEventType("MyNestedEvent", metaNested);

        ConfigurationCommonEventTypeXMLDOM metaNestedArray = new ConfigurationCommonEventTypeXMLDOM();
        metaNestedArray.setRootElementName("nested4");
        configuration.getCommon().addEventType("MyNestedArrayEvent", metaNestedArray);

        configuration.getCompiler().getViewResources().setIterableUnbound(true);

        ConfigurationCommonEventTypeXMLDOM testXMLSchemaTypeTXG = new ConfigurationCommonEventTypeXMLDOM();
        testXMLSchemaTypeTXG.setRootElementName("simpleEvent");
        testXMLSchemaTypeTXG.setSchemaResource(schemaUriSimpleSchema);
        testXMLSchemaTypeTXG.setXPathPropertyExpr(true);       // <== note this
        testXMLSchemaTypeTXG.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        configuration.getCommon().addEventType("TestXMLSchemaTypeTXG", testXMLSchemaTypeTXG);

        ConfigurationCommonEventTypeXMLDOM myEventWTypeAndUID = new ConfigurationCommonEventTypeXMLDOM();
        myEventWTypeAndUID.addXPathProperty("event.type", "/event/@type", XPathConstants.STRING);
        myEventWTypeAndUID.addXPathProperty("event.uid", "/event/@uid", XPathConstants.STRING);
        myEventWTypeAndUID.setRootElementName("event");
        configuration.getCommon().addEventType("MyEventWTypeAndUID", myEventWTypeAndUID);

        ConfigurationCommonEventTypeXMLDOM stockQuote = new ConfigurationCommonEventTypeXMLDOM();
        stockQuote.addXPathProperty("symbol_a", "//m0:symbol", XPathConstants.STRING);
        stockQuote.addXPathProperty("symbol_b", "//*[local-name(.) = 'getQuote' and namespace-uri(.) = 'http://services.samples/xsd']", XPathConstants.STRING);
        stockQuote.addXPathProperty("symbol_c", "/m0:getQuote/m0:request/m0:symbol", XPathConstants.STRING);
        stockQuote.setRootElementName("getQuote");
        stockQuote.setDefaultNamespace("http://services.samples/xsd");
        stockQuote.setRootElementNamespace("http://services.samples/xsd");
        stockQuote.addNamespacePrefix("m0", "http://services.samples/xsd");
        stockQuote.setXPathResolvePropertiesAbsolute(true);
        stockQuote.setXPathPropertyExpr(true);
        configuration.getCommon().addEventType("StockQuote", stockQuote);

        ConfigurationCommonEventTypeXMLDOM stockQuoteSimpleConfig = new ConfigurationCommonEventTypeXMLDOM();
        stockQuoteSimpleConfig.setRootElementName("getQuote");
        stockQuoteSimpleConfig.setDefaultNamespace("http://services.samples/xsd");
        stockQuoteSimpleConfig.setRootElementNamespace("http://services.samples/xsd");
        stockQuoteSimpleConfig.addNamespacePrefix("m0", "http://services.samples/xsd");
        stockQuoteSimpleConfig.setXPathResolvePropertiesAbsolute(false);
        stockQuoteSimpleConfig.setXPathPropertyExpr(true);
        configuration.getCommon().addEventType("StockQuoteSimpleConfig", stockQuoteSimpleConfig);

        ConfigurationCommonEventTypeXMLDOM testXMLNoSchemaType = new ConfigurationCommonEventTypeXMLDOM();
        testXMLNoSchemaType.setRootElementName("myevent");
        testXMLNoSchemaType.setXPathPropertyExpr(false);    // <== DOM getter
        configuration.getCommon().addEventType("TestXMLNoSchemaType", testXMLNoSchemaType);

        ConfigurationCommonEventTypeXMLDOM testXMLNoSchemaTypeWXPathPropTrue = new ConfigurationCommonEventTypeXMLDOM();
        testXMLNoSchemaTypeWXPathPropTrue.setRootElementName("myevent");
        testXMLNoSchemaTypeWXPathPropTrue.setXPathPropertyExpr(true);    // <== XPath getter
        configuration.getCommon().addEventType("TestXMLNoSchemaTypeWXPathPropTrue", testXMLNoSchemaTypeWXPathPropTrue);

        ConfigurationCommonEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationCommonEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathElement1", "/myevent/element1", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathCountE21", "count(/myevent/element2/element21)", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrString", "/myevent/element3/@attrString", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/element3/@attrNum", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrBool", "/myevent/element3/@attrBool", XPathConstants.BOOLEAN);
        xmlDOMEventTypeDesc.addXPathProperty("stringCastLong", "/myevent/element3/@attrNum", XPathConstants.STRING, "long");
        xmlDOMEventTypeDesc.addXPathProperty("stringCastDouble", "/myevent/element3/@attrNum", XPathConstants.STRING, "double");
        xmlDOMEventTypeDesc.addXPathProperty("numCastInt", "/myevent/element3/@attrNum", XPathConstants.NUMBER, "int");
        xmlDOMEventTypeDesc.setXPathFunctionResolver(SupportXPathFunctionResolver.class.getName());
        xmlDOMEventTypeDesc.setXPathVariableResolver(SupportXPathVariableResolver.class.getName());
        configuration.getCommon().addEventType("TestXMLNoSchemaTypeWMoreXPath", xmlDOMEventTypeDesc);

        xmlDOMEventTypeDesc = new ConfigurationCommonEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("my.event2");
        configuration.getCommon().addEventType("TestXMLWithDots", xmlDOMEventTypeDesc);

        ConfigurationCommonEventTypeXMLDOM testXMLNoSchemaTypeWNum = new ConfigurationCommonEventTypeXMLDOM();
        testXMLNoSchemaTypeWNum.setRootElementName("myevent");
        testXMLNoSchemaTypeWNum.addXPathProperty("xpathAttrNum", "/myevent/@attrnum", XPathConstants.STRING, "long");
        testXMLNoSchemaTypeWNum.addXPathProperty("xpathAttrNumTwo", "/myevent/@attrnumtwo", XPathConstants.STRING, "long");
        configuration.getCommon().addEventType("TestXMLNoSchemaTypeWNum", testXMLNoSchemaTypeWNum);

        ConfigurationCommonEventTypeXMLDOM event = new ConfigurationCommonEventTypeXMLDOM();
        event.setRootElementName("Event");
        event.addXPathProperty("A", "//Field[@Name='A']/@Value", XPathConstants.NODESET, "String[]");
        configuration.getCommon().addEventType("Event", event);

        configuration.getCommon().addEventType("XMLSchemaConfigOne", getConfigTestType(null, true, schemaUriSimpleSchema));
        configuration.getCommon().addEventType("XMLSchemaConfigTwo", getConfigTestType(null, false, schemaUriSimpleSchema));

        ConfigurationCommonEventTypeXMLDOM typecfg = new ConfigurationCommonEventTypeXMLDOM();
        typecfg.setRootElementName("Sensor");
        typecfg.setSchemaResource(schemaUriSensorEvent);
        configuration.getCompiler().getViewResources().setIterableUnbound(true);
        configuration.getCommon().addEventType("SensorEvent", typecfg);

        ConfigurationCommonEventTypeXMLDOM sensorcfg = new ConfigurationCommonEventTypeXMLDOM();
        sensorcfg.setRootElementName("Sensor");
        sensorcfg.addXPathProperty("countTags", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER);
        sensorcfg.addXPathProperty("countTagsInt", "count(/ss:Sensor/ss:Observation/ss:Tag)", XPathConstants.NUMBER, "int");
        sensorcfg.addNamespacePrefix("ss", "SensorSchema");
        sensorcfg.addXPathProperty("idarray", "//ss:Tag/ss:ID", XPathConstants.NODESET, "String[]");
        sensorcfg.addXPathPropertyFragment("tagArray", "//ss:Tag", XPathConstants.NODESET, "TagEvent");
        sensorcfg.addXPathPropertyFragment("tagOne", "//ss:Tag[position() = 1]", XPathConstants.NODE, "TagEvent");
        sensorcfg.setSchemaResource(schemaUriSensorEvent);
        configuration.getCommon().addEventType("SensorEventWithXPath", sensorcfg);

        ConfigurationCommonEventTypeXMLDOM tagcfg = new ConfigurationCommonEventTypeXMLDOM();
        tagcfg.setRootElementName("//Tag");
        tagcfg.setSchemaResource(schemaUriSensorEvent);
        configuration.getCommon().addEventType("TagEvent", tagcfg);

        ConfigurationCommonEventTypeXMLDOM eventABC = new ConfigurationCommonEventTypeXMLDOM();
        eventABC.setRootElementName("a");
        eventABC.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.getCommon().addEventType("EventABC", eventABC);

        ConfigurationCommonEventTypeXMLDOM bEvent = new ConfigurationCommonEventTypeXMLDOM();
        bEvent.setRootElementName("a");
        bEvent.addXPathProperty("element2", "//c", XPathConstants.STRING);
        bEvent.setEventSenderValidatesRoot(false);
        configuration.getCommon().addEventType("BEvent", bEvent);

        ConfigurationCommonEventTypeXMLDOM simpleEventWSchema = new ConfigurationCommonEventTypeXMLDOM();
        simpleEventWSchema.setRootElementName("simpleEvent");
        simpleEventWSchema.setSchemaResource(schemaUriSimpleSchema);
        // eventTypeMeta.setXPathPropertyExpr(false); <== the default
        configuration.getCommon().addEventType("SimpleEventWSchema", simpleEventWSchema);

        ConfigurationCommonEventTypeXMLDOM abcType = new ConfigurationCommonEventTypeXMLDOM();
        abcType.setRootElementName("simpleEvent");
        abcType.setSchemaResource(schemaUriSimpleSchema);
        configuration.getCommon().addEventType("ABCType", abcType);

        ConfigurationCommonEventTypeXMLDOM testNested2 = new ConfigurationCommonEventTypeXMLDOM();
        testNested2.setRootElementName("//nested2");
        testNested2.setSchemaResource(schemaUriSimpleSchema);
        testNested2.setEventSenderValidatesRoot(false);
        configuration.getCommon().addEventType("TestNested2", testNested2);

        ConfigurationCommonEventTypeXMLDOM myXMLEventXPC = new ConfigurationCommonEventTypeXMLDOM();
        myXMLEventXPC.setRootElementName("simpleEvent");
        myXMLEventXPC.setSchemaResource(schemaUriSimpleSchema);
        myXMLEventXPC.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        myXMLEventXPC.addXPathPropertyFragment("nested1simple", "/ss:simpleEvent/ss:nested1", XPathConstants.NODE, "MyNestedEventXPC");
        myXMLEventXPC.addXPathPropertyFragment("nested4array", "//ss:nested4", XPathConstants.NODESET, "MyNestedArrayEventXPC");
        myXMLEventXPC.setAutoFragment(false);
        configuration.getCommon().addEventType("MyXMLEventXPC", myXMLEventXPC);

        ConfigurationCommonEventTypeXMLDOM myNestedEventXPC = new ConfigurationCommonEventTypeXMLDOM();
        myNestedEventXPC.setRootElementName("//nested1");
        myNestedEventXPC.setSchemaResource(schemaUriSimpleSchema);
        myNestedEventXPC.setAutoFragment(false);
        configuration.getCommon().addEventType("MyNestedEventXPC", myNestedEventXPC);

        ConfigurationCommonEventTypeXMLDOM myNestedArrayEventXPC = new ConfigurationCommonEventTypeXMLDOM();
        myNestedArrayEventXPC.setRootElementName("//nested4");
        myNestedArrayEventXPC.setSchemaResource(schemaUriSimpleSchema);
        configuration.getCommon().addEventType("MyNestedArrayEventXPC", myNestedArrayEventXPC);

        ConfigurationCommonEventTypeXMLDOM testXMLSchemaTypeWithSS = new ConfigurationCommonEventTypeXMLDOM();
        testXMLSchemaTypeWithSS.setRootElementName("simpleEvent");
        testXMLSchemaTypeWithSS.setSchemaResource(schemaUriSimpleSchema);
        testXMLSchemaTypeWithSS.setXPathPropertyExpr(true);       // <== note this
        testXMLSchemaTypeWithSS.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        configuration.getCommon().addEventType("TestXMLSchemaTypeWithSS", testXMLSchemaTypeWithSS);

        ConfigurationCommonEventTypeXMLDOM testTypesEvent = new ConfigurationCommonEventTypeXMLDOM();
        testTypesEvent.setRootElementName("typesEvent");
        testTypesEvent.setSchemaResource(schemaUriTypeTestSchema);
        configuration.getCommon().addEventType("TestTypesEvent", testTypesEvent);

        ConfigurationCommonEventTypeXMLDOM myEventWithPrefix = new ConfigurationCommonEventTypeXMLDOM();
        myEventWithPrefix.setRootElementName("simpleEvent");
        myEventWithPrefix.setSchemaResource(schemaUriSimpleSchema);
        myEventWithPrefix.setXPathPropertyExpr(false);
        myEventWithPrefix.setEventSenderValidatesRoot(false);
        myEventWithPrefix.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        myEventWithPrefix.setDefaultNamespace("samples:schemas:simpleSchema");
        configuration.getCommon().addEventType("MyEventWithPrefix", myEventWithPrefix);

        ConfigurationCommonEventTypeXMLDOM myEventWithXPath = new ConfigurationCommonEventTypeXMLDOM();
        myEventWithXPath.setRootElementName("simpleEvent");
        myEventWithXPath.setSchemaResource(schemaUriSimpleSchema);
        myEventWithXPath.setXPathPropertyExpr(true);
        myEventWithXPath.setEventSenderValidatesRoot(false);
        myEventWithXPath.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        myEventWithXPath.setDefaultNamespace("samples:schemas:simpleSchema");
        configuration.getCommon().addEventType("MyEventWithXPath", myEventWithXPath);

        ConfigurationCommonEventTypeXMLDOM pageVisitEvent = new ConfigurationCommonEventTypeXMLDOM();
        pageVisitEvent.setRootElementName("event-page-visit");
        pageVisitEvent.setSchemaResource(schemaUriSimpleSchemaWithAll);
        pageVisitEvent.addNamespacePrefix("ss", "samples:schemas:simpleSchemaWithAll");
        pageVisitEvent.addXPathProperty("url", "/ss:event-page-visit/ss:url", XPathConstants.STRING);
        configuration.getCommon().addEventType("PageVisitEvent", pageVisitEvent);

        ConfigurationCommonEventTypeXMLDOM orderEvent = new ConfigurationCommonEventTypeXMLDOM();
        orderEvent.setRootElementName("order");
        orderEvent.setSchemaText(schemaTextSimpleSchemaWithRestriction);
        configuration.getCommon().addEventType("OrderEvent", orderEvent);
    }

    protected static ConfigurationCommonEventTypeXMLDOM getConfigTestType(String additionalXPathProperty, boolean isUseXPathPropertyExpression, String schemaUriSimpleSchema) {
        ConfigurationCommonEventTypeXMLDOM eventTypeMeta = new ConfigurationCommonEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("simpleEvent");
        eventTypeMeta.setSchemaResource(schemaUriSimpleSchema);
        eventTypeMeta.addNamespacePrefix("ss", "samples:schemas:simpleSchema");
        eventTypeMeta.addXPathProperty("customProp", "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        eventTypeMeta.setXPathPropertyExpr(isUseXPathPropertyExpression);
        if (additionalXPathProperty != null) {
            eventTypeMeta.addXPathProperty(additionalXPathProperty, "count(/ss:simpleEvent/ss:nested3/ss:nested4)", XPathConstants.NUMBER);
        }
        return eventTypeMeta;
    }
}
