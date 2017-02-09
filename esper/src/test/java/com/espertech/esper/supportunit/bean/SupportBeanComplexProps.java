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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SupportBeanComplexProps implements Serializable, SupportMarkerInterface {
    private String simpleProperty;
    private Properties mappedProps;
    private int[] indexedProps;
    private SupportBeanSpecialGetterNested nested;
    private Map<String, String> mapProperty;
    private int[] arrayProperty;
    private Object[] objectArray;

    public static String[] PROPERTIES =
            {"simpleProperty", "mapped", "indexed", "mapProperty", "arrayProperty", "nested", "objectArray"};

    public static SupportBeanComplexProps makeDefaultBean() {
        Properties properties = new Properties();
        properties.put("keyOne", "valueOne");
        properties.put("keyTwo", "valueTwo");

        Map<String, String> mapProp = new HashMap<String, String>();
        mapProp.put("xOne", "yOne");
        mapProp.put("xTwo", "yTwo");

        int[] arrayProp = new int[]{10, 20, 30};

        return new SupportBeanComplexProps("simple", properties, new int[]{1, 2}, mapProp, arrayProp, "nestedValue", "nestedNestedValue");
    }

    public SupportBeanComplexProps() {
    }

    public SupportBeanComplexProps(int[] indexedProps) {
        this.indexedProps = indexedProps;
    }

    public SupportBeanComplexProps(String simpleProperty, Properties mappedProps, int[] indexedProps, Map<String, String> mapProperty, int[] arrayProperty, String nestedValue, String nestedNestedValue) {
        this.simpleProperty = simpleProperty;
        this.mappedProps = mappedProps;
        this.indexedProps = indexedProps;
        this.mapProperty = mapProperty;
        this.arrayProperty = arrayProperty;
        this.nested = new SupportBeanSpecialGetterNested(nestedValue, nestedNestedValue);
    }

    public String getSimpleProperty() {
        return simpleProperty;
    }

    public void setSimpleProperty(String simpleProperty) {
        this.simpleProperty = simpleProperty;
    }

    public Map<String, String> getMapProperty() {
        return mapProperty;
    }

    public String getMapped(String key) {
        return (String) mappedProps.get(key);
    }

    public int getIndexed(int index) {
        return indexedProps[index];
    }

    public SupportBeanSpecialGetterNested getNested() {
        return nested;
    }

    public int[] getArrayProperty() {
        return arrayProperty;
    }

    public void setIndexed(int index, int value) {
        indexedProps[index] = value;
    }

    public void setArrayProperty(int[] arrayProperty) {
        this.arrayProperty = arrayProperty;
    }

    public void setIndexedProps(int[] indexedProps) {
        this.indexedProps = indexedProps;
    }

    public void setMappedProps(Properties mappedProps) {
        this.mappedProps = mappedProps;
    }

    public void setMapProperty(Map<String, String> mapProperty) {
        this.mapProperty = mapProperty;
    }

    public void setNested(SupportBeanSpecialGetterNested nested) {
        this.nested = nested;
    }

    public Object[] getObjectArray() {
        return objectArray;
    }

    public void setObjectArray(Object[] objectArray) {
        this.objectArray = objectArray;
    }

    public static class SupportBeanSpecialGetterNested implements Serializable {
        private String nestedValue;
        private SupportBeanSpecialGetterNestedNested nestedNested;

        public SupportBeanSpecialGetterNested(String nestedValue, String nestedNestedValue) {
            this.nestedValue = nestedValue;
            this.nestedNested = new SupportBeanSpecialGetterNestedNested(nestedNestedValue);
        }

        public String getNestedValue() {
            return nestedValue;
        }

        public void setNestedValue(String nestedValue) {
            this.nestedValue = nestedValue;
        }

        public SupportBeanSpecialGetterNestedNested getNestedNested() {
            return nestedNested;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SupportBeanSpecialGetterNested that = (SupportBeanSpecialGetterNested) o;

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
