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
package com.espertech.esper.common.internal.epl.output.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.AuditEnum;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactoryFactory;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactoryForge;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewConditionForge;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewDirectDistinctOrAfterFactoryForge;
import com.espertech.esper.common.internal.epl.output.view.OutputProcessViewDirectForge;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorType;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for factories for output processing views.
 */
public class OutputProcessViewForgeFactory {

    public static OutputProcessViewFactoryForgeDesc make(EventType[] typesPerStream, EventType resultEventType, ResultSetProcessorType resultSetProcessorType, StatementSpecCompiled statementSpec, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        InsertIntoDesc insertIntoDesc = statementSpec.getRaw().getInsertIntoDesc();
        SelectClauseStreamSelectorEnum selectStreamSelector = statementSpec.getRaw().getSelectStreamSelectorEnum();
        OutputLimitSpec outputLimitSpec = statementSpec.getRaw().getOutputLimitSpec();
        int streamCount = statementSpec.getStreamSpecs().length;
        boolean isDistinct = statementSpec.getRaw().getSelectClauseSpec().isDistinct();
        boolean isGrouped = statementSpec.getGroupByExpressions() != null && statementSpec.getGroupByExpressions().getGroupByNodes().length > 0;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(1);

        // determine routing
        boolean isRouted = false;
        boolean routeToFront = false;
        if (insertIntoDesc != null) {
            isRouted = true;
            routeToFront = services.getNamedWindowCompileTimeResolver().resolve(insertIntoDesc.getEventTypeName()) != null;
        }

        OutputStrategyPostProcessForge outputStrategyPostProcessForge = null;
        if ((insertIntoDesc != null) || (selectStreamSelector == SelectClauseStreamSelectorEnum.RSTREAM_ONLY)) {
            SelectClauseStreamSelectorEnum insertIntoStreamSelector = null;
            TableMetaData table = null;

            if (insertIntoDesc != null) {
                insertIntoStreamSelector = insertIntoDesc.getStreamSelector();
                table = services.getTableCompileTimeResolver().resolve(statementSpec.getRaw().getInsertIntoDesc().getEventTypeName());
                if (table != null) {
                    EPLValidationUtil.validateContextName(true, table.getTableName(), table.getOptionalContextName(), statementSpec.getRaw().getOptionalContextName(), true);
                }
            }

            boolean audit = AuditEnum.INSERT.getAudit(statementSpec.getAnnotations()) != null;
            outputStrategyPostProcessForge = new OutputStrategyPostProcessForge(isRouted, insertIntoStreamSelector, selectStreamSelector, routeToFront, table, audit);
        }

        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKeyDistinct(isDistinct, resultEventType, statementRawInfo, SerdeCompileTimeResolverNonHA.INSTANCE);
        MultiKeyClassRef distinctMultiKey = multiKeyPlan.getClassRef();
        additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());

        OutputProcessViewFactoryForge outputProcessViewFactoryForge;
        if (outputLimitSpec == null) {
            if (!isDistinct) {
                if (outputStrategyPostProcessForge == null || !outputStrategyPostProcessForge.hasTable()) {
                    // without table we have a shortcut implementation
                    outputProcessViewFactoryForge = new OutputProcessViewDirectSimpleForge(outputStrategyPostProcessForge);
                } else {
                    outputProcessViewFactoryForge = new OutputProcessViewDirectForge(outputStrategyPostProcessForge);
                }
            } else {
                outputProcessViewFactoryForge = new OutputProcessViewDirectDistinctOrAfterFactoryForge(outputStrategyPostProcessForge, isDistinct, distinctMultiKey, null, null, resultEventType);
            }
        } else if (outputLimitSpec.getRateType() == OutputLimitRateType.AFTER) {
            outputProcessViewFactoryForge = new OutputProcessViewDirectDistinctOrAfterFactoryForge(outputStrategyPostProcessForge, isDistinct, distinctMultiKey, outputLimitSpec.getAfterTimePeriodExpr(), outputLimitSpec.getAfterNumberOfEvents(), resultEventType);
        } else {
            try {
                boolean isWithHavingClause = statementSpec.getRaw().getHavingClause() != null;
                boolean isStartConditionOnCreation = hasOnlyTables(statementSpec.getStreamSpecs());
                OutputConditionFactoryForge outputConditionFactoryForge = OutputConditionFactoryFactory.createCondition(outputLimitSpec, isGrouped, isWithHavingClause, isStartConditionOnCreation, statementRawInfo, services);
                boolean hasOrderBy = statementSpec.getRaw().getOrderByList() != null && statementSpec.getRaw().getOrderByList().size() > 0;
                boolean hasAfter = outputLimitSpec.getAfterNumberOfEvents() != null || outputLimitSpec.getAfterTimePeriodExpr() != null;

                // hint checking with order-by
                boolean hasOptHint = ResultSetProcessorOutputConditionType.getOutputLimitOpt(statementSpec.getAnnotations(), services.getConfiguration(), hasOrderBy);
                ResultSetProcessorOutputConditionType conditionType = ResultSetProcessorOutputConditionType.getConditionType(outputLimitSpec.getDisplayLimit(), resultSetProcessorType.isAggregated(), hasOrderBy, hasOptHint, resultSetProcessorType.isGrouped());

                // plan serdes
                for (EventType eventType : typesPerStream) {
                    List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(eventType, statementRawInfo, services.getSerdeEventTypeRegistry(), services.getSerdeResolver());
                    additionalForgeables.addAll(serdeForgeables);
                }

                boolean terminable = outputLimitSpec.getRateType() == OutputLimitRateType.TERM || outputLimitSpec.isAndAfterTerminate();
                outputProcessViewFactoryForge = new OutputProcessViewConditionForge(outputStrategyPostProcessForge, isDistinct, distinctMultiKey, outputLimitSpec.getAfterTimePeriodExpr(), outputLimitSpec.getAfterNumberOfEvents(), outputConditionFactoryForge, streamCount, conditionType, terminable, hasAfter, resultSetProcessorType.isUnaggregatedUngrouped(), selectStreamSelector, typesPerStream, resultEventType);
            } catch (Exception ex) {
                throw new ExprValidationException("Error in the output rate limiting clause: " + ex.getMessage(), ex);
            }
        }

        return new OutputProcessViewFactoryForgeDesc(outputProcessViewFactoryForge, additionalForgeables);
    }

    public static OutputProcessViewFactoryForge make() {
        return null;
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
