/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.regression.script.SupportScriptUtil;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.*;

public class TestContainedEventSplitExpr extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testSingleRowSplitAndType() {
        runAssertionSingleRowSplitAndType(EventRepresentationEnum.OBJECTARRAY);
        runAssertionSingleRowSplitAndType(EventRepresentationEnum.MAP);
        runAssertionSingleRowSplitAndType(EventRepresentationEnum.DEFAULT);
    }

    private void runAssertionSingleRowSplitAndType(EventRepresentationEnum eventRepresentationEnum) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitSentence", this.getClass().getName(), "splitSentenceMethodReturnObjectArray");
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitSentenceBean", this.getClass().getName(), "splitSentenceBeanMethodReturnObjectArray");
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitWord", this.getClass().getName(), "splitWordMethodReturnObjectArray");
        }
        else {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitSentence", this.getClass().getName(), "splitSentenceMethodReturnMap");
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitSentenceBean", this.getClass().getName(), "splitSentenceBeanMethodReturnMap");
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("splitWord", this.getClass().getName(), "splitWordMethodReturnMap");
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
        assertEquals(eventRepresentationEnum.getOutputClass(), stmt.getEventType().getUnderlyingType());

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
        if (!eventRepresentationEnum.isObjectArrayEvent()) {
            if (SupportScriptUtil.JAVA_VERSION <= 1.7) {
                stmtText = "expression Collection js:splitSentenceJS(sentence) [" +
                        "  importPackage(java.util);" +
                        "  var words = new ArrayList();" +
                        "  words.add(Collections.singletonMap('word', 'wordOne'));" +
                        "  words.add(Collections.singletonMap('word', 'wordTwo'));" +
                        "  words;" +
                        "]" +
                        "select * from SentenceEvent[splitSentenceJS(sentence)@type(WordEvent)]";
            }
            else {
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

            Object[][] rows = new Object[][] {{"this"}, {"is"}, {"collection"}};
            epService.getEPRuntime().sendEvent(new ObjectArrayEvent(rows));
            EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"this"}, {"is"}, {"collection"}});
        }
        else {
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
        }

        // invalid: event type not found
        tryInvalid("select * from SentenceEvent[splitSentence(sentence)@type(XYZ)]",
                   "Event type by name 'XYZ' could not be found [select * from SentenceEvent[splitSentence(sentence)@type(XYZ)]]");

        // invalid lib-function annotation
        tryInvalid("select * from SentenceEvent[splitSentence(sentence)@dummy(WordEvent)]",
                   "Invalid annotation for property selection, expected 'type' but found 'dummy' in text '[splitSentence(sentence)@dummy(WordEvent)]' [select * from SentenceEvent[splitSentence(sentence)@dummy(WordEvent)]]");
        
        // invalid type assignment to event type
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            tryInvalid("select * from SentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                       "Event type 'WordEvent' underlying type [Ljava.lang.Object; cannot be assigned a value of type");
        }
        else {
            tryInvalid("select * from SentenceEvent[invalidSentence(sentence)@type(WordEvent)]",
                       "Event type 'WordEvent' underlying type java.util.Map cannot be assigned a value of type");
        }

        // invalid subquery
        tryInvalid("select * from SentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]",
                   "Invalid contained-event expression 'splitSentence(subselect_0)': Aggregation, sub-select, previous or prior functions are not supported in this context [select * from SentenceEvent[splitSentence((select * from SupportBean#keepall))@type(WordEvent)]]");

        epService.initialize();
    }

    private void sendSentenceEvent(EventRepresentationEnum eventRepresentationEnum, String sentence) {
        if (eventRepresentationEnum.isObjectArrayEvent()) {
            epService.getEPRuntime().sendEvent(new Object[] {sentence}, "SentenceEvent");
        }
        else {
            epService.getEPRuntime().sendEvent(Collections.singletonMap("sentence", sentence), "SentenceEvent");
        }
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
            events[i] = new Object[] {words[i]};
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
            events[i] = new Object[] {Character.toString(word.charAt(i))};
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

    public static SupportBean[] invalidSentenceMethod(String sentence) {
        return null;
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
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
}
