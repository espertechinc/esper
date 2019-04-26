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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexIndexMultiKeyPart;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexItem {
    private final String[] hashProps;
    private final Class[] hashPropTypes;
    private final EventPropertyValueGetter hashGetter;
    private final MultiKeyFromObjectArray transformFireAndForget;
    private final DataInputOutputSerde<Object> hashKeySerde;
    private final String[] rangeProps;
    private final Class[] rangePropTypes;
    private final EventPropertyValueGetter[] rangeGetters;
    private final DataInputOutputSerde<Object>[] rangeKeySerdes;
    private final boolean unique;
    private final EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc;

    public QueryPlanIndexItem(String[] hashProps, Class[] hashPropTypes, EventPropertyValueGetter hashGetter, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> hashKeySerde,
                              String[] rangeProps, Class[] rangePropTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde<Object>[] rangeKeySerdes,
                              boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc) {
        this.hashProps = hashProps;
        this.hashPropTypes = hashPropTypes;
        this.hashGetter = hashGetter;
        this.hashKeySerde = hashKeySerde;
        this.transformFireAndForget = transformFireAndForget;
        this.rangeProps = (rangeProps == null || rangeProps.length == 0) ? null : rangeProps;
        this.rangePropTypes = rangePropTypes;
        this.rangeGetters = rangeGetters;
        this.rangeKeySerdes = rangeKeySerdes;
        this.unique = unique;
        this.advancedIndexProvisionDesc = advancedIndexProvisionDesc;
    }

    public String[] getHashProps() {
        return hashProps;
    }

    public EventPropertyValueGetter getHashGetter() {
        return hashGetter;
    }

    public Class[] getHashPropTypes() {
        return hashPropTypes;
    }

    public String[] getRangeProps() {
        return rangeProps;
    }

    public Class[] getRangePropTypes() {
        return rangePropTypes;
    }

    public EventPropertyValueGetter[] getRangeGetters() {
        return rangeGetters;
    }

    public boolean isUnique() {
        return unique;
    }

    public EventAdvancedIndexProvisionRuntime getAdvancedIndexProvisionDesc() {
        return advancedIndexProvisionDesc;
    }

    public DataInputOutputSerde<Object> getHashKeySerde() {
        return hashKeySerde;
    }

    public DataInputOutputSerde<Object>[] getRangeKeySerdes() {
        return rangeKeySerdes;
    }

    public MultiKeyFromObjectArray getTransformFireAndForget() {
        return transformFireAndForget;
    }

    @Override
    public String toString() {
        return "QueryPlanIndexItem{" +
            "unique=" + unique +
            ", hashProps=" + Arrays.asList(hashProps) +
            ", rangeProps=" + Arrays.asList(rangeProps) +
            ", hashPropTypes=" + Arrays.asList(hashPropTypes) +
            ", rangePropTypes=" + Arrays.asList(rangePropTypes) +
            ", advanced=" + advancedIndexProvisionDesc +
            "}";
    }

    public boolean equalsCompareSortedProps(QueryPlanIndexItem other) {
        if (unique != other.unique) {
            return false;
        }
        String[] otherIndexProps = CollectionUtil.copySortArray(other.getHashProps());
        String[] thisIndexProps = CollectionUtil.copySortArray(this.getHashProps());
        String[] otherRangeProps = CollectionUtil.copySortArray(other.getRangeProps());
        String[] thisRangeProps = CollectionUtil.copySortArray(this.getRangeProps());
        boolean compared = CollectionUtil.compare(otherIndexProps, thisIndexProps) && CollectionUtil.compare(otherRangeProps, thisRangeProps);
        return compared && advancedIndexProvisionDesc == null && other.advancedIndexProvisionDesc == null;
    }

    public List<IndexedPropDesc> getHashPropsAsList() {
        return asList(hashProps, hashPropTypes);
    }

    public List<IndexedPropDesc> getBtreePropsAsList() {
        return asList(rangeProps, rangePropTypes);
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

    public IndexMultiKey toIndexMultiKey() {
        AdvancedIndexIndexMultiKeyPart part = null;
        if (advancedIndexProvisionDesc != null) {
            part = new AdvancedIndexIndexMultiKeyPart(advancedIndexProvisionDesc.getIndexTypeName(), advancedIndexProvisionDesc.getIndexExpressionTexts(), advancedIndexProvisionDesc.getIndexProperties());
        }
        return new IndexMultiKey(unique, getHashPropsAsList(), getBtreePropsAsList(), part);
    }
}
