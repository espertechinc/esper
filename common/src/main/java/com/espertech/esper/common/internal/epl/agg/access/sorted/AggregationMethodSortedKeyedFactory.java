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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class AggregationMethodSortedKeyedFactory {

    public static AggregationMethodSortedKeyedEval makeSortedAggregationWithKey(ExprEvaluator keyEval, AggregationMethodSortedEnum method, Class underlyingClass) {
        if (method.getFootprint() != AggregationMethodSortedFootprintEnum.KEYONLY) {
            throw new IllegalStateException("Unrecognized aggregation method " + method);
        }
        if (method == AggregationMethodSortedEnum.CEILINGEVENT) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> firstUnd(map.ceilingEntry(key)),
                (map, key) -> firstBean(map.ceilingEntry(key)),
                (map, key) -> firstColl(map.ceilingEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.CEILINGEVENTS) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> eventsArrayUnd(map.ceilingEntry(key), underlyingClass),
                (map, key) -> null,
                (map, key) -> eventsColl(map.ceilingEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.CEILINGKEY) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> map.ceilingKey(key),
                (map, key) -> null,
                (map, key) -> null
            );
        }
        if (method == AggregationMethodSortedEnum.FLOOREVENT) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> firstUnd(map.floorEntry(key)),
                (map, key) -> firstBean(map.floorEntry(key)),
                (map, key) -> firstColl(map.floorEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.FLOOREVENTS) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> eventsArrayUnd(map.floorEntry(key), underlyingClass),
                (map, key) -> null,
                (map, key) -> eventsColl(map.floorEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.FLOORKEY) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> map.floorKey(key),
                (map, key) -> null,
                (map, key) -> null
            );
        }
        if (method == AggregationMethodSortedEnum.LOWEREVENT) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> firstUnd(map.lowerEntry(key)),
                (map, key) -> firstBean(map.lowerEntry(key)),
                (map, key) -> firstColl(map.lowerEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.LOWEREVENTS) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> eventsArrayUnd(map.lowerEntry(key), underlyingClass),
                (map, key) -> null,
                (map, key) -> eventsColl(map.lowerEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.LOWERKEY) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> map.lowerKey(key),
                (map, key) -> null,
                (map, key) -> null
            );
        }
        if (method == AggregationMethodSortedEnum.HIGHEREVENT) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> firstUnd(map.higherEntry(key)),
                (map, key) -> firstBean(map.higherEntry(key)),
                (map, key) -> firstColl(map.higherEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.HIGHEREVENTS) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> eventsArrayUnd(map.higherEntry(key), underlyingClass),
                (map, key) -> null,
                (map, key) -> eventsColl(map.higherEntry(key))
            );
        }
        if (method == AggregationMethodSortedEnum.HIGHERKEY) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> map.higherKey(key),
                (map, key) -> null,
                (map, key) -> null
            );
        }
        if (method == AggregationMethodSortedEnum.GETEVENT) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> firstUnd(map.get(key)),
                (map, key) -> firstBean(map.get(key)),
                (map, key) -> firstColl(map.get(key))
            );
        }
        if (method == AggregationMethodSortedEnum.GETEVENTS) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> eventsArrayUnd(map.get(key), underlyingClass),
                (map, key) -> null,
                (map, key) -> eventsColl(map.get(key))
            );
        }
        if (method == AggregationMethodSortedEnum.CONTAINSKEY) {
            return new AggregationMethodSortedKeyedEval(keyEval,
                (map, key) -> map.containsKey(key),
                (map, key) -> null,
                (map, key) -> null
            );
        }
        throw new IllegalStateException("Unrecognized aggregation method " + method);
    }

    static Object firstUnd(Map.Entry<Object, Object> entry) {
        if (entry == null) {
            return null;
        }
        EventBean event = AggregatorAccessSortedImpl.checkedPayloadMayDeque(entry.getValue());
        return event.getUnderlying();
    }

    private static Object firstUnd(Object value) {
        if (value == null) {
            return null;
        }
        EventBean event = AggregatorAccessSortedImpl.checkedPayloadMayDeque(value);
        return event.getUnderlying();
    }

    static Object eventsArrayUnd(Map.Entry<Object, Object> entry, Class underlyingClass) {
        if (entry == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadGetUnderlyingArray(entry.getValue(), underlyingClass);
    }

    private static Object eventsArrayUnd(Object value, Class underlyingClass) {
        if (value == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadGetUnderlyingArray(value, underlyingClass);
    }

    static EventBean firstBean(Map.Entry<Object, Object> entry) {
        if (entry == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadMayDeque(entry.getValue());
    }

    private static EventBean firstBean(Object value) {
        if (value == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadMayDeque(value);
    }

    static Collection<EventBean> firstColl(Map.Entry<Object, Object> entry) {
        if (entry == null) {
            return null;
        }
        return Collections.singletonList(AggregatorAccessSortedImpl.checkedPayloadMayDeque(entry.getValue()));
    }

    private static Collection<EventBean> firstColl(Object value) {
        if (value == null) {
            return null;
        }
        return Collections.singletonList(AggregatorAccessSortedImpl.checkedPayloadMayDeque(value));
    }

    static Collection<EventBean> eventsColl(Map.Entry<Object, Object> entry) {
        if (entry == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(entry.getValue());
    }

    private static Collection<EventBean> eventsColl(Object value) {
        if (value == null) {
            return null;
        }
        return AggregatorAccessSortedImpl.checkedPayloadGetCollEvents(value);
    }
}
