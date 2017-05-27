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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * indexed[0].mapped('0ma').value = 0ma0
 */
public class SupportBeanCombinedProps implements Serializable {
    public static String[] PROPERTIES = {"indexed", "array"};

    public static SupportBeanCombinedProps makeDefaultBean() {
        NestedLevOne[] nested = new NestedLevOne[4];        // [3] left empty on purpose
        nested[0] = new NestedLevOne(new String[][]{{"0ma", "0ma0"}, {"0mb", "0ma1"}});
        nested[1] = new NestedLevOne(new String[][]{{"1ma", "1ma0"}, {"1mb", "1ma1"}});
        nested[2] = new NestedLevOne(new String[][]{{"2ma", "valueOne"}, {"2mb", "2ma1"}});

        return new SupportBeanCombinedProps(nested);
    }

    private NestedLevOne[] indexed;

    public SupportBeanCombinedProps(NestedLevOne[] indexed) {
        this.indexed = indexed;
    }

    public NestedLevOne getIndexed(int index) {
        return indexed[index];
    }

    public NestedLevOne[] getArray() {
        return indexed;
    }

    public static class NestedLevOne implements Serializable {
        private Map<String, NestedLevTwo> map = new HashMap<String, NestedLevTwo>();

        public NestedLevOne(String[][] keysAndValues) {
            for (int i = 0; i < keysAndValues.length; i++) {
                map.put(keysAndValues[i][0], new NestedLevTwo(keysAndValues[i][1]));
            }
        }

        public NestedLevTwo getMapped(String key) {
            return map.get(key);
        }

        public Map<String, NestedLevTwo> getMapprop() {
            return map;
        }

        public String getNestLevOneVal() {
            return "abc";
        }
    }

    public static class NestedLevTwo implements Serializable {
        private String value;

        public NestedLevTwo(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
