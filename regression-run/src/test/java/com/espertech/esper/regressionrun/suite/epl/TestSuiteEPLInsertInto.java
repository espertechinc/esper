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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeAvro;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeXMLDOM;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.suite.epl.insertinto.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.*;

public class TestSuiteEPLInsertInto extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLInsertInto() {
        RegressionRunner.run(session, EPLInsertInto.executions());
    }

    public void testEPLInsertIntoEmptyPropType() {
        RegressionRunner.run(session, EPLInsertIntoEmptyPropType.executions());
    }

    public void testEPLInsertIntoIRStreamFunc() {
        RegressionRunner.run(session, new EPLInsertIntoIRStreamFunc());
    }

    public void testEPLInsertIntoPopulateCreateStream() {
        RegressionRunner.run(session, new EPLInsertIntoPopulateCreateStream());
    }

    public void testEPLInsertIntoPopulateCreateStreamAvro() {
        RegressionRunner.run(session, EPLInsertIntoPopulateCreateStreamAvro.executions());
    }

    public void testEPLInsertIntoPopulateEventTypeColumn() {
        RegressionRunner.run(session, EPLInsertIntoPopulateEventTypeColumn.executions());
    }

    public void testEPLInsertIntoPopulateSingleColByMethodCall() {
        RegressionRunner.run(session, new EPLInsertIntoPopulateSingleColByMethodCall());
    }

    public void testEPLInsertIntoPopulateUnderlying() {
        RegressionRunner.run(session, EPLInsertIntoPopulateUnderlying.executions());
    }

    public void testEPLInsertIntoPopulateUndStreamSelect() {
        RegressionRunner.run(session, EPLInsertIntoPopulateUndStreamSelect.executions());
    }

    public void testEPLInsertIntoTransposePattern() {
        RegressionRunner.run(session, EPLInsertIntoTransposePattern.executions());
    }

    public void testEPLInsertIntoTransposeStream() {
        RegressionRunner.run(session, EPLInsertIntoTransposeStream.executions());
    }

    public void testEPLInsertIntoFromPattern() {
        RegressionRunner.run(session, EPLInsertIntoFromPattern.executions());
    }

    public void testEPLInsertIntoWrapper() {
        RegressionRunner.run(session, EPLInsertIntoWrapper.executions());
    }

    public void testEPLInsertIntoEventTypedColumnFromProp() {
        RegressionRunner.run(session, EPLInsertIntoEventTypedColumnFromProp.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportObjectArrayOneDim.class, SupportBeanSimple.class,
            SupportBean_A.class, SupportRFIDEvent.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportMarketDataBean.class, SupportTemperatureBean.class, SupportBeanComplexProps.class, SupportBeanInterfaceProps.class,
            SupportBeanErrorTestingOne.class, SupportBeanErrorTestingTwo.class, SupportBeanReadOnly.class,
            SupportBeanArrayCollMap.class, SupportBean_N.class, SupportBeanObject.class,
            SupportBeanCtorOne.class, SupportBeanCtorTwo.class, SupportBean_ST0.class, SupportBean_ST1.class,
            SupportEventWithCtorSameType.class, SupportBeanCtorThree.class, SupportBeanCtorOne.class,
            SupportBean_ST0.class, SupportBean_ST1.class, SupportEventWithMapFieldSetter.class,
            SupportBeanNumeric.class, SupportBeanArrayEvent.class, SupportBeanWithThis.class,
            SupportBean_A.class, SupportBean_B.class, SupportEventContainsSupportBean.class
        }) {
            configuration.getCommon().addEventType(clazz);
        }

        Schema avroExistingTypeSchema = record("name").fields()
            .requiredLong("myLong")
            .name("myLongArray").type(array().items(builder().longType())).noDefault()
            .name("myByteArray").type("bytes").noDefault()
            .name("myMap").type(map().values().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro("AvroExistingType", new ConfigurationCommonEventTypeAvro(avroExistingTypeSchema));

        Map<String, Object> mapTypeInfo = new HashMap<>();
        mapTypeInfo.put("one", String.class);
        mapTypeInfo.put("two", String.class);
        configuration.getCommon().addEventType("MapOne", mapTypeInfo);
        configuration.getCommon().addEventType("MapTwo", mapTypeInfo);

        String[] props = {"one", "two"};
        Object[] types = {String.class, String.class};
        configuration.getCommon().addEventType("OAOne", props, types);
        configuration.getCommon().addEventType("OATwo", props, types);

        Schema avroOneAndTwoSchema = record("name").fields().requiredString("one").requiredString("two").endRecord();
        configuration.getCommon().addEventTypeAvro("AvroOne", new ConfigurationCommonEventTypeAvro(avroOneAndTwoSchema));
        configuration.getCommon().addEventTypeAvro("AvroTwo", new ConfigurationCommonEventTypeAvro(avroOneAndTwoSchema));

        ConfigurationCommonEventTypeBean legacySupportBeanString = new ConfigurationCommonEventTypeBean();
        legacySupportBeanString.setFactoryMethod("getInstance");
        configuration.getCommon().addEventType("SupportBeanString", SupportBeanString.class.getName(), legacySupportBeanString);

        ConfigurationCommonEventTypeBean legacySupportSensorEvent = new ConfigurationCommonEventTypeBean();
        legacySupportSensorEvent.setFactoryMethod(SupportSensorEventFactory.class.getName() + ".getInstance");
        configuration.getCommon().addEventType("SupportSensorEvent", SupportSensorEvent.class.getName(), legacySupportSensorEvent);
        configuration.getCommon().addImport(SupportEnum.class);

        Map<String, Object> mymapDef = new HashMap<>();
        mymapDef.put("anint", int.class);
        mymapDef.put("intBoxed", Integer.class);
        mymapDef.put("floatBoxed", Float.class);
        mymapDef.put("intArr", int[].class);
        mymapDef.put("mapProp", Map.class);
        mymapDef.put("isaImpl", ISupportAImpl.class);
        mymapDef.put("isbImpl", ISupportBImpl.class);
        mymapDef.put("isgImpl", ISupportAImplSuperGImpl.class);
        mymapDef.put("isabImpl", ISupportBaseABImpl.class);
        mymapDef.put("nested", SupportBeanComplexProps.SupportBeanSpecialGetterNested.class);
        configuration.getCommon().addEventType("MyMap", mymapDef);

        Map<String, Object> defMap = new HashMap<>();
        defMap.put("intVal", int.class);
        defMap.put("stringVal", String.class);
        defMap.put("doubleVal", Double.class);
        defMap.put("nullVal", null);
        configuration.getCommon().addEventType("MyMapType", defMap);

        String[] propsMyOAType = new String[]{"intVal", "stringVal", "doubleVal", "nullVal"};
        Object[] typesMyOAType = new Object[]{int.class, String.class, Double.class, null};
        configuration.getCommon().addEventType("MyOAType", propsMyOAType, typesMyOAType);

        Schema schema = record("MyAvroType").fields()
            .requiredInt("intVal")
            .requiredString("stringVal")
            .requiredDouble("doubleVal")
            .name("nullVal").type("null").noDefault()
            .endRecord();
        configuration.getCommon().addEventTypeAvro("MyAvroType", new ConfigurationCommonEventTypeAvro(schema));

        ConfigurationCommonEventTypeXMLDOM xml = new ConfigurationCommonEventTypeXMLDOM();
        xml.setRootElementName("abc");
        configuration.getCommon().addEventType("xmltype", xml);

        Map<String, Object> mapDef = new HashMap<>();
        mapDef.put("intPrimitive", int.class);
        mapDef.put("longBoxed", Long.class);
        mapDef.put("theString", String.class);
        mapDef.put("boolPrimitive", Boolean.class);
        configuration.getCommon().addEventType("MySupportMap", mapDef);

        Map<String, Object> type = makeMap(new Object[][]{{"id", String.class}});
        configuration.getCommon().addEventType("AEventMap", type);
        configuration.getCommon().addEventType("BEventMap", type);

        Map<String, Object> metadata = makeMap(new Object[][]{{"id", String.class}});
        configuration.getCommon().addEventType("AEventTE", metadata);
        configuration.getCommon().addEventType("BEventTE", metadata);

        configuration.getCommon().addImport(SupportStaticMethodLib.class);
        configuration.getCommon().addImport(EPLInsertIntoPopulateUnderlying.class.getPackage().getName() + ".*");

        Map<String, Object> complexMapMetadata = makeMap(new Object[][]{
            {"nested", makeMap(new Object[][]{{"nestedValue", String.class}})}
        });
        configuration.getCommon().addEventType("ComplexMap", complexMapMetadata);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getCompiler().addPlugInSingleRowFunction("generateMap", EPLInsertIntoTransposeStream.class.getName(), "localGenerateMap");
        configuration.getCompiler().addPlugInSingleRowFunction("generateOA", EPLInsertIntoTransposeStream.class.getName(), "localGenerateOA");
        configuration.getCompiler().addPlugInSingleRowFunction("generateAvro", EPLInsertIntoTransposeStream.class.getName(), "localGenerateAvro");
        configuration.getCompiler().addPlugInSingleRowFunction("generateJson", EPLInsertIntoTransposeStream.class.getName(), "localGenerateJson");
        configuration.getCompiler().addPlugInSingleRowFunction("custom", SupportStaticMethodLib.class.getName(), "makeSupportBean");
        configuration.getCompiler().addPlugInSingleRowFunction("customOne", SupportStaticMethodLib.class.getName(), "makeSupportBean");
        configuration.getCompiler().addPlugInSingleRowFunction("customTwo", SupportStaticMethodLib.class.getName(), "makeSupportBeanNumeric");
    }

    private static Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (Object[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }
}
