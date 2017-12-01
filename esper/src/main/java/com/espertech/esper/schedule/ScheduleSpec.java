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
package com.espertech.esper.schedule;

import com.espertech.esper.type.CronParameter;
import com.espertech.esper.type.ScheduleUnit;

import java.io.Serializable;
import java.util.*;

/**
 * Holds a schedule specification which consists of a set of integer values or a null
 * value for each schedule unit to indicate a wildcard.
 * There is always an element in the specification for each unit minutes, hours, day of month, month, and day of week.
 * There is optionally an element in the specification for the unit seconds.
 */
public final class ScheduleSpec implements Serializable {
    // Per unit hold the set of valid integer values, or null if wildcarded.
    // The seconds unit is optional.
    private final EnumMap<ScheduleUnit, SortedSet<Integer>> unitValues;
    private String optionalTimeZone;
    private CronParameter optionalDayOfMonthOperator;
    private CronParameter optionalDayOfWeekOperator;
    private static final long serialVersionUID = -7050807714879367353L;

    public ScheduleSpec(EnumMap<ScheduleUnit, SortedSet<Integer>> unitValues, String optionalTimeZone, CronParameter optionalDayOfMonthOperator, CronParameter optionalDayOfWeekOperator) throws IllegalArgumentException {
        validate(unitValues);

        // Reduce to wildcards any unit's values set, if possible
        compress(unitValues);

        this.unitValues = unitValues;
        this.optionalTimeZone = optionalTimeZone;
        this.optionalDayOfMonthOperator = optionalDayOfMonthOperator;
        this.optionalDayOfWeekOperator = optionalDayOfWeekOperator;
    }

    /**
     * Constructor - for unit testing, initialize to all wildcards but leave seconds empty.
     */
    public ScheduleSpec() {
        unitValues = new EnumMap<ScheduleUnit, SortedSet<Integer>>(ScheduleUnit.class);
        unitValues.put(ScheduleUnit.MINUTES, null);
        unitValues.put(ScheduleUnit.HOURS, null);
        unitValues.put(ScheduleUnit.DAYS_OF_MONTH, null);
        unitValues.put(ScheduleUnit.MONTHS, null);
        unitValues.put(ScheduleUnit.DAYS_OF_WEEK, null);
        optionalTimeZone = null;
    }

    public CronParameter getOptionalDayOfMonthOperator() {
        return optionalDayOfMonthOperator;
    }

    public CronParameter getOptionalDayOfWeekOperator() {
        return optionalDayOfWeekOperator;
    }

    public void setOptionalTimeZone(String optionalTimeZone) {
        this.optionalTimeZone = optionalTimeZone;
    }

    /**
     * Return map of ordered set of valid schedule values for minute, hour, day, month etc. units
     *
     * @return map of 5 or 6 entries each with a set of integers
     */
    public final EnumMap<ScheduleUnit, SortedSet<Integer>> getUnitValues() {
        return unitValues;
    }

    public String getOptionalTimeZone() {
        return optionalTimeZone;
    }

    /**
     * For unit testing, add a single value, changing wildcards to value sets.
     *
     * @param element to add
     * @param value   to add
     */
    public final void addValue(ScheduleUnit element, int value) {
        SortedSet<Integer> set = unitValues.get(element);
        if (set == null) {
            set = new TreeSet<Integer>();
            unitValues.put(element, set);
        }
        set.add(value);
    }

    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        for (ScheduleUnit element : ScheduleUnit.values()) {
            if (!unitValues.containsKey(element)) {
                continue;
            }

            Set<Integer> valueSet = unitValues.get(element);
            buffer.append(element + "={");
            if (valueSet == null) {
                buffer.append("null");
            } else {
                String delimiter = "";
                for (int i : valueSet) {
                    buffer.append(delimiter + i);
                    delimiter = ",";
                }
            }
            buffer.append("} ");
        }
        return buffer.toString();
    }

    public final boolean equals(Object otherObject) {
        if (otherObject == this) {
            return true;
        }

        if (otherObject == null) {
            return false;
        }

        if (getClass() != otherObject.getClass()) {
            return false;
        }

        ScheduleSpec other = (ScheduleSpec) otherObject;
        if (this.unitValues.size() != other.unitValues.size()) {
            return false;
        }

        for (Map.Entry<ScheduleUnit, SortedSet<Integer>> entry : unitValues.entrySet()) {
            Set<Integer> mySet = entry.getValue();
            Set<Integer> otherSet = other.unitValues.get(entry.getKey());

            if ((otherSet == null) && (mySet != null)) {
                return false;
            }
            if ((otherSet != null) && (mySet == null)) {
                return false;
            }
            if ((otherSet == null) && (mySet == null)) {
                continue;
            }
            if (mySet.size() != otherSet.size()) {
                return false;
            }

            // Commpare value by value
            for (int i : mySet) {
                if (!(otherSet.contains(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    public int hashCode() {
        int hashCode = 0;
        for (Map.Entry<ScheduleUnit, SortedSet<Integer>> entry : unitValues.entrySet()) {
            if (entry.getValue() != null) {
                hashCode *= 31;
                hashCode ^= entry.getValue().iterator().next();
            }
        }
        return hashCode;
    }

    /**
     * Function to reduce value sets for unit that cover the whole range down to a wildcard.
     * I.e. reduce 0,1,2,3,4,5,6 for week value to 'null' indicating the wildcard.
     *
     * @param unitValues is the set of valid values per unit
     */
    protected static void compress(Map<ScheduleUnit, SortedSet<Integer>> unitValues) {
        for (Map.Entry<ScheduleUnit, SortedSet<Integer>> entry : unitValues.entrySet()) {
            int elementValueSetSize = entry.getKey().max() - entry.getKey().min() + 1;
            if (entry.getValue() != null) {
                if (entry.getValue().size() == elementValueSetSize) {
                    unitValues.put(entry.getKey(), null);
                }
            }
        }
    }

    /**
     * Validate units and their value sets.
     *
     * @param unitValues is the set of valid values per unit
     */
    protected static void validate(Map<ScheduleUnit, SortedSet<Integer>> unitValues) {
        if ((!unitValues.containsKey(ScheduleUnit.MONTHS)) ||
                (!unitValues.containsKey(ScheduleUnit.DAYS_OF_WEEK)) ||
                (!unitValues.containsKey(ScheduleUnit.HOURS)) ||
                (!unitValues.containsKey(ScheduleUnit.MINUTES)) ||
                (!unitValues.containsKey(ScheduleUnit.DAYS_OF_MONTH))) {
            throw new IllegalArgumentException("Incomplete information for schedule specification, only the following keys are supplied=" +
                    Arrays.toString(unitValues.keySet().toArray()));
        }

        for (ScheduleUnit unit : ScheduleUnit.values()) {
            if ((unit == ScheduleUnit.SECONDS) && (!unitValues.containsKey(unit))) {
                // Seconds are optional
                continue;
            }

            if (unitValues.get(unit) == null) {
                // Wildcard - no validation for unit
                continue;
            }

            SortedSet<Integer> values = unitValues.get(unit);
            for (Integer value : values) {
                if ((value < unit.min()) || (value > unit.max())) {
                    throw new IllegalArgumentException("Invalid value found for schedule unit, value of " +
                            value + " is not valid for unit " + unit);
                }
            }
        }
    }
}
