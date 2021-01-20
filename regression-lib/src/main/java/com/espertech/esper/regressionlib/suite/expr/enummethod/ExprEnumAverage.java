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
package com.espertech.esper.regressionlib.suite.expr.enummethod;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_Container;
import com.espertech.esper.regressionlib.support.bean.SupportCollection;
import com.espertech.esper.regressionlib.support.expreval.SupportEvalBuilder;

import java.math.BigDecimal;
import java.util.*;

import static com.espertech.esper.common.client.type.EPTypePremade.BIGDECIMAL;
import static com.espertech.esper.common.client.type.EPTypePremade.DOUBLEBOXED;
import static com.espertech.esper.common.internal.support.SupportEventPropUtil.assertTypes;

public class ExprEnumAverage {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ExprEnumAverageEvents());
        execs.add(new ExprEnumAverageScalar());
        execs.add(new ExprEnumAverageScalarMore());
        execs.add(new ExprEnumAverageInvalid());
        return execs;
    }

    private static class ExprEnumAverageEvents implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6,c7,c8".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportBean_Container");
            builder.expression(fields[0], "beans.average(x => intBoxed)");
            builder.expression(fields[1], "beans.average(x => doubleBoxed)");
            builder.expression(fields[2], "beans.average(x => longBoxed)");
            builder.expression(fields[3], "beans.average(x => bigDecimal)");
            builder.expression(fields[4], "beans.average( (x, i) => intBoxed + i*10)");
            builder.expression(fields[5], "beans.average( (x, i) => bigDecimal + i*10)");
            builder.expression(fields[6], "beans.average( (x, i, s) => intBoxed + i*10 + s*100)");
            builder.expression(fields[7], "beans.average( (x, i, s) => bigDecimal + i*10 + s*100)");
            builder.expression(fields[8], "beans.average( (x, i, s) => case when i = 1 then null else 2 end)");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields,
                new EPTypeClass[]{DOUBLEBOXED.getEPType(), DOUBLEBOXED.getEPType(), DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType()}));

            builder.assertion(new SupportBean_Container(null))
                .expect(fields, null, null, null, null, null, null, null, null, null);

            builder.assertion(new SupportBean_Container(Collections.emptyList()))
                .expect(fields, null, null, null, null, null, null, null, null, null);

            List<SupportBean> listOne = new ArrayList<>(Arrays.asList(make(2, 3d, 4L, 5)));
            builder.assertion(new SupportBean_Container(listOne))
                .expect(fields, 2d, 3d, 4d, new BigDecimal(5.0d), 2d, new BigDecimal(5.0d), 102d, new BigDecimal(105.0d), 2d);

            List<SupportBean> listTwo = new ArrayList<>(Arrays.asList(make(2, 3d, 4L, 5), make(4, 6d, 8L, 10)));
            builder.assertion(new SupportBean_Container(listTwo))
                .expect(fields, (2 + 4) / 2d, (3d + 6d) / 2d, (4L + 8L) / 2d, new BigDecimal((5 + 10) / 2d),
                    (2 + 14) / 2d, new BigDecimal((5 + 20) / 2d), (202 + 214) / 2d, new BigDecimal((205 + 220) / 2d), 2d);

            builder.run(env);
        }
    }

    private static class ExprEnumAverageScalar implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "intvals.average()");
            builder.expression(fields[1], "bdvals.average()");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields, new EPTypeClass[]{DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType()}));

            builder.assertion(SupportCollection.makeNumeric("1,2,3")).expect(fields, 2d, new BigDecimal(2d));

            builder.assertion(SupportCollection.makeNumeric("1,null,3")).expect(fields, 2d, new BigDecimal(2d));

            builder.assertion(SupportCollection.makeNumeric("4")).expect(fields, 4d, new BigDecimal(4d));

            builder.run(env);
        }
    }

    private static class ExprEnumAverageScalarMore implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "c0,c1,c2,c3,c4,c5,c6".split(",");
            SupportEvalBuilder builder = new SupportEvalBuilder("SupportCollection");
            builder.expression(fields[0], "strvals.average(v => extractNum(v))");
            builder.expression(fields[1], "strvals.average(v => extractBigDecimal(v))");
            builder.expression(fields[2], "strvals.average( (v, i) => extractNum(v) + i*10)");
            builder.expression(fields[3], "strvals.average( (v, i) => extractBigDecimal(v) + i*10)");
            builder.expression(fields[4], "strvals.average( (v, i, s) => extractNum(v) + i*10 + s*100)");
            builder.expression(fields[5], "strvals.average( (v, i, s) => extractBigDecimal(v) + i*10 + s*100)");
            builder.expression(fields[6], "strvals.average( (v, i, s) => case when i = 1 then null else 2 end)");

            builder.statementConsumer(stmt -> assertTypes(stmt.getEventType(), fields,
                new EPTypeClass[]{DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType(), BIGDECIMAL.getEPType(), DOUBLEBOXED.getEPType()}));

            builder.assertion(SupportCollection.makeString("E2,E1,E5,E4"))
                .expect(fields, (2 + 1 + 5 + 4) / 4d, new BigDecimal((2 + 1 + 5 + 4) / 4d),
                    (2 + 11 + 25 + 34) / 4d, new BigDecimal((2 + 11 + 25 + 34) / 4d), (402 + 411 + 425 + 434) / 4d, new BigDecimal((402 + 411 + 425 + 434) / 4d), 2d);

            builder.assertion(SupportCollection.makeString("E1"))
                .expect(fields, 1d, new BigDecimal(1), 1d, new BigDecimal(1), 101d, new BigDecimal(101), 2d);

            builder.assertion(SupportCollection.makeString(null)).expect(fields, null, null, null, null, null, null, null);

            builder.assertion(SupportCollection.makeString("")).expect(fields, null, null, null, null, null, null, null);

            builder.run(env);
        }
    }

    private static class ExprEnumAverageInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "select strvals.average() from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'strvals.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of numeric values as input, received java.util.Collection<String> [select strvals.average() from SupportCollection]");

            epl = "select beans.average() from SupportBean_Container";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'beans.average()': Invalid input for built-in enumeration method 'average' and 0-parameter footprint, expecting collection of values (typically scalar values) as input, received collection of events of type '" + SupportBean.class.getName() + "'");

            epl = "select strvals.average(v => null) from SupportCollection";
            env.tryInvalidCompile(epl, "Failed to validate select-clause expression 'strvals.average()': Failed to validate enumeration method 'average', expected a non-null result for expression parameter 0 but received a null-typed expression");
        }
    }

    private static SupportBean make(Integer intBoxed, Double doubleBoxed, Long longBoxed, int bigDecimal) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setDoubleBoxed(doubleBoxed);
        bean.setLongBoxed(longBoxed);
        bean.setBigDecimal(new BigDecimal(bigDecimal));
        return bean;
    }
}
