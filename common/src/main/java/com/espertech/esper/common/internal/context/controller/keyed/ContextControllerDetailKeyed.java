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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptor;
import com.espertech.esper.common.internal.context.controller.condition.ContextConditionDescriptorFilter;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerDetail;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

import java.util.ArrayList;
import java.util.List;

public class ContextControllerDetailKeyed implements ContextControllerDetail, StatementReadyCallback {

    private ContextControllerDetailKeyedItem[] items;
    private ContextConditionDescriptorFilter[] optionalInit;
    private ContextConditionDescriptor optionalTermination;

    private List<FilterSpecActivatable> filterSpecActivatables;
    private boolean hasAsName;
    private MultiKeyFromObjectArray multiKeyFromObjectArray;

    public void setItems(ContextControllerDetailKeyedItem[] items) {
        this.items = items;
    }

    public ContextControllerDetailKeyedItem[] getItems() {
        return items;
    }

    public ContextConditionDescriptorFilter[] getOptionalInit() {
        return optionalInit;
    }

    public void setOptionalInit(ContextConditionDescriptorFilter[] optionalInit) {
        this.optionalInit = optionalInit;
    }

    public ContextConditionDescriptor getOptionalTermination() {
        return optionalTermination;
    }

    public void setOptionalTermination(ContextConditionDescriptor optionalTermination) {
        this.optionalTermination = optionalTermination;
    }

    public MultiKeyFromObjectArray getMultiKeyFromObjectArray() {
        return multiKeyFromObjectArray;
    }

    public void setMultiKeyFromObjectArray(MultiKeyFromObjectArray multiKeyFromObjectArray) {
        this.multiKeyFromObjectArray = multiKeyFromObjectArray;
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        filterSpecActivatables = new ArrayList<>();
        for (ContextControllerDetailKeyedItem item : items) {
            filterSpecActivatables.add(item.getFilterSpecActivatable());
        }
        if (optionalTermination != null) {
            optionalTermination.addFilterSpecActivatable(filterSpecActivatables);
        }

        // determine whether we have named-partitioning-events
        for (ContextControllerDetailKeyedItem item : items) {
            if (item.getAliasName() != null) {
                hasAsName = true;
            }
        }
        if (!hasAsName && optionalInit != null) {
            for (ContextConditionDescriptorFilter filter : optionalInit) {
                if (filter.getOptionalFilterAsName() != null) {
                    hasAsName = true;
                }
            }
        }
    }

    public List<FilterSpecActivatable> getFilterSpecActivatables() {
        return filterSpecActivatables;
    }

    public boolean isHasAsName() {
        return hasAsName;
    }
}
