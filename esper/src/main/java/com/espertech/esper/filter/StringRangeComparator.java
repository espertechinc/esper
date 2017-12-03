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
package com.espertech.esper.filter;

import com.espertech.esper.filterspec.StringRange;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for DoubleRange values.
 * <p>Sorts double ranges as this:     sort by min asc, max asc.
 * I.e. same minimum value sorts maximum value ascending.
 */
public final class StringRangeComparator implements Comparator<StringRange>, Serializable {
    private static final long serialVersionUID = 612230810237318028L;

    public final int compare(StringRange r1, StringRange r2) {
        if (r1.getMin() == null) {
            if (r2.getMin() != null) {
                return -1;
            }
        } else {
            if (r2.getMin() == null) {
                return 1;
            }
            int comp = r1.getMin().compareTo(r2.getMin());
            if (comp != 0) {
                return comp;
            }
        }

        if (r1.getMax() == null) {
            if (r2.getMax() != null) {
                return 1;
            }
            return 0;
        } else {
            if (r2.getMax() == null) {
                return 0;
            }
            return r1.getMax().compareTo(r2.getMax());
        }
    }
}
