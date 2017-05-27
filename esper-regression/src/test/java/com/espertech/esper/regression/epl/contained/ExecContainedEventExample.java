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
package com.espertech.esper.regression.epl.contained;

import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.util.JSONEventRenderer;
import com.espertech.esper.supportregression.event.SupportXML;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.Serializable;

import static org.junit.Assert.assertFalse;

public class ExecContainedEventExample implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        ConfigurationEventTypeXMLDOM config = new ConfigurationEventTypeXMLDOM();
        String schemaUri = this.getClass().getClassLoader().getResource("regression/mediaOrderSchema.xsd").toString();
        config.setSchemaResource(schemaUri);
        config.setRootElementName("mediaorder");

        epService.getEPAdministrator().getConfiguration().addEventType("MediaOrder", config);
        epService.getEPAdministrator().getConfiguration().addEventType("Cancel", config);

        InputStream xmlStreamOne = this.getClass().getClassLoader().getResourceAsStream("regression/mediaOrderOne.xml");
        Document eventDocOne = SupportXML.getDocument(xmlStreamOne);

        InputStream xmlStreamTwo = this.getClass().getClassLoader().getResourceAsStream("regression/mediaOrderTwo.xml");
        Document eventDocTwo = SupportXML.getDocument(xmlStreamTwo);

        runAssertionExample(epService, eventDocOne);
        runAssertionJoinSelfJoin(epService, eventDocOne, eventDocTwo);
        runAssertionJoinSelfLeftOuterJoin(epService, eventDocOne, eventDocTwo);
        runAssertionJoinSelfFullOuterJoin(epService, eventDocOne, eventDocTwo);
        runAssertionSolutionPattern(epService);
    }

    private void runAssertionExample(EPServiceProvider epService, Document eventDocOne) throws Exception {
        String stmtTextOne = "select orderId, items.item[0].itemId from MediaOrder";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtTextOne);
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        stmtOne.addListener(listenerOne);

        String stmtTextTwo = "select * from MediaOrder[books.book]";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtTextTwo);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        stmtTwo.addListener(listenerTwo);

        String stmtTextThree = "select * from MediaOrder(orderId='PO200901')[books.book]";
        EPStatement stmtThree = epService.getEPAdministrator().createEPL(stmtTextThree);
        SupportUpdateListener listenerThree = new SupportUpdateListener();
        stmtThree.addListener(listenerThree);

        String stmtTextFour = "select count(*) from MediaOrder[books.book]#unique(bookId)";
        EPStatement stmtFour = epService.getEPAdministrator().createEPL(stmtTextFour);
        SupportUpdateListener listenerFour = new SupportUpdateListener();
        stmtFour.addListener(listenerFour);

        String stmtTextFive = "select * from MediaOrder[books.book][review]";
        EPStatement stmtFive = epService.getEPAdministrator().createEPL(stmtTextFive);
        SupportUpdateListener listenerFive = new SupportUpdateListener();
        stmtFive.addListener(listenerFive);

        String stmtTextSix = "select * from pattern [c=Cancel -> o=MediaOrder(orderId = c.orderId)[books.book]]";
        EPStatement stmtSix = epService.getEPAdministrator().createEPL(stmtTextSix);
        SupportUpdateListener listenerSix = new SupportUpdateListener();
        stmtSix.addListener(listenerSix);

        String stmtTextSeven = "select * from MediaOrder[select orderId, bookId from books.book][select * from review]";
        EPStatement stmtSeven = epService.getEPAdministrator().createEPL(stmtTextSeven);
        SupportUpdateListener listenerSeven = new SupportUpdateListener();
        stmtSeven.addListener(listenerSeven);

        String stmtTextEight = "select * from MediaOrder[select * from books.book][select reviewId, comment from review]";
        EPStatement stmtEight = epService.getEPAdministrator().createEPL(stmtTextEight);
        SupportUpdateListener listenerEight = new SupportUpdateListener();
        stmtEight.addListener(listenerEight);

        String stmtTextNine = "select * from MediaOrder[books.book as book][select book.*, reviewId, comment from review]";
        EPStatement stmtNine = epService.getEPAdministrator().createEPL(stmtTextNine);
        SupportUpdateListener listenerNine = new SupportUpdateListener();
        stmtNine.addListener(listenerNine);

        String stmtTextTen = "select * from MediaOrder[books.book as book][select mediaOrder.*, bookId, reviewId from review] as mediaOrder";
        EPStatement stmtTen = epService.getEPAdministrator().createEPL(stmtTextTen);
        SupportUpdateListener listenerTen = new SupportUpdateListener();
        stmtTen.addListener(listenerTen);

        String stmtTextElevenZero = "insert into ReviewStream select * from MediaOrder[books.book as book]\n" +
                "    [select mediaOrder.* as mediaOrder, book.* as book, review.* as review from review as review] as mediaOrder";
        epService.getEPAdministrator().createEPL(stmtTextElevenZero);
        String stmtTextElevenOne = "select mediaOrder.orderId, book.bookId, review.reviewId from ReviewStream";
        EPStatement stmtElevenOne = epService.getEPAdministrator().createEPL(stmtTextElevenOne);
        SupportUpdateListener listenerEleven = new SupportUpdateListener();
        stmtElevenOne.addListener(listenerEleven);

        String stmtTextTwelve = "select * from MediaOrder[books.book where author = 'Orson Scott Card'][review]";
        EPStatement stmtTwelve = epService.getEPAdministrator().createEPL(stmtTextTwelve);
        SupportUpdateListener listenerTwelve = new SupportUpdateListener();
        stmtTwelve.addListener(listenerTwelve);

        epService.getEPRuntime().sendEvent(eventDocOne);

        EPAssertionUtil.assertProps(listenerOne.assertOneGetNewAndReset(), "orderId,items.item[0].itemId".split(","), new Object[]{"PO200901", "100001"});
        EPAssertionUtil.assertPropsPerRow(listenerTwo.getLastNewData(), "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
        EPAssertionUtil.assertPropsPerRow(listenerThree.getLastNewData(), "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
        EPAssertionUtil.assertPropsPerRow(listenerFour.getLastNewData(), "count(*)".split(","), new Object[][]{{2L}});
        EPAssertionUtil.assertPropsPerRow(listenerFive.getLastNewData(), "reviewId".split(","), new Object[][]{{"1"}});
        assertFalse(listenerSix.isInvoked());
        EPAssertionUtil.assertPropsPerRow(listenerSeven.getLastNewData(), "orderId,bookId,reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
        EPAssertionUtil.assertPropsPerRow(listenerEight.getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
        EPAssertionUtil.assertPropsPerRow(listenerNine.getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
        EPAssertionUtil.assertPropsPerRow(listenerTen.getLastNewData(), "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
        EPAssertionUtil.assertPropsPerRow(listenerEleven.getLastNewData(), "mediaOrder.orderId,book.bookId,review.reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
        EPAssertionUtil.assertPropsPerRow(listenerTwelve.getLastNewData(), "reviewId".split(","), new Object[][]{{"1"}});

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionJoinSelfJoin(EPServiceProvider epService, Document eventDocOne, Document eventDocTwo) {
        String stmtText = "select book.bookId,item.itemId from MediaOrder[books.book] as book, MediaOrder[items.item] as item where productId = bookId order by bookId, item.itemId asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "book.bookId,item.itemId".split(",");
        epService.getEPRuntime().sendEvent(eventDocOne);
        printRows(epService, listener.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}});

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}});

        // count
        stmt.destroy();
        fields = "count(*)".split(",");
        stmtText = "select count(*) from MediaOrder[books.book] as book, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{3L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{4L}});

        // unidirectional count
        stmt.destroy();
        stmtText = "select count(*) from MediaOrder[books.book] as book unidirectional, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{3L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{1L}});

        stmt.destroy();
    }

    private void runAssertionJoinSelfLeftOuterJoin(EPServiceProvider epService, Document eventDocOne, Document eventDocTwo) {
        String stmtText = "select book.bookId,item.itemId from MediaOrder[books.book] as book left outer join MediaOrder[items.item] as item on productId = bookId order by bookId, item.itemId asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "book.bookId,item.itemId".split(",");
        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        printRows(epService, listener.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}, {"B002", null}});

        // count
        stmt.destroy();
        fields = "count(*)".split(",");
        stmtText = "select count(*) from MediaOrder[books.book] as book left outer join MediaOrder[items.item] as item on productId = bookId";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{4L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{6L}});

        // unidirectional count
        stmt.destroy();
        stmtText = "select count(*) from MediaOrder[books.book] as book unidirectional left outer join MediaOrder[items.item] as item on productId = bookId";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{4L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{2L}});

        stmt.destroy();
    }

    private void runAssertionJoinSelfFullOuterJoin(EPServiceProvider epService, Document eventDocOne, Document eventDocTwo) {
        String stmtText = "select orderId, book.bookId,item.itemId from MediaOrder[books.book] as book full outer join MediaOrder[select orderId, * from items.item] as item on productId = bookId order by bookId, item.itemId asc";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "book.bookId,item.itemId".split(",");
        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{null, "200003"}, {"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        printRows(epService, listener.getLastNewData());
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"B001", "100001"}, {"B002", null}});

        // count
        stmt.destroy();
        fields = "count(*)".split(",");
        stmtText = "select count(*) from MediaOrder[books.book] as book full outer join MediaOrder[items.item] as item on productId = bookId";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{5L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{7L}});

        // unidirectional count
        stmt.destroy();
        stmtText = "select count(*) from MediaOrder[books.book] as book unidirectional full outer join MediaOrder[items.item] as item on productId = bookId";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(eventDocTwo);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{4L}});

        epService.getEPRuntime().sendEvent(eventDocOne);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{2L}});

        stmt.destroy();
    }

    private void printRows(EPServiceProvider epService, EventBean[] rows) {
        JSONEventRenderer renderer = epService.getEPRuntime().getEventRenderer().getJSONRenderer(rows[0].getEventType());
        for (int i = 0; i < rows.length; i++) {
            // System.out.println(renderer.render("event#" + i, rows[i]));
            renderer.render("event#" + i, rows[i]);
        }
    }

    private void runAssertionSolutionPattern(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("ResponseEvent", ResponseEvent.class);

        String[] fields = "category,subEventType,avgTime".split(",");
        String stmtText = "select category, subEventType, avg(responseTimeMillis) as avgTime from ResponseEvent[select category, * from subEvents]#time(1 min) group by category, subEventType order by category, subEventType";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new ResponseEvent("svcOne", new SubEvent[]{new SubEvent(1000, "typeA"), new SubEvent(800, "typeB")}));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"svcOne", "typeA", 1000.0}, {"svcOne", "typeB", 800.0}});

        epService.getEPRuntime().sendEvent(new ResponseEvent("svcOne", new SubEvent[]{new SubEvent(400, "typeB"), new SubEvent(500, "typeA")}));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{"svcOne", "typeA", 750.0}, {"svcOne", "typeB", 600.0}});

        stmt.destroy();
    }

    public static class ResponseEvent implements Serializable {
        private String category;
        private SubEvent[] subEvents;

        public ResponseEvent(String category, SubEvent[] subEvents) {
            this.category = category;
            this.subEvents = subEvents;
        }

        public String getCategory() {
            return category;
        }

        public SubEvent[] getSubEvents() {
            return subEvents;
        }
    }

    public static class SubEvent implements Serializable {
        private long responseTimeMillis;
        private String subEventType;

        public SubEvent(long responseTimeMillis, String subEventType) {
            this.responseTimeMillis = responseTimeMillis;
            this.subEventType = subEventType;
        }

        public long getResponseTimeMillis() {
            return responseTimeMillis;
        }

        public String getSubEventType() {
            return subEventType;
        }
    }
}