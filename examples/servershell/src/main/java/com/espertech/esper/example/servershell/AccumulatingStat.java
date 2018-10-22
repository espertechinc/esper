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
package com.espertech.esper.example.servershell;

import java.util.LinkedList;

public class AccumulatingStat {
    private final int size;
    private final LinkedList<Double> data;

    public AccumulatingStat(int size) {
        this.size = size;
        data = new LinkedList<Double>();
    }

    public void add(double point) {
        data.add(point);
        if (data.size() > size) {
            data.removeFirst();
        }
    }

    public double getAvg() {
        if (data.size() < size) {
            return -1;
        }

        int total = 0;
        for (Double vals : data) {
            if (vals < 1) {
                return -1;
            }
            total += vals;
        }
        double result = total / size;
        return result;
    }
}
