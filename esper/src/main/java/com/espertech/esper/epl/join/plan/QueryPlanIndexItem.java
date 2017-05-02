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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.epl.index.service.EventAdvancedIndexProvisionDesc;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.lookup.IndexedPropDesc;
import com.espertech.esper.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexItem {
    private final String[] indexProps;
    private Class[] optIndexCoercionTypes;
    private final String[] rangeProps;
    private final Class[] optRangeCoercionTypes;
    private final boolean unique;
    private final EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc;

    /**
     * Ctor.
     *
     * @param indexProps                 - array of property names with the first dimension suplying the number of
     *                                   distinct indexes. The second dimension can be empty and indicates a full table scan.
     * @param optIndexCoercionTypes      - array of coercion types for each index, or null entry for no coercion required
     * @param rangeProps                 range props
     * @param optRangeCoercionTypes      coercion for ranges
     * @param unique                     whether index is unique on index props (not applicable to range-only)
     * @param advancedIndexProvisionDesc advanced indexes
     */
    public QueryPlanIndexItem(String[] indexProps, Class[] optIndexCoercionTypes, String[] rangeProps, Class[] optRangeCoercionTypes, boolean unique, EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc) {
        if (advancedIndexProvisionDesc == null) {
            if (unique && indexProps.length == 0) {
                throw new IllegalArgumentException("Invalid unique index planned without hash index props");
            }
            if (unique && rangeProps.length > 0) {
                throw new IllegalArgumentException("Invalid unique index planned that includes range props");
            }
        }
        this.indexProps = indexProps;
        this.optIndexCoercionTypes = optIndexCoercionTypes;
        this.rangeProps = (rangeProps == null || rangeProps.length == 0) ? null : rangeProps;
        this.optRangeCoercionTypes = optRangeCoercionTypes;
        this.unique = unique;
        this.advancedIndexProvisionDesc = advancedIndexProvisionDesc;
    }

    public QueryPlanIndexItem(List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps, boolean unique, EventAdvancedIndexProvisionDesc advancedIndexProvisionDesc) {
        this(getNames(hashProps), getTypes(hashProps), getNames(btreeProps), getTypes(btreeProps), unique, advancedIndexProvisionDesc);
    }

    public String[] getIndexProps() {
        return indexProps;
    }

    public Class[] getOptIndexCoercionTypes() {
        return optIndexCoercionTypes;
    }

    public String[] getRangeProps() {
        return rangeProps;
    }

    public Class[] getOptRangeCoercionTypes() {
        return optRangeCoercionTypes;
    }

    public void setOptIndexCoercionTypes(Class[] optIndexCoercionTypes) {
        this.optIndexCoercionTypes = optIndexCoercionTypes;
    }

    public boolean isUnique() {
        return unique;
    }

    public EventAdvancedIndexProvisionDesc getAdvancedIndexProvisionDesc() {
        return advancedIndexProvisionDesc;
    }

    @Override
    public String toString() {
        return "QueryPlanIndexItem{" +
                "unique=" + unique +
                ", indexProps=" + (indexProps == null ? null : Arrays.asList(indexProps)) +
                ", rangeProps=" + (rangeProps == null ? null : Arrays.asList(rangeProps)) +
                ", optIndexCoercionTypes=" + (optIndexCoercionTypes == null ? null : Arrays.asList(optIndexCoercionTypes)) +
                ", optRangeCoercionTypes=" + (optRangeCoercionTypes == null ? null : Arrays.asList(optRangeCoercionTypes)) +
                ", advanced=" + (advancedIndexProvisionDesc == null ? null : advancedIndexProvisionDesc.getIndexDesc().getIndexTypeName()) +
                "}";
    }

    public boolean equalsCompareSortedProps(QueryPlanIndexItem other) {
        if (unique != other.unique) {
            return false;
        }
        String[] otherIndexProps = CollectionUtil.copySortArray(other.getIndexProps());
        String[] thisIndexProps = CollectionUtil.copySortArray(this.getIndexProps());
        String[] otherRangeProps = CollectionUtil.copySortArray(other.getRangeProps());
        String[] thisRangeProps = CollectionUtil.copySortArray(this.getRangeProps());
        boolean compared = CollectionUtil.compare(otherIndexProps, thisIndexProps) && CollectionUtil.compare(otherRangeProps, thisRangeProps);
        return compared && advancedIndexProvisionDesc == null && other.advancedIndexProvisionDesc == null;
    }

    public List<IndexedPropDesc> getHashPropsAsList() {
        return asList(indexProps, optIndexCoercionTypes);
    }

    public List<IndexedPropDesc> getBtreePropsAsList() {
        return asList(rangeProps, optRangeCoercionTypes);
    }

    private List<IndexedPropDesc> asList(String[] props, Class[] types) {
        if (props == null || props.length == 0) {
            return Collections.emptyList();
        }
        List<IndexedPropDesc> list = new ArrayList<>(props.length);
        for (int i = 0; i < props.length; i++) {
            list.add(new IndexedPropDesc(props[i], types[i]));
        }
        return list;
    }

    private static String[] getNames(IndexedPropDesc[] props) {
        String[] names = new String[props.length];
        for (int i = 0; i < props.length; i++) {
            names[i] = props[i].getIndexPropName();
        }
        return names;
    }

    private static Class[] getTypes(IndexedPropDesc[] props) {
        Class[] types = new Class[props.length];
        for (int i = 0; i < props.length; i++) {
            types[i] = props[i].getCoercionType();
        }
        return types;
    }

    private static String[] getNames(List<IndexedPropDesc> props) {
        String[] names = new String[props.size()];
        for (int i = 0; i < props.size(); i++) {
            names[i] = props.get(i).getIndexPropName();
        }
        return names;
    }

    private static Class[] getTypes(List<IndexedPropDesc> props) {
        Class[] types = new Class[props.size()];
        for (int i = 0; i < props.size(); i++) {
            types[i] = props.get(i).getCoercionType();
        }
        return types;
    }

    public static QueryPlanIndexItem fromIndexMultikeyTablePrimaryKey(IndexMultiKey indexMultiKey) {
        return new QueryPlanIndexItem(
                getNames(indexMultiKey.getHashIndexedProps()),
                getTypes(indexMultiKey.getHashIndexedProps()),
                getNames(indexMultiKey.getRangeIndexedProps()),
                getTypes(indexMultiKey.getRangeIndexedProps()),
                indexMultiKey.isUnique(), null);
    }
}
