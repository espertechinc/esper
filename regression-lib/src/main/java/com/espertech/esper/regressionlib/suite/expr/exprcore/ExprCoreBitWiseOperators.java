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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

public class ExprCoreBitWiseOperators {
    private static final byte FIRST_EVENT = 1;
    private static final short SECOND_EVENT = 2;
    private static final int THIRD_EVENT = FIRST_EVENT | SECOND_EVENT;
    private static final long FOURTH_EVENT = 4;
    private static final boolean FITH_EVENT = false;

    private final static String EPL = "select bytePrimitive&byteBoxed as myFirstProperty, " +
        "shortPrimitive|shortBoxed as mySecondProperty, " +
        "intPrimitive|intBoxed as myThirdProperty, " +
        "longPrimitive^longBoxed as myFourthProperty, " +
        "boolPrimitive&boolBoxed as myFifthProperty " +
        "from SupportBean";

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreBitWiseOp());
        executions.add(new ExprCoreBitWiseOpOM());
        return executions;
    }

    private static class ExprCoreBitWiseOpOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create()
                .add(Expressions.binaryAnd().add("bytePrimitive").add("byteBoxed"), "myFirstProperty")
                .add(Expressions.binaryOr().add("shortPrimitive").add("shortBoxed"), "mySecondProperty")
                .add(Expressions.binaryOr().add("intPrimitive").add("intBoxed"), "myThirdProperty")
                .add(Expressions.binaryXor().add("longPrimitive").add("longBoxed"), "myFourthProperty")
                .add(Expressions.binaryAnd().add("boolPrimitive").add("boolBoxed"), "myFifthProperty")
            );
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            model = SerializableObjectCopier.copyMayFail(model);
            TestCase.assertEquals(EPL, model.toEPL());

            env.compileDeploy("@name('s0')  " + EPL).addListener("s0");

            runBitWiseOperators(env);

            env.undeployAll();
        }
    }

    private static class ExprCoreBitWiseOp implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') " + EPL).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            TestCase.assertEquals(Byte.class, type.getPropertyType("myFirstProperty"));
            TestCase.assertEquals(Short.class, type.getPropertyType("mySecondProperty"));
            TestCase.assertEquals(Integer.class, type.getPropertyType("myThirdProperty"));
            TestCase.assertEquals(Long.class, type.getPropertyType("myFourthProperty"));
            TestCase.assertEquals(Boolean.class, type.getPropertyType("myFifthProperty"));

            runBitWiseOperators(env);

            env.undeployAll();
        }
    }

    private static void runBitWiseOperators(RegressionEnvironment env) {
        sendEvent(env, FIRST_EVENT, new Byte(FIRST_EVENT), SECOND_EVENT, new Short(SECOND_EVENT),
            FIRST_EVENT, new Integer(THIRD_EVENT), 3L, new Long(FOURTH_EVENT),
            FITH_EVENT, new Boolean(FITH_EVENT));

        EventBean received = env.listener("s0").getAndResetLastNewData()[0];
        TestCase.assertEquals((byte) 1, received.get("myFirstProperty"));
        assertTrue(((Short) (received.get("mySecondProperty")) & SECOND_EVENT) == SECOND_EVENT);
        assertTrue(((Integer) (received.get("myThirdProperty")) & FIRST_EVENT) == FIRST_EVENT);
        TestCase.assertEquals(7L, received.get("myFourthProperty"));
        TestCase.assertEquals(false, received.get("myFifthProperty"));
    }

    protected static void sendEvent(RegressionEnvironment env, byte bytePrimitive, Byte byteBoxed, short shortPrimitive, Short shortBoxed,
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
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(ExprCoreBitWiseOp.class);
}
