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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Holds property information for joined properties in a lookup.
 */
public class IndexedPropDesc implements Comparable {
    private final String indexPropName;
    private final Class coercionType;

    /**
     * Ctor.
     *
     * @param indexPropName is the property name of the indexed field
     * @param coercionType  is the type to coerce to
     */
    public IndexedPropDesc(String indexPropName, Class coercionType) {
        this.indexPropName = indexPropName;
        this.coercionType = coercionType;
    }

    /**
     * Returns the property name of the indexed field.
     *
     * @return property name of indexed field
     */
    public String getIndexPropName() {
        return indexPropName;
    }

    /**
     * Returns the coercion type of key to index field.
     *
     * @return type to coerce to
     */
    public Class getCoercionType() {
        return coercionType;
    }

    /**
     * Returns the index property names given an array of descriptors.
     *
     * @param descList descriptors of joined properties
     * @return array of index property names
     */
    public static String[] getIndexProperties(IndexedPropDesc[] descList) {
        String[] result = new String[descList.length];
        int count = 0;
        for (IndexedPropDesc desc : descList) {
            result[count++] = desc.getIndexPropName();
        }
        return result;
    }

    public static String[] getIndexProperties(List<IndexedPropDesc> descList) {
        String[] result = new String[descList.size()];
        int count = 0;
        for (IndexedPropDesc desc : descList) {
            result[count++] = desc.getIndexPropName();
        }
        return result;
    }

    public static int getPropertyIndex(String propertyName, IndexedPropDesc[] descList) {
        for (int i = 0; i < descList.length; i++) {
            if (descList[i].getIndexPropName().equals(propertyName)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the key coercion types.
     *
     * @param descList a list of descriptors
     * @return key coercion types
     */
    public static Class[] getCoercionTypes(IndexedPropDesc[] descList) {
        Class[] result = new Class[descList.length];
        int count = 0;
        for (IndexedPropDesc desc : descList) {
            result[count++] = desc.getCoercionType();
        }
        return result;
    }

    public int compareTo(Object o) {
        IndexedPropDesc other = (IndexedPropDesc) o;
        return indexPropName.compareTo(other.getIndexPropName());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IndexedPropDesc that = (IndexedPropDesc) o;

        if (!coercionType.equals(that.coercionType)) {
            return false;
        }
        if (!indexPropName.equals(that.indexPropName)) {
            return false;
        }
        return true;
    }

    public static boolean compare(List<IndexedPropDesc> first, List<IndexedPropDesc> second) {
        if (first.size() != second.size()) {
            return false;
        }
        List<IndexedPropDesc> copyFirst = new ArrayList<IndexedPropDesc>(first);
        List<IndexedPropDesc> copySecond = new ArrayList<IndexedPropDesc>(second);
        Comparator<IndexedPropDesc> comparator = new Comparator<IndexedPropDesc>() {
            public int compare(IndexedPropDesc o1, IndexedPropDesc o2) {
                return o1.getIndexPropName().compareTo(o2.getIndexPropName());
            }
        };
        Collections.sort(copyFirst, comparator);
        Collections.sort(copySecond, comparator);
        for (int i = 0; i < copyFirst.size(); i++) {
            if (!copyFirst.get(i).equals(copySecond.get(i))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result;
        result = indexPropName.hashCode();
        result = 31 * result + coercionType.hashCode();
        return result;
    }

    public static void toQueryPlan(StringWriter writer, IndexedPropDesc[] indexedProps) {
        String delimiter = "";
        for (IndexedPropDesc prop : indexedProps) {
            writer.write(delimiter);
            writer.write(prop.getIndexPropName());
            writer.write("(");
            writer.write(JavaClassHelper.getSimpleNameForClass(prop.getCoercionType()));
            writer.write(")");
            delimiter = ",";
        }
    }
}
