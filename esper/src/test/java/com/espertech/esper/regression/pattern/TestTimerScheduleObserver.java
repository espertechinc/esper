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

package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanConstants;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportMessageAssertUtil;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TestTimerScheduleObserver extends TestCase implements SupportBeanConstants
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getViewResources().setShareViews(false);
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        listener = new SupportUpdateListener();
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testScheduling() {
        // just-date: "<date>" : non-recurring, typically a future start time, no period
        runAssertionJustFutureDate();
        runAssertionJustPastDate();

        // just-period: "P<...>" : non-recurring
        runAssertionJustPeriod();

        // partial-form-2: "<date>/P<period>": non-recurring, no start date (starts from current date), with period
        runAssertionDateWithPeriod();

        // partial-form-1: "R<?>/P<period>": recurring, no start date (starts from current date), with period
        runAssertionRecurringLimitedWithPeriod();
        runAssertionRecurringUnlimitedWithPeriod();
        runAssertionRecurringAnchoring();

        // full form: "R<?>/<date>/P<period>" : recurring, start time, with period
        runAssertionFullFormLimitedFutureDated();
        runAssertionFullFormLimitedPastDated();
        runAssertionFullFormUnlimitedFutureDated();
        runAssertionFullFormUnlimitedPastDated();
        runAssertionFullFormUnlimitedPastDatedAnchoring();

        // equivalent formulations
        runAssertionEquivalent();

        // invalid tests
        runAssertionInvalid();

        // followed-by
        runAssertionFollowedBy();
        runAssertionFollowedByDynamicallyComputed();

        // named parameters
        runAssertionNameParameters();

        /**
         * For Testing, could also use this:
         */
        /*
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSecWZone("2001-10-01T5:51:00.000GMT-0:00")));
        epService.getEPAdministrator().createEPL("select * from pattern[timer:schedule('2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]").addListener(listener);

        long next = epService.getEPRuntime().getNextScheduledTime();
        System.out.println(DateTime.print(next));
        */
    }

    private void runAssertionFollowedByDynamicallyComputed() {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("computeISO8601String", this.getClass().getName(), "computeISO8601String");

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:07.000GMT-0:00");

        String epl = "select * from pattern[every sb=SupportBean -> timer:schedule(iso: computeISO8601String(sb))]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        SupportBean b1 = makeSendEvent(iso, "E1", 5);

        sendCurrentTime(iso, "2012-10-01T5:51:9.999GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-01T5:51:10.000GMT-0:00");
        assertEquals(b1, listener.assertOneGetNewAndReset().get("sb"));

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFollowedBy() {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:07.000GMT-0:00");

        String epl = "select * from pattern[every sb=SupportBean -> timer:schedule(iso: 'R/1980-01-01T00:00:00Z/PT15S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        SupportBean b1 = makeSendEvent(iso, "E1");

        sendCurrentTime(iso, "2012-10-01T5:51:14.999GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-01T5:51:15.000GMT-0:00");
        assertEquals(b1, listener.assertOneGetNewAndReset().get("sb"));

        sendCurrentTime(iso, "2012-10-01T5:51:16.000GMT-0:00");
        SupportBean b2 = makeSendEvent(iso, "E2");

        sendCurrentTime(iso, "2012-10-01T5:51:18.000GMT-0:00");
        SupportBean b3 = makeSendEvent(iso, "E3");

        sendCurrentTime(iso, "2012-10-01T5:51:30.000GMT-0:00");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "sb".split(","), new Object[][]{{b2}, {b3}});

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionInvalid() {

        // the ISO 8601 parse tests reside with the parser
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[every timer:schedule(iso: 'x')]",
                "Invalid parameter for pattern observer 'timer:schedule(iso:\"x\")': Failed to parse 'x': Exception parsing date 'x', the date is not a supported ISO 8601 date");

        // named parameter tests: absence, typing, etc.
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule()]",
                "Invalid parameter for pattern observer 'timer:schedule()': No parameters provided");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(x:1)]",
                "Invalid parameter for pattern observer 'timer:schedule(x:1)': Unexpected named parameter 'x', expecting any of the following: [iso, repetitions, date, period]");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(period:1)]",
                "Invalid parameter for pattern observer 'timer:schedule(period:1)': Failed to validate named parameter 'period', expected a single expression returning a TimePeriod-typed value");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(repetitions:'a', period:1 seconds)]",
                "Invalid parameter for pattern observer 'timer:schedule(repetitions:\"a\",period:1 seconds)': Failed to validate named parameter 'repetitions', expected a single expression returning any of the following types: int,long");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(date:1 seconds)]",
                "Invalid parameter for pattern observer 'timer:schedule(date:1 seconds)': Failed to validate named parameter 'date', expected a single expression returning any of the following types: string,Calendar,Date,long");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(repetitions:1)]",
                "Invalid parameter for pattern observer 'timer:schedule(repetitions:1)': Either the date or period parameter is required");
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[timer:schedule(iso: 'R/1980-01-01T00:00:00Z/PT15S', repetitions:1)]",
                "Invalid parameter for pattern observer 'timer:schedule(iso:\"R/1980-01-01T00:00:00Z/PT15S\",repetitions:1)': The 'iso' parameter is exclusive of other parameters");
    }

    private void runAssertionEquivalent() {

        String first = "select * from pattern[every timer:schedule(iso: 'R2/2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]";
        runAssertionEquivalent(first);

        String second = "select * from pattern[every " +
                "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
                " timer:schedule(iso: '2009-05-11T15:30:00Z'))]";
        runAssertionEquivalent(second);

        String third = "select * from pattern[every " +
                "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
                " timer:schedule(iso: '2008-03-01T13:00:00Z/P1Y2M10DT2H30M'))]";
        runAssertionEquivalent(third);
    }

    private void runAssertionEquivalent(String epl) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2001-10-01T5:51:00.000GMT-0:00");

        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2008-03-01T13:00:00.000GMT-0:00");
        assertReceivedAtTime(iso, "2009-05-11T15:30:00.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:52:04.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionDateWithPeriod() {
        runAssertionDateWithPeriod("iso: '2012-10-01T05:52:00Z/PT2S'");
        runAssertionDateWithPeriod("date: '2012-10-01T05:52:00Z', period: 2 seconds");
    }

    private void runAssertionDateWithPeriod(String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = "select * from pattern[timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:02.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:52:04.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormLimitedFutureDated() {
        runAssertionFullFormLimitedFutureDated(true, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'");
        runAssertionFullFormLimitedFutureDated(false, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'");
        runAssertionFullFormLimitedFutureDated(false, "repetitions: 3L, date:'2012-10-01T05:52:00Z', period: 2 seconds");
    }

    private void runAssertionFullFormLimitedFutureDated(boolean audit, String parameters) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = (audit ? "@Audit " : "") + "select * from pattern[every timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:02.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:04.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:52:06.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustFutureDate() {
        runAssertionJustFutureDate(true, "iso: '2012-10-01T05:52:00Z'");
        runAssertionJustFutureDate(false, "iso: '2012-10-01T05:52:00Z'");
        runAssertionJustFutureDate(false, "date: '2012-10-01T05:52:00Z'");
    }

    private void runAssertionJustFutureDate(boolean hasEvery, String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:53:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustPastDate() {
        runAssertionJustPastDate(true);
        runAssertionJustPastDate(false);
    }

    private void runAssertionJustPastDate(boolean hasEvery) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(iso: '2010-10-01T05:52:00Z')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertSendNoMoreCallback(iso, "2012-10-01T5:53:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustPeriod() {
        runAssertionJustPeriod("iso:'P1DT2H'");
        runAssertionJustPeriod("period: 1 day 2 hours");
    }

    private void runAssertionJustPeriod(String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T5:51:00.000GMT-0:00");

        // Fire once after 1 day and 2 hours
        String epl = "select * from pattern[timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-02T7:51:00.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-03T9:51:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringLimitedWithPeriod() {
        runAssertionRecurringLimitedWithPeriod("iso:'R3/PT2S'");
        runAssertionRecurringLimitedWithPeriod("repetitions:3L, period: 2 seconds");
    }

    private void runAssertionRecurringLimitedWithPeriod(String parameters) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:02.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:04.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:06.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:52:08.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringUnlimitedWithPeriod() {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/PT1M10S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:53:10.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:54:20.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:55:30.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:56:40.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedPastDated() {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT1S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:01.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:02.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:03.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionNameParameters() {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("getThe1980Calendar", this.getClass().getName(), "getThe1980Calendar");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("getThe1980Date", this.getClass().getName(), "getThe1980Date");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("getThe1980Long", this.getClass().getName(), "getThe1980Long");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("getTheSeconds", this.getClass().getName(), "getTheSeconds");

        runAssertionNameParameters("repetitions:-1L, date:'1980-01-01T00:00:00Z', period: 1 seconds");
        runAssertionNameParameters("repetitions:-1, date:getThe1980Calendar(), period: 1 seconds");
        runAssertionNameParameters("repetitions:-1, date:getThe1980Date(), period: getTheSeconds() seconds");
        runAssertionNameParameters("repetitions:-1, date:getThe1980Long(), period: 1 seconds");
    }

    private void runAssertionNameParameters(String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(" + parameters + ")]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:01.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:02.000GMT-0:00");
        assertReceivedAtTime(iso, "2012-10-01T5:52:03.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedPastDatedAnchoring() {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-01-01T0:0:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT10S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        sendCurrentTime(iso, "2012-01-01T0:0:15.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-01-01T0:0:20.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(iso, "2012-01-01T0:0:30.000GMT-0:00");

        sendCurrentTime(iso, "2012-01-01T0:0:55.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(iso, "2012-01-01T0:1:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringAnchoring() {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-01-01T0:0:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R/PT10S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        sendCurrentTime(iso, "2012-01-01T0:0:15.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-01-01T0:0:20.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(iso, "2012-01-01T0:0:30.000GMT-0:00");

        sendCurrentTime(iso, "2012-01-01T0:0:55.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(iso, "2012-01-01T0:1:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormLimitedPastDated() {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R8/2012-10-01T05:51:00Z/PT10S')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2012-10-01T5:52:10.000GMT-0:00");
        assertSendNoMoreCallback(iso, "2012-10-01T5:52:20.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedFutureDated() {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to future date, period of 1 day
        sendCurrentTime(iso, "2012-10-01T5:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R/2013-01-01T02:00:05Z/P1D')]";
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(iso, "2013-01-01T02:00:05.000GMT-0:00");
        assertReceivedAtTime(iso, "2013-01-02T02:00:05.000GMT-0:00");
        assertReceivedAtTime(iso, "2013-01-03T02:00:05.000GMT-0:00");
        assertReceivedAtTime(iso, "2013-01-04T02:00:05.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void assertSendNoMoreCallback(EPServiceProviderIsolated iso, String time) {
        sendCurrentTime(iso, time);
        assertFalse(listener.getIsInvokedAndReset());
        sendCurrentTime(iso, "2999-01-01T0:0:00.000GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());
    }

    private void assertReceivedAtTime(EPServiceProviderIsolated iso, String time) {
        long msec = DateTime.parseDefaultMSecWZone(time);

        iso.getEPRuntime().sendEvent(new CurrentTimeEvent(msec - 1));
        assertFalse(listener.getIsInvokedAndReset());

        iso.getEPRuntime().sendEvent(new CurrentTimeEvent(msec));
        assertTrue("expected but not received at " + time, listener.getIsInvokedAndReset());
    }

    private void sendCurrentTime(EPServiceProviderIsolated iso, String time) {
        iso.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSecWZone(time)));
    }

    private SupportBean makeSendEvent(EPServiceProviderIsolated iso, String theString) {
        return makeSendEvent(iso, theString, 0);
    }

    private SupportBean makeSendEvent(EPServiceProviderIsolated iso, String theString, int intPrimitive) {
        SupportBean b = new SupportBean(theString, intPrimitive);
        iso.getEPRuntime().sendEvent(b);
        return b;
    }

    public static String computeISO8601String(SupportBean bean) {
        return "R/1980-01-01T00:00:00Z/PT" + bean.getIntPrimitive() + "S";
    }

    public static Calendar getThe1980Calendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-0:00"));
        cal.setTimeInMillis(DateTime.parseDefaultMSecWZone("1980-01-01T0:0:0.000GMT-0:00"));
        return cal;
    }

    public static Date getThe1980Date() {
        return getThe1980Calendar().getTime();
    }

    public static long getThe1980Long() {
        return getThe1980Calendar().getTimeInMillis();
    }

    public static int getTheSeconds() {
        return 1;
    }
}


