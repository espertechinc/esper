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
package com.espertech.esper.collection;

import java.util.ArrayList;

/**
 * Sorted, reference-counting set based on an ArrayList implementation that it being kept sorted.
 */
public class SortedDoubleVector {
    private ArrayList<Double> values;

    /**
     * Constructor.
     */
    public SortedDoubleVector() {
        values = new ArrayList<Double>();
    }

    public SortedDoubleVector(ArrayList<Double> values) {
        this.values = values;
    }

    /**
     * Clear out the collection.
     */
    public void clear() {
        values.clear();
    }

    /**
     * Returns the number of items in the collection.
     *
     * @return size
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns the value at a given index.
     *
     * @param index for which to return value for
     * @return value at index
     */
    public double getValue(int index) {
        return values.get(index);
    }

    /**
     * Add a value to the collection.
     *
     * @param value is the double-type value to add
     */
    public void add(double value) {
        if (Double.isNaN(value)) {
            return;
        }

        int index = findInsertIndex(value);

        if (index == -1) {
            values.add(value);
        } else {
            values.add(index, value);
        }
    }

    /**
     * Remove a value from the collection.
     *
     * @param value to remove
     */
    public void remove(double value) {
        if (Double.isNaN(value)) {
            return;
        }

        int index = findInsertIndex(value);
        if (index == -1) {
            return;
        }
        Double valueAtIndex = values.get(index);
        if ((valueAtIndex != null) && (!valueAtIndex.equals(value))) {
            return;
        }
        values.remove(index);
    }

    /**
     * Returns underlying ArrayList, for testing purposes only.
     *
     * @return sorted double values list
     */
    public ArrayList<Double> getValues() {
        return values;
    }

    /**
     * Returns the index into which to insert to.
     * Proptected access level for convenient testing.
     *
     * @param value to find insert index
     * @return position to insert the value to, or -1 to indicate to add to the end.
     */
    protected int findInsertIndex(double value) {
        if (values.size() > 2) {
            int startIndex = values.size() >> 1;
            double startValue = values.get(startIndex);
            int insertAt;

            if (value < startValue) {
                // find in lower half
                insertAt = findInsertIndex(0, startIndex - 1, value);
            } else if (value > startValue) {
                // find in upper half
                insertAt = findInsertIndex(startIndex + 1, values.size() - 1, value);
            } else {
                // we hit the value
                insertAt = startIndex;
            }

            if (insertAt == values.size()) {
                return -1;
            }
            return insertAt;
        }

        if (values.size() == 2) {
            if (value > values.get(1)) {
                return -1;
            } else if (value <= values.get(0)) {
                return 0;
            } else {
                return 1;
            }
        }

        if (values.size() == 1) {
            if (value > values.get(0)) {
                return -1;
            } else {
                return 0;
            }
        }

        return -1;
    }

    private int findInsertIndex(int lowerBound, int upperBound, double value) {
        while (true) {
            if (upperBound == lowerBound) {
                double valueLowerBound = values.get(lowerBound);
                if (value <= valueLowerBound) {
                    return lowerBound;
                } else {
                    return lowerBound + 1;
                }
            }

            if (upperBound - lowerBound == 1) {
                double valueLowerBound = values.get(lowerBound);
                if (value <= valueLowerBound) {
                    return lowerBound;
                }

                double valueUpperBound = values.get(upperBound);
                if (value > valueUpperBound) {
                    return upperBound + 1;
                }

                return upperBound;
            }

            int nextMiddle = lowerBound + ((upperBound - lowerBound) >> 1);
            double valueAtMiddle = values.get(nextMiddle);

            if (value < valueAtMiddle) {
                // find in lower half
                upperBound = nextMiddle - 1;
            } else if (value > valueAtMiddle) {
                // find in upper half
                lowerBound = nextMiddle;
            } else {
                return nextMiddle;
            }
        }
    }
}
