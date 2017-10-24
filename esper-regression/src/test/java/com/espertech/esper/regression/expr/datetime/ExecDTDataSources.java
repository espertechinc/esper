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
package com.espertech.esper.regression.expr.datetime;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.io.StringWriter;
import java.util.Calendar;

import static java.time.DayOfWeek.THURSDAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExecDTDataSources implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(SupportDateTime.class);
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionFieldWValue(epService);
        runAssertionStartEndTS(epService);
        runAssertionAllCombinations(epService);
        runAssertionMinMax(epService);
    }

    private void runAssertionMinMax(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "min(longPrimitive).before(max(longBoxed), 1 second) as c0," +
                "min(longPrimitive, longBoxed).before(20000L, 1 second) as c1" +
                " from SupportBean#length(2)");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = "c0,c1".split(",");

        sendBean(epService, 20000, 20000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, false});

        sendBean(epService, 19000, 20000);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true});

        stmt.destroy();
    }

    private void runAssertionAllCombinations(EPServiceProvider epService) {
        String[] fields = "utildate,longdate,caldate,zoneddate,localdate".split(",");

        for (String field : fields) {
            runAssertionAllCombinations(epService, field);
        }
    }

    private void runAssertionAllCombinations(EPServiceProvider epService, String field) {
        String[] methods = "getMinuteOfHour,getMonthOfYear,getDayOfMonth,getDayOfWeek,getDayOfYear,getEra,gethourOfDay,getmillisOfSecond,getsecondOfMinute,getweekyear,getyear".split(",");
        StringWriter epl = new StringWriter();
        epl.append("select ");
        int count = 0;
        String delimiter = "";
        for (String method : methods) {
            epl.append(delimiter).append(field).append(".").append(method).append("() ").append("c").append(Integer.toString(count++));
            delimiter = ",";
        }
        epl.append(" from SupportDateTime");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl.toString());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportDateTime sdt = SupportDateTime.make("2002-05-30T09:01:02.003");
        sdt.getCaldate().set(Calendar.MILLISECOND, 3);
        epService.getEPRuntime().sendEvent(sdt);

        boolean java8date = field.equals("zoneddate") || field.equals("localdate");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10".split(","), new Object[]{
                1, java8date ? 5 : 4, 30, java8date ? THURSDAY : 5, 150, 1, 9, 3, 2, 22, 2002
        });

        stmt.destroy();
    }

    private void runAssertionFieldWValue(EPServiceProvider epService) throws Exception {
        String startTime = "2002-05-30T09:01:02.003";   // use 2-digit hour, see https://bugs.openjdk.java.net/browse/JDK-8066806
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "valmoh,valmoy,valdom,valdow,valdoy,valera,valhod,valmos,valsom,valwye,valyea,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "current_timestamp.getMinuteOfHour() as valmoh," +
                "current_timestamp.getMonthOfYear() as valmoy," +
                "current_timestamp.getDayOfMonth() as valdom," +
                "current_timestamp.getDayOfWeek() as valdow," +
                "current_timestamp.getDayOfYear() as valdoy," +
                "current_timestamp.getEra() as valera," +
                "current_timestamp.gethourOfDay() as valhod," +
                "current_timestamp.getmillisOfSecond()  as valmos," +
                "current_timestamp.getsecondOfMinute() as valsom," +
                "current_timestamp.getweekyear() as valwye," +
                "current_timestamp.getyear() as valyea," +
                "utildate.gethourOfDay() as val1," +
                "longdate.gethourOfDay() as val2," +
                "caldate.gethourOfDay() as val3," +
                "zoneddate.gethourOfDay() as val4," +
                "localdate.gethourOfDay() as val5" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtFragment.addListener(listener);
        for (String field : fields) {
            assertEquals(Integer.class, stmtFragment.getEventType().getPropertyType(field));
        }

        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{
                1, 4, 30, 5, 150, 1, 9, 3, 2, 22, 2002, 9, 9, 9, 9, 9
        });

        stmtFragment.destroy();
    }

    private void runAssertionStartEndTS(EPServiceProvider epService) {

        // test Map inheritance via create-schema
        epService.getEPAdministrator().createEPL("create schema ParentType as (startTS long, endTS long) starttimestamp startTS endtimestamp endTS");
        epService.getEPAdministrator().createEPL("create schema ChildType as (foo string) inherits ParentType");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from ChildType dt where dt.before(current_timestamp())");
        assertEquals("startTS", stmt.getEventType().getStartTimestampPropertyName());
        assertEquals("endTS", stmt.getEventType().getEndTimestampPropertyName());

        // test POJO inheritance via create-schema
        epService.getEPAdministrator().createEPL("create schema InterfaceType as " + MyInterface.class.getName() + " starttimestamp startTS endtimestamp endTS");
        epService.getEPAdministrator().createEPL("create schema DerivedType as " + MyImplOne.class.getName());
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select * from DerivedType dt where dt.before(current_timestamp())");
        assertEquals("startTS", stmtTwo.getEventType().getStartTimestampPropertyName());
        assertEquals("endTS", stmtTwo.getEventType().getEndTimestampPropertyName());

        // test incompatible
        epService.getEPAdministrator().createEPL("create schema T1 as (startTS long, endTS long) starttimestamp startTS endtimestamp endTS");
        epService.getEPAdministrator().createEPL("create schema T2 as (startTSOne long, endTSOne long) starttimestamp startTSOne endtimestamp endTSOne");
        try {
            epService.getEPAdministrator().createEPL("create schema T12 as () inherits T1,T2");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Event type declares start timestamp as property 'startTS' however inherited event type 'T2' declares start timestamp as property 'startTSOne' [create schema T12 as () inherits T1,T2]", ex.getMessage());
        }
        try {
            epService.getEPAdministrator().createEPL("create schema T12 as (startTSOne long, endTSXXX long) inherits T2 starttimestamp startTSOne endtimestamp endTSXXX");
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Event type declares end timestamp as property 'endTSXXX' however inherited event type 'T2' declares end timestamp as property 'endTSOne' [create schema T12 as (startTSOne long, endTSXXX long) inherits T2 starttimestamp startTSOne endtimestamp endTSXXX]", ex.getMessage());
        }
    }

    private void sendBean(EPServiceProvider epService, long longPrimitive, long longBoxed) {
        SupportBean bean = new SupportBean();
        bean.setLongPrimitive(longPrimitive);
        bean.setLongBoxed(longBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static interface MyInterface {
        public long getStartTS();

        public long getEndTS();
    }

    public static class MyImplOne implements MyInterface {
        private final long start;
        private final long end;

        public MyImplOne(String datestr, long duration) {
            start = DateTime.parseDefaultMSec(datestr);
            end = start + duration;
        }

        public long getStartTS() {
            return start;
        }

        public long getEndTS() {
            return end;
        }
    }
}
