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
package com.espertech.esper.supportregression.bean;

import java.util.ArrayList;
import java.util.List;

public class SupportBean_ST0_Container {

    private static String[] samples;

    public static void setSamples(String[] samples) {
        SupportBean_ST0_Container.samples = samples;
    }

    private List<SupportBean_ST0> contained;
    private List<SupportBean_ST0> containedTwo;

    public SupportBean_ST0_Container(List<SupportBean_ST0> contained) {
        this.contained = contained;
    }

    public SupportBean_ST0_Container(List<SupportBean_ST0> contained, List<SupportBean_ST0> containedTwo) {
        this.contained = contained;
        this.containedTwo = containedTwo;
    }

    public static List<SupportBean_ST0> makeSampleList() {
        if (samples == null) {
            return null;
        }
        return make2Value(samples).getContained();
    }

    public static SupportBean_ST0[] makeSampleArray() {
        if (samples == null) {
            return null;
        }
        List<SupportBean_ST0> items = make2Value(samples).getContained();
        return items.toArray(new SupportBean_ST0[items.size()]);
    }

    public static SupportBean_ST0_Container make3Value(String... values) {
        if (values == null) {
            return new SupportBean_ST0_Container(null);
        }
        List<SupportBean_ST0> contained = new ArrayList<SupportBean_ST0>();
        for (int i = 0; i < values.length; i++) {
            String[] triplet = values[i].split(",");
            contained.add(new SupportBean_ST0(triplet[0], triplet[1], Integer.parseInt(triplet[2])));
        }
        return new SupportBean_ST0_Container(contained);
    }

    public static List<SupportBean_ST0> make2ValueList(String... values) {
        if (values == null) {
            return null;
        }
        List<SupportBean_ST0> result = new ArrayList<SupportBean_ST0>();
        for (int i = 0; i < values.length; i++) {
            String[] pair = values[i].split(",");
            result.add(new SupportBean_ST0(pair[0], Integer.parseInt(pair[1])));
        }
        return result;
    }

    public static SupportBean_ST0_Container make2Value(String... values) {
        return new SupportBean_ST0_Container(make2ValueList(values));
    }

    public List<SupportBean_ST0> getContained() {
        return contained;
    }

    public List<SupportBean_ST0> getContainedTwo() {
        return containedTwo;
    }

    public static SupportBean_ST0 makeTest(String value) {
        return make2Value(value).getContained().get(0);
    }
}
