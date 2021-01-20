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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportResponseEvent;
import com.espertech.esper.regressionlib.support.bean.SupportResponseSubEvent;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.*;

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
        execs.add(new EPLContainedSolutionPatternFinancial());

        return execs;
    }

    private static class EPLContainedSolutionPatternFinancial implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Could have also used a mapping event however here we uses fire-and-forget to load the mapping instead:
            //   @public @buseventtype create schema MappingEvent(foreignSymbol string, localSymbol string);
            //   on MappingEvent merge Mapping insert select foreignSymbol, localSymbol;
            // The events are:
            //   MappingEvent={foreignSymbol="ABC", localSymbol="123"}
            //   MappingEvent={foreignSymbol="DEF", localSymbol="456"}
            //   MappingEvent={foreignSymbol="GHI", localSymbol="789"}
            //   MappingEvent={foreignSymbol="JKL", localSymbol="666"}
            //   ForeignSymbols={companies={{symbol='ABC', value=500}, {symbol='DEF', value=300}, {symbol='JKL', value=400}}}
            //   LocalSymbols={companies={{symbol='123', value=600}, {symbol='456', value=100}, {symbol='789', value=200}}}
            RegressionPath path = new RegressionPath();
            String epl =
                "create schema Symbol(symbol string, value double);\n" +
                    "@public @buseventtype create schema ForeignSymbols(companies Symbol[]);\n" +
                    "@public @buseventtype create schema LocalSymbols(companies Symbol[]);\n" +
                    "\n" +
                    "create table Mapping(foreignSymbol string primary key, localSymbol string primary key);\n" +
                    "create index MappingIndexForeignSymbol on Mapping(foreignSymbol);\n" +
                    "create index MappingIndexLocalSymbol on Mapping(localSymbol);\n" +
                    "\n" +
                    "insert into SymbolsPair select * from ForeignSymbols#lastevent as foreign, LocalSymbols#lastevent as local;\n" +
                    "on SymbolsPair\n" +
                    "  insert into SymbolsPairBeginEvent select null\n" +
                    "  insert into ForeignSymbolRow select * from [foreign.companies]\n" +
                    "  insert into LocalSymbolRow select * from [local.companies]\n" +
                    "  insert into SymbolsPairOutputEvent select null" +
                    "  insert into SymbolsPairEndEvent select null" +
                    "  output all;\n" +
                    "\n" +
                    "create context SymbolsPairContext start SymbolsPairBeginEvent end SymbolsPairEndEvent;\n" +
                    "context SymbolsPairContext create table Result(foreignSymbol string primary key, localSymbol string primary key, value double);\n" +
                    "\n" +
                    "context SymbolsPairContext on ForeignSymbolRow as fsr merge Result as result where result.foreignSymbol = fsr.symbol\n" +
                    "  when not matched then insert select fsr.symbol as foreignSymbol,\n" +
                    "    (select localSymbol from Mapping as mapping where mapping.foreignSymbol = fsr.symbol) as localSymbol, fsr.value as value\n" +
                    "  when matched and fsr.value > result.value then update set value = fsr.value;\n" +
                    "\n" +
                    "context SymbolsPairContext on LocalSymbolRow as lsr merge Result as result where result.localSymbol = lsr.symbol\n" +
                    "  when not matched then insert select (select foreignSymbol from Mapping as mapping where mapping.localSymbol = lsr.symbol) as foreignSymbol," +
                    "    lsr.symbol as localSymbol, lsr.value as value\n" +
                    "  when matched and lsr.value > result.value then update set value = lsr.value;\n" +
                    "\n" +
                    "@name('out') context SymbolsPairContext on SymbolsPairOutputEvent select foreignSymbol, localSymbol, value from Result order by foreignSymbol asc;\n";
            env.compileDeploy(epl, path).addListener("out");

            // load mapping table
            EPCompiled compiledFAF = env.compileFAF("insert into Mapping select ?::string as foreignSymbol, ?::string as localSymbol", path);
            EPFireAndForgetPreparedQueryParameterized preparedFAF = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiledFAF);
            loadMapping(env, preparedFAF, "ABC", "123");
            loadMapping(env, preparedFAF, "DEF", "456");
            loadMapping(env, preparedFAF, "GHI", "789");
            loadMapping(env, preparedFAF, "JKL", "666");

            sendForeignSymbols(env, "ABC=500,DEF=300,JKL=400");
            sendLocalSymbols(env, "123=600,456=100,789=200");

            env.assertPropsPerRowLastNew("out", "foreignSymbol,localSymbol,value".split(","),
                new Object[][]{{"ABC", "123", 600d}, {"DEF", "456", 300d}, {"GHI", "789", 200d}, {"JKL", "666", 400d}});

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.FIREANDFORGET);
        }

        private void sendForeignSymbols(RegressionEnvironment env, String symbolCsv) {
            Map<String, Object>[] companies = parseSymbols(symbolCsv);
            env.sendEventMap(Collections.singletonMap("companies", companies), "ForeignSymbols");
        }

        private void sendLocalSymbols(RegressionEnvironment env, String symbolCsv) {
            Map<String, Object>[] companies = parseSymbols(symbolCsv);
            env.sendEventMap(Collections.singletonMap("companies", companies), "LocalSymbols");
        }

        private Map<String, Object>[] parseSymbols(String symbolCsv) {
            String[] pairs = symbolCsv.split(",");
            Map<String, Object>[] companies = new Map[pairs.length];
            for (int i = 0; i < pairs.length; i++) {
                String[] nameAndValue = pairs[i].split("=");
                String symbol = nameAndValue[0];
                double value = Double.parseDouble(nameAndValue[1]);
                companies[i] = CollectionUtil.buildMap("symbol", symbol, "value", value);
            }
            return companies;
        }

        private void loadMapping(RegressionEnvironment env, EPFireAndForgetPreparedQueryParameterized preparedFAF, String foreignSymbol, String localSymbol) {
            preparedFAF.setObject(1, foreignSymbol);
            preparedFAF.setObject(2, localSymbol);
            env.runtime().getFireAndForgetService().executeQuery(preparedFAF);
        }
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

            env.assertPropsNew("s1", "orderId,items.item[0].itemId".split(","), new Object[]{"PO200901", "100001"});
            env.assertPropsPerRowNewOnly("s2", "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
            env.assertPropsPerRowNewOnly("s3", "bookId".split(","), new Object[][]{{"B001"}, {"B002"}});
            env.assertPropsPerRowNewOnly("s4", "count(*)".split(","), new Object[][]{{2L}});
            env.assertPropsPerRowNewOnly("s5", "reviewId".split(","), new Object[][]{{"1"}});
            env.assertListenerNotInvoked("s6");
            env.assertPropsPerRowNewOnly("s7", "orderId,bookId,reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
            env.assertPropsPerRowNewOnly("s8", "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            env.assertPropsPerRowNewOnly("s9", "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            env.assertPropsPerRowNewOnly("s10", "reviewId,bookId".split(","), new Object[][]{{"1", "B001"}});
            env.assertPropsPerRowNewOnly("s11", "mediaOrder.orderId,book.bookId,review.reviewId".split(","), new Object[][]{{"PO200901", "B001", "1"}});
            env.assertPropsPerRowNewOnly("s12", "reviewId".split(","), new Object[][]{{"1"}});

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

            final String[] fieldsItems = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertListener("s0", listener -> {
                printRows(env, listener.getLastNewData());
                EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fieldsItems, new Object[][]{{"B001", "100001"}});
            });

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsItems, new Object[][]{{"B001", "100001"}});

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsItems, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}});

            // count
            env.undeployAll();
            String[] fieldsCount = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{3L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{4L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional, MediaOrder[items.item] as item where productId = bookId order by bookId asc";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{3L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{1L}});

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

            final String[] fieldsItems = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsItems, new Object[][]{{"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertListener("s0", listener -> {
                printRows(env, listener.getLastNewData());
                EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsItems, new Object[][]{{"B001", "100001"}, {"B002", null}});
            });

            // count
            env.undeployAll();
            String[] fieldsCount = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book left outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{6L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional left outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{2L}});

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

            String[] fieldsItems = "book.bookId,item.itemId".split(",");
            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsItems, new Object[][]{{null, "200003"}, {"B005", "200002"}, {"B005", "200004"}, {"B006", "200001"}, {"B008", null}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertListener("s0", listener -> {
                printRows(env, listener.getLastNewData());
                EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fieldsItems, new Object[][]{{"B001", "100001"}, {"B002", null}});
            });

            // count
            env.undeployAll();
            String[] fieldsCount = "count(*)".split(",");
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book full outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{5L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{7L}});

            // unidirectional count
            env.undeployAll();
            stmtText = "@name('s0') select count(*) from MediaOrder[books.book] as book unidirectional full outer join MediaOrder[items.item] as item on productId = bookId";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventXMLDOM(eventDocTwo, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{4L}});

            env.sendEventXMLDOM(eventDocOne, "MediaOrder");
            env.assertPropsPerRowLastNew("s0", fieldsCount, new Object[][]{{2L}});

            env.undeployAll();
        }
    }

    private static class EPLContainedSolutionPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[] fields = "category,subEventType,avgTime".split(",");
            String stmtText = "@name('s0') select category, subEventType, avg(responseTimeMillis) as avgTime from SupportResponseEvent[select category, * from subEvents]#time(1 min) group by category, subEventType order by category, subEventType";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportResponseEvent("svcOne", new SupportResponseSubEvent[]{new SupportResponseSubEvent(1000, "typeA"), new SupportResponseSubEvent(800, "typeB")}));
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{"svcOne", "typeA", 1000.0}, {"svcOne", "typeB", 800.0}});

            env.sendEventBean(new SupportResponseEvent("svcOne", new SupportResponseSubEvent[]{new SupportResponseSubEvent(400, "typeB"), new SupportResponseSubEvent(500, "typeA")}));
            env.assertPropsPerRowLastNew("s0", fields, new Object[][]{{"svcOne", "typeA", 750.0}, {"svcOne", "typeB", 600.0}});

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