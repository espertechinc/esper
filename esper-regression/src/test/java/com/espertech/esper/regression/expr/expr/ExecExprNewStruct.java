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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.support.EventRepresentationChoice;
import com.espertech.esper.util.JavaClassHelper;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecExprNewStruct implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLogging().setEnableCode(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        runAssertionNewWRepresentation(epService);
        runAssertionDefaultColumnsAndSODA(epService);
        runAssertionNewWithCase(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionNewWRepresentation(EPServiceProvider epService) {
        for (EventRepresentationChoice rep : EventRepresentationChoice.values()) {
            tryAssertionNewWRepresentation(epService, rep);
        }
    }

    private void runAssertionDefaultColumnsAndSODA(EPServiceProvider epService) {
        String epl = "select " +
                "case theString" +
                " when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\"}" +
                " when \"B\" then new{theString,intPrimitive=10,col2=theString||\"B\"} " +
                "end as val0 from SupportBean as sb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        tryAssertionDefault(epService, stmt, listener);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        stmt.addListener(listener);
        tryAssertionDefault(epService, stmt, listener);

        // test to-expression string
        epl = "select " +
                "case theString" +
                " when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\" }" +
                " when \"B\" then new{theString,intPrimitive = 10,col2=theString||\"B\" } " +
                "end from SupportBean as sb";
        stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        assertEquals("case theString when \"A\" then new{theString=\"Q\",intPrimitive,col2=theString||\"A\"} when \"B\" then new{theString,intPrimitive=10,col2=theString||\"B\"} end", stmt.getEventType().getPropertyNames()[0]);
        stmt.destroy();
    }

    private void tryAssertionDefault(EPServiceProvider epService, EPStatement stmt, SupportUpdateListener listener) {

        assertEquals(Map.class, stmt.getEventType().getPropertyType("val0"));
        FragmentEventType fragType = stmt.getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        assertEquals(String.class, fragType.getFragmentType().getPropertyType("theString"));
        assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("intPrimitive"));
        assertEquals(String.class, fragType.getFragmentType().getPropertyType("col2"));

        String[] fieldsInner = "theString,intPrimitive,col2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null, null});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Q", 2, "AA"});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"B", 10, "BB"});

        stmt.destroy();
    }

    private void runAssertionNewWithCase(EPServiceProvider epService) {
        String epl = "select " +
                "case " +
                "  when theString = 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when theString = 'B' then new { col1 = 'Y', col2 = 20 } " +
                "  when theString = 'C' then new { col1 = null, col2 = null } " +
                "  else new { col1 = 'Z', col2 = 30 } " +
                "end as val0 from SupportBean sb";
        tryAssertion(epService, epl);

        epl = "select " +
                "case theString " +
                "  when 'A' then new { col1 = 'X', col2 = 10 } " +
                "  when 'B' then new { col1 = 'Y', col2 = 20 } " +
                "  when 'C' then new { col1 = null, col2 = null } " +
                "  else new{ col1 = 'Z', col2 = 30 } " +
                "end as val0 from SupportBean sb";
        tryAssertion(epService, epl);
    }

    private void tryAssertion(EPServiceProvider epService, String epl) {
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        assertEquals(Map.class, stmt.getEventType().getPropertyType("val0"));
        FragmentEventType fragType = stmt.getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        assertEquals(String.class, fragType.getFragmentType().getPropertyType("col1"));
        assertEquals(Integer.class, fragType.getFragmentType().getPropertyType("col2"));

        String[] fieldsInner = "col1,col2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Z", 30});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"X", 10});

        epService.getEPRuntime().sendEvent(new SupportBean("B", 3));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{"Y", 20});

        epService.getEPRuntime().sendEvent(new SupportBean("C", 4));
        EPAssertionUtil.assertPropsMap((Map) listener.assertOneGetNewAndReset().get("val0"), fieldsInner, new Object[]{null, null});

        stmt.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        String epl;

        epl = "select case when true then new { col1 = 'a' } else 1 end from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(44 chars)': Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value, check the else-condition [select case when true then new { col1 = 'a' } else 1 end from SupportBean]");

        epl = "select case when true then new { col1 = 'a' } when false then 1 end from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'case when true then new{col1=\"a\"} w...(55 chars)': Case node 'when' expressions require that all results either return a single value or a Map-type (new-operator) value, check when-condition number 1 [select case when true then new { col1 = 'a' } when false then 1 end from SupportBean]");

        epl = "select case when true then new { col1 = 'a' } else new { col1 = 1 } end from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(54 chars)': Incompatible case-when return types by new-operator in case-when number 1: Type by name 'Case-when number 1' in property 'col1' expected class java.lang.String but receives class java.lang.Integer [select case when true then new { col1 = 'a' } else new { col1 = 1 } end from SupportBean]");

        epl = "select case when true then new { col1 = 'a' } else new { col2 = 'a' } end from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'case when true then new{col1=\"a\"} e...(56 chars)': Incompatible case-when return types by new-operator in case-when number 1: The property 'col1' is not provided but required [select case when true then new { col1 = 'a' } else new { col2 = 'a' } end from SupportBean]");

        epl = "select case when true then new { col1 = 'a', col1 = 'b' } end from SupportBean";
        tryInvalid(epService, epl, "Error starting statement: Failed to validate select-clause expression 'case when true then new{col1=\"a\",co...(46 chars)': Failed to validate new-keyword property names, property 'col1' has already been declared [select case when true then new { col1 = 'a', col1 = 'b' } end from SupportBean]");
    }

    private void tryAssertionNewWRepresentation(EPServiceProvider epService, EventRepresentationChoice rep) {
        String epl = rep.getAnnotationText() + "select new { theString = 'x' || theString || 'x', intPrimitive = intPrimitive + 2} as val0 from SupportBean as sb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertEquals(rep.isAvroEvent() ? GenericData.Record.class : Map.class, stmt.getEventType().getPropertyType("val0"));
        FragmentEventType fragType = stmt.getEventType().getFragmentType("val0");
        assertFalse(fragType.isIndexed());
        assertFalse(fragType.isNative());
        assertEquals(String.class, fragType.getFragmentType().getPropertyType("theString"));
        assertEquals(Integer.class, JavaClassHelper.getBoxedType(fragType.getFragmentType().getPropertyType("intPrimitive")));

        String[] fieldsInner = "theString,intPrimitive".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", -5));
        EventBean event = listener.assertOneGetNewAndReset();
        if (rep.isAvroEvent()) {
            SupportAvroUtil.avroToJson(event);
            GenericData.Record inner = (GenericData.Record) event.get("val0");
            assertEquals("xE1x", inner.get("theString"));
            assertEquals(-3, inner.get("intPrimitive"));
        } else {
            EPAssertionUtil.assertPropsMap((Map) event.get("val0"), fieldsInner, new Object[]{"xE1x", -3});
        }

        stmt.destroy();
    }
}
