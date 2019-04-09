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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;

public class InfraOnSelectUtil {
    public static EventBean[] handleDistintAndInsert(EventBean[] newData, InfraOnSelectViewFactory parent, AgentInstanceContext agentInstanceContext, TableInstance tableInstanceInsertInto, boolean audit) {
        if (parent.isDistinct()) {
            newData = EventBeanUtility.getDistinctByProp(newData, parent.getDistinctKeyGetter());
        }

        if (tableInstanceInsertInto != null) {
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    tableInstanceInsertInto.addEventUnadorned(aNewData);
                }
            }
        } else if (parent.isInsertInto()) {
            if (newData != null) {
                for (EventBean aNewData : newData) {
                    if (audit) {
                        agentInstanceContext.getAuditProvider().insert(aNewData, agentInstanceContext);
                    }
                    agentInstanceContext.getInternalEventRouter().route(aNewData, agentInstanceContext, parent.isAddToFront());
                }
            }
        }

        return newData;
    }
}
