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
package com.espertech.esper.regressionlib.suite.epl.contained;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.hook.expr.EPLMethodInvocationContext;
import com.espertech.esper.common.client.hook.expr.EPLScriptContext;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.avro.core.AvroEventType;
import com.espertech.esper.common.internal.avro.support.SupportAvroArrayEvent;
import com.espertech.esper.regressionlib.support.bean.SupportJsonArrayEvent;
import com.espertech.esper.common.client.json.util.JsonEventObject;
import com.espertech.esper.common.client.json.minimaljson.JsonObject;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.common.internal.support.SupportJavaVersionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportCollectionEvent;
import com.espertech.esper.regressionlib.support.bean.SupportObjectArrayEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.*;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.*;

public class EPLContainedEventSplitExpr {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLContainedScriptContextValue());
        execs.add(new EPLContainedSplitExprReturnsEventBean());
        execs.add(new EPLContainedSingleRowSplitAndType());
        return execs;
    }

    private static class EPLContainedScriptContextValue implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String script = "@name('mystmt') create expression Object js:myGetScriptContext() [\n" +
                "myGetScriptContext();" +
                "function myGetScriptContext() {" +
                "  return epl;\n" +
                "}]";
            env.compileDeploy(script, path);

            env.compileDeploy("@name('s0') select myGetScriptContext() as c0 from SupportBean", path).addListener("s0");
            env.sendEventBean(new SupportBean());
            EPLScriptContext context = (EPLScriptContext) env.listener("s0").assertOneGetNewAndReset().get("c0");
            assertNotNull(context.getEventBeanService());

            env.undeployAll();
        }
    }

    private static class EPLContainedSplitExprReturnsEventBean implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            RegressionPath path = new RegressionPath();
            String script = "create expression EventBean[] js:mySplitScriptReturnEventBeanArray(value) [\n" +
                "mySplitScriptReturnEventBeanArray(value);" +
                "function mySplitScriptReturnEventBeanArray(value) {" +
                "  var split = value.split(',');\n" +
                "  var EventBeanArray = Java.type(\"com.espertech.esper.common.client.EventBean[]\");\n" +
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
            env.compileDeploy(script, path);

            String epl = "create schema BaseEvent();\n" +
                "create schema AEvent(p0 string) inherits BaseEvent;\n" +
                "create schema BEvent(p1 string) inherits BaseEvent;\n" +
                "create schema SplitEvent(value string);\n";
            EPCompiled compiled = env.compileWBusPublicType(epl);
            env.deploy(compiled);
            path.add(compiled);

            tryAssertionSplitExprReturnsEventBean(env, path, "mySplitUDFReturnEventBeanArray");
            tryAssertionSplitExprReturnsEventBean(env, path, "mySplitScriptReturnEventBeanArray");

            env.undeployAll();
        }

        private void tryAssertionSplitExprReturnsEventBean(RegressionEnvironment env, RegressionPath path, String functionOrScript) {

            String epl = "@name('s0') select * from SplitEvent[" + functionOrScript + "(value) @type(BaseEvent)]";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventMap(Collections.singletonMap("value", "AE1,BE2,AE3"), "SplitEvent");
            EventBean[] events = env.listener("s0").getAndResetLastNewData();
            assertSplitEx(events[0], "AEvent", "p0", "E1");
            assertSplitEx(events[1], "BEvent", "p1", "E2");
            assertSplitEx(events[2], "AEvent", "p0", "E3");

            env.undeployModuleContaining("s0");
        }
    }

    private static class EPLContainedSingleRowSplitAndType implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {
            for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
                tryAssertionSingleRowSplitAndType(env, rep);
            }
        }
    }

    private static void tryAssertionSingleRowSplitAndType(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum) {

        RegressionPath path = new RegressionPath();
        String types = eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonSentence.class) + " create schema MySentenceEvent(sentence String);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonWord.class) + " create schema WordEvent(word String);\n" +
                eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonCharacter.class) + " create schema CharacterEvent(character String);\n";
        env.compileDeployWBusPublicType(types, path);

        String stmtText;
        String[] fields = "word".split(",");

        // test single-row method
        stmtText = "@name('s0') select * from MySentenceEvent[splitSentence" + "_" + eventRepresentationEnum.name() + "(sentence)@type(WordEvent)]";
        env.compileDeploy(stmtText, path).addListener("s0");
        assertEquals("WordEvent", env.statement("s0").getEventType().getName());
        assertTrue(eventRepresentationEnum.matchesClass(env.statement("s0").getEventType().getUnderlyingType()));

        sendMySentenceEvent(env, eventRepresentationEnum, "I am testing this code");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"I"}, {"am"}, {"testing"}, {"this"}, {"code"}});

        sendMySentenceEvent(env, eventRepresentationEnum, "the second event");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"the"}, {"second"}, {"event"}});

        env.undeployModuleContaining("s0");

        // test SODA
        env.eplToModelCompileDeploy(stmtText, path).addListener("s0");
        sendMySentenceEvent(env, eventRepresentationEnum, "the third event");
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"the"}, {"third"}, {"event"}});
        env.undeployModuleContaining("s0");

        // test script
        if (eventRepresentationEnum.isMapEvent()) {
            if (SupportJavaVersionUtil.JAVA_VERSION <= 1.7) {
                stmtText = "@name('s0') expression Collection js:splitSentenceJS(sentence) [" +
                    "  importPackage(java.util);" +
                    "  var words = new ArrayList();" +
                    "  words.add(Collections.singletonMap('word', 'wordOne'));" +
                    "  words.add(Collections.singletonMap('word', 'wordTwo'));" +
                    "  words;" +
                    "]" +
                    "select * from MySentenceEvent[splitSentenceJS(sentence)@type(WordEvent)]";
            } else {
                stmtText = "@name('s0') expression Collection js:splitSentenceJS(sentence) [" +
                    "  var CollectionsClazz = Java.type('java.util.Collections');" +
                    "  var words = new java.util.ArrayList();" +
                    "  words.add(CollectionsClazz.singletonMap('word', 'wordOne'));" +
                    "  words.add(CollectionsClazz.singletonMap('word', 'wordTwo'));" +
                    "  words;" +
                    "]" +
                    "select * from MySentenceEvent[splitSentenceJS(sentence)@type(WordEvent)]";
            }

            env.compileDeploy(stmtText, path).addListener("s0");
            assertEquals("WordEvent", env.statement("s0").getEventType().getName());

            env.sendEventMap(Collections.emptyMap(), "MySentenceEvent");
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"wordOne"}, {"wordTwo"}});

            env.undeployModuleContaining("s0");
        }

        // test multiple splitters
        stmtText = "@name('s0') select * from MySentenceEvent[splitSentence_" + eventRepresentationEnum.name() + "(sentence)@type(WordEvent)][splitWord_" + eventRepresentationEnum.name() + "(word)@type(CharacterEvent)]";
        env.compileDeploy(stmtText, path).addListener("s0");
        assertEquals("CharacterEvent", env.statement("s0").getEventType().getName());

        sendMySentenceEvent(env, eventRepresentationEnum, "I am");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), "character".split(","), new Object[][]{{"I"}, {"a"}, {"m"}});

        env.undeployModuleContaining("s0");

        // test wildcard parameter
        stmtText = "@name('s0') select * from MySentenceEvent[splitSentenceBean_" + eventRepresentationEnum.name() + "(*)@type(WordEvent)]";
        env.compileDeploy(stmtText, path).addListener("s0");
        assertEquals("WordEvent", env.statement("s0").getEventType().getName());

        sendMySentenceEvent(env, eventRepresentationEnum, "another test sentence");
        EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"another"}, {"test"}, {"sentence"}});

        env.undeployModuleContaining("s0");

        // test property returning untyped collection
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            stmtText = eventRepresentationEnum.getAnnotationText() + " @name('s0') select * from SupportObjectArrayEvent[someObjectArray@type(WordEvent)]";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertEquals("WordEvent", env.statement("s0").getEventType().getName());

            Object[][] rows = new Object[][]{{"this"}, {"is"}, {"collection"}};
            env.sendEventBean(new SupportObjectArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"collection"}});
            env.undeployAll();
        } else if (eventRepresentationEnum.isMapEvent()) {
            stmtText = eventRepresentationEnum.getAnnotationText() + " @name('s0') select * from SupportCollectionEvent[someCollection@type(WordEvent)]";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertEquals("WordEvent", env.statement("s0").getEventType().getName());

            Collection<Map> coll = new ArrayList<>();
            coll.add(Collections.singletonMap("word", "this"));
            coll.add(Collections.singletonMap("word", "is"));
            coll.add(Collections.singletonMap("word", "collection"));

            env.sendEventBean(new SupportCollectionEvent(coll));
            EPAssertionUtil.assertPropsPerRowAnyOrder(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"collection"}});
            env.undeployAll();
        } else if (eventRepresentationEnum.isAvroEvent()) {
            stmtText = "@name('s0') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonWord.class) + " select * from SupportAvroArrayEvent[someAvroArray@type(WordEvent)]";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertEquals("WordEvent", env.statement("s0").getEventType().getName());

            GenericData.Record[] rows = new GenericData.Record[3];
            String[] words = "this,is,avro".split(",");
            for (int i = 0; i < words.length; i++) {
                rows[i] = new GenericData.Record(((AvroEventType) env.statement("s0").getEventType()).getSchemaAvro());
                rows[i].put("word", words[i]);
            }
            env.sendEventBean(new SupportAvroArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"avro"}});
            env.undeployAll();
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            stmtText = "@name('s0') " + eventRepresentationEnum.getAnnotationTextWJsonProvided(MyLocalJsonWord.class) + " select * from SupportJsonArrayEvent[someJsonArray@type(WordEvent)]";
            env.compileDeploy(stmtText, path).addListener("s0");
            assertEquals("WordEvent", env.statement("s0").getEventType().getName());

            String[] rows = new String[3];
            String[] words = "this,is,avro".split(",");
            for (int i = 0; i < words.length; i++) {
                rows[i] = "{ \"word\": \"" + words[i] + "\"}";
            }
            env.sendEventBean(new SupportJsonArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"avro"}});
            env.undeployAll();
        } else {
            throw new IllegalArgumentException("Unrecognized enum " + eventRepresentationEnum);
        }

        // invalid: event type not found
        tryInvalidCompile(env, path, "select * from MySentenceEvent[splitSentence_" + eventRepresentationEnum.name() + "(sentence)@type(XYZ)]",
            "Event type by name 'XYZ' could not be found");

        // invalid lib-function annotation
        tryInvalidCompile(env, path, "select * from MySentenceEvent[splitSentence_" + eventRepresentationEnum.name() + "(sentence)@dummy(WordEvent)]",
            "Invalid annotation for property selection, expected 'type' but found 'dummy' in text '@dummy(WordEvent)'");

        // invalid type assignment to event type
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            tryInvalidCompile(env, path, "select * from MySentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                "Event type 'WordEvent' underlying type [Ljava.lang.Object; cannot be assigned a value of type");
        } else if (eventRepresentationEnum.isMapEvent()) {
            tryInvalidCompile(env, path, "select * from MySentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                "Event type 'WordEvent' underlying type java.util.Map cannot be assigned a value of type");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            tryInvalidCompile(env, path, "select * from MySentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                "Event type 'WordEvent' underlying type " + JavaClassHelper.APACHE_AVRO_GENERIC_RECORD_CLASSNAME + " cannot be assigned a value of type");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            tryInvalidCompile(env, path, "select * from MySentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                "Event type 'WordEvent' requires string-type array and cannot be assigned from value of type " + JavaClassHelper.getClassNameFullyQualPretty(SupportBean[].class));
        } else {
            fail();
        }

        // invalid subquery
        tryInvalidCompile(env, path, "select * from MySentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]",
            "Invalid contained-event expression 'splitSentence(subselect_0)': Aggregation, sub-select, previous or prior functions are not supported in this context [select * from MySentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]]");

        env.undeployAll();
    }

    private static void sendMySentenceEvent(RegressionEnvironment env, EventRepresentationChoice eventRepresentationEnum, String sentence) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            env.sendEventObjectArray(new Object[]{sentence}, "MySentenceEvent");
        } else if (eventRepresentationEnum.isMapEvent()) {
            env.sendEventMap(Collections.singletonMap("sentence", sentence), "MySentenceEvent");
        } else if (eventRepresentationEnum.isAvroEvent()) {
            Schema schema = record("sentence").fields().requiredString("sentence").endRecord();
            GenericData.Record record = new GenericData.Record(schema);
            record.put("sentence", sentence);
            env.sendEventAvro(record, "MySentenceEvent");
        } else if (eventRepresentationEnum.isJsonEvent() || eventRepresentationEnum.isJsonProvidedClassEvent()) {
            JsonObject object = new JsonObject();
            object.add("sentence", sentence);
            env.sendEventJson(object.toString(), "MySentenceEvent");
        } else {
            throw new IllegalStateException("Unrecognized enum " + eventRepresentationEnum);
        }
    }

    private static void assertSplitEx(EventBean event, String typeName, String propertyName, String propertyValue) {
        assertEquals(typeName, event.getEventType().getName());
        assertEquals(propertyValue, event.get(propertyName));
    }

    public static EventBean[] mySplitUDFReturnEventBeanArray(String value, EPLMethodInvocationContext context) {
        String[] split = value.split(",");
        EventBean[] events = new EventBean[split.length];
        for (int i = 0; i < split.length; i++) {
            String pvalue = split[i].substring(1);
            if (split[i].startsWith("A")) {
                events[i] = context.getEventBeanService().adapterForMap(Collections.singletonMap("p0", pvalue), "AEvent");
            } else if (split[i].startsWith("B")) {
                events[i] = context.getEventBeanService().adapterForMap(Collections.singletonMap("p1", pvalue), "BEvent");
            } else {
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
            maps.add(Collections.singletonMap("character", Character.toString(word.charAt(i))));
        }
        return maps.toArray(new Map[maps.size()]);
    }

    public static SupportBean[] invalidSentenceMethod(String sentence) {
        return null;
    }

    public static GenericData.Record[] splitWordMethodReturnAvro(String word) {
        Schema schema = record("chars").fields().requiredString("character").endRecord();
        GenericData.Record[] records = new GenericData.Record[word.length()];
        for (int i = 0; i < word.length(); i++) {
            records[i] = new GenericData.Record(schema);
            records[i].put("character", Character.toString(word.charAt(i)));
        }
        return records;
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

    public static GenericData.Record[] splitSentenceBeanMethodReturnAvro(GenericData.Record sentenceEvent) {
        return splitSentenceMethodReturnAvro((String) sentenceEvent.get("sentence"));
    }

    public static String[] splitWordMethodReturnJson(String word) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            String c = Character.toString(word.charAt(i));
            strings.add("{ \"character\": \"" + c + "\"}");
        }
        return strings.toArray(new String[0]);

    }

    public static String[] splitSentenceMethodReturnJson(String sentence) {
        String[] words = sentence.split(" ");
        String[] events = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            events[i] = "{ \"word\": \"" + words[i] + "\"}";
        }
        return events;
    }

    public static String[] splitSentenceBeanMethodReturnJson(Object sentenceEvent) {
        String sentence;
        if (sentenceEvent instanceof JsonEventObject) {
            sentence = ((JsonEventObject) sentenceEvent).get("sentence").toString();
        } else {
            sentence = ((MyLocalJsonSentence) sentenceEvent).sentence;
        }
        return splitSentenceMethodReturnJson(sentence);
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

    public static class MyLocalJsonSentence {
        public String sentence;
    }

    public static class MyLocalJsonWord {
        public String word;
    }

    public static class MyLocalJsonCharacter {
        public String character;
    }
}
