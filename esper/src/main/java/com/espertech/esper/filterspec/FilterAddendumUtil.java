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
package com.espertech.esper.filterspec;

public class FilterAddendumUtil {

    public static FilterValueSetParam[][] addAddendum(FilterValueSetParam[][] filters, FilterValueSetParam toAdd) {
        return addAddendum(filters, new FilterValueSetParam[]{toAdd});
    }

    public static FilterValueSetParam[][] addAddendum(FilterValueSetParam[][] filters, FilterValueSetParam[] toAdd) {
        if (filters.length == 0) {
            filters = new FilterValueSetParam[1][];
            filters[0] = new FilterValueSetParam[0];
        }

        FilterValueSetParam[][] params = new FilterValueSetParam[filters.length][];
        for (int i = 0; i < params.length; i++) {
            params[i] = append(filters[i], toAdd);
        }
        return params;
    }

    public static FilterValueSetParam[][] multiplyAddendum(FilterValueSetParam[][] filtersFirst, FilterValueSetParam[][] filtersSecond) {

        if (filtersFirst == null || filtersFirst.length == 0) {
            return filtersSecond;
        }
        if (filtersSecond == null || filtersSecond.length == 0) {
            return filtersFirst;
        }

        int size = filtersFirst.length * filtersSecond.length;
        FilterValueSetParam[][] result = new FilterValueSetParam[size][];

        int count = 0;
        for (FilterValueSetParam[] lineFirst : filtersFirst) {
            for (FilterValueSetParam[] lineSecond : filtersSecond) {
                result[count] = append(lineFirst, lineSecond);
                count++;
            }
        }

        return result;
    }

    private static FilterValueSetParam[] append(FilterValueSetParam[] first, FilterValueSetParam[] second) {
        FilterValueSetParam[] appended = new FilterValueSetParam[first.length + second.length];
        System.arraycopy(first, 0, appended, 0, first.length);
        System.arraycopy(second, 0, appended, first.length, second.length);
        return appended;
    }
}

