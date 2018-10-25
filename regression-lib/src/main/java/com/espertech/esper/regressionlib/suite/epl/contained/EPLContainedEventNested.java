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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bookexample.OrderBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.util.SupportAdminUtil.assertStatelessStmt;
import static org.junit.Assert.*;

public class EPLContainedEventNested {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLContainedNamedWindowFilter());
        execs.add(new EPLContainedNamedWindowSubquery());
        execs.add(new EPLContainedNamedWindowOnTrigger());
        execs.add(new EPLContainedSimple());
        execs.add(new EPLContainedWhere());
        execs.add(new EPLContainedColumnSelect());
        execs.add(new EPLContainedPatternSelect());
        execs.add(new EPLContainedSubSelect());
        execs.add(new EPLContainedUnderlyingSelect());
        execs.add(new EPLContainedInvalid());
        return execs;
    }

    private static class EPLContainedNamedWindowFilter implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "reviewId".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window OrderWindowNWF#lastevent as OrderBean", path);
            env.compileDeploy("insert into OrderWindowNWF select * from OrderBean", path);

            String stmtText = "@name('s0') select reviewId from OrderWindowNWF[books][reviews] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{1}, {2}, {10}});
            env.listener("s0").reset();

            env.sendEventBean(OrderBeanFactory.makeEventFour());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{201}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLContainedNamedWindowSubquery implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,totalPrice".split(",");

            RegressionPath path = new RegressionPath();
            env.compileDeploy("create window OrderWindowNWS#lastevent as OrderBean", path);
            env.compileDeploy("insert into OrderWindowNWS select * from OrderBean", path);

            String stmtText = "@name('s0') select *, (select sum(price) from OrderWindowNWS[books]) as totalPrice from SupportBean";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            env.sendEventBean(new SupportBean("E1", 1));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E1", 24d + 35d + 27d}});
            env.listener("s0").reset();

            env.sendEventBean(OrderBeanFactory.makeEventFour());
            env.sendEventBean(new SupportBean("E2", 2));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"E2", 15d + 13d}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLContainedNamedWindowOnTrigger implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "theString,intPrimitive".split(",");
            RegressionPath path = new RegressionPath();

            env.compileDeploy("create window SupportBeanWindow#lastevent as SupportBean", path);
            env.compileDeploy("insert into SupportBeanWindow select * from SupportBean", path);
            env.compileDeploy("create window OrderWindowNWOT#lastevent as OrderBean", path);
            env.compileDeploy("insert into OrderWindowNWOT select * from OrderBean", path);

            String stmtText = "@name('s0') on OrderWindowNWOT[books] owb select sbw.* from SupportBeanWindow sbw where theString = title";
            env.compileDeploy(stmtText, path).addListener("s0");

            env.sendEventBean(new SupportBean("E1", 1));
            env.sendEventBean(OrderBeanFactory.makeEventFour());
            assertFalse(env.listener("s0").isInvoked());

            env.sendEventBean(new SupportBean("Foundation 2", 2));
            env.sendEventBean(OrderBeanFactory.makeEventFour());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"Foundation 2", 2}});

            env.undeployAll();
        }
    }

    private static class EPLContainedSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "reviewId".split(",");

            String stmtText = "@name('s0') select reviewId from OrderBean[books][reviews] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText).addListener("s0");
            assertStatelessStmt(env, "s0", true);

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{1}, {2}, {10}});
            env.listener("s0").reset();

            env.sendEventBean(OrderBeanFactory.makeEventFour());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{201}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLContainedWhere implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "reviewId".split(",");

            // try where in root
            String stmtText = "@name('s0') select reviewId from OrderBean[books where title = 'Enders Game'][reviews] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{1}, {2}});
            env.listener("s0").reset();

            // try where in different levels
            env.undeployAll();
            stmtText = "@name('s0') select reviewId from OrderBean[books where title = 'Enders Game'][reviews where reviewId in (1, 10)] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{1}});
            env.listener("s0").reset();

            // try where in combination
            env.undeployAll();
            stmtText = "@name('s0') select reviewId from OrderBean[books as bc][reviews as rw where rw.reviewId in (1, 10) and bc.title = 'Enders Game'] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{1}});
            env.listener("s0").reset();
            assertFalse(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLContainedColumnSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // columns supplied
            String stmtText = "@name('s0') select * from OrderBean[select bookId, orderdetail.orderId as orderId from books][select reviewId from reviews] bookReviews order by reviewId asc";
            env.compileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // stream wildcards identify fragments
            stmtText = "@name('s0') select orderFrag.orderdetail.orderId as orderId, bookFrag.bookId as bookId, reviewFrag.reviewId as reviewId " +
                "from OrderBean[books as book][select myorder.* as orderFrag, book.* as bookFrag, review.* as reviewFrag from reviews as review] as myorder";
            env.compileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // one event type dedicated as underlying
            stmtText = "@name('s0') select orderdetail.orderId as orderId, bookFrag.bookId as bookId, reviewFrag.reviewId as reviewId " +
                "from OrderBean[books as book][select myorder.*, book.* as bookFrag, review.* as reviewFrag from reviews as review] as myorder";
            env.compileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // wildcard unnamed as underlying
            stmtText = "@name('s0') select orderFrag.orderdetail.orderId as orderId, bookId, reviewId " +
                "from OrderBean[select * from books][select myorder.* as orderFrag, reviewId from reviews as review] as myorder";
            env.compileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // wildcard named as underlying
            stmtText = "@name('s0') select orderFrag.orderdetail.orderId as orderId, bookFrag.bookId as bookId, reviewFrag.reviewId as reviewId " +
                "from OrderBean[select * from books as bookFrag][select myorder.* as orderFrag, review.* as reviewFrag from reviews as review] as myorder";
            env.compileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // object model
            stmtText = "@name('s0') select orderFrag.orderdetail.orderId as orderId, bookId, reviewId " +
                "from OrderBean[select * from books][select myorder.* as orderFrag, reviewId from reviews as review] as myorder";
            env.eplToModelCompileDeploy(stmtText).addListener("s0");
            tryAssertionColumnSelect(env);
            env.undeployAll();

            // with where-clause
            stmtText = "@name('s0') select * from AccountEvent[select * from wallets where currency=\"USD\"]";
            EPStatementObjectModel model = env.eplToModel(stmtText);
            assertEquals(stmtText, model.toEPL());
        }
    }

    private static class EPLContainedPatternSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from pattern [" +
                "every r=OrderBean[books][reviews] -> SupportBean(intPrimitive = r[0].reviewId)]").addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            env.sendEventBean(OrderBeanFactory.makeEventFour());

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E2", -1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E2", 201));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class EPLContainedSubSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select theString from SupportBean s0 where " +
                "exists (select * from OrderBean[books][reviews]#unique(reviewId) where reviewId = s0.intPrimitive)")
                .addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            env.sendEventBean(OrderBeanFactory.makeEventFour());

            env.sendEventBean(new SupportBean("E1", 1));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E2", -1));
            assertFalse(env.listener("s0").getAndClearIsInvoked());

            env.sendEventBean(new SupportBean("E2", 201));
            assertTrue(env.listener("s0").getAndClearIsInvoked());

            env.undeployAll();
        }
    }

    private static class EPLContainedUnderlyingSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "orderId,bookId,reviewId".split(",");

            String stmtText = "@name('s0') select orderdetail.orderId as orderId, bookFrag.bookId as bookId, reviewFrag.reviewId as reviewId " +
                "from OrderBean[books as book][select myorder.*, book.* as bookFrag, review.* as reviewFrag from reviews as review] as myorder";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(OrderBeanFactory.makeEventOne());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
                {"PO200901", "10020", 1}, {"PO200901", "10020", 2}, {"PO200901", "10021", 10}});
            env.listener("s0").reset();

            env.sendEventBean(OrderBeanFactory.makeEventFour());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"PO200904", "10031", 201}});
            env.listener("s0").reset();

            env.undeployAll();
        }
    }

    private static class EPLContainedInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            tryInvalidCompile(env, "select bookId from OrderBean[select count(*) from books]",
                "Expression in a property-selection may not utilize an aggregation function [select bookId from OrderBean[select count(*) from books]]");

            tryInvalidCompile(env, "select bookId from OrderBean[select bookId, (select abc from review#lastevent) from books]",
                "Expression in a property-selection may not utilize a subselect [select bookId from OrderBean[select bookId, (select abc from review#lastevent) from books]]");

            tryInvalidCompile(env, "select bookId from OrderBean[select prev(1, bookId) from books]",
                "Failed to validate contained-event expression 'prev(1,bookId)': Previous function cannot be used in this context [select bookId from OrderBean[select prev(1, bookId) from books]]");

            tryInvalidCompile(env, "select bookId from OrderBean[select * from books][select * from reviews]",
                "A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation [select bookId from OrderBean[select * from books][select * from reviews]]");

            tryInvalidCompile(env, "select bookId from OrderBean[select abc from books][reviews]",
                "Failed to validate contained-event expression 'abc': Property named 'abc' is not valid in any stream [select bookId from OrderBean[select abc from books][reviews]]");

            tryInvalidCompile(env, "select bookId from OrderBean[books][reviews]",
                "Failed to validate select-clause expression 'bookId': Property named 'bookId' is not valid in any stream [select bookId from OrderBean[books][reviews]]");

            tryInvalidCompile(env, "select orderId from OrderBean[books]",
                "Failed to validate select-clause expression 'orderId': Property named 'orderId' is not valid in any stream [select orderId from OrderBean[books]]");

            tryInvalidCompile(env, "select * from OrderBean[books where abc=1]",
                "Failed to validate contained-event expression 'abc=1': Property named 'abc' is not valid in any stream [select * from OrderBean[books where abc=1]]");

            tryInvalidCompile(env, "select * from OrderBean[abc]",
                "Failed to validate contained-event expression 'abc': Property named 'abc' is not valid in any stream [select * from OrderBean[abc]]");
        }
    }

    private static void tryAssertionColumnSelect(RegressionEnvironment env) {
        String[] fields = "orderId,bookId,reviewId".split(",");

        env.sendEventBean(OrderBeanFactory.makeEventOne());
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{
            {"PO200901", "10020", 1}, {"PO200901", "10020", 2}, {"PO200901", "10021", 10}});
        env.listener("s0").reset();

        env.sendEventBean(OrderBeanFactory.makeEventFour());
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getLastNewData(), fields, new Object[][]{{"PO200904", "10031", 201}});
        env.listener("s0").reset();
    }
}