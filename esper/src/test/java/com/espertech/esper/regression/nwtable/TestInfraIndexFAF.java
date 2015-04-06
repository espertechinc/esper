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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.support.util.IndexAssertion;
import com.espertech.esper.support.util.IndexAssertionFAF;
import com.espertech.esper.support.util.IndexBackingTableInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestInfraIndexFAF extends TestCase implements IndexBackingTableInfo
{
    private static Log log = LogFactory.getLog(TestInfraIndexFAF.class);

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
        SupportQueryPlanIndexHook.reset();
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testSelectIndexChoiceJoin() {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);

        runAssertionSelectIndexChoiceJoin(true);
        runAssertionSelectIndexChoiceJoin(false);
    }

    public void testSelectIndexChoice() {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);

        runAssertionSelectIndexChoice(true);
        runAssertionSelectIndexChoice(false);
    }

    private void runAssertionSelectIndexChoiceJoin(boolean namedWindow) {

        Object[] preloadedEventsOne = new Object[] {
                new SupportSimpleBeanOne("E1", 10, 1, 2),
                new SupportSimpleBeanOne("E2", 11, 3, 4),
                new SupportSimpleBeanTwo("E1", 20, 1, 2),
                new SupportSimpleBeanTwo("E2", 21, 3, 4),
        };
        IndexAssertionFAF fafAssertion = new IndexAssertionFAF() {
            public void run(EPOnDemandQueryResult result) {
                String[] fields = "w1.s1,w2.s2,w1.i1,w2.i2".split(",");
                EPAssertionUtil.assertPropsPerRowAnyOrder(result.getArray(), fields,
                        new Object[][]{{"E1", "E1", 10, 20}, {"E2", "E2", 11, 21}});
            }
        };

        IndexAssertion[] assertionsSingleProp = new IndexAssertion[] {
                new IndexAssertion(null, "s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "s1 = s2 and l1 = l2", true, fafAssertion),
                new IndexAssertion(null, "l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2", false, fafAssertion),
        };

        // single prop, no index, both declared unique (named window only)
        if (namedWindow) {
            assertIndexChoiceJoin(namedWindow, new String[0], preloadedEventsOne, "std:unique(s1)", "std:unique(s2)", assertionsSingleProp);
        }

        // single prop, unique indexes, both declared keepall
        String[] uniqueIndex = new String[] {"create unique index W1I1 on W1(s1)", "create unique index W1I2 on W2(s2)"};
        assertIndexChoiceJoin(namedWindow, uniqueIndex, preloadedEventsOne, "win:keepall()", "win:keepall()", assertionsSingleProp);

        // single prop, mixed indexes, both declared keepall
        IndexAssertion[] assertionsMultiProp = new IndexAssertion[] {
                new IndexAssertion(null, "s1 = s2", false, fafAssertion),
                new IndexAssertion(null, "s1 = s2 and l1 = l2", true, fafAssertion),
                new IndexAssertion(null, "l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2 and s1 = s2", true, fafAssertion),
                new IndexAssertion(null, "d1 = d2 and l1 = l2", false, fafAssertion),
        };
        if (namedWindow) {
            String[] mixedIndex = new String[] {"create index W1I1 on W1(s1, l1)", "create unique index W1I2 on W2(s2)"};
            assertIndexChoiceJoin(namedWindow, mixedIndex, preloadedEventsOne, "std:unique(s1)", "win:keepall()", assertionsSingleProp);

            // multi prop, no index, both declared unique
            assertIndexChoiceJoin(namedWindow, new String[0], preloadedEventsOne, "std:unique(s1, l1)", "std:unique(s2, l2)", assertionsMultiProp);
        }

        // multi prop, unique indexes, both declared keepall
        String[] uniqueIndexMulti = new String[] {"create unique index W1I1 on W1(s1, l1)", "create unique index W1I2 on W2(s2, l2)"};
        assertIndexChoiceJoin(namedWindow, uniqueIndexMulti, preloadedEventsOne, "win:keepall()", "win:keepall()", assertionsMultiProp);

        // multi prop, mixed indexes, both declared keepall
        if (namedWindow) {
            String[] mixedIndexMulti = new String[] {"create index W1I1 on W1(s1)", "create unique index W1I2 on W2(s2, l2)"};
            assertIndexChoiceJoin(namedWindow, mixedIndexMulti, preloadedEventsOne, "std:unique(s1, l1)", "win:keepall()", assertionsMultiProp);
        }
    }

    private void assertIndexChoiceJoin(boolean namedWindow, String[] indexes, Object[] preloadedEvents, String datawindowOne, String datawindowTwo,
                                       IndexAssertion... assertions) {
        if (namedWindow) {
            epService.getEPAdministrator().createEPL("create window W1." + datawindowOne + " as SSB1");
            epService.getEPAdministrator().createEPL("create window W2." + datawindowTwo + " as SSB2");
        }
        else {
            epService.getEPAdministrator().createEPL("create table W1 (s1 String primary key, i1 int primary key, d1 double primary key, l1 long primary key)");
            epService.getEPAdministrator().createEPL("create table W2 (s2 String primary key, i2 int primary key, d2 double primary key, l2 long primary key)");
        }
        epService.getEPAdministrator().createEPL("insert into W1 select s1,i1,d1,l1 from SSB1");
        epService.getEPAdministrator().createEPL("insert into W2 select s2,i2,d2,l2 from SSB2");

        for (String index : indexes) {
            epService.getEPAdministrator().createEPL(index);
        }
        for (Object event : preloadedEvents) {
            epService.getEPRuntime().sendEvent(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) +
                    "select * from W1 as w1, W2 as w2 " +
                    "where " + assertion.getWhereClause();
            EPOnDemandQueryResult result;
            try {
                result = epService.getEPRuntime().executeQuery(epl);
            }
            catch (EPStatementException ex) {
                log.error("Failed to process:" + ex.getMessage(), ex);
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // assert index and access
            SupportQueryPlanIndexHook.assertJoinAllStreamsAndReset(assertion.getUnique());
            assertion.getFafAssertion().run(result);
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("W1", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("W2", false);
    }

    private void runAssertionSelectIndexChoice(boolean namedWindow) {
        Object[] preloadedEventsOne = new Object[] {new SupportSimpleBeanOne("E1", 10, 11, 12), new SupportSimpleBeanOne("E2", 20, 21, 22)};
        IndexAssertionFAF fafAssertion = new IndexAssertionFAF() {
            public void run(EPOnDemandQueryResult result) {
                String[] fields = "s1,i1".split(",");
                EPAssertionUtil.assertPropsPerRow(result.getArray(), fields, new Object[][]{{"E2", 20}});
            }
        };

        // single index one field (plus declared unique)
        String[] noindexes = new String[0];
        assertIndexChoice(namedWindow, noindexes, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = 'E2'", null, null, fafAssertion),
                        new IndexAssertion(null, "s1 = 'E2' and l1 = 22", null, null, fafAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", null, null, fafAssertion),
                        new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"), // should bust
                });

        // single index one field (plus declared unique)
        String[] indexOneField = new String[] {"create unique index One on MyInfra (s1)"};
        assertIndexChoice(namedWindow, indexOneField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = 'E2'", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                        new IndexAssertion(null, "s1 in ('E2')", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                        new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_UNIQUE, fafAssertion),
                        new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"), // should bust
                });

        // single index two field (plus declared unique)
        String[] indexTwoField = new String[] {"create unique index One on MyInfra (s1, l1)"};
        assertIndexChoice(namedWindow, indexTwoField, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = 'E2'", null, null, fafAssertion),
                        new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_MULTI_UNIQUE, fafAssertion),
                });

        // two index one unique (plus declared unique)
        String[] indexSetTwo = new String[] {
                "create index One on MyInfra (s1)",
                "create unique index Two on MyInfra (s1, d1)"};
        assertIndexChoice(namedWindow, indexSetTwo, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "s1 = 'E2'", "One", BACKING_SINGLE_DUPS, fafAssertion),
                        new IndexAssertion(null, "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                        new IndexAssertion("@Hint('index(One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                        new IndexAssertion("@Hint('index(Two,One)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                        new IndexAssertion("@Hint('index(Two,bust)')", "s1 = 'E2' and l1 = 22"),  // busted
                        new IndexAssertion("@Hint('index(explicit,bust)')", "s1 = 'E2' and l1 = 22", "One", BACKING_SINGLE_DUPS, fafAssertion),
                        new IndexAssertion(null, "s1 = 'E2' and d1 = 21 and l1 = 22", "Two", BACKING_MULTI_UNIQUE, fafAssertion),
                        new IndexAssertion("@Hint('index(explicit,bust)')", "d1 = 22 and l1 = 22"),   // busted
                });

        // range (unique)
        String[] indexSetThree = new String[] {
                "create index One on MyInfra (l1 btree)",
                "create index Two on MyInfra (d1 btree)"};
        assertIndexChoice(namedWindow, indexSetThree, preloadedEventsOne, "std:unique(s1)",
                new IndexAssertion[] {
                        new IndexAssertion(null, "l1 between 22 and 23", "One", BACKING_SORTED_COERCED, fafAssertion),
                        new IndexAssertion(null, "d1 between 21 and 22", "Two", BACKING_SORTED_COERCED, fafAssertion),
                        new IndexAssertion("@Hint('index(One, bust)')", "d1 between 21 and 22"), // busted
                });
    }

    private void assertIndexChoice(boolean namedWindow, String[] indexes, Object[] preloadedEvents, String datawindow,
                                   IndexAssertion[] assertions) {

        String eplCreate = namedWindow ?
                "create window MyInfra." + datawindow + " as SSB1" :
                "create table MyInfra(s1 String primary key, i1 int primary key, d1 double primary key, l1 long primary key)";
        epService.getEPAdministrator().createEPL(eplCreate);
        epService.getEPAdministrator().createEPL("insert into MyInfra select s1,i1,d1,l1 from SSB1");
        for (String index : indexes) {
            epService.getEPAdministrator().createEPL(index);
        }
        for (Object event : preloadedEvents) {
            epService.getEPRuntime().sendEvent(event);
        }

        int count = 0;
        for (IndexAssertion assertion : assertions) {
            log.info("======= Testing #" + count++);
            String epl = INDEX_CALLBACK_HOOK +
                    (assertion.getHint() == null ? "" : assertion.getHint()) +
                    "select * from MyInfra where " + assertion.getWhereClause();
            EPOnDemandQueryResult result;
            try {
                result = epService.getEPRuntime().executeQuery(epl);
            }
            catch (EPStatementException ex) {
                if (assertion.getEventSendAssertion() == null) {
                    // no assertion, expected
                    assertTrue(ex.getMessage().contains("index hint busted"));
                    continue;
                }
                throw new RuntimeException("Unexpected statement exception: " + ex.getMessage(), ex);
            }

            // assert index and access
            SupportQueryPlanIndexHook.assertFAFAndReset(assertion.getExpectedIndexName(), assertion.getIndexBackingClass());
            assertion.getFafAssertion().run(result);
        }

        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyInfra", false);
    }
}
