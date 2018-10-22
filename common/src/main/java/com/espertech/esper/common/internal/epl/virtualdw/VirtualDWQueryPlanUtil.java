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
package com.espertech.esper.common.internal.epl.virtualdw;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowLookupFieldDesc;
import com.espertech.esper.common.client.hook.vdw.VirtualDataWindowLookupOp;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VirtualDWQueryPlanUtil {
    private final static EventTableOrganization TABLE_ORGANIZATION = new EventTableOrganization(null, false, false, 0, null, EventTableOrganizationType.VDW);

    public static Pair<IndexMultiKey, VirtualDWEventTable> getSubordinateQueryDesc(boolean unique, IndexedPropDesc[] hashedProps, IndexedPropDesc[] btreeProps) {
        List<VirtualDataWindowLookupFieldDesc> hashFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        for (IndexedPropDesc hashprop : hashedProps) {
            hashFields.add(new VirtualDataWindowLookupFieldDesc(hashprop.getIndexPropName(), VirtualDataWindowLookupOp.EQUALS, hashprop.getCoercionType()));
        }
        List<VirtualDataWindowLookupFieldDesc> btreeFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        for (IndexedPropDesc btreeprop : btreeProps) {
            btreeFields.add(new VirtualDataWindowLookupFieldDesc(btreeprop.getIndexPropName(), null, btreeprop.getCoercionType()));
        }
        VirtualDWEventTable eventTable = new VirtualDWEventTable(unique, hashFields, btreeFields, TABLE_ORGANIZATION);
        IndexMultiKey imk = new IndexMultiKey(unique, hashedProps, btreeProps, null);
        return new Pair<>(imk, eventTable);
    }

    public static EventTable getJoinIndexTable(QueryPlanIndexItem queryPlanIndexItem) {

        List<VirtualDataWindowLookupFieldDesc> hashFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        int count = 0;
        if (queryPlanIndexItem.getHashProps() != null) {
            for (String indexProp : queryPlanIndexItem.getHashProps()) {
                Class coercionType = queryPlanIndexItem.getHashPropTypes() == null ? null : queryPlanIndexItem.getHashPropTypes()[count];
                hashFields.add(new VirtualDataWindowLookupFieldDesc(indexProp, VirtualDataWindowLookupOp.EQUALS, coercionType));
                count++;
            }
        }

        List<VirtualDataWindowLookupFieldDesc> btreeFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        count = 0;
        if (queryPlanIndexItem.getRangeProps() != null) {
            for (String btreeprop : queryPlanIndexItem.getRangeProps()) {
                Class coercionType = queryPlanIndexItem.getRangePropTypes() == null ? null : queryPlanIndexItem.getRangePropTypes()[count];
                btreeFields.add(new VirtualDataWindowLookupFieldDesc(btreeprop, null, coercionType));
                count++;
            }
        }

        return new VirtualDWEventTable(false, hashFields, btreeFields, TABLE_ORGANIZATION);
    }

    public static Pair<IndexMultiKey, EventTable> getFireAndForgetDesc(EventType eventType, Set<String> keysAvailable, Set<String> rangesAvailable) {
        List<VirtualDataWindowLookupFieldDesc> hashFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        List<IndexedPropDesc> hashIndexedFields = new ArrayList<IndexedPropDesc>();
        for (String hashprop : keysAvailable) {
            hashFields.add(new VirtualDataWindowLookupFieldDesc(hashprop, VirtualDataWindowLookupOp.EQUALS, null));
            hashIndexedFields.add(new IndexedPropDesc(hashprop, eventType.getPropertyType(hashprop)));
        }

        List<VirtualDataWindowLookupFieldDesc> btreeFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        List<IndexedPropDesc> btreeIndexedFields = new ArrayList<IndexedPropDesc>();
        for (String btreeprop : rangesAvailable) {
            btreeFields.add(new VirtualDataWindowLookupFieldDesc(btreeprop, null, null));
            btreeIndexedFields.add(new IndexedPropDesc(btreeprop, eventType.getPropertyType(btreeprop)));
        }

        VirtualDWEventTable noopTable = new VirtualDWEventTable(false, hashFields, btreeFields, TABLE_ORGANIZATION);
        IndexMultiKey imk = new IndexMultiKey(false, hashIndexedFields, btreeIndexedFields, null);

        return new Pair<IndexMultiKey, EventTable>(imk, noopTable);
    }
}
