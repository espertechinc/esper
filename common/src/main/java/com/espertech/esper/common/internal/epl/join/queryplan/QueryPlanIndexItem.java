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
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexIndexMultiKeyPart;
import com.espertech.esper.common.client.util.StateMgmtSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexItem {
    public final static EPTypeClass EPTYPE = new EPTypeClass(QueryPlanIndexItem.class);

    private final String[] hashProps;
    private final EPTypeClass[] hashPropTypes;
    private final EventPropertyValueGetter hashGetter;
    private final MultiKeyFromObjectArray transformFireAndForget;
    private final DataInputOutputSerde<Object> hashKeySerde;
    private final String[] rangeProps;
    private final EPTypeClass[] rangePropTypes;
    private final EventPropertyValueGetter[] rangeGetters;
    private final DataInputOutputSerde<Object>[] rangeKeySerdes;
    private final boolean unique;
    private final EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc;
    private final StateMgmtSetting stateMgmtSettings;

    public QueryPlanIndexItem(String[] hashProps, EPTypeClass[] hashPropTypes, EventPropertyValueGetter hashGetter, MultiKeyFromObjectArray transformFireAndForget, DataInputOutputSerde<Object> hashKeySerde,
                              String[] rangeProps, EPTypeClass[] rangePropTypes, EventPropertyValueGetter[] rangeGetters, DataInputOutputSerde<Object>[] rangeKeySerdes,
                              boolean unique, EventAdvancedIndexProvisionRuntime advancedIndexProvisionDesc, StateMgmtSetting stateMgmtSettings) {
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
        this.stateMgmtSettings = stateMgmtSettings;
    }

    public String[] getHashProps() {
        return hashProps;
    }

    public EventPropertyValueGetter getHashGetter() {
        return hashGetter;
    }

    public EPTypeClass[] getHashPropTypes() {
        return hashPropTypes;
    }

    public String[] getRangeProps() {
        return rangeProps;
    }

    public EPTypeClass[] getRangePropTypes() {
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

    public List<IndexedPropDesc> getHashPropsAsList() {
        return asList(hashProps, hashPropTypes);
    }

    public List<IndexedPropDesc> getBtreePropsAsList() {
        return asList(rangeProps, rangePropTypes);
    }

    private List<IndexedPropDesc> asList(String[] props, EPTypeClass[] types) {
        if (props == null || props.length == 0) {
            return Collections.emptyList();
        }
        List<IndexedPropDesc> list = new ArrayList<>(props.length);
        for (int i = 0; i < props.length; i++) {
            list.add(new IndexedPropDesc(props[i], types[i]));
        }
        return list;
    }

    public IndexMultiKey toIndexMultiKey() {
        AdvancedIndexIndexMultiKeyPart part = null;
        if (advancedIndexProvisionDesc != null) {
            part = new AdvancedIndexIndexMultiKeyPart(advancedIndexProvisionDesc.getIndexTypeName(), advancedIndexProvisionDesc.getIndexExpressionTexts(), advancedIndexProvisionDesc.getIndexProperties());
        }
        return new IndexMultiKey(unique, getHashPropsAsList(), getBtreePropsAsList(), part);
    }

    public StateMgmtSetting getStateMgmtSettings() {
        return stateMgmtSettings;
    }
}
