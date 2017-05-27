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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderIsolated;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanConstants;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ExecPatternObserverTimerSchedule implements RegressionExecution, SupportBeanConstants {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setShareViews(false);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        // just-date: "<date>" : non-recurring, typically a future start time, no period
        runAssertionJustFutureDate(epService);
        runAssertionJustPastDate(epService);

        // just-period: "P<...>" : non-recurring
        runAssertionJustPeriod(epService);

        // partial-form-2: "<date>/P<period>": non-recurring, no start date (starts from current date), with period
        tryAssertionDateWithPeriod(epService);

        // partial-form-1: "R<?>/P<period>": recurring, no start date (starts from current date), with period
        runAssertionRecurringLimitedWithPeriod(epService);
        runAssertionRecurringUnlimitedWithPeriod(epService);
        runAssertionRecurringAnchoring(epService);

        // full form: "R<?>/<date>/P<period>" : recurring, start time, with period
        runAssertionFullFormLimitedFutureDated(epService);
        runAssertionFullFormLimitedPastDated(epService);
        runAssertionFullFormUnlimitedFutureDated(epService);
        runAssertionFullFormUnlimitedPastDated(epService);
        runAssertionFullFormUnlimitedPastDatedAnchoring(epService);

        // equivalent formulations
        runAssertionEquivalent(epService);

        // invalid tests
        runAssertionInvalid(epService);

        // followed-by
        runAssertionFollowedBy(epService);
        runAssertionFollowedByDynamicallyComputed(epService);

        // named parameters
        runAssertionNameParameters(epService);

        /**
         * For Testing, could also use this:
         */
        /*
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSecWZone("2001-10-01T05:51:00.000GMT-0:00")));
        epService.getEPAdministrator().createEPL("select * from pattern[timer:schedule('2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]").addListener(listener);

        long next = epService.getEPRuntime().getNextScheduledTime();
        System.out.println(DateTime.print(next));
        */
    }

    private void runAssertionFollowedByDynamicallyComputed(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("computeISO8601String", this.getClass().getName(), "computeISO8601String");

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:07.000GMT-0:00");

        String epl = "select * from pattern[every sb=SupportBean -> timer:schedule(iso: computeISO8601String(sb))]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        SupportBean b1 = makeSendEvent(iso, "E1", 5);

        sendCurrentTime(iso, "2012-10-01T05:51:9.999GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-01T05:51:10.000GMT-0:00");
        assertEquals(b1, listener.assertOneGetNewAndReset().get("sb"));

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFollowedBy(EPServiceProvider epService) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:07.000GMT-0:00");

        String epl = "select * from pattern[every sb=SupportBean -> timer:schedule(iso: 'R/1980-01-01T00:00:00Z/PT15S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        SupportBean b1 = makeSendEvent(iso, "E1");

        sendCurrentTime(iso, "2012-10-01T05:51:14.999GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-10-01T05:51:15.000GMT-0:00");
        assertEquals(b1, listener.assertOneGetNewAndReset().get("sb"));

        sendCurrentTime(iso, "2012-10-01T05:51:16.000GMT-0:00");
        SupportBean b2 = makeSendEvent(iso, "E2");

        sendCurrentTime(iso, "2012-10-01T05:51:18.000GMT-0:00");
        SupportBean b3 = makeSendEvent(iso, "E3");

        sendCurrentTime(iso, "2012-10-01T05:51:30.000GMT-0:00");
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "sb".split(","), new Object[][]{{b2}, {b3}});

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {

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

    private void runAssertionEquivalent(EPServiceProvider epService) {

        String first = "select * from pattern[every timer:schedule(iso: 'R2/2008-03-01T13:00:00Z/P1Y2M10DT2H30M')]";
        tryAssertionEquivalent(epService, first);

        String second = "select * from pattern[every " +
                "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
                " timer:schedule(iso: '2009-05-11T15:30:00Z'))]";
        tryAssertionEquivalent(epService, second);

        String third = "select * from pattern[every " +
                "(timer:schedule(iso: '2008-03-01T13:00:00Z') or" +
                " timer:schedule(iso: '2008-03-01T13:00:00Z/P1Y2M10DT2H30M'))]";
        tryAssertionEquivalent(epService, third);
    }

    private void tryAssertionEquivalent(EPServiceProvider epService, String epl) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2001-10-01T05:51:00.000GMT-0:00");

        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2008-03-01T13:00:00.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2009-05-11T15:30:00.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:52:04.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void tryAssertionDateWithPeriod(EPServiceProvider epService) {
        tryAssertionDateWithPeriod(epService, "iso: '2012-10-01T05:52:00Z/PT2S'");
        tryAssertionDateWithPeriod(epService, "date: '2012-10-01T05:52:00Z', period: 2 seconds");
    }

    private void tryAssertionDateWithPeriod(EPServiceProvider epService, String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = "select * from pattern[timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:02.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:52:04.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormLimitedFutureDated(EPServiceProvider epService) {
        tryAssertionFullFormLimitedFutureDated(epService, true, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'");
        tryAssertionFullFormLimitedFutureDated(epService, false, "iso: 'R3/2012-10-01T05:52:00Z/PT2S'");
        tryAssertionFullFormLimitedFutureDated(epService, false, "repetitions: 3L, date:'2012-10-01T05:52:00Z', period: 2 seconds");
    }

    private void tryAssertionFullFormLimitedFutureDated(EPServiceProvider epService, boolean audit, String parameters) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:00.000GMT-0:00");

        // Repeat 3 times, starting "2012-10-01T05:52:00Z" (UTC), period of 2 seconds
        String epl = (audit ? "@Audit " : "") + "select * from pattern[every timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:00.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:04.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:52:06.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustFutureDate(EPServiceProvider epService) {
        tryAssertionJustFutureDate(epService, true, "iso: '2012-10-01T05:52:00Z'");
        tryAssertionJustFutureDate(epService, false, "iso: '2012-10-01T05:52:00Z'");
        tryAssertionJustFutureDate(epService, false, "date: '2012-10-01T05:52:00Z'");
    }

    private void tryAssertionJustFutureDate(EPServiceProvider epService, boolean hasEvery, String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:00.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:53:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustPastDate(EPServiceProvider epService) {
        tryAssertionJustPastDate(epService, true);
        tryAssertionJustPastDate(epService, false);
    }

    private void tryAssertionJustPastDate(EPServiceProvider epService, boolean hasEvery) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once at "2012-10-01T05:52:00Z" (UTC)
        String epl = "select * from pattern[" + (hasEvery ? "every " : "") + "timer:schedule(iso: '2010-10-01T05:52:00Z')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:53:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionJustPeriod(EPServiceProvider epService) {
        tryAssertionJustPeriod(epService, "iso:'P1DT2H'");
        tryAssertionJustPeriod(epService, "period: 1 day 2 hours");
    }

    private void tryAssertionJustPeriod(EPServiceProvider epService, String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");
        sendCurrentTime(iso, "2012-10-01T05:51:00.000GMT-0:00");

        // Fire once after 1 day and 2 hours
        String epl = "select * from pattern[timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-02T07:51:00.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-03T09:51:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringLimitedWithPeriod(EPServiceProvider epService) {
        tryAssertionRecurringLimitedWithPeriod(epService, "iso:'R3/PT2S'");
        tryAssertionRecurringLimitedWithPeriod(epService, "repetitions:3L, period: 2 seconds");
    }

    private void tryAssertionRecurringLimitedWithPeriod(EPServiceProvider epService, String parameters) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:04.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:06.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:52:08.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringUnlimitedWithPeriod(EPServiceProvider epService) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Fire 3 times after 2 seconds from current time
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/PT1M10S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:53:10.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:54:20.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:55:30.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:56:40.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedPastDated(EPServiceProvider epService) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT1S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:01.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:03.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionNameParameters(EPServiceProvider epService) {
        for (String name : "getThe1980Calendar,getThe1980Date,getThe1980Long,getTheSeconds,getThe1980LocalDateTime,getThe1980ZonedDateTime".split(",")) {
            epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(name, this.getClass().getName(), name);
        }

        tryAssertionNameParameters(epService, "repetitions:-1L, date:'1980-01-01T00:00:00Z', period: 1 seconds");
        tryAssertionNameParameters(epService, "repetitions:-1, date:getThe1980Calendar(), period: 1 seconds");
        tryAssertionNameParameters(epService, "repetitions:-1, date:getThe1980Date(), period: getTheSeconds() seconds");
        tryAssertionNameParameters(epService, "repetitions:-1, date:getThe1980Long(), period: 1 seconds");
        tryAssertionNameParameters(epService, "repetitions:-1, date:getThe1980LocalDateTime(), period: 1 seconds");
        tryAssertionNameParameters(epService, "repetitions:-1, date:getThe1980ZonedDateTime(), period: 1 seconds");
    }

    private void tryAssertionNameParameters(EPServiceProvider epService, String parameters) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(" + parameters + ")]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:01.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:02.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:03.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedPastDatedAnchoring(EPServiceProvider epService) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-01-01T00:0:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso:'R/1980-01-01T00:00:00Z/PT10S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        sendCurrentTime(iso, "2012-01-01T00:0:15.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-01-01T00:0:20.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(listener, iso, "2012-01-01T00:0:30.000GMT-0:00");

        sendCurrentTime(iso, "2012-01-01T00:0:55.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(listener, iso, "2012-01-01T00:1:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionRecurringAnchoring(EPServiceProvider epService) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-01-01T00:0:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R/PT10S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        sendCurrentTime(iso, "2012-01-01T00:0:15.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        sendCurrentTime(iso, "2012-01-01T00:0:20.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(listener, iso, "2012-01-01T00:0:30.000GMT-0:00");

        sendCurrentTime(iso, "2012-01-01T00:0:55.000GMT-0:00");
        assertTrue(listener.getIsInvokedAndReset());

        assertReceivedAtTime(listener, iso, "2012-01-01T00:1:00.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormLimitedPastDated(EPServiceProvider epService) {

        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to "1980-01-01T00:00:00Z" (UTC), period of 1 second
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R8/2012-10-01T05:51:00Z/PT10S')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2012-10-01T05:52:10.000GMT-0:00");
        assertSendNoMoreCallback(listener, iso, "2012-10-01T05:52:20.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void runAssertionFullFormUnlimitedFutureDated(EPServiceProvider epService) {
        EPServiceProviderIsolated iso = epService.getEPServiceIsolated("E1");

        // Repeat unlimited number of times, reference-dated to future date, period of 1 day
        sendCurrentTime(iso, "2012-10-01T05:52:00.000GMT-0:00");
        String epl = "select * from pattern[every timer:schedule(iso: 'R/2013-01-01T02:00:05Z/P1D')]";
        SupportUpdateListener listener = new SupportUpdateListener();
        iso.getEPAdministrator().createEPL(epl, null, null).addListener(listener);

        assertReceivedAtTime(listener, iso, "2013-01-01T02:00:05.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2013-01-02T02:00:05.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2013-01-03T02:00:05.000GMT-0:00");
        assertReceivedAtTime(listener, iso, "2013-01-04T02:00:05.000GMT-0:00");

        epService.getEPAdministrator().destroyAllStatements();
        iso.destroy();
    }

    private void assertSendNoMoreCallback(SupportUpdateListener listener, EPServiceProviderIsolated iso, String time) {
        sendCurrentTime(iso, time);
        assertFalse(listener.getIsInvokedAndReset());
        sendCurrentTime(iso, "2999-01-01T00:0:00.000GMT-0:00");
        assertFalse(listener.getIsInvokedAndReset());
    }

    private void assertReceivedAtTime(SupportUpdateListener listener, EPServiceProviderIsolated iso, String time) {
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
        cal.setTimeInMillis(DateTime.parseDefaultMSecWZone("1980-01-01T00:0:0.000GMT-0:00"));
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

    public static LocalDateTime getThe1980LocalDateTime() {
        long millis = getThe1980Long();
        Instant instant = Instant.ofEpochMilli(millis);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static ZonedDateTime getThe1980ZonedDateTime() {
        return ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 0, ZoneId.of("GMT"));
    }
}


