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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage1.spec.NamedWindowConsumerStreamSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecOptions;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.epl.util.EPLValidationUtil;
import com.espertech.esper.common.internal.statement.helper.EPStatementStartMethodHelperValidate;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import com.espertech.esper.common.internal.view.core.*;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SubSelectHelperFilters {
    public static List<StmtClassForgeableFactory> handleSubselectSelectClauses(ExprSubselectNode subselect, EventType outerEventType, String outerEventTypeName, String outerStreamName,
                                                                               LinkedHashMap<String, Pair<EventType, String>> taggedEventTypes, LinkedHashMap<String, Pair<EventType, String>> arrayEventTypes,
                                                                               StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {

        if (subselect.getSubselectNumber() == -1) {
            throw new IllegalStateException("Subselect is unassigned");
        }

        StatementSpecCompiled statementSpec = subselect.getStatementSpecCompiled();
        StreamSpecCompiled filterStreamSpec = statementSpec.getStreamSpecs()[0];

        List<ViewFactoryForge> viewForges;
        String subselecteventTypeName;
        List<StmtClassForgeableFactory> additionalForgeables;

        // construct view factory chain
        EventType eventType;
        try {
            ViewFactoryForgeArgs args = new ViewFactoryForgeArgs(-1, true, subselect.getSubselectNumber(), StreamSpecOptions.DEFAULT, null, statementRawInfo, services);

            if (statementSpec.getStreamSpecs()[0] instanceof FilterStreamSpecCompiled) {
                FilterStreamSpecCompiled filterStreamSpecCompiled = (FilterStreamSpecCompiled) statementSpec.getStreamSpecs()[0];
                subselecteventTypeName = filterStreamSpecCompiled.getFilterSpecCompiled().getFilterForEventTypeName();

                // A child view is required to limit the stream
                if (filterStreamSpec.getViewSpecs().length == 0) {
                    throw new ExprValidationException("Subqueries require one or more views to limit the stream, consider declaring a length or time window");
                }

                ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(filterStreamSpecCompiled.getViewSpecs(), args, filterStreamSpecCompiled.getFilterSpecCompiled().getResultEventType());
                viewForges = viewForgeDesc.getForges();
                additionalForgeables = viewForgeDesc.getMultikeyForges();
                // Register filter, create view factories
                eventType = viewForges.isEmpty() ? filterStreamSpecCompiled.getFilterSpecCompiled().getResultEventType() : viewForges.get(viewForges.size() - 1).getEventType();
                subselect.setRawEventType(eventType);
            } else {
                NamedWindowConsumerStreamSpec namedSpec = (NamedWindowConsumerStreamSpec) statementSpec.getStreamSpecs()[0];
                NamedWindowMetaData namedWindow = namedSpec.getNamedWindow();
                ViewFactoryForgeDesc viewForgeDesc = ViewFactoryForgeUtil.createForges(namedSpec.getViewSpecs(), args, namedWindow.getEventType());
                viewForges = viewForgeDesc.getForges();
                additionalForgeables = viewForgeDesc.getMultikeyForges();
                String namedWindowName = namedWindow.getEventType().getName();
                subselecteventTypeName = namedWindowName;
                EPLValidationUtil.validateContextName(false, namedWindowName, namedWindow.getContextName(), statementRawInfo.getContextName(), true);
                subselect.setRawEventType(namedWindow.getEventType());
                eventType = namedWindow.getEventType();
            }
        } catch (ViewProcessingException ex) {
            throw new ExprValidationException("Error validating subexpression: " + ex.getMessage(), ex);
        }

        // determine a stream name unless one was supplied
        String subexpressionStreamName = SubselectUtil.getStreamName(filterStreamSpec.getOptionalStreamName(), subselect.getSubselectNumber());

        // Named windows don't allow data views
        if (filterStreamSpec instanceof NamedWindowConsumerStreamSpec) {
            EPStatementStartMethodHelperValidate.validateNoDataWindowOnNamedWindow(viewForges);
        }

        // Streams event types are the original stream types with the stream zero the subselect stream
        LinkedHashMap<String, Pair<EventType, String>> namesAndTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        namesAndTypes.put(subexpressionStreamName, new Pair<EventType, String>(eventType, subselecteventTypeName));
        namesAndTypes.put(outerStreamName, new Pair<EventType, String>(outerEventType, outerEventTypeName));
        if (taggedEventTypes != null) {
            for (Map.Entry<String, Pair<EventType, String>> entry : taggedEventTypes.entrySet()) {
                namesAndTypes.put(entry.getKey(), new Pair<EventType, String>(entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
        }
        if (arrayEventTypes != null) {
            for (Map.Entry<String, Pair<EventType, String>> entry : arrayEventTypes.entrySet()) {
                namesAndTypes.put(entry.getKey(), new Pair<EventType, String>(entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
        }
        StreamTypeService subselectTypeService = new StreamTypeServiceImpl(namesAndTypes, true, true);
        ViewResourceDelegateExpr viewResourceDelegateSubselect = new ViewResourceDelegateExpr();
        subselect.setFilterSubqueryStreamTypes(subselectTypeService);

        // Validate select expression
        SelectClauseSpecCompiled selectClauseSpec = subselect.getStatementSpecCompiled().getSelectClauseCompiled();
        if (selectClauseSpec.getSelectExprList().length > 0) {
            if (selectClauseSpec.getSelectExprList().length > 1) {
                throw new ExprValidationException("Subquery multi-column select is not allowed in this context.");
            }

            SelectClauseElementCompiled element = selectClauseSpec.getSelectExprList()[0];
            if (element instanceof SelectClauseExprCompiledSpec) {
                // validate
                SelectClauseExprCompiledSpec compiled = (SelectClauseExprCompiledSpec) element;
                ExprNode selectExpression = compiled.getSelectExpression();
                ExprValidationContext validationContext = new ExprValidationContextBuilder(subselectTypeService, statementRawInfo, services)
                        .withViewResourceDelegate(viewResourceDelegateSubselect).withAllowBindingConsumption(true)
                        .withMemberName(new ExprValidationMemberNameQualifiedSubquery(subselect.getSubselectNumber())).build();
                selectExpression = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.SUBQUERYSELECT, selectExpression, validationContext);
                subselect.setSelectClause(new ExprNode[]{selectExpression});
                subselect.setSelectAsNames(new String[]{compiled.getAssignedName()});

                // handle aggregation
                List<ExprAggregateNode> aggExprNodes = new LinkedList<ExprAggregateNode>();
                ExprAggregateNodeUtil.getAggregatesBottomUp(selectExpression, aggExprNodes);
                if (aggExprNodes.size() > 0) {
                    // Other stream properties, if there is aggregation, cannot be under aggregation.
                    for (ExprAggregateNode aggNode : aggExprNodes) {
                        List<Pair<Integer, String>> propertiesNodesAggregated = ExprNodeUtilityQuery.getExpressionProperties(aggNode, true);
                        for (Pair<Integer, String> pair : propertiesNodesAggregated) {
                            if (pair.getFirst() != 0) {
                                throw new ExprValidationException("Subselect aggregation function cannot aggregate across correlated properties");
                            }
                        }
                    }

                    // This stream (stream 0) properties must either all be under aggregation, or all not be.
                    List<Pair<Integer, String>> propertiesNotAggregated = ExprNodeUtilityQuery.getExpressionProperties(selectExpression, false);
                    for (Pair<Integer, String> pair : propertiesNotAggregated) {
                        if (pair.getFirst() == 0) {
                            throw new ExprValidationException("Subselect properties must all be within aggregation functions");
                        }
                    }
                }
            }
        }

        return additionalForgeables;
    }
}
