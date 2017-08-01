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
package com.espertech.esper.epl.table.onaction;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.metric.StatementMetricHandle;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.merge.TableOnMergeHelper;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelperFactory;
import com.espertech.esper.event.EventBeanReader;
import com.espertech.esper.event.EventTypeSPI;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class TableOnViewFactoryFactory {
    public static TableOnViewFactory make(TableMetadata tableMetadata,
                                          OnTriggerDesc onTriggerDesc,
                                          EventType filterEventType,
                                          String filterStreamName,
                                          StatementContext statementContext,
                                          StatementMetricHandle metricsHandle,
                                          boolean isDistinct,
                                          InternalEventRouter internalEventRouter
    )
            throws ExprValidationException {
        if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_DELETE) {
            return new TableOnDeleteViewFactory(statementContext.getStatementResultService(), tableMetadata);
        } else if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_SELECT) {
            EventBeanReader eventBeanReader = null;
            if (isDistinct) {
                eventBeanReader = tableMetadata.getInternalEventType().getReader();
            }
            OnTriggerWindowDesc windowDesc = (OnTriggerWindowDesc) onTriggerDesc;
            return new TableOnSelectViewFactory(tableMetadata, internalEventRouter, statementContext.getEpStatementHandle(),
                    eventBeanReader, isDistinct, statementContext.getStatementResultService(), statementContext.getInternalEventEngineRouteDest(), windowDesc.isDeleteAndSelect());
        } else if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_UPDATE) {
            OnTriggerWindowUpdateDesc updateDesc = (OnTriggerWindowUpdateDesc) onTriggerDesc;
            EventBeanUpdateHelper updateHelper = EventBeanUpdateHelperFactory.make(tableMetadata.getTableName(), (EventTypeSPI) tableMetadata.getInternalEventType(), updateDesc.getAssignments(), updateDesc.getOptionalAsName(), filterEventType, false, statementContext.getStatementName(), statementContext.getEngineURI(), statementContext.getEventAdapterService(), false);
            TableUpdateStrategy updateStrategy = statementContext.getTableService().getTableUpdateStrategy(tableMetadata, updateHelper, false);
            TableOnUpdateViewFactory onUpdateViewFactory = new TableOnUpdateViewFactory(statementContext.getStatementResultService(), tableMetadata, updateHelper, updateStrategy);
            statementContext.getTableService().addTableUpdateStrategyReceiver(tableMetadata, statementContext.getStatementName(), onUpdateViewFactory, updateHelper, false);
            return onUpdateViewFactory;
        } else if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_MERGE) {
            OnTriggerMergeDesc onMergeTriggerDesc = (OnTriggerMergeDesc) onTriggerDesc;
            TableOnMergeHelper onMergeHelper = new TableOnMergeHelper(statementContext, onMergeTriggerDesc, filterEventType, filterStreamName, internalEventRouter, tableMetadata);
            return new TableOnMergeViewFactory(tableMetadata, onMergeHelper, statementContext.getStatementResultService(), metricsHandle, statementContext.getMetricReportingService());
        } else {
            throw new IllegalStateException("Unknown trigger type " + onTriggerDesc.getOnTriggerType());
        }
    }
}
