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
package com.espertech.esper.regressionrun.suite.expr;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.suite.expr.exprcore.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.HashMap;

public class TestSuiteExprCore extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testExprCoreRelOp() {
        RegressionRunner.run(session, new ExprCoreRelOp());
    }

    public void testExprCoreAndOrNot() {
        RegressionRunner.run(session, new ExprCoreAndOrNot());
    }

    public void testExprCoreConcat() {
        RegressionRunner.run(session, new ExprCoreConcat());
    }

    public void testExprCoreLikeRegexp() {
        RegressionRunner.run(session, ExprCoreLikeRegexp.executions());
    }

    public void testExprCoreMath() {
        RegressionRunner.run(session, ExprCoreMath.executions());
    }

    public void testExprCoreInBetweenLike() {
        RegressionRunner.run(session, ExprCoreInBetweenLike.executions());
    }

    public void testExprCoreArray() {
        RegressionRunner.run(session, ExprCoreArray.executions());
    }

    public void testExprCoreAnyAllSome() {
        RegressionRunner.run(session, ExprCoreAnyAllSome.executions());
    }

    public void testExprCoreBitWiseOperators() {
        RegressionRunner.run(session, ExprCoreBitWiseOperators.executions());
    }

    public void testExprCoreCoalesce() {
        RegressionRunner.run(session, ExprCoreCoalesce.executions());
    }

    public void testExprCoreNewInstance() {
        RegressionRunner.run(session, ExprCoreNewInstance.executions());
    }

    public void testExprCoreCast() {
        RegressionRunner.run(session, ExprCoreCast.executions());
    }

    public void testExprCoreCase() {
        RegressionRunner.run(session, ExprCoreCase.executions());
    }

    public void testExprCoreCurrentTimestamp() {
        RegressionRunner.run(session, ExprCoreCurrentTimestamp.executions());
    }

    public void testExprCoreEqualsIs() {
        RegressionRunner.run(session, ExprCoreEqualsIs.executions());
    }

    public void testExprCoreInstanceOf() {
        RegressionRunner.run(session, ExprCoreInstanceOf.executions());
    }

    public void testExprCoreExists() {
        RegressionRunner.run(session, ExprCoreExists.executions());
    }

    public void testExprCoreNewStruct() {
        RegressionRunner.run(session, ExprCoreNewStruct.executions());
    }

    public void testExprCoreDotExpression() {
        RegressionRunner.run(session, ExprCoreDotExpression.executions());
    }

    public void testExprCoreMinMaxNonAgg() {
        RegressionRunner.run(session, ExprCoreMinMaxNonAgg.executions());
    }

    public void testExprCoreBigNumberSupport() {
        RegressionRunner.run(session, ExprCoreBigNumberSupport.executions());
    }

    public void testExprCoreCurrentEvaluationContext() {
        RegressionRunner.run(session, ExprCoreCurrentEvaluationContext.executions());
    }

    public void testExprCoreTypeOf() {
        RegressionRunner.run(session, ExprCoreTypeOf.executions());
    }

    public void testExprCorePrevious() {
        RegressionRunner.run(session, ExprCorePrevious.executions());
    }

    public void testExprCorePrior() {
        RegressionRunner.run(session, ExprCorePrior.executions());
    }

    public void testExprEventIdentityEquals() {
        RegressionRunner.run(session, ExprCoreEventIdentityEquals.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBeanArrayCollMap.class, SupportBeanComplexProps.class, SupportBean_StringAlphabetic.class, SupportMarkerInterface.class,
            SupportBeanDynRoot.class, SupportMarketDataBean.class, SupportBeanWithEnum.class, SupportEnumTwo.class,
            SupportEventTypeErasure.class, SupportChainTop.class, SupportLevelZero.class, SupportEventNode.class,
            SupportEventNodeData.class, SupportBeanCombinedProps.class, SupportBeanNumeric.class,
            ISupportA.class, ISupportABCImpl.class, ISupportAImpl.class, SupportBean_ST0.class, SupportBeanObject.class,
            SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addEventType("MyDateType", CollectionUtil.populateNameValueMap("yyyymmdd", String.class, "yyyymmddhhmmss", String.class, "hhmmss", String.class, "yyyymmddhhmmssvv", String.class));

        configuration.getCommon().addImport(SupportBean.class);
        configuration.getCommon().addImport(SupportEnum.class);
        configuration.getCommon().addImport(SupportPrivateCtor.class);
        configuration.getCommon().addImport(SupportObjectCtor.class);
        configuration.getCommon().addImport(SupportEnumTwo.class);
        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());

        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("anInt", String.class);
        map.put("anDouble", String.class);
        map.put("anLong", String.class);
        map.put("anFloat", String.class);
        map.put("anByte", String.class);
        map.put("anShort", String.class);
        map.put("intPrimitive", int.class);
        map.put("intBoxed", Integer.class);
        configuration.getCommon().addEventType("StaticTypeMapEvent", map);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
    }
}
