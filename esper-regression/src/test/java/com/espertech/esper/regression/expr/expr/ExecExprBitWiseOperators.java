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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;


public class ExecExprBitWiseOperators implements RegressionExecution {
    private static final byte FIRST_EVENT = 1;
    private static final short SECOND_EVENT = 2;
    private static final int THIRD_EVENT = FIRST_EVENT | SECOND_EVENT;
    private static final long FOURTH_EVENT = 4;
    private static final boolean FITH_EVENT = false;

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionBitWiseOperators_OM(epService);
        runAssertionBitWiseOperators(epService);
    }

    private void runAssertionBitWiseOperators_OM(EPServiceProvider epService) throws Exception {
        String epl = "select bytePrimitive&byteBoxed as myFirstProperty, " +
                "shortPrimitive|shortBoxed as mySecondProperty, " +
                "intPrimitive|intBoxed as myThirdProperty, " +
                "longPrimitive^longBoxed as myFourthProperty, " +
                "boolPrimitive&boolBoxed as myFifthProperty " +
                "from " + SupportBean.class.getName() + "#length(3)";

        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.binaryAnd().add("bytePrimitive").add("byteBoxed"), "myFirstProperty")
                .add(Expressions.binaryOr().add("shortPrimitive").add("shortBoxed"), "mySecondProperty")
                .add(Expressions.binaryOr().add("intPrimitive").add("intBoxed"), "myThirdProperty")
                .add(Expressions.binaryXor().add("longPrimitive").add("longBoxed"), "myFourthProperty")
                .add(Expressions.binaryAnd().add("boolPrimitive").add("boolBoxed"), "myFifthProperty")
        );
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName()).addView("length", Expressions.constant(3))));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(epl, model.toEPL());

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        runBitWiseOperators(epService, listener);

        stmt.destroy();
    }

    private void runAssertionBitWiseOperators(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = setUpBitWiseStmt(epService, listener);
        runBitWiseOperators(epService, listener);
        stmt.destroy();
    }

    private void runBitWiseOperators(EPServiceProvider epService, SupportUpdateListener listener) {
        sendEvent(epService, FIRST_EVENT, new Byte(FIRST_EVENT), SECOND_EVENT, new Short(SECOND_EVENT),
                FIRST_EVENT, new Integer(THIRD_EVENT), 3L, new Long(FOURTH_EVENT),
                FITH_EVENT, new Boolean(FITH_EVENT));

        EventBean received = listener.getAndResetLastNewData()[0];
        assertEquals((byte) 1, received.get("myFirstProperty"));
        assertTrue(((Short) (received.get("mySecondProperty")) & SECOND_EVENT) == SECOND_EVENT);
        assertTrue(((Integer) (received.get("myThirdProperty")) & FIRST_EVENT) == FIRST_EVENT);
        assertEquals(7L, received.get("myFourthProperty"));
        assertEquals(false, received.get("myFifthProperty"));
    }

    private EPStatement setUpBitWiseStmt(EPServiceProvider epService, SupportUpdateListener listener) {
        String epl = "select (bytePrimitive & byteBoxed) as myFirstProperty, " +
                "(shortPrimitive | shortBoxed) as mySecondProperty, " +
                "(intPrimitive | intBoxed) as myThirdProperty, " +
                "(longPrimitive ^ longBoxed) as myFourthProperty, " +
                "(boolPrimitive & boolBoxed) as myFifthProperty " +
                " from " + SupportBean.class.getName() + "#length(3) ";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        EventType type = stmt.getEventType();
        assertEquals(Byte.class, type.getPropertyType("myFirstProperty"));
        assertEquals(Short.class, type.getPropertyType("mySecondProperty"));
        assertEquals(Integer.class, type.getPropertyType("myThirdProperty"));
        assertEquals(Long.class, type.getPropertyType("myFourthProperty"));
        assertEquals(Boolean.class, type.getPropertyType("myFifthProperty"));

        return stmt;
    }

    protected void sendEvent(EPServiceProvider epService, byte bytePrimitive, Byte byteBoxed, short shortPrimitive, Short shortBoxed,
             int intPrimitive, Integer intBoxed, long longPrimitive, Long longBoxed,
             boolean boolPrimitive, Boolean boolBoxed) {
        SupportBean bean = new SupportBean();
        bean.setBytePrimitive(bytePrimitive);
        bean.setByteBoxed(byteBoxed);
        bean.setShortPrimitive(shortPrimitive);
        bean.setShortBoxed(shortBoxed);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        bean.setBoolPrimitive(boolPrimitive);
        bean.setBoolBoxed(boolBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExecExprBitWiseOperators.class);
}
