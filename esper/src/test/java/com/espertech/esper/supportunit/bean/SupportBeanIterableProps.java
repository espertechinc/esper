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
package com.espertech.esper.supportunit.bean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SupportBeanIterableProps implements Serializable, SupportMarkerInterface {
    public static SupportBeanIterableProps makeDefaultBean() {
        return new SupportBeanIterableProps();
    }

    public Iterable<SupportBeanIterableProps.SupportBeanSpecialGetterNested> getIterableNested() {
        return Arrays.asList(new SupportBeanIterableProps.SupportBeanSpecialGetterNested("IN1", "INN1"), new SupportBeanIterableProps.SupportBeanSpecialGetterNested("IN2", "INN2"));
    }

    public Iterable<Integer> getIterableInteger() {
        return Arrays.asList(10, 20);
    }

    public Iterable getIterableUndefined() {
        return Arrays.asList(10, 20);
    }

    public Iterable<Object> getIterableObject() {
        return Arrays.asList((Object) Integer.valueOf(20), Integer.valueOf(30));
    }

    public List<SupportBeanSpecialGetterNested> getListNested() {
        return Arrays.asList(new SupportBeanIterableProps.SupportBeanSpecialGetterNested("LN1", "LNN1"), new SupportBeanIterableProps.SupportBeanSpecialGetterNested("LN2", "LNN2"));
    }

    public List<Integer> getListInteger() {
        return Arrays.asList(100, 200);
    }

    public Map<String, SupportBeanIterableProps.SupportBeanSpecialGetterNested> getMapNested() {
        Map<String, SupportBeanIterableProps.SupportBeanSpecialGetterNested> map = new HashMap<String, SupportBeanIterableProps.SupportBeanSpecialGetterNested>();
        map.put("a", new SupportBeanIterableProps.SupportBeanSpecialGetterNested("MN1", "MNN1"));
        map.put("b", new SupportBeanIterableProps.SupportBeanSpecialGetterNested("MN2", "MNN2"));
        return map;
    }

    public Map<String, Integer> getMapInteger() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("c", 1000);
        map.put("d", 2000);
        return map;
    }

    public static class SupportBeanSpecialGetterNested implements Serializable {
        private String nestedValue;
        private SupportBeanIterableProps.SupportBeanSpecialGetterNestedNested nestedNested;

        public SupportBeanSpecialGetterNested(String nestedValue, String nestedNestedValue) {
            this.nestedValue = nestedValue;
            this.nestedNested = new SupportBeanIterableProps.SupportBeanSpecialGetterNestedNested(nestedNestedValue);
        }

        public String getNestedValue() {
            return nestedValue;
        }

        public void setNestedValue(String nestedValue) {
            this.nestedValue = nestedValue;
        }

        public SupportBeanIterableProps.SupportBeanSpecialGetterNestedNested getNestedNested() {
            return nestedNested;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SupportBeanIterableProps.SupportBeanSpecialGetterNested that = (SupportBeanIterableProps.SupportBeanSpecialGetterNested) o;

            if (!nestedValue.equals(that.nestedValue)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return nestedValue.hashCode();
        }
    }

    public static class SupportBeanSpecialGetterNestedNested implements Serializable {
        private String nestedNestedValue;

        public SupportBeanSpecialGetterNestedNested(String nestedNestedValue) {
            this.nestedNestedValue = nestedNestedValue;
        }

        public String getNestedNestedValue() {
            return nestedNestedValue;
        }

        public void setNestedNestedValue(String nestedNestedValue) {
            this.nestedNestedValue = nestedNestedValue;
        }
    }
}
