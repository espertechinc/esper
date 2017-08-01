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
package com.espertech.esper.epl.datetime.eval;

import com.espertech.esper.epl.methodbase.DotMethodFP;

import java.util.Locale;

public enum DatetimeMethodEnum {

    // calendar ops
    WITHTIME("withTime", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.WITHTIME),
    WITHDATE("withDate", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.WITHDATE),
    PLUS("plus", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.PLUSMINUS),
    MINUS("minus", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.PLUSMINUS),
    WITHMAX("withMax", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),
    WITHMIN("withMin", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),
    SET("set", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD_PLUS_INT),
    ROUNDCEILING("roundCeiling", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),
    ROUNDFLOOR("roundFloor", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),
    ROUNDHALF("roundHalf", DatetimeMethodEnumStatics.CALENDAR_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),

    // reformat ops
    GET("get", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.CALFIELD),
    FORMAT("format", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.FORMAT),
    TOCALENDAR("toCalendar", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    TODATE("toDate", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    TOMILLISEC("toMillisec", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETMINUTEOFHOUR("getMinuteOfHour", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETMONTHOFYEAR("getMonthOfYear", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFMONTH("getDayOfMonth", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFWEEK("getDayOfWeek", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETDAYOFYEAR("getDayOfYear", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETERA("getEra", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETHOUROFDAY("getHourOfDay", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETMILLISOFSECOND("getMillisOfSecond", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETSECONDOFMINUTE("getSecondOfMinute", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETWEEKYEAR("getWeekyear", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    GETYEAR("getYear", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.NOPARAM),
    BETWEEN("between", DatetimeMethodEnumStatics.REFORMAT_FORGE_FACTORY, DatetimeMethodEnumParams.BETWEEN),

    // interval ops
    BEFORE("before", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_BEFORE_AFTER),
    AFTER("after", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_BEFORE_AFTER),
    COINCIDES("coincides", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_COINCIDES),
    DURING("during", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_DURING_INCLUDES),
    INCLUDES("includes", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_DURING_INCLUDES),
    FINISHES("finishes", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_FINISHES_FINISHEDBY),
    FINISHEDBY("finishedBy", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_FINISHES_FINISHEDBY),
    MEETS("meets", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_MEETS_METBY),
    METBY("metBy", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_MEETS_METBY),
    OVERLAPS("overlaps", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_DURING_OVERLAPS_OVERLAPBY),
    OVERLAPPEDBY("overlappedBy", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_DURING_OVERLAPS_OVERLAPBY),
    STARTS("starts", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_STARTS_STARTEDBY),
    STARTEDBY("startedBy", DatetimeMethodEnumStatics.INTERVAL_FORGE_FACTORY, DatetimeMethodEnumParams.INTERVAL_STARTS_STARTEDBY);

    private final String nameCamel;
    private final ForgeFactory forgeFactory;
    private DotMethodFP[] footprints;

    private DatetimeMethodEnum(String nameCamel, ForgeFactory forgeFactory, DotMethodFP[] footprints) {
        this.nameCamel = nameCamel;
        this.forgeFactory = forgeFactory;
        this.footprints = footprints;
    }

    public ForgeFactory getForgeFactory() {
        return forgeFactory;
    }

    public String getNameCamel() {
        return nameCamel;
    }

    public static boolean isDateTimeMethod(String name) {
        for (DatetimeMethodEnum e : DatetimeMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        return false;
    }

    public static DatetimeMethodEnum fromName(String name) {
        for (DatetimeMethodEnum e : DatetimeMethodEnum.values()) {
            if (e.getNameCamel().toLowerCase(Locale.ENGLISH).equals(name.toLowerCase(Locale.ENGLISH))) {
                return e;
            }
        }
        return null;
    }

    public DotMethodFP[] getFootprints() {
        return footprints;
    }
}
