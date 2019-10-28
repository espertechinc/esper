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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.TableQueryStreamSpec;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.StatementBaseInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.activator.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityMake;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeArgs;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeDesc;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil;

import java.util.*;

public class SubSelectHelperActivations {

    public static SubSelectActivationDesc createSubSelectActivation(List<FilterSpecCompiled> filterSpecCompileds, List<NamedWindowConsumerStreamSpec> namedWindowConsumers, StatementBaseInfo statement, StatementCompileTimeServices services)
            throws ExprValidationException {
        Map<ExprSubselectNode, SubSelectActivationPlan> result = new LinkedHashMap<>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>();

        // Process all subselect expression nodes
        for (ExprSubselectNode subselect : statement.getStatementSpec().getSubselectNodes()) {
            StatementSpecCompiled statementSpec = subselect.getStatementSpecCompiled();
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[0];
            int subqueryNumber = subselect.getSubselectNumber();
            if (subqueryNumber == -1) {
                throw new IllegalStateException("Unexpected subquery");
            }
            ViewFactoryForgeArgs args = new ViewFactoryForgeArgs(-1, true, subqueryNumber, streamSpec.getOptions(), null, statement.getStatementRawInfo(), services);

            if (streamSpec instanceof FilterStreamSpecCompiled) {
                if (services.isFireAndForget()) {
                    throw new ExprValidationException("Fire-and-forget queries only allow subqueries against named windows and tables");
                }
                FilterStreamSpecCompiled filterStreamSpec = (FilterStreamSpecCompiled) statementSpec.getStreamSpecs()[0];

                // Register filter, create view factories
                ViewableActivatorForge activatorDeactivator = new ViewableActivatorFilterForge(filterStreamSpec.getFilterSpecCompiled(), false, null, true, subqueryNumber);
                ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, filterStreamSpec.getFilterSpecCompiled().getResultEventType());
                List<ViewFactoryForge> forges = viewForgeDesc.getForges();
                additionalForgeables.addAll(viewForgeDesc.getMultikeyForges());
                EventType eventType = forges.isEmpty() ? filterStreamSpec.getFilterSpecCompiled().getResultEventType() : forges.get(forges.size() - 1).getEventType();
                subselect.setRawEventType(eventType);
                filterSpecCompileds.add(filterStreamSpec.getFilterSpecCompiled());

                // Add lookup to list, for later starts
                result.put(subselect, new SubSelectActivationPlan(filterStreamSpec.getFilterSpecCompiled().getResultEventType(), forges, activatorDeactivator, streamSpec));
            } else if (streamSpec instanceof TableQueryStreamSpec) {
                TableQueryStreamSpec table = (TableQueryStreamSpec) streamSpec;
                ExprNode filter = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(table.getFilterExpressions());
                ViewableActivatorForge viewableActivator = new ViewableActivatorTableForge(table.getTable(), filter);
                result.put(subselect, new SubSelectActivationPlan(table.getTable().getInternalEventType(), Collections.emptyList(), viewableActivator, streamSpec));
                subselect.setRawEventType(table.getTable().getInternalEventType());
            } else {
                NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) statementSpec.getStreamSpecs()[0];
                namedWindowConsumers.add(namedSpec);
                NamedWindowMetaData nwinfo = namedSpec.getNamedWindow();

                EventType namedWindowType = nwinfo.getEventType();
                if (namedSpec.getOptPropertyEvaluator() != null) {
                    namedWindowType = namedSpec.getOptPropertyEvaluator().getFragmentEventType();
                }

                // if named-window index sharing is disabled (the default) or filter expressions are provided then consume the insert-remove stream
                boolean disableIndexShare = HintEnum.DISABLE_WINDOW_SUBQUERY_INDEXSHARE.getHint(statement.getStatementRawInfo().getAnnotations()) != null;
                boolean processorDisableIndexShare = !namedSpec.getNamedWindow().isEnableIndexShare();
                if (disableIndexShare && namedSpec.getNamedWindow().isVirtualDataWindow()) {
                    disableIndexShare = false;
                }

                if ((!namedSpec.getFilterExpressions().isEmpty() || processorDisableIndexShare || disableIndexShare) && (!services.isFireAndForget())) {
                    ExprNode filterEvaluator = null;
                    if (!namedSpec.getFilterExpressions().isEmpty()) {
                        filterEvaluator = ExprNodeUtilityMake.connectExpressionsByLogicalAndWhenNeeded(namedSpec.getFilterExpressions());
                    }
                    ViewableActivatorForge activatorNamedWindow = new ViewableActivatorNamedWindowForge(namedSpec, nwinfo, filterEvaluator, null, true, namedSpec.getOptPropertyEvaluator());
                    ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, namedWindowType);
                    List<ViewFactoryForge> forges = viewForgeDesc.getForges();
                    additionalForgeables.addAll(viewForgeDesc.getMultikeyForges());
                    subselect.setRawEventType(forges.isEmpty() ? namedWindowType : forges.get(forges.size() - 1).getEventType());
                    result.put(subselect, new SubSelectActivationPlan(namedWindowType, forges, activatorNamedWindow, streamSpec));
                } else {
                    // else if there are no named window stream filter expressions and index sharing is enabled
                    ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(streamSpec.getViewSpecs(), args, namedWindowType);
                    List<ViewFactoryForge> forges = viewForgeDesc.getForges();
                    additionalForgeables.addAll(viewForgeDesc.getMultikeyForges());
                    subselect.setRawEventType(namedWindowType);
                    ViewableActivatorForge activatorNamedWindow = new ViewableActivatorSubselectNoneForge(namedWindowType);
                    result.put(subselect, new SubSelectActivationPlan(namedWindowType, forges, activatorNamedWindow, streamSpec));
                }
            }
        }

        return new SubSelectActivationDesc(result, additionalForgeables);
    }
}
