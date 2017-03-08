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
package com.espertech.esper.regression.epl;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.hook.EPLScriptContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.epl.script.AgentInstanceScriptContext;
import com.espertech.esper.event.avro.AvroConstantsNoDep;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.script.SupportScriptUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.*;

import static org.apache.avro.SchemaBuilder.record;

public class TestContainedEventSplitExpr extends TestCase {
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.startTest(epService, this.getClass(), getName());
        }
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        }
        listener = null;
    }

    public void testScriptContextValue() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        String script = "@name('mystmt') create expression Object js:myGetScriptContext() [\n" +
                "myGetScriptContext();" +
                "function myGetScriptContext() {" +
                "  return epl;\n" +
                "}]";
        epService.getEPAdministrator().createEPL(script);

        epService.getEPAdministrator().createEPL("select myGetScriptContext() as c0 from SupportBean").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPLScriptContext context = (EPLScriptContext) listener.assertOneGetNewAndReset().get("c0");
        assertNotNull(context.getEventBeanService());
    }

    public void testSplitExprReturnsEventBean() throws Exception {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("mySplitUDFReturnEventBeanArray", this.getClass().getName(), "mySplitUDFReturnEventBeanArray");

        String script = "create expression EventBean[] js:mySplitScriptReturnEventBeanArray(value) [\n" +
                "mySplitScriptReturnEventBeanArray(value);" +
                "function mySplitScriptReturnEventBeanArray(value) {" +
                "  var split = value.split(',');\n" +
                "  var EventBeanArray = Java.type(\"com.espertech.esper.client.EventBean[]\");\n" +
                "  var events = new EventBeanArray(split.length);  " +
                "  for (var i = 0; i < split.length; i++) {\n" +
                "    var pvalue = split[i].substring(1);\n" +
                "    if (split[i].startsWith(\"A\")) {\n" +
                "      events[i] =  epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"p0\", pvalue), \"AEvent\");\n" +
                "    }\n" +
                "    else if (split[i].startsWith(\"B\")) {\n" +
                "      events[i] =  epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"p1\", pvalue), \"BEvent\");\n" +
                "    }\n" +
                "    else {\n" +
                "      throw new UnsupportedOperationException(\"Unrecognized type\");\n" +
                "    }\n" +
                "  }\n" +
                "  return events;\n" +
                "}]";
        epService.getEPAdministrator().createEPL(script);

        String epl = "create schema BaseEvent();\n" +
                "create schema AEvent(p0 string) inherits BaseEvent;\n" +
                "create schema BEvent(p1 string) inherits BaseEvent;\n" +
                "create schema SplitEvent(value string);\n";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        runAssertionSplitExprReturnsEventBean("mySplitUDFReturnEventBeanArray");
        runAssertionSplitExprReturnsEventBean("mySplitScriptReturnEventBeanArray");
    }

    private void runAssertionSplitExprReturnsEventBean(String functionOrScript) {

        String epl = "@name('s0') select * from SplitEvent[" + functionOrScript + "(value) @type(BaseEvent)]";
        EPStatement statement = epService.getEPAdministrator().createEPL(epl);
        statement.addListener(listener);

        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", "AE1,BE2,AE3"), "SplitEvent");
        EventBean[] events = listener.getAndResetLastNewData();
        assertSplitEx(events[0], "AEvent", "p0", "E1");
        assertSplitEx(events[1], "BEvent", "p1", "E2");
        assertSplitEx(events[2], "AEvent", "p0", "E3");

        statement.destroy();
    }

    public void testSingleRowSplitAndType() {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runAssertionSingleRowSplitAndType(rep);
        }
    }

    private void runAssertionSingleRowSplitAndType(EventRepresentationChoice eventRepresentationEnum) {
        String[] methods;
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            methods = "splitSentenceMethodReturnObjectArray,splitSentenceBeanMethodReturnObjectArray,splitWordMethodReturnObjectArray".split(",");
        } else if (eventRepresentationEnum.isMapEvent()) {
            methods = "splitSentenceMethodReturnMap,splitSentenceBeanMethodReturnMap,splitWordMethodReturnMap".split(",");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            methods = "splitSentenceMethodReturnAvro,splitSentenceBeanMethodReturnAvro,splitWordMethodReturnAvro".split(",");
        } else {
            throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
        }
        String[] funcs = "splitSentence,splitSentenceBean,splitWord".split(",");
        for (int i = 0; i < funcs.length; i++) {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(funcs[i], this.getClass().getName(), methods[i]);
        }
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("invalidSentence", this.getClass().getName(), "invalidSentenceMethod");

        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema SentenceEvent(sentence String)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema WordEvent(word String)");
        epService.getEPAdministrator().createEPL(eventRepresentationEnum.getAnnotationText() + " create schema CharacterEvent(char String)");

        String stmtText;
        EPStatement stmt;
        String[] fields = "word".split(",");

        // test single-row method
        stmtText = "select * from SentenceEvent[splitSentence(sentence)@type(WordEvent)]";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertEquals("WordEvent", stmt.getEventType().getName());
        assertTrue(eventRepresentationEnum.matchesClass(stmt.getEventType().getUnderlyingType()));

        sendSentenceEvent(eventRepresentationEnum, "I am testing this code");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"I"}, {"am"}, {"testing"}, {"this"}, {"code"}});

        sendSentenceEvent(eventRepresentationEnum, "the second event");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"the"}, {"second"}, {"event"}});

        stmt.destroy();

        // test SODA
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(stmtText, stmt.getText());
        stmt.addListener(listener);

        sendSentenceEvent(eventRepresentationEnum, "the third event");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"the"}, {"third"}, {"event"}});

        stmt.destroy();

        // test script
        if (eventRepresentationEnum.isMapEvent()) {
            if (SupportScriptUtil.JAVA_VERSION <= 1.7) {
                stmtText = "expression Collection js:splitSentenceJS(sentence) [" +
                        "  importPackage(java.util);" +
                        "  var words = new ArrayList();" +
                        "  words.add(Collections.singletonMap('word', 'wordOne'));" +
                        "  words.add(Collections.singletonMap('word', 'wordTwo'));" +
                        "  words;" +
                        "]" +
                        "select * from SentenceEvent[splitSentenceJS(sentence)@type(WordEvent)]";
            } else {
                stmtText = "expression Collection js:splitSentenceJS(sentence) [" +
                        "  var CollectionsClazz = Java.type('java.util.Collections');" +
                        "  var words = new java.util.ArrayList();" +
                        "  words.add(CollectionsClazz.singletonMap('word', 'wordOne'));" +
                        "  words.add(CollectionsClazz.singletonMap('word', 'wordTwo'));" +
                        "  words;" +
                        "]" +
                        "select * from SentenceEvent[splitSentenceJS(sentence)@type(WordEvent)]";
            }
            stmt = epService.getEPAdministrator().createEPL(stmtText);
            stmt.addListener(listener);
            assertEquals("WordEvent", stmt.getEventType().getName());

            epService.getEPRuntime().sendEvent(Collections.emptyMap(), "SentenceEvent");
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"wordOne"}, {"wordTwo"}});

            stmt.destroy();
        }

        // test multiple splitters
        stmtText = "select * from SentenceEvent[splitSentence(sentence)@type(WordEvent)][splitWord(word)@type(CharacterEvent)]";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertEquals("CharacterEvent", stmt.getEventType().getName());

        sendSentenceEvent(eventRepresentationEnum, "I am");
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), "char".split(","), new Object[][]{{"I"}, {"a"}, {"m"}});

        stmt.destroy();

        // test wildcard parameter
        stmtText = "select * from SentenceEvent[splitSentenceBean(*)@type(WordEvent)]";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertEquals("WordEvent", stmt.getEventType().getName());

        sendSentenceEvent(eventRepresentationEnum, "another test sentence");
        EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"another"}, {"test"}, {"sentence"}});

        stmt.destroy();

        // test property returning untyped collection
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType(ObjectArrayEvent.class);
            stmtText = eventRepresentationEnum.getAnnotationText() + " select * from ObjectArrayEvent[someObjectArray@type(WordEvent)]";
            stmt = epService.getEPAdministrator().createEPL(stmtText);
            stmt.addListener(listener);
            assertEquals("WordEvent", stmt.getEventType().getName());

            Object[][] rows = new Object[][]{{"this"}, {"is"}, {"collection"}};
            epService.getEPRuntime().sendEvent(new ObjectArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"collection"}});
            stmt.destroy();
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType(CollectionEvent.class);
            stmtText = eventRepresentationEnum.getAnnotationText() + " select * from CollectionEvent[someCollection@type(WordEvent)]";
            stmt = epService.getEPAdministrator().createEPL(stmtText);
            stmt.addListener(listener);
            assertEquals("WordEvent", stmt.getEventType().getName());

            Collection<Map> coll = new ArrayList<Map>();
            coll.add(Collections.singletonMap("word", "this"));
            coll.add(Collections.singletonMap("word", "is"));
            coll.add(Collections.singletonMap("word", "collection"));

            epService.getEPRuntime().sendEvent(new CollectionEvent(coll));
            EPAssertionUtil.assertPropsPerRowAnyOrder(listener.getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"collection"}});
            stmt.destroy();
        } else if (eventRepresentationEnum.isAvroEvent()) {
            epService.getEPAdministrator().getConfiguration().addEventType(AvroArrayEvent.class);
            stmtText = eventRepresentationEnum.getAnnotationText() + " select * from AvroArrayEvent[someAvroArray@type(WordEvent)]";
            stmt = epService.getEPAdministrator().createEPL(stmtText);
            stmt.addListener(listener);
            assertEquals("WordEvent", stmt.getEventType().getName());

            GenericData.Record[] rows = new GenericData.Record[3];
            String[] words = "this,is,avro".split(",");
            for (int i = 0; i < words.length; i++) {
                rows[i] = new GenericData.Record(((AvroEventType) stmt.getEventType()).getSchemaAvro());
                rows[i].put("word", words[i]);
            }
            epService.getEPRuntime().sendEvent(new AvroArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"avro"}});
            stmt.destroy();
        } else {
            throw new IllegalArgumentException("Unrecognized enum " + eventRepresentationEnum);
        }

        // invalid: event type not found
        tryInvalid("select * from SentenceEvent[splitSentence(sentence)@type(XYZ)]",
                "Event type by name 'XYZ' could not be found [select * from SentenceEvent[splitSentence(sentence)@type(XYZ)]]");

        // invalid lib-function annotation
        tryInvalid("select * from SentenceEvent[splitSentence(sentence)@dummy(WordEvent)]",
                "Invalid annotation for property selection, expected 'type' but found 'dummy' in text '@dummy(WordEvent)'");

        // invalid type assignment to event type
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            tryInvalid("select * from SentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                    "Event type 'WordEvent' underlying type [Ljava.lang.Object; cannot be assigned a value of type");
        } else if (eventRepresentationEnum.isMapEvent()) {
            tryInvalid("select * from SentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                    "Event type 'WordEvent' underlying type java.util.Map cannot be assigned a value of type");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            tryInvalid("select * from SentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                    "Event type 'WordEvent' underlying type " + AvroConstantsNoDep.GENERIC_RECORD_CLASSNAME + " cannot be assigned a value of type");
        } else {
            fail();
        }

        // invalid subquery
        tryInvalid("select * from SentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]",
                "Invalid contained-event expression 'splitSentence(subselect_0)': Aggregation, sub-select, previous or prior functions are not supported in this context [select * from SentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]]");

        epService.initialize();
    }

    private void sendSentenceEvent(EventRepresentationChoice eventRepresentationEnum, String sentence) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[]{sentence}, "SentenceEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("sentence", sentence), "SentenceEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = record("sentence").fields().requiredString("sentence").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("sentence", sentence);
            epService.getEPRuntime().sendEventAvro(record, "SentenceEvent");
        } else {
            throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
        }
    }

    private void assertSplitEx(EventBean event, String typeName, String propertyName, String propertyValue) {
        assertEquals(typeName, event.getEventType().getName());
        assertEquals(propertyValue, event.get(propertyName));
    }

    public static EventBean[] mySplitUDFReturnEventBeanArray(String value, EPLMethodInvocationContext context) {
        String[] split = value.split(",");
        EventBean[] events = new EventBean[split.length];
        for (int i = 0; i < split.length; i++) {
            String pvalue = split[i].substring(1);
            if (split[i].startsWith("A")) {
                events[i] =  context.getEventBeanService().adapterForMap(Collections.singletonMap("p0", pvalue), "AEvent");
            }
            else if (split[i].startsWith("B")) {
                events[i] =  context.getEventBeanService().adapterForMap(Collections.singletonMap("p1", pvalue), "BEvent");
            }
            else {
                throw new UnsupportedOperationException("Unrecognized type");
            }
        }
        return events;
    }

    public static Map[] splitSentenceMethodReturnMap(String sentence) {
        String[] words = sentence.split(" ");
        Map[] events = new Map[words.length];
        for (int i = 0; i < words.length; i++) {
            events[i] = Collections.singletonMap("word", words[i]);
        }
        return events;
    }

    public static GenericData.Record[] splitSentenceMethodReturnAvro(String sentence) {
        Schema wordSchema = record("word").fields().requiredString("word").endRecord();
        String[] words = sentence.split(" ");
        GenericData.Record[] events = new GenericData.Record[words.length];
        for (int i = 0; i < words.length; i++) {
            events[i] = new GenericData.Record(wordSchema);
            events[i].put(0, words[i]);
        }
        return events;
    }

    public static Object[][] splitSentenceMethodReturnObjectArray(String sentence) {
        String[] words = sentence.split(" ");
        Object[][] events = new Object[words.length][];
        for (int i = 0; i < words.length; i++) {
            events[i] = new Object[]{words[i]};
        }
        return events;
    }

    public static Map[] splitSentenceBeanMethodReturnMap(Map sentenceEvent) {
        return splitSentenceMethodReturnMap((String) sentenceEvent.get("sentence"));
    }

    public static GenericData.Record[] splitSentenceBeanMethodReturnAvro(GenericData.Record sentenceEvent) {
        return splitSentenceMethodReturnAvro((String) sentenceEvent.get("sentence"));
    }

    public static Object[][] splitSentenceBeanMethodReturnObjectArray(Object[] sentenceEvent) {
        return splitSentenceMethodReturnObjectArray((String) sentenceEvent[0]);
    }

    public static Object[][] splitWordMethodReturnObjectArray(String word) {
        int count = word.length();
        Object[][] events = new Object[count][];
        for (int i = 0; i < word.length(); i++) {
            events[i] = new Object[]{Character.toString(word.charAt(i))};
        }
        return events;
    }

    public static Map[] splitWordMethodReturnMap(String word) {
        List<Map> maps = new ArrayList<Map>();
        for (int i = 0; i < word.length(); i++) {
            maps.add(Collections.singletonMap("char", Character.toString(word.charAt(i))));
        }
        return maps.toArray(new Map[maps.size()]);
    }

    public static GenericData.Record[] splitWordMethodReturnAvro(String word) {
        Schema schema = record("chars").fields().requiredString("char").endRecord();
        GenericData.Record[] records = new GenericData.Record[word.length()];
        for (int i = 0; i < word.length(); i++) {
            records[i] = new GenericData.Record(schema);
            records[i].put("char", Character.toString(word.charAt(i)));
        }
        return records;
    }

    public static SupportBean[] invalidSentenceMethod(String sentence) {
        return null;
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            assertFalse(expected.isEmpty());
            assertTrue("Received message: " + ex.getMessage(), ex.getMessage().startsWith(expected));
        }
    }

    public static class CollectionEvent {
        private Collection someCollection;

        public CollectionEvent(Collection someCollection) {
            this.someCollection = someCollection;
        }

        public Collection getSomeCollection() {
            return someCollection;
        }

        public void setSomeCollection(Collection someCollection) {
            this.someCollection = someCollection;
        }
    }

    public static class ObjectArrayEvent {
        private Object[][] someObjectArray;

        public ObjectArrayEvent(Object[][] someObjectArray) {
            this.someObjectArray = someObjectArray;
        }

        public Object[][] getSomeObjectArray() {
            return someObjectArray;
        }
    }

    public static class AvroArrayEvent {
        private GenericData.Record[] someAvroArray;

        public AvroArrayEvent(GenericData.Record[] someAvroArray) {
            this.someAvroArray = someAvroArray;
        }

        public GenericData.Record[] getSomeAvroArray() {
            return someAvroArray;
        }
    }
}
