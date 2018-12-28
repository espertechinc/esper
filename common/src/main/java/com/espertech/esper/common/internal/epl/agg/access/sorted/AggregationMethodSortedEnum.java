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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Locale;
import java.util.NavigableMap;

public enum AggregationMethodSortedEnum {
    FLOOREVENT(AggregationMethodSortedFootprintEnum.KEYONLY, true, false),
    FLOOREVENTS(AggregationMethodSortedFootprintEnum.KEYONLY, false, true),
    FLOORKEY(AggregationMethodSortedFootprintEnum.KEYONLY, false, false),
    CEILINGEVENT(AggregationMethodSortedFootprintEnum.KEYONLY, true, false),
    CEILINGEVENTS(AggregationMethodSortedFootprintEnum.KEYONLY, false, true),
    CEILINGKEY(AggregationMethodSortedFootprintEnum.KEYONLY, false, false),
    LOWEREVENT(AggregationMethodSortedFootprintEnum.KEYONLY, true, false),
    LOWEREVENTS(AggregationMethodSortedFootprintEnum.KEYONLY, false, true),
    LOWERKEY(AggregationMethodSortedFootprintEnum.KEYONLY, false, false),
    HIGHEREVENT(AggregationMethodSortedFootprintEnum.KEYONLY, true, false),
    HIGHEREVENTS(AggregationMethodSortedFootprintEnum.KEYONLY, false, true),
    HIGHERKEY(AggregationMethodSortedFootprintEnum.KEYONLY, false, false),

    FIRSTEVENT(AggregationMethodSortedFootprintEnum.NOPARAM, true, false),
    FIRSTEVENTS(AggregationMethodSortedFootprintEnum.NOPARAM, false, true),
    FIRSTKEY(AggregationMethodSortedFootprintEnum.NOPARAM, false, false),
    LASTEVENT(AggregationMethodSortedFootprintEnum.NOPARAM, true, false),
    LASTEVENTS(AggregationMethodSortedFootprintEnum.NOPARAM, false, true),
    LASTKEY(AggregationMethodSortedFootprintEnum.NOPARAM, false, false),

    GETEVENT(AggregationMethodSortedFootprintEnum.KEYONLY, true, false),
    GETEVENTS(AggregationMethodSortedFootprintEnum.KEYONLY, false, true),
    CONTAINSKEY(AggregationMethodSortedFootprintEnum.KEYONLY, false, false),
    COUNTEVENTS(AggregationMethodSortedFootprintEnum.NOPARAM, false, false),
    COUNTKEYS(AggregationMethodSortedFootprintEnum.NOPARAM, false, false),

    EVENTSBETWEEN(AggregationMethodSortedFootprintEnum.SUBMAP, false, true),
    SUBMAP(AggregationMethodSortedFootprintEnum.SUBMAP, false, false),
    NAVIGABLEMAPREFERENCE(AggregationMethodSortedFootprintEnum.NOPARAM, false, false);

    private final AggregationMethodSortedFootprintEnum footprint;
    private final boolean returnsSingleEvent;
    private final boolean returnsCollectionOfEvents;

    AggregationMethodSortedEnum(AggregationMethodSortedFootprintEnum footprint, boolean returnsSingleEvent, boolean returnsCollectionOfEvents) {
        this.footprint = footprint;
        this.returnsSingleEvent = returnsSingleEvent;
        this.returnsCollectionOfEvents = returnsCollectionOfEvents;
    }

    public static AggregationMethodSortedEnum fromString(String nameMixed) {
        try {
            return valueOf(nameMixed.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isReturnsCollectionOfEvents() {
        return returnsCollectionOfEvents;
    }

    public boolean isReturnsSingleEvent() {
        return returnsSingleEvent;
    }

    public AggregationMethodSortedFootprintEnum getFootprint() {
        return footprint;
    }

    public Class getResultType(Class underlyingType, Class keyType) {
        if (this == CONTAINSKEY) {
            return Boolean.class;
        }
        if (this == COUNTEVENTS || this == COUNTKEYS) {
            return int.class;
        }
        if (this == SUBMAP || this == NAVIGABLEMAPREFERENCE) {
            return NavigableMap.class;
        }
        if (!isReturnsSingleEvent() && !isReturnsCollectionOfEvents()) {
            return keyType;
        }
        if (isReturnsSingleEvent()) {
            return underlyingType;
        }
        if (isReturnsCollectionOfEvents()) {
            return JavaClassHelper.getArrayType(underlyingType);
        }
        throw new UnsupportedOperationException("Unrecognized type for " + this);
    }
}
