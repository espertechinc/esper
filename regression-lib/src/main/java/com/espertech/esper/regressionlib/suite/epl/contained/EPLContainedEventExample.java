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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportResponseEvent;
import com.espertech.esper.regressionlib.support.bean.SupportResponseSubEvent;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class EPLContainedEventExample {

    public static List<RegressionExecution> executions() {

        InputStream xmlStreamOne = EPLContainedEventExample.class.getClassLoader().getResourceAsStream("regression/mediaOrderOne.xml");
        Document eventDocOne = SupportXML.getDocument(xmlStreamOne);

        InputStream xmlStreamTwo = EPLContainedEventExample.class.getClassLoader().getResourceAsStream("regression/mediaOrderTwo.xml");
        Document eventDocTwo = SupportXML.getDocument(xmlStreamTwo);

        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLContainedExample(eventDocOne));
        execs.add(new EPLContainedSolutionPattern());
        execs.add(new EPLContainedJoinSelfJoin(eventDocOne, eventDocTwo));
        execs.add(new EPLContainedJoinSelfLeftOuterJoin(eventDocOne, eventDocTwo));
        execs.add(new EPLContainedJoinSelfFullOuterJoin(eventDocOne, eventDocTwo));

        return execs;
    }

    private static class EPLContainedExample implements RegressionExecution {
        private final Document eventDocOne;

        public EPLContainedExample(Document eventDocOne) {
            this.eventDocOne = eventDocOne;
        }

        public void run(RegressionEnvironment env) {
            String stmtTextOne = "@name('s1') select orderId, items.item[0].itemId from MediaOrder";
            env.compileDeploy(stmtTextOne).addListener("s1");

            String stmtTextTwo = "@name('s2') select * from MediaOrder[books.book]";
            env.compileDeploy(stmtTextTwo).addListener("s2");

            String stmtTextThree = "@name('s3') select * from MediaOrder(orderId='PO200901')[books.book]";
            env.compileDeploy(stmtTextThree).addListener("s3");

            String stmtTextFour = "@name('s4') select count(*) from MediaOrder[books.book]#unique(bookId)";
            env.compileDeploy(stmtTextFour).addListener("s4");

            String stmtTextFive = "@name('s5') select * from MediaOrder[books.book][review]";
            env.compileDeploy(stmtTextFive).addListener("s5");

            String stmtTextSix = "@name('s6') select * from pattern [c=Cancel -> o=MediaOrder(orderId = c.orderId)[books.book]]";
            env.compileDeploy(stmtTextSix).addListener("s6");

            String stmtTextSeven = "@name('s7') select * from MediaOrder[select orderId, bookId from books.book][select * from review]";
            env.compileDeploy(stmtTextSeven).addListener("s7");

            String stmtTextEight = "@name('s8') select * from MediaOrder[select * from books.book][select reviewId, comment from review]";
            env.compileDeploy(stmtTextEight).addListener("s8");

            String stmtTextNine = "@name('s9') select * from MediaOrder[books.book as book][select book.*, reviewId, comment from review]";
            env.compileDeploy(stmtTextNine).addListener("s9");

            String stmtTextTen = "@name('s10') select * from MediaOrder[books.book as book][select mediaOrder.*, bookId, reviewId from review] as mediaOrder";
            env.compileDeploy(stmtTextTen).addListener("s10");

            RegressionPath path = new RegressionPath();
            String stmtTextElevenZero = "@name('s11_0') insert into ReviewStream select * from MediaOrder[books.book as book]\n" +
                "    [select mediaOrder.* as mediaOrder, book.* as book, review.* as review from review as review] as mediaOrder";
            env.compileDeploy(stmtTextElevenZero, path);
            String stmtTextElevenOne = "@name('s11') select mediaOrder.orderId, book.bookId, review.reviewId from ReviewStream";
            env.compileDeploy(stmtTextElevenOne, path).addListener("s11");

            String stmtTextTwelve = "@name('s12') select * from MediaOrder[books.book where author = 'Orson Scott Card'][review]";
            env.compileDeploy(stmtTextTwelve).addListener("s12");

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");

            EPAssertionUtil.assertProps(env.listener("s1").assertOneGetNewAndReset(), "orderId,items.item[0].itemId".split(","), new Object[]{"PO200901", "100001"});
            EPAssertionUtil.assertPropsPerRow(env.listener("s2").getLastNewData(), "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s3").getLastNewData(), "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s4").getLastNewData(), "count(*)".split(","), new Object[][]{{2L}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s5").getLastNewData(), "reviewId".split(","), new Object[][]{{"1"}});
            assertFalse(env.listener("s6").isInvoked());
            EPAssertionUtil.assertPropsPerRow(env.listener("s7").getLastNewData(), "orderId,bookId,reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s8").getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s9").getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s10").getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s11").getLastNewData(), "mediaOrder.orderId,book.bookId,review.reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
            EPAssertionUtil.assertPropsPerRow(env.listener("s12").getLastNewData(), "reviewId".split(","), new Object[][]{{"1"}});

            env.undeployAll();
        }
    }

    private static class EPLContainedJoinSelfJoin implements RegressionExecution {
        private final Document eventDocOne;
        private final Document eventDocTwo;

        public EPLContainedJoinSelfJoin(Document eventDocOne, Document eventDocTwo) {
            this.eventDocOne = eventDocOne;
            this.eventDocTwo = eventDocTwo;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select book.bookId,item.itemId from MediaOrder[books.book] as book, MediaOrder[items.item] as item where productId = bookId order by bookId, item.itemId asc";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            printRows(env, env.listener("s0").getLastNewData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}});

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}});

            // count
            env.undeployAll();
            fields = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{3L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{4L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{3L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{1L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedJoinSelfLeftOuterJoin implements RegressionExecution {
        private final Document eventDocOne;
        private final Document eventDocTwo;

        public EPLContainedJoinSelfLeftOuterJoin(Document eventDocOne, Document eventDocTwo) {
            this.eventDocOne = eventDocOne;
            this.eventDocTwo = eventDocTwo;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select book.bookId,item.itemId from MediaOrder[books.book] as book left outer join MediaOrder[items.item] as item on productId = bookId order by bookId, item.itemId asc";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            printRows(env, env.listener("s0").getLastNewData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}, {"B002", null}});

            // count
            env.undeployAll();
            fields = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book left outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{6L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional left outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{2L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedJoinSelfFullOuterJoin implements RegressionExecution {
        private final Document eventDocOne;
        private final Document eventDocTwo;

        public EPLContainedJoinSelfFullOuterJoin(Document eventDocOne, Document eventDocTwo) {
            this.eventDocOne = eventDocOne;
            this.eventDocTwo = eventDocTwo;
        }

        public void run(RegressionEnvironment env) {
            String stmtText = "@name('s0') select orderId, book.bookId,item.itemId from MediaOrder[books.book] as book full outer join MediaOrder[select orderId, * from items.item] as item on productId = bookId order by bookId, item.itemId asc";
            env.compileDeploy(stmtText).addListener("s0");

            String[] fields = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{null, "200003"}, {"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            printRows(env, env.listener("s0").getLastNewData());
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}, {"B002", null}});

            // count
            env.undeployAll();
            fields = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book full outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{5L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{7L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional full outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{2L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedSolutionPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "category,subEventType,avgTime".split(",");
            String stmtText = "@name('s0') select category, subEventType, avg(responseTimeMillis) as avgTime from SupportResponseEvent[select category, * from subEvents]#time(1 min) group by category, subEventType order by category, subEventType";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportResponseEvent("svcOne", new SupportResponseSubEvent[]{new SupportResponseSubEvent(1000, "typeA"), new SupportResponseSubEvent(800, "typeB")}));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"svcOne", "typeA", 1000.0}, {"svcOne", "typeB", 800.0}});

            env.sendEventBean(new SupportResponseEvent("svcOne", new SupportResponseSubEvent[]{new SupportResponseSubEvent(400, "typeB"), new SupportResponseSubEvent(500, "typeA")}));
            EPAssertionUtil.assertPropsPerRow(env.listener("s0").getAndResetLastNewData(), fields, new Object[][]{{"svcOne", "typeA", 750.0}, {"svcOne", "typeB", 600.0}});

            env.undeployAll();
        }
    }

    private static void printRows(RegressionEnvironment env, EventBean[] rows) {
        JSONEventRenderer renderer = env.runtime().getRenderEventService().getJSONRenderer(rows[0].getEventType());
        for (int i = 0; i < rows.length; i++) {
            // System.out.println(renderer.render("event#" + i, rows[i]));
            renderer.render("event#" + i, rows[i]);
        }
    }
}