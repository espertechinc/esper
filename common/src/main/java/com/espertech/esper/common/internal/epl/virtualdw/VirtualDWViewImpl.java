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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.hook.vdw.*;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.index.base.EventTable;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganization;
import com.espertech.esper.common.internal.epl.index.base.EventTableOrganizationType;
import com.espertech.esper.common.internal.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.common.internal.epl.join.exec.util.RangeIndexLookupValue;
import com.espertech.esper.common.internal.epl.join.exec.util.RangeIndexLookupValueRange;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlan;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.filterspec.Range;
import com.espertech.esper.common.internal.view.core.ViewSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

public class VirtualDWViewImpl extends ViewSupport implements VirtualDWView {

    private final static EventTableOrganization TABLE_ORGANIZATION = new EventTableOrganization(null, false, false, 0, null, EventTableOrganizationType.VDW);

    private static final Logger log = LoggerFactory.getLogger(VirtualDWViewImpl.class);

    private final VirtualDWViewFactory factory;
    private final AgentInstanceContext agentInstanceContext;
    private final VirtualDataWindow dataExternal;
    private String lastAccessedByDeploymentId;
    private String lastAccessedByStatementName;
    private int lastAccessedByNum;

    public VirtualDWViewImpl(VirtualDWViewFactory factory, AgentInstanceContext agentInstanceContext, VirtualDataWindow dataExternal) {
        this.factory = factory;
        this.agentInstanceContext = agentInstanceContext;
        this.dataExternal = dataExternal;
    }

    public VirtualDataWindow getVirtualDataWindow() {
        return dataExternal;
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        dataExternal.update(newData, oldData);
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public Iterator<EventBean> iterator() {
        return dataExternal.iterator();
    }

    public void destroy() {
        dataExternal.destroy();
    }

    public SubordTableLookupStrategy getSubordinateLookupStrategy(SubordTableLookupStrategyFactoryVDW subordTableFactory, AgentInstanceContext agentInstanceContext) {
        Pair<IndexMultiKey, VirtualDWEventTable> tableVW = VirtualDWQueryPlanUtil.getSubordinateQueryDesc(false, subordTableFactory.getIndexHashedProps(), subordTableFactory.getIndexBtreeProps());
        VirtualDWEventTable noopTable = tableVW.getSecond();
        for (int i = 0; i < noopTable.getBtreeAccess().size(); i++) {
            String opRange = subordTableFactory.getRangeEvals()[i].getType().getStringOp();
            VirtualDataWindowLookupOp op = VirtualDataWindowLookupOp.fromOpString(opRange);
            noopTable.getBtreeAccess().get(i).setOperator(op);
        }

        // allocate a number within the statement
        if (lastAccessedByStatementName == null || !lastAccessedByDeploymentId.equals(agentInstanceContext.getDeploymentId()) || !lastAccessedByStatementName.equals(agentInstanceContext.getStatementName())) {
            lastAccessedByNum = 0;
        }
        lastAccessedByNum++;

        VirtualDataWindowLookupContextSPI context = new VirtualDataWindowLookupContextSPI(agentInstanceContext.getDeploymentId(), agentInstanceContext.getStatementName(),
                agentInstanceContext.getStatementId(), agentInstanceContext.getAnnotations(), false, factory.getNamedWindowName(), noopTable.getHashAccess(), noopTable.getBtreeAccess(), lastAccessedByNum);
        VirtualDataWindowLookup index;
        try {
            index = dataExternal.getLookup(context);
        } catch (Throwable t) {
            throw new EPException("Failed to obtain lookup for virtual data window '" + factory.getNamedWindowName() + "': " + t.getMessage(), t);
        }
        return new SubordTableLookupStrategyVDW(factory, subordTableFactory, index);
    }

    public JoinExecTableLookupStrategy getJoinLookupStrategy(TableLookupPlan tableLookupPlan, AgentInstanceContext agentInstanceContext, EventTable[] eventTables, int lookupStream) {
        VirtualDWEventTable noopTable = (VirtualDWEventTable) eventTables[0];
        for (int i = 0; i < noopTable.getHashAccess().size(); i++) {
            Class hashKeyType = tableLookupPlan.getVirtualDWHashTypes()[i];
            noopTable.getHashAccess().get(i).setLookupValueType(hashKeyType);
        }
        for (int i = 0; i < noopTable.getBtreeAccess().size(); i++) {
            QueryGraphValueEntryRange range = tableLookupPlan.getVirtualDWRangeEvals()[i];
            VirtualDataWindowLookupOp op = VirtualDataWindowLookupOp.fromOpString(range.getType().getStringOp());
            VirtualDataWindowLookupFieldDesc rangeField = noopTable.getBtreeAccess().get(i);
            rangeField.setOperator(op);
            rangeField.setLookupValueType(tableLookupPlan.getVirtualDWRangeTypes()[i]);
        }

        VirtualDataWindowLookup index = dataExternal.getLookup(new VirtualDataWindowLookupContext(agentInstanceContext.getDeploymentId(), agentInstanceContext.getStatementName(),
                agentInstanceContext.getStatementId(), agentInstanceContext.getAnnotations(),
                false, factory.getNamedWindowName(), noopTable.getHashAccess(), noopTable.getBtreeAccess()));
        checkIndex(index);
        return new JoinExecTableLookupStrategyVirtualDW(factory.getNamedWindowName(), index, tableLookupPlan);
    }

    private void checkIndex(VirtualDataWindowLookup index) {
        if (index == null) {
            throw new EPException("Exception obtaining index lookup from virtual data window, the implementation has returned a null index");
        }
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

        String namedWindowName = factory.getNamedWindowName();
        VirtualDataWindowLookup index = dataExternal.getLookup(new VirtualDataWindowLookupContext(null, null, -1, annotations,
                true, namedWindowName, noopTable.getHashAccess(), noopTable.getBtreeAccess()));
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

    public void handleStartIndex(String indexName, QueryPlanIndexItem explicitIndexDesc) {
        try {
            List<VirtualDataWindowEventStartIndex.VDWCreateIndexField> fields = new ArrayList<VirtualDataWindowEventStartIndex.VDWCreateIndexField>();
            for (String hash : explicitIndexDesc.getHashProps()) {
                fields.add(new VirtualDataWindowEventStartIndex.VDWCreateIndexField(hash, "hash"));
            }
            for (String range : explicitIndexDesc.getRangeProps()) {
                fields.add(new VirtualDataWindowEventStartIndex.VDWCreateIndexField(range, "btree"));
            }
            VirtualDataWindowEventStartIndex create = new VirtualDataWindowEventStartIndex(factory.getNamedWindowName(), indexName, fields, explicitIndexDesc.isUnique());
            dataExternal.handleEvent(create);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle start-index event for window '" + factory.getNamedWindowName() + "': " + ex.getMessage();
            log.warn(message, ex);
            throw new EPException(message, ex);
        }
    }

    public void handleStopIndex(String indexName, QueryPlanIndexItem explicitIndexDesc) {
        try {
            VirtualDataWindowEventStopIndex theEvent = new VirtualDataWindowEventStopIndex(factory.getNamedWindowName(), indexName);
            dataExternal.handleEvent(theEvent);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle stop-index event for window '" + factory.getNamedWindowName() + "': " + ex.getMessage();
            log.warn(message, ex);
        }
    }

    public void handleDestroy(int agentInstanceId) {
        try {
            VirtualDataWindowEventStopWindow theEvent = new VirtualDataWindowEventStopWindow(factory.getNamedWindowName(), agentInstanceId);
            dataExternal.handleEvent(theEvent);
        } catch (Exception ex) {
            String message = "Exception encountered invoking virtual data window handle stop-window event for window '" + factory.getNamedWindowName() + "': " + ex.getMessage();
            log.warn(message, ex);
        }
    }
}
