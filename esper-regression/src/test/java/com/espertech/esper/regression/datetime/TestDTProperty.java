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

package com.espertech.esper.regression.datetime;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportDateTime;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import junit.framework.TestCase;

public class TestDTProperty extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportDateTime", SupportDateTime.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
        listener = null;
    }

    public void testProp() {
        String startTime = "2002-05-30T09:01:02.003";   // use 2-digit hour, see https://bugs.openjdk.java.net/browse/JDK-8066806
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(DateTime.parseDefaultMSec(startTime)));

        String[] fields = "valmoh,valmoy,valdom,valdow,valdoy,valera,valhod,valmos,valsom,valwye,valyea,val1,val2,val3,val4,val5".split(",");
        String eplFragment = "select " +
                "current_timestamp.getMinuteOfHour() as valmoh,"+
                "current_timestamp.getMonthOfYear() as valmoy,"+
                "current_timestamp.getDayOfMonth() as valdom,"+
                "current_timestamp.getDayOfWeek() as valdow,"+
                "current_timestamp.getDayOfYear() as valdoy,"+
                "current_timestamp.getEra() as valera,"+
                "current_timestamp.gethourOfDay() as valhod,"+
                "current_timestamp.getmillisOfSecond()  as valmos,"+
                "current_timestamp.getsecondOfMinute() as valsom,"+
                "current_timestamp.getweekyear() as valwye,"+
                "current_timestamp.getyear() as valyea,"+
                "utildate.gethourOfDay() as val1," +
                "longdate.gethourOfDay() as val2," +
                "caldate.gethourOfDay() as val3," +
                "zoneddate.gethourOfDay() as val4," +
                "localdate.gethourOfDay() as val5" +
                " from SupportDateTime";
        EPStatement stmtFragment = epService.getEPAdministrator().createEPL(eplFragment);
        stmtFragment.addListener(listener);
        for (String field : fields) {
            assertEquals(Integer.class, stmtFragment.getEventType().getPropertyType(field));
        }

        epService.getEPRuntime().sendEvent(SupportDateTime.make(startTime));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{
                1, 4, 30, 5, 150, 1, 9, 3, 2, 22, 2002, 9, 9, 9, 9, 9
        });

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
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Event type declares start timestamp as property 'startTS' however inherited event type 'T2' declares start timestamp as property 'startTSOne' [create schema T12 as () inherits T1,T2]", ex.getMessage());
        }
        try {
            epService.getEPAdministrator().createEPL("create schema T12 as (startTSOne long, endTSXXX long) inherits T2 starttimestamp startTSOne endtimestamp endTSXXX");
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals("Error starting statement: Event type declares end timestamp as property 'endTSXXX' however inherited event type 'T2' declares end timestamp as property 'endTSOne' [create schema T12 as (startTSOne long, endTSXXX long) inherits T2 starttimestamp startTSOne endtimestamp endTSXXX]", ex.getMessage());
        }
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
