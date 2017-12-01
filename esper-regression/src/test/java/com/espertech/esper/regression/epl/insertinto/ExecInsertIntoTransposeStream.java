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
package com.espertech.esper.regression.epl.insertinto;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecInsertIntoTransposeStream implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("generateMap", this.getClass().getName(), "localGenerateMap");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("generateOA", this.getClass().getName(), "localGenerateOA");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("generateAvro", this.getClass().getName(), "localGenerateAvro");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("custom", SupportStaticMethodLib.class.getName(), "makeSupportBean");

        runAssertionTransposeMapAndObjectArray(epService);
        runAssertionTransposeFunctionToStreamWithProps(epService);
        runAssertionTransposeFunctionToStream(epService);
        runAssertionTransposeSingleColumnInsert(epService);
        runAssertionTransposeEventJoinMap(epService);
        runAssertionTransposeEventJoinPOJO(epService);
        runAssertionTransposePOJOPropertyStream(epService);
        runAssertionInvalidTranspose(epService);
    }

    private void runAssertionTransposeMapAndObjectArray(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            runTransposeMapAndObjectArray(epService, rep);
        }
    }

    private void runTransposeMapAndObjectArray(EPServiceProvider epService, EventRepresentationChoice representation) {

        String[] fields = "p0,p1".split(",");
        epService.getEPAdministrator().createEPL("create " + representation.getOutputTypeCreateSchemaName() + " schema MySchema(p0 string, p1 int)");

        String generateFunction;
        if (representation.isObjectArrayEvent()) {
            generateFunction = "generateOA";
        } else if (representation.isMapEvent()) {
            generateFunction = "generateMap";
        } else if (representation.isAvroEvent()) {
            generateFunction = "generateAvro";
        } else {
            throw new IllegalStateException("Unrecognized code " + representation);
        }
        String epl = "insert into MySchema select transpose(" + generateFunction + "(theString, intPrimitive)) from SupportBean";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl, "first").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});

        // MySchema already exists, start second statement
        epService.getEPAdministrator().createEPL(epl, "second").addListener(listener);
        epService.getEPAdministrator().getStatement("first").destroy();

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 3});

        epService.getEPAdministrator().getConfiguration().removeEventType("MySchema", true);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposeFunctionToStreamWithProps(EPServiceProvider epService) {
        String stmtTextOne = "insert into MyStream select 1 as dummy, transpose(custom('O' || theString, 10)) from SupportBean(theString like 'I%')";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select * from MyStream";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EventType type = stmt.getEventType();
        assertEquals(Pair.class, type.getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("I1", 1));
        EventBean result = listener.assertOneGetNewAndReset();
        Pair underlying = (Pair) result.getUnderlying();
        EPAssertionUtil.assertProps(result, "dummy,theString,intPrimitive".split(","), new Object[]{1, "OI1", 10});
        assertEquals("OI1", ((SupportBean) underlying.getFirst()).getTheString());

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposeFunctionToStream(EPServiceProvider epService) {
        String stmtTextOne = "insert into OtherStream select transpose(custom('O' || theString, 10)) from SupportBean(theString like 'I%')";
        epService.getEPAdministrator().createEPL(stmtTextOne, "first");

        String stmtTextTwo = "select * from OtherStream(theString like 'O%')";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EventType type = stmt.getEventType();
        assertEquals(SupportBean.class, type.getUnderlyingType());

        epService.getEPRuntime().sendEvent(new SupportBean("I1", 1));
        EventBean result = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(result, "theString,intPrimitive".split(","), new Object[]{"OI1", 10});
        assertEquals("OI1", ((SupportBean) result.getUnderlying()).getTheString());

        // try second statement as "OtherStream" now already exists
        epService.getEPAdministrator().createEPL(stmtTextOne, "second");
        epService.getEPAdministrator().getStatement("first").destroy();
        epService.getEPRuntime().sendEvent(new SupportBean("I2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "theString,intPrimitive".split(","), new Object[]{"OI2", 10});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposeSingleColumnInsert(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBeanNumeric.class);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("customOne", SupportStaticMethodLib.class.getName(), "makeSupportBean");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("customTwo", SupportStaticMethodLib.class.getName(), "makeSupportBeanNumeric");

        // with transpose and same input and output
        String stmtTextOne = "insert into SupportBean select transpose(customOne('O' || theString, 10)) from SupportBean(theString like 'I%')";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);
        assertEquals(SupportBean.class, stmtOne.getEventType().getUnderlyingType());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtOne.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("I1", 1));
        EventBean resultOne = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(resultOne, "theString,intPrimitive".split(","), new Object[]{"OI1", 10});
        assertEquals("OI1", ((SupportBean) resultOne.getUnderlying()).getTheString());
        stmtOne.destroy();

        // with transpose but different input and output (also test ignore column name)
        String stmtTextTwo = "insert into SupportBeanNumeric select transpose(customTwo(intPrimitive, intPrimitive+1)) as col1 from SupportBean(theString like 'I%')";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        assertEquals(SupportBeanNumeric.class, stmtTwo.getEventType().getUnderlyingType());
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("I2", 10));
        EventBean resultTwo = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertProps(resultTwo, "intOne,intTwo".split(","), new Object[]{10, 11});
        assertEquals(11, (int) ((SupportBeanNumeric) resultTwo.getUnderlying()).getIntTwo());
        stmtTwo.destroy();

        // invalid wrong-bean target
        try {
            epService.getEPAdministrator().createEPL("insert into SupportBeanNumeric select transpose(customOne('O', 10)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Expression-returned value of type '" + SupportBean.class.getName() + "' cannot be converted to target event type 'SupportBeanNumeric' with underlying type '" + SupportBeanNumeric.class.getName() + "' [insert into SupportBeanNumeric select transpose(customOne('O', 10)) from SupportBean]", ex.getMessage());
        }

        // invalid additional properties
        try {
            epService.getEPAdministrator().createEPL("insert into SupportBean select 1 as dummy, transpose(customOne('O', 10)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Cannot transpose additional properties in the select-clause to target event type 'SupportBean' with underlying type '" + SupportBean.class.getName() + "', the transpose function must occur alone in the select clause [insert into SupportBean select 1 as dummy, transpose(customOne('O', 10)) from SupportBean]", ex.getMessage());
        }

        // invalid occurs twice
        try {
            epService.getEPAdministrator().createEPL("insert into SupportBean select transpose(customOne('O', 10)), transpose(customOne('O', 11)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation [insert into SupportBean select transpose(customOne('O', 10)), transpose(customOne('O', 11)) from SupportBean]", ex.getMessage());
        }

        // invalid wrong-type target
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("SomeOtherStream", new HashMap<>());
            epService.getEPAdministrator().createEPL("insert into SomeOtherStream select transpose(customOne('O', 10)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Expression-returned value of type '" + SupportBean.class.getName() + "' cannot be converted to target event type 'SomeOtherStream' with underlying type 'java.util.Map' [insert into SomeOtherStream select transpose(customOne('O', 10)) from SupportBean]", ex.getMessage());
        }

        // invalid two parameters
        try {
            epService.getEPAdministrator().createEPL("select transpose(customOne('O', 10), customOne('O', 10)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'transpose(customOne(\"O\",10),customO...(46 chars)': The transpose function requires a single parameter expression [select transpose(customOne('O', 10), customOne('O', 10)) from SupportBean]", ex.getMessage());
        }

        // test not a top-level function or used in where-clause (possible but not useful)
        epService.getEPAdministrator().createEPL("select * from SupportBean where transpose(customOne('O', 10)) is not null");
        epService.getEPAdministrator().createEPL("select transpose(customOne('O', 10)) is not null from SupportBean");

        // invalid insert of object-array into undefined stream
        try {
            epService.getEPAdministrator().createEPL("insert into SomeOther select transpose(generateOA('a', 1)) from SupportBean");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Invalid expression return type '[Ljava.lang.Object;' for transpose function [insert into SomeOther select transpose(generateOA('a', 1)) from SupportBean]", ex.getMessage());
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposeEventJoinMap(EPServiceProvider epService) {
        Map<String, Object> metadata = makeMap(new Object[][]{{"id", String.class}});
        epService.getEPAdministrator().getConfiguration().addEventType("AEventTE", metadata);
        epService.getEPAdministrator().getConfiguration().addEventType("BEventTE", metadata);

        String stmtTextOne = "insert into MyStreamTE select a, b from AEventTE#keepall as a, BEventTE#keepall as b";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select a.id, b.id from MyStreamTE";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Map<String, Object> eventOne = makeMap(new Object[][]{{"id", "A1"}});
        Map<String, Object> eventTwo = makeMap(new Object[][]{{"id", "B1"}});
        epService.getEPRuntime().sendEvent(eventOne, "AEventTE");
        epService.getEPRuntime().sendEvent(eventTwo, "BEventTE");

        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposeEventJoinPOJO(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("AEventBean", SupportBean_A.class);
        epService.getEPAdministrator().getConfiguration().addEventType("BEventBean", SupportBean_B.class);

        String stmtTextOne = "insert into MyStream2Bean select a.* as a, b.* as b from AEventBean#keepall as a, BEventBean#keepall as b";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select a.id, b.id from MyStream2Bean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean_A("A1"));
        epService.getEPRuntime().sendEvent(new SupportBean_B("B1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "a.id,b.id".split(","), new Object[]{"A1", "B1"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionTransposePOJOPropertyStream(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("Complex", SupportBeanComplexProps.class);

        String stmtTextOne = "insert into MyStreamComplex select nested as inneritem from Complex";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        String stmtTextTwo = "select inneritem.nestedValue as result from MyStreamComplex";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "result".split(","), new Object[]{"nestedValue"});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalidTranspose(EPServiceProvider epService) {
        Map<String, Object> metadata = makeMap(new Object[][]{
                {"nested", makeMap(new Object[][]{{"nestedValue", String.class}})}
        });
        epService.getEPAdministrator().getConfiguration().addEventType("ComplexMap", metadata);

        String stmtTextOne = "insert into MyStreamComplexMap select nested as inneritem from ComplexMap";
        epService.getEPAdministrator().createEPL(stmtTextOne);

        try {
            String stmtTextTwo = "select inneritem.nestedValue as result from MyStreamComplexMap";
            epService.getEPAdministrator().createEPL(stmtTextTwo);
        } catch (Exception ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'inneritem.nestedValue': Failed to resolve property 'inneritem.nestedValue' (property 'inneritem' is a mapped property and requires keyed access) [select inneritem.nestedValue as result from MyStreamComplexMap]", ex.getMessage());
        }

        // test invalid unwrap-properties
        epService.getEPAdministrator().getConfiguration().addEventType(E1.class);
        epService.getEPAdministrator().getConfiguration().addEventType(E2.class);
        epService.getEPAdministrator().getConfiguration().addEventType(EnrichedE2.class);

        try {
            epService.getEPAdministrator().createEPL("@Resilient insert into EnrichedE2 " +
                    "select e2.* as event, e1.otherId as playerId " +
                    "from E1#length(20) as e1, E2#length(1) as e2 " +
                    "where e1.id = e2.id ");
        } catch (Exception ex) {
            assertEquals("Error starting statement: The 'e2.* as event' syntax is not allowed when inserting into an existing bean event type, use the 'e2 as event' syntax instead [@Resilient insert into EnrichedE2 select e2.* as event, e1.otherId as playerId from E1#length(20) as e1, E2#length(1) as e2 where e1.id = e2.id ]", ex.getMessage());
        }
    }

    public static Map localGenerateMap(String string, int intPrimitive) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("p0", string);
        out.put("p1", intPrimitive);
        return out;
    }

    public static Object[] localGenerateOA(String string, int intPrimitive) {
        return new Object[]{string, intPrimitive};
    }

    public static GenericData.Record localGenerateAvro(String string, int intPrimitive) {
        Schema schema = record("name").fields().requiredString("p0").requiredInt("p1").endRecord();
        GenericData.Record record = new GenericData.Record(schema);
        record.put("p0", string);
        record.put("p1", intPrimitive);
        return record;
    }

    private Map<String, Object> makeMap(Object[][] entries) {
        Map result = new HashMap<String, Object>();
        for (Object[] entry : entries) {
            result.put(entry[0], entry[1]);
        }
        return result;
    }

    public static class E1 implements Serializable {
        private final String id;
        private final String otherId;

        public E1(String id, String otherId) {
            this.id = id;
            this.otherId = otherId;
        }

        public String getId() {
            return id;
        }

        public String getOtherId() {
            return otherId;
        }
    }

    public static class E2 implements Serializable {
        private final String id;
        private final String value;

        public E2(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public String getValue() {
            return value;
        }
    }

    public static class EnrichedE2 implements Serializable {
        private final E2 event;
        private final String otherId;

        public EnrichedE2(E2 event, String playerId) {
            this.event = event;
            this.otherId = playerId;
        }

        public E2 getEvent() {
            return event;
        }

        public String getOtherId() {
            return otherId;
        }
    }
}
