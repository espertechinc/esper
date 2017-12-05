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
package com.espertech.esper.epl.view;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.service.InternalEventRouter;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.core.service.StatementVariableRef;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorHelperFactory;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessorType;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.util.EPLValidationUtil;

/**
 * Factory for factories for output processing views.
 */
public class OutputProcessViewFactoryFactory {
    public static OutputProcessViewFactory make(StatementSpecCompiled statementSpec, InternalEventRouter internalEventRouter, StatementContext statementContext, EventType resultEventType, OutputProcessViewCallback optionalOutputProcessViewCallback, TableService tableService, ResultSetProcessorType resultSetProcessorType, ResultSetProcessorHelperFactory resultSetProcessorHelperFactory, StatementVariableRef statementVariableRef)
            throws ExprValidationException {
        // determine direct-callback
        if (optionalOutputProcessViewCallback != null) {
            return new OutputProcessViewFactoryCallback(optionalOutputProcessViewCallback);
        }

        // determine routing
        boolean isRouted = false;
        boolean routeToFront = false;
        if (statementSpec.getInsertIntoDesc() != null) {
            isRouted = true;
            routeToFront = statementContext.getNamedWindowMgmtService().isNamedWindow(statementSpec.getInsertIntoDesc().getEventTypeName());
        }

        OutputStrategyPostProcessFactory outputStrategyPostProcessFactory = null;
        if ((statementSpec.getInsertIntoDesc() != null) || (statementSpec.getSelectStreamSelectorEnum() == SelectClauseStreamSelectorEnum.RSTREAM_ONLY)) {
            SelectClauseStreamSelectorEnum insertIntoStreamSelector = null;
            String tableName = null;

            if (statementSpec.getInsertIntoDesc() != null) {
                insertIntoStreamSelector = statementSpec.getInsertIntoDesc().getStreamSelector();
                TableMetadata tableMetadata = tableService.getTableMetadata(statementSpec.getInsertIntoDesc().getEventTypeName());
                if (tableMetadata != null) {
                    tableName = tableMetadata.getTableName();
                    EPLValidationUtil.validateContextName(true, tableName, tableMetadata.getContextName(), statementSpec.getOptionalContextName(), true);
                    statementVariableRef.addReferences(statementContext.getStatementName(), tableMetadata.getTableName());
                }
            }

            outputStrategyPostProcessFactory = new OutputStrategyPostProcessFactory(isRouted, insertIntoStreamSelector, statementSpec.getSelectStreamSelectorEnum(), internalEventRouter, statementContext.getEpStatementHandle(), routeToFront, tableService, tableName);
        }

        // Do we need to enforce an output policy?
        int streamCount = statementSpec.getStreamSpecs().length;
        OutputLimitSpec outputLimitSpec = statementSpec.getOutputLimitSpec();
        boolean isDistinct = statementSpec.getSelectClauseSpec().isDistinct();
        boolean isGrouped = statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getGroupByNodes().length > 0;

        OutputProcessViewFactory outputProcessViewFactory;
        if (outputLimitSpec == null) {
            if (!isDistinct) {
                outputProcessViewFactory = new OutputProcessViewDirectFactory(statementContext, outputStrategyPostProcessFactory, resultSetProcessorHelperFactory);
            } else {
                outputProcessViewFactory = new OutputProcessViewDirectDistinctOrAfterFactory(statementContext, outputStrategyPostProcessFactory, resultSetProcessorHelperFactory, isDistinct, null, null, resultEventType);
            }
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.AFTER) {
            outputProcessViewFactory = new OutputProcessViewDirectDistinctOrAfterFactory(statementContext, outputStrategyPostProcessFactory, resultSetProcessorHelperFactory, isDistinct, outputLimitSpec.getAfterTimePeriodExpr(), outputLimitSpec.getAfterNumberOfEvents(), resultEventType);
        } else {
            try {
                boolean isWithHavingClause = statementSpec.getHavingExprRootNode() != null;
                boolean isStartConditionOnCreation = hasOnlyTables(statementSpec.getStreamSpecs());
                OutputConditionFactory outputConditionFactory = OutputConditionFactoryFactory.createCondition(outputLimitSpec, statementContext, isGrouped, isWithHavingClause, isStartConditionOnCreation, resultSetProcessorHelperFactory);
                boolean hasOrderBy = statementSpec.getOrderByList() != null && statementSpec.getOrderByList().length > 0;
                boolean hasAfter = outputLimitSpec.getAfterNumberOfEvents() != null || outputLimitSpec.getAfterTimePeriodExpr() != null;

                // hint checking with order-by
                boolean hasOptHint = ResultSetProcessorOutputConditionType.getOutputLimitOpt(statementSpec.getAnnotations(), statementContext.getConfigSnapshot(), hasOrderBy);
                ResultSetProcessorOutputConditionType conditionType = ResultSetProcessorOutputConditionType.getConditionType(outputLimitSpec.getDisplayLimit(), resultSetProcessorType.isAggregated(), hasOrderBy, hasOptHint, resultSetProcessorType.isGrouped());

                SelectClauseStreamSelectorEnum selectClauseStreamSelectorEnum = statementSpec.getSelectStreamSelectorEnum();
                boolean terminable = outputLimitSpec.getRateType() == OutputLimitRateType.TERM || outputLimitSpec.isAndAfterTerminate();
                outputProcessViewFactory = new OutputProcessViewConditionFactory(statementContext, outputStrategyPostProcessFactory, isDistinct, outputLimitSpec.getAfterTimePeriodExpr(), outputLimitSpec.getAfterNumberOfEvents(), resultEventType, outputConditionFactory, streamCount, conditionType, outputLimitSpec.getDisplayLimit(), terminable, hasAfter, resultSetProcessorType.isUnaggregatedUngrouped(), selectClauseStreamSelectorEnum, resultSetProcessorHelperFactory);
            } catch (Exception ex) {
                throw new ExprValidationException("Error in the output rate limiting clause: " + ex.getMessage(), ex);
            }
        }

        return outputProcessViewFactory;
    }

    private static boolean hasOnlyTables(StreamSpecCompiled[] streamSpecs) {
        if (streamSpecs.length == 0) {
            return false;
        }
        for (StreamSpecCompiled streamSpec : streamSpecs) {
            if (!(streamSpec instanceof TableQueryStreamSpec)) {
                return false;
            }
        }
        return true;
    }
}
