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
package com.espertech.esper.epl.virtualdw;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValue;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValueRange;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableOrganization;
import com.espertech.esper.epl.join.table.EventTableOrganizationType;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.spec.CreateIndexDesc;
import com.espertech.esper.epl.spec.CreateIndexItem;
import com.espertech.esper.filterspec.Range;
import com.espertech.esper.view.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

public class VirtualDWViewImpl extends ViewSupport implements VirtualDWView {

    private final static EventTableOrganization TABLE_ORGANIZATION = new EventTableOrganization(null, false, false, 0, null, EventTableOrganizationType.VDW);

    private static final Logger log = LoggerFactory.getLogger(VirtualDWViewImpl.class);

    private final VirtualDataWindow dataExternal;
    private final EventType eventType;
    private final String namedWindowName;
    private String lastAccessedByStatementName;
    private int lastAccessedByNum;

    public VirtualDWViewImpl(VirtualDataWindow dataExternal, EventType eventType, String namedWindowName) {
        this.dataExternal = dataExternal;
        this.eventType = eventType;
        this.namedWindowName = namedWindowName;
    }

    public VirtualDataWindow getVirtualDataWindow() {
        return dataExternal;
    }

    public Pair<IndexMultiKey, EventTable> getSubordinateQueryDesc(boolean unique, List<IndexedPropDesc> hashedProps, List<IndexedPropDesc> btreeProps) {
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
        return new Pair<IndexMultiKey, EventTable>(imk, eventTable);
    }

    public SubordTableLookupStrategy getSubordinateLookupStrategy(String accessedByStatementName, int accessedByStatementId, Annotation[] accessedByStmtAnnotations, EventType[] outerStreamTypes, List<SubordPropHashKey> hashKeys, CoercionDesc hashKeyCoercionTypes, List<SubordPropRangeKey> rangeKeys, CoercionDesc rangeKeyCoercionTypes, boolean nwOnTrigger, EventTable eventTable, SubordPropPlan joinDesc, boolean forceTableScan) {
        VirtualDWEventTable noopTable = (VirtualDWEventTable) eventTable;
        for (int i = 0; i < noopTable.getBtreeAccess().size(); i++) {
            VirtualDataWindowLookupOp op = VirtualDataWindowLookupOp.fromOpString(rangeKeys.get(i).getRangeInfo().getType().getStringOp());
            noopTable.getBtreeAccess().get(i).setOperator(op);
        }

        // allocate a number within the statement
        if (lastAccessedByStatementName == null || !lastAccessedByStatementName.equals(accessedByStatementName)) {
            lastAccessedByNum = 0;
        }
        lastAccessedByNum++;

        VirtualDataWindowLookupContextSPI context = new VirtualDataWindowLookupContextSPI(accessedByStatementName, accessedByStatementId, accessedByStmtAnnotations, false, namedWindowName, noopTable.getHashAccess(), noopTable.getBtreeAccess(), joinDesc, forceTableScan, outerStreamTypes, accessedByStatementName, lastAccessedByNum);
        VirtualDataWindowLookup index = dataExternal.getLookup(context);
        checkIndex(index);
        return new SubordTableLookupStrategyVirtualDW(namedWindowName, index, hashKeys, hashKeyCoercionTypes, rangeKeys, rangeKeyCoercionTypes, nwOnTrigger, outerStreamTypes.length);
    }

    public EventTable getJoinIndexTable(QueryPlanIndexItem queryPlanIndexItem) {

        List<VirtualDataWindowLookupFieldDesc> hashFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        int count = 0;
        if (queryPlanIndexItem.getIndexProps() != null) {
            for (String indexProp : queryPlanIndexItem.getIndexProps()) {
                Class coercionType = queryPlanIndexItem.getOptIndexCoercionTypes() == null ? null : queryPlanIndexItem.getOptIndexCoercionTypes()[count];
                hashFields.add(new VirtualDataWindowLookupFieldDesc(indexProp, VirtualDataWindowLookupOp.EQUALS, coercionType));
                count++;
            }
        }

        List<VirtualDataWindowLookupFieldDesc> btreeFields = new ArrayList<VirtualDataWindowLookupFieldDesc>();
        count = 0;
        if (queryPlanIndexItem.getRangeProps() != null) {
            for (String btreeprop : queryPlanIndexItem.getRangeProps()) {
                Class coercionType = queryPlanIndexItem.getOptRangeCoercionTypes() == null ? null : queryPlanIndexItem.getOptRangeCoercionTypes()[count];
                btreeFields.add(new VirtualDataWindowLookupFieldDesc(btreeprop, null, coercionType));
                count++;
            }
        }

        return new VirtualDWEventTable(false, hashFields, btreeFields, TABLE_ORGANIZATION);
    }

    public JoinExecTableLookupStrategy getJoinLookupStrategy(String accessedByStmtName, int accessedByStmtId, Annotation[] accessedByStmtAnnotations, EventTable[] eventTables, TableLookupKeyDesc keyDescriptor, int lookupStreamNum) {
        VirtualDWEventTable noopTable = (VirtualDWEventTable) eventTables[0];
        for (int i = 0; i < noopTable.getHashAccess().size(); i++) {
            QueryGraphValueEntryHashKeyed hashKey = keyDescriptor.getHashes().get(i);
            noopTable.getHashAccess().get(i).setLookupValueType(hashKey.getKeyExpr().getForge().getEvaluationType());
        }
        for (int i = 0; i < noopTable.getBtreeAccess().size(); i++) {
            QueryGraphValueEntryRange range = keyDescriptor.getRanges().get(i);
            VirtualDataWindowLookupOp op = VirtualDataWindowLookupOp.fromOpString(range.getType().getStringOp());
            VirtualDataWindowLookupFieldDesc rangeField = noopTable.getBtreeAccess().get(i);
            rangeField.setOperator(op);
            if (range instanceof QueryGraphValueEntryRangeRelOp) {
                rangeField.setLookupValueType(((QueryGraphValueEntryRangeRelOp) range).getExpression().getForge().getEvaluationType());
            } else {
                rangeField.setLookupValueType(((QueryGraphValueEntryRangeIn) range).getExprStart().getForge().getEvaluationType());
            }
        }

        VirtualDataWindowLookup index = dataExternal.getLookup(new VirtualDataWindowLookupContext(accessedByStmtName, accessedByStmtId, accessedByStmtAnnotations, false, namedWindowName, noopTable.getHashAccess(), noopTable.getBtreeAccess()));
        checkIndex(index);
        return new JoinExecTableLookupStrategyVirtualDW(namedWindowName, index, keyDescriptor, lookupStreamNum);
    }

    public Pair<IndexMultiKey, EventTable> getFireAndForgetDesc(Set<String> keysAvailable, Set<String> rangesAvailable) {
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

    public Collection<EventBean> getFireAndForgetData(EventTable eventTable, Object[] keyValues, RangeIndexLookupValue[] rangeValues, Annotation[] annotations) {
        VirtualDWEventTable noopTable = (VirtualDWEventTable) eventTable;
        for (int i = 0; i < noopTable.getBtreeAccess().size(); i++) {
            RangeIndexLookupValueRange range = (RangeIndexLookupValueRange) rangeValues[i];
            VirtualDataWindowLookupOp op = VirtualDataWindowLookupOp.fromOpString(range.getOperator().getStringOp());
            noopTable.getBtreeAccess().get(i).setOperator(op);
        }

        Object[] keys = new Object[keyValues.length + rangeValues.length];
        for (int i = 0; i < keyValues.length; i++) {
            keys[i] = keyValues[i];
            noopTable.getHashAccess().get(i).setLookupValueType(keyValues[i] == null ? null : keyValues[i].getClass());
        }
        int offset = keyValues.length;
        for (int j = 0; j < rangeValues.length; j++) {
            Object rangeValue = rangeValues[j].getValue();
            if (rangeValue instanceof Range) {
                Range range = (Range) rangeValue;
                keys[j + offset] = new VirtualDataWindowKeyRange(range.getLowEndpoint(), range.getHighEndpoint());
                noopTable.getBtreeAccess().get(j).setLookupValueType(range.getLowEndpoint() == null ? null : range.getLowEndpoint().getClass());
            } else {
                keys[j + offset] = rangeValue;
                noopTable.getBtreeAccess().get(j).setLookupValueType(rangeValue == null ? null : rangeValue.getClass());
            }
        }

        VirtualDataWindowLookup index = dataExternal.getLookup(new VirtualDataWindowLookupContext(null, -1, annotations, true, namedWindowName, noopTable.getHashAccess(), noopTable.getBtreeAccess()));
        checkIndex(index);
        if (index == null) {
            throw new EPException("Exception obtaining index from virtual data window '" + namedWindowName + "'");
        }

        Set<EventBean> events = null;
        try {
            events = index.lookup(keys, null);
        } catch (RuntimeException ex) {
            log.warn("Exception encountered invoking virtual data window external index for window '" + namedWindowName + "': " + ex.getMessage(), ex);
        }
        return events;
    }

    private void checkIndex(VirtualDataWindowLookup index) {
        if (index == null) {
            throw new EPException("Exception obtaining index lookup from virtual data window, the implementation has returned a null index");
        }
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        dataExternal.update(newData, oldData);
    }

    public EventType getEventType() {
        return eventType;
    }

    public void destroy() {
        dataExternal.destroy();
    }

    public Iterator<EventBean> iterator() {
        return dataExternal.iterator();
    }

    public void handleStartIndex(CreateIndexDesc spec) {
        try {
            List<VirtualDataWindowEventStartIndex.VDWCreateIndexField> fields = new ArrayList<VirtualDataWindowEventStartIndex.VDWCreateIndexField>();
            for (CreateIndexItem col : spec.getColumns()) {
                fields.add(new VirtualDataWindowEventStartIndex.VDWCreateIndexField(col.getExpressions(), col.getType(), col.getParameters()));
            }
            VirtualDataWindowEventStartIndex create = new VirtualDataWindowEventStartIndex(spec.getWindowName(), spec.getIndexName(), fields, spec.isUnique());
            dataExternal.handleEvent(create);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle start-index event for window '" + namedWindowName + "': " + ex.getMessage();
            log.warn(message, ex);
            throw new EPException(message, ex);
        }
    }

    public void handleStopIndex(CreateIndexDesc spec) {
        try {
            VirtualDataWindowEventStopIndex theEvent = new VirtualDataWindowEventStopIndex(spec.getWindowName(), spec.getIndexName());
            dataExternal.handleEvent(theEvent);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle stop-index event for window '" + namedWindowName + "': " + ex.getMessage();
            log.warn(message, ex);
        }
    }

    public void handleStopWindow() {
        try {
            VirtualDataWindowEventStopWindow theEvent = new VirtualDataWindowEventStopWindow(namedWindowName);
            dataExternal.handleEvent(theEvent);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle stop-window event for window '" + namedWindowName + "': " + ex.getMessage();
            log.warn(message, ex);
        }
    }
}
