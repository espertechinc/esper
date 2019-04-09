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
import com.espertech.esper.common.client.util.EventUnderlyingType;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.epl.join.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.apache.avro.SchemaBuilder.record;

public class TestSuiteEPLJoin extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLJoin2StreamSimple() {
        RegressionRunner.run(session, new EPLJoin2StreamSimple());
    }

    public void testEPLJoinSelectClause() {
        RegressionRunner.run(session, new EPLJoinSelectClause());
    }

    public void testEPLJoinSingleOp3Stream() {
        RegressionRunner.run(session, EPLJoinSingleOp3Stream.executions());
    }

    public void testEPLJoin2StreamAndPropertyPerformance() {
        RegressionRunner.run(session, EPLJoin2StreamAndPropertyPerformance.executions());
    }

    public void testEPLJoin2StreamSimplePerformance() {
        RegressionRunner.run(session, EPLJoin2StreamSimplePerformance.executions());
    }

    public void testEPLJoin2StreamRangePerformance() {
        RegressionRunner.run(session, EPLJoin2StreamRangePerformance.executions());
    }

    public void testEPLJoin2StreamSimpleCoercionPerformance() {
        RegressionRunner.run(session, EPLJoin2StreamSimpleCoercionPerformance.executions());
    }

    public void testEPLJoin3StreamRangePerformance() {
        RegressionRunner.run(session, EPLJoin3StreamRangePerformance.executions());
    }

    public void testEPLJoin5StreamPerformance() {
        RegressionRunner.run(session, new EPLJoin5StreamPerformance());
    }

    public void testEPLJoin2StreamExprPerformance() {
        RegressionRunner.run(session, new EPLJoin2StreamExprPerformance());
    }

    public void testEPLJoinCoercion() {
        RegressionRunner.run(session, EPLJoinCoercion.executions());
    }

    public void testEPLJoinMultiKeyAndRange() {
        RegressionRunner.run(session, EPLJoinMultiKeyAndRange.executions());
    }

    public void testEPLJoinStartStop() {
        RegressionRunner.run(session, EPLJoinStartStop.executions());
    }

    public void testEPLJoinDerivedValueViews() {
        RegressionRunner.run(session, new EPLJoinDerivedValueViews());
    }

    public void testEPLJoinNoTableName() {
        RegressionRunner.run(session, new EPLJoinNoTableName());
    }

    public void testEPLJoinNoWhereClause() {
        RegressionRunner.run(session, EPLJoinNoWhereClause.executions());
    }

    public void testEPLJoinEventRepresentation() {
        RegressionRunner.run(session, EPLJoinEventRepresentation.executions());
    }

    public void testEPLJoinInheritAndInterface() {
        RegressionRunner.run(session, new EPLJoinInheritAndInterface());
    }

    public void testEPLJoinPatterns() {
        RegressionRunner.run(session, EPLJoinPatterns.executions());
    }

    public void testEPLJoin2StreamInKeywordPerformance() {
        RegressionRunner.run(session, EPLJoin2StreamInKeywordPerformance.executions());
    }

    public void testEPLOuterJoin2Stream() {
        RegressionRunner.run(session, EPLOuterJoin2Stream.executions());
    }

    public void testEPLJoinUniqueIndex() {
        RegressionRunner.run(session, new EPLJoinUniqueIndex());
    }

    public void testEPLOuterFullJoin3Stream() {
        RegressionRunner.run(session, EPLOuterFullJoin3Stream.executions());
    }

    public void testEPLOuterInnerJoin3Stream() {
        RegressionRunner.run(session, EPLOuterInnerJoin3Stream.executions());
    }

    public void testEPLOuterInnerJoin4Stream() {
        RegressionRunner.run(session, EPLOuterInnerJoin4Stream.executions());
    }

    public void testEPLOuterJoin6Stream() {
        RegressionRunner.run(session, EPLOuterJoin6Stream.executions());
    }

    public void testEPLOuterJoin7Stream() {
        RegressionRunner.run(session, EPLOuterJoin7Stream.executions());
    }

    public void testEPLOuterJoinCart4Stream() {
        RegressionRunner.run(session, EPLOuterJoinCart4Stream.executions());
    }

    public void testEPLOuterJoinCart5Stream() {
        RegressionRunner.run(session, EPLOuterJoinCart5Stream.executions());
    }

    public void testEPLOuterJoinChain4Stream() {
        RegressionRunner.run(session, EPLOuterJoinChain4Stream.executions());
    }

    public void testEPLOuterJoinUnidirectional() {
        RegressionRunner.run(session, EPLOuterJoinUnidirectional.executions());
    }

    public void testEPLOuterJoinVarA3Stream() {
        RegressionRunner.run(session, EPLOuterJoinVarA3Stream.executions());
    }

    public void testEPLOuterJoinVarB3Stream() {
        RegressionRunner.run(session, EPLOuterJoinVarB3Stream.executions());
    }

    public void testEPLOuterJoinVarC3Stream() {
        RegressionRunner.run(session, EPLOuterJoinVarC3Stream.executions());
    }

    public void testEPLOuterJoinLeftWWhere() {
        RegressionRunner.run(session, EPLOuterJoinLeftWWhere.executions());
    }

    public void testEPLJoinUnidirectionalStream() {
        RegressionRunner.run(session, EPLJoinUnidirectionalStream.executions());
    }

    public void testEPLJoin3StreamAndPropertyPerformance() {
        RegressionRunner.run(session, EPLJoin3StreamAndPropertyPerformance.executions());
    }

    public void testEPLJoin3StreamCoercionPerformance() {
        RegressionRunner.run(session, EPLJoin3StreamCoercionPerformance.executions());
    }

    public void testEPLJoin3StreamOuterJoinCoercionPerformance() {
        RegressionRunner.run(session, EPLJoin3StreamOuterJoinCoercionPerformance.executions());
    }

    public void testEPLJoin20Stream() {
        RegressionRunner.run(session, new EPLJoin20Stream());
    }

    public void testEPLJoin3StreamInKeywordPerformance() {
        RegressionRunner.run(session, new EPLJoin3StreamInKeywordPerformance());
    }

    public void testEPLJoinPropertyAccess() {
        RegressionRunner.run(session, EPLJoinPropertyAccess.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_A.class, SupportBean_B.class, SupportBean_C.class, SupportBean_D.class,
            SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class, SupportBean_S3.class, SupportBean_S4.class,
            SupportBean_S5.class, SupportBean_S6.class, SupportBeanComplexProps.class, ISupportA.class, ISupportB.class, ISupportAImpl.class, ISupportBImpl.class,
            SupportBeanCombinedProps.class, SupportSimpleBeanOne.class, SupportSimpleBeanTwo.class, SupportEventWithManyArray.class,
            SupportMarketDataBean.class, SupportBean_ST0.class, SupportBean_ST1.class, SupportBeanRange.class,
            SupportEventWithIntArray.class, SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> typeInfo = new HashMap<String, Object>();
        typeInfo.put("id", String.class);
        typeInfo.put("p00", int.class);
        configuration.getCommon().addEventType("MapS0", typeInfo);
        configuration.getCommon().addEventType("MapS1", typeInfo);

        Map<String, Object> mapType = new HashMap<>();
        mapType.put("col1", String.class);
        mapType.put("col2", String.class);
        configuration.getCommon().addEventType("Type1", mapType);
        configuration.getCommon().addEventType("Type2", mapType);
        configuration.getCommon().addEventType("Type3", mapType);

        Map<String, Object> typeInfoS0S0 = new HashMap<>();
        typeInfoS0S0.put("id", String.class);
        typeInfoS0S0.put("p00", int.class);
        configuration.getCommon().addEventType("S0_" + EventUnderlyingType.MAP.name(), typeInfoS0S0);
        configuration.getCommon().addEventType("S1_" + EventUnderlyingType.MAP.name(), typeInfoS0S0);

        String[] names = "id,p00".split(",");
        Object[] types = new Object[]{String.class, int.class};
        configuration.getCommon().addEventType("S0_" + EventUnderlyingType.OBJECTARRAY.name(), names, types);
        configuration.getCommon().addEventType("S1_" + EventUnderlyingType.OBJECTARRAY.name(), names, types);

        Schema schema = record("name").fields()
            .name("id").type(SchemaBuilder.builder().stringBuilder().prop(AvroConstant.PROP_JAVA_STRING_KEY, AvroConstant.PROP_JAVA_STRING_VALUE).endString()).noDefault()
            .requiredInt("p00").endRecord();
        configuration.getCommon().addEventTypeAvro("S0_" + EventUnderlyingType.AVRO.name(), new ConfigurationCommonEventTypeAvro().setAvroSchema(schema));
        configuration.getCommon().addEventTypeAvro("S1_" + EventUnderlyingType.AVRO.name(), new ConfigurationCommonEventTypeAvro().setAvroSchema(schema));

        configuration.getCompiler().addPlugInSingleRowFunction("myStaticEvaluator", EPLJoin2StreamAndPropertyPerformance.MyStaticEval.class.getName(), "myStaticEvaluator");

        configuration.getCommon().getLogging().setEnableQueryPlan(true);
    }
}
