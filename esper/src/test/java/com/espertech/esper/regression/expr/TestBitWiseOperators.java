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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestBitWiseOperators extends TestCase
{
    private static final byte FIRST_EVENT = 1;
    private static final short SECOND_EVENT = 2;
    private static final int THIRD_EVENT = FIRST_EVENT | SECOND_EVENT;
    private static final long FOURTH_EVENT = 4;
    private static final boolean FITH_EVENT = false;

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testBitWiseOperators_OM() throws Exception
    {
        String viewExpr = "select bytePrimitive&byteBoxed as myFirstProperty, " +
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
        assertEquals(viewExpr, model.toEPL());

        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        runBitWiseOperators();
    }

    public void testBitWiseOperators()
    {
        setUpBitWiseStmt();
        listener.reset();

        runBitWiseOperators();
    }

    private void runBitWiseOperators()
    {
        sendEvent(FIRST_EVENT, new Byte(FIRST_EVENT), SECOND_EVENT, new Short(SECOND_EVENT),
                FIRST_EVENT, new Integer(THIRD_EVENT), 3L, new Long(FOURTH_EVENT),
                FITH_EVENT, new Boolean(FITH_EVENT));

        EventBean received = listener.getAndResetLastNewData()[0];
        assertEquals((byte) 1, (received.get("myFirstProperty")));
        assertTrue(((Short) (received.get("mySecondProperty")) & SECOND_EVENT) == SECOND_EVENT);
        assertTrue(((Integer) (received.get("myThirdProperty")) & FIRST_EVENT) == FIRST_EVENT);
        assertEquals(7L, (received.get("myFourthProperty")));
        assertEquals(false, (received.get("myFifthProperty")));
    }

    private EPStatement setUpBitWiseStmt()
    {
        String viewExpr = "select (bytePrimitive & byteBoxed) as myFirstProperty, " +
                "(shortPrimitive | shortBoxed) as mySecondProperty, " +
                "(intPrimitive | intBoxed) as myThirdProperty, " +
                "(longPrimitive ^ longBoxed) as myFourthProperty, " +
                "(boolPrimitive & boolBoxed) as myFifthProperty " +
                " from " + SupportBean.class.getName() + "#length(3) ";
        EPStatement selectTestView = epService.getEPAdministrator().createEPL(viewExpr);
        selectTestView.addListener(listener);

        EventType type = selectTestView.getEventType();
        assertEquals(Byte.class, type.getPropertyType("myFirstProperty"));
        assertEquals(Short.class, type.getPropertyType("mySecondProperty"));
        assertEquals(Integer.class, type.getPropertyType("myThirdProperty"));
        assertEquals(Long.class, type.getPropertyType("myFourthProperty"));
        assertEquals(Boolean.class, type.getPropertyType("myFifthProperty"));

        return selectTestView;
    }

    protected void sendEvent
            (byte bytePrimitive_, Byte byteBoxed_, short shortPrimitive_, Short shortBoxed,
             int intPrimitive_, Integer intBoxed_, long longPrimitive_, Long longBoxed_,
             boolean boolPrimitive_, Boolean boolBoxed_)
    {
        SupportBean bean = new SupportBean();
        bean.setBytePrimitive(bytePrimitive_);
        bean.setByteBoxed(byteBoxed_);
        bean.setShortPrimitive(shortPrimitive_);
        bean.setShortBoxed(shortBoxed);
        bean.setIntPrimitive(intPrimitive_);
        bean.setIntBoxed(intBoxed_);
        bean.setLongPrimitive(longPrimitive_);
        bean.setLongBoxed(longBoxed_);
        bean.setBoolPrimitive(boolPrimitive_);
        bean.setBoolBoxed(boolBoxed_);
        epService.getEPRuntime().sendEvent(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(TestBitWiseOperators.class);
}
