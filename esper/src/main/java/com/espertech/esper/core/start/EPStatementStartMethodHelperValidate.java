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
package com.espertech.esper.core.start;

import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.service.ExprEvaluatorContextStatement;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.core.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.epl.core.viewres.ViewResourceDelegateUnverified;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNode;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeUtil;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.named.NamedWindowMgmtService;
import com.espertech.esper.epl.spec.OnTriggerSetAssignment;
import com.espertech.esper.epl.spec.OuterJoinDesc;
import com.espertech.esper.epl.spec.OutputLimitRateType;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.epl.view.OutputConditionExpressionFactory;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.view.DataWindowViewFactory;
import com.espertech.esper.view.ViewFactory;
import com.espertech.esper.view.std.GroupByViewFactory;
import com.espertech.esper.view.std.MergeViewFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EPStatementStartMethodHelperValidate {
    private static final Logger log = LoggerFactory.getLogger(EPStatementStartMethodHelperValidate.class);

    public static void validateNoDataWindowOnNamedWindow(List<ViewFactory> viewFactories) throws ExprValidationException {
        for (ViewFactory viewFactory : viewFactories) {
            if ((viewFactory instanceof GroupByViewFactory) || ((viewFactory instanceof MergeViewFactory))) {
                continue;
            }
            if (viewFactory instanceof DataWindowViewFactory) {
                throw new ExprValidationException(NamedWindowMgmtService.ERROR_MSG_NO_DATAWINDOW_ALLOWED);
            }
        }
    }

    /**
     * Validate filter and join expression nodes.
     *
     * @param statementSpec        the compiled statement
     * @param statementContext     the statement services
     * @param typeService          the event types for streams
     * @param viewResourceDelegate the delegate to verify expressions that use view resources
     */
    protected static void validateNodes(StatementSpecCompiled statementSpec,
                                        StatementContext statementContext,
                                        StreamTypeService typeService,
                                        ViewResourceDelegateUnverified viewResourceDelegate) {
        EngineImportService engineImportService = statementContext.getEngineImportService();
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        String intoTableName = statementSpec.getIntoTableSpec() == null ? null : statementSpec.getIntoTableSpec().getName();

        if (statementSpec.getFilterRootNode() != null) {
            ExprNode optionalFilterNode = statementSpec.getFilterRootNode();

            // Validate where clause, initializing nodes to the stream ids used
            try {
                ExprValidationContext validationContext = new ExprValidationContext(typeService, engineImportService, statementContext.getStatementExtensionServicesContext(), viewResourceDelegate, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, intoTableName, false);
                optionalFilterNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.FILTER, optionalFilterNode, validationContext);
                if (optionalFilterNode.getForge().getEvaluationType() != boolean.class && optionalFilterNode.getForge().getEvaluationType() != Boolean.class) {
                    throw new ExprValidationException("The where-clause filter expression must return a boolean value");
                }
                statementSpec.setFilterExprRootNode(optionalFilterNode);

                // Make sure there is no aggregation in the where clause
                List<ExprAggregateNode> aggregateNodes = new LinkedList<ExprAggregateNode>();
                ExprAggregateNodeUtil.getAggregatesBottomUp(optionalFilterNode, aggregateNodes);
                if (!aggregateNodes.isEmpty()) {
                    throw new ExprValidationException("An aggregate function may not appear in a WHERE clause (use the HAVING clause)");
                }
            } catch (ExprValidationException ex) {
                log.debug(".validateNodes Validation exception for filter=" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(optionalFilterNode), ex);
                throw new EPStatementException("Error validating expression: " + ex.getMessage(), ex, statementContext.getExpression());
            }
        }

        if ((statementSpec.getOutputLimitSpec() != null) && ((statementSpec.getOutputLimitSpec().getWhenExpressionNode() != null) || (statementSpec.getOutputLimitSpec().getAndAfterTerminateExpr() != null))) {
            // Validate where clause, initializing nodes to the stream ids used
            try {
                EventType outputLimitType = OutputConditionExpressionFactory.getBuiltInEventType(statementContext.getEventAdapterService());
                StreamTypeService typeServiceOutputWhen = new StreamTypeServiceImpl(new EventType[]{outputLimitType}, new String[]{null}, new boolean[]{true}, statementContext.getEngineURI(), false, false);
                ExprValidationContext validationContext = new ExprValidationContext(typeServiceOutputWhen, engineImportService, statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, false, false, intoTableName, false);

                ExprNode outputLimitWhenNode = statementSpec.getOutputLimitSpec().getWhenExpressionNode();
                if (outputLimitWhenNode != null) {
                    outputLimitWhenNode = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitWhenNode, validationContext);
                    statementSpec.getOutputLimitSpec().setWhenExpressionNode(outputLimitWhenNode);

                    if (JavaClassHelper.getBoxedType(outputLimitWhenNode.getForge().getEvaluationType()) != Boolean.class) {
                        throw new ExprValidationException("The when-trigger expression in the OUTPUT WHEN clause must return a boolean-type value");
                    }
                    EPStatementStartMethodHelperValidate.validateNoAggregations(outputLimitWhenNode, "An aggregate function may not appear in a OUTPUT LIMIT clause");
                }

                // validate and-terminate expression if provided
                if (statementSpec.getOutputLimitSpec().getAndAfterTerminateExpr() != null) {
                    if (statementSpec.getOutputLimitSpec().getRateType() != OutputLimitRateType.WHEN_EXPRESSION && statementSpec.getOutputLimitSpec().getRateType() != OutputLimitRateType.TERM) {
                        throw new ExprValidationException("A terminated-and expression must be used with the OUTPUT WHEN clause");
                    }
                    ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, statementSpec.getOutputLimitSpec().getAndAfterTerminateExpr(), validationContext);
                    statementSpec.getOutputLimitSpec().setAndAfterTerminateExpr(validated);

                    if (JavaClassHelper.getBoxedType(validated.getForge().getEvaluationType()) != Boolean.class) {
                        throw new ExprValidationException("The terminated-and expression must return a boolean-type value");
                    }
                    EPStatementStartMethodHelperValidate.validateNoAggregations(validated, "An aggregate function may not appear in a terminated-and clause");
                }

                // validate then-expression
                validateThenSetAssignments(statementSpec.getOutputLimitSpec().getThenExpressions(), validationContext);

                // validate after-terminated then-expression
                validateThenSetAssignments(statementSpec.getOutputLimitSpec().getAndAfterTerminateThenExpressions(), validationContext);
            } catch (ExprValidationException ex) {
                throw new EPStatementException("Error validating expression: " + ex.getMessage(), statementContext.getExpression());
            }
        }

        for (int outerJoinCount = 0; outerJoinCount < statementSpec.getOuterJoinDescList().length; outerJoinCount++) {
            OuterJoinDesc outerJoinDesc = statementSpec.getOuterJoinDescList()[outerJoinCount];

            // validate on-expression nodes, if provided
            if (outerJoinDesc.getOptLeftNode() != null) {
                UniformPair<Integer> streamIdPair = validateOuterJoinPropertyPair(statementContext, outerJoinDesc.getOptLeftNode(), outerJoinDesc.getOptRightNode(), outerJoinCount,
                        typeService, viewResourceDelegate);

                if (outerJoinDesc.getAdditionalLeftNodes() != null) {
                    Set<Integer> streamSet = new HashSet<Integer>();
                    streamSet.add(streamIdPair.getFirst());
                    streamSet.add(streamIdPair.getSecond());
                    for (int i = 0; i < outerJoinDesc.getAdditionalLeftNodes().length; i++) {
                        UniformPair<Integer> streamIdPairAdd = validateOuterJoinPropertyPair(statementContext, outerJoinDesc.getAdditionalLeftNodes()[i], outerJoinDesc.getAdditionalRightNodes()[i], outerJoinCount,
                                typeService, viewResourceDelegate);

                        // make sure all additional properties point to the same two streams
                        if (!streamSet.contains(streamIdPairAdd.getFirst()) || (!streamSet.contains(streamIdPairAdd.getSecond()))) {
                            String message = "Outer join ON-clause columns must refer to properties of the same joined streams" +
                                    " when using multiple columns in the on-clause";
                            throw new EPStatementException("Error validating expression: " + message, statementContext.getExpression());
                        }

                    }
                }
            }
        }
    }

    private static void validateThenSetAssignments(List<OnTriggerSetAssignment> assignments, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        for (OnTriggerSetAssignment assign : assignments) {
            ExprNode node = ExprNodeUtilityRich.getValidatedAssignment(assign, validationContext);
            assign.setExpression(node);
            EPStatementStartMethodHelperValidate.validateNoAggregations(node, "An aggregate function may not appear in a OUTPUT LIMIT clause");
        }
    }

    protected static UniformPair<Integer> validateOuterJoinPropertyPair(
            StatementContext statementContext,
            ExprIdentNode leftNode,
            ExprIdentNode rightNode,
            int outerJoinCount,
            StreamTypeService typeService,
            ViewResourceDelegateUnverified viewResourceDelegate) {
        // Validate the outer join clause using an artificial equals-node on top.
        // Thus types are checked via equals.
        // Sets stream ids used for validated nodes.
        ExprNode equalsNode = new ExprEqualsNodeImpl(false, false);
        equalsNode.addChildNode(leftNode);
        equalsNode.addChildNode(rightNode);
        ExprEvaluatorContextStatement evaluatorContextStmt = new ExprEvaluatorContextStatement(statementContext, false);
        try {
            ExprValidationContext validationContext = new ExprValidationContext(typeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), viewResourceDelegate, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), evaluatorContextStmt, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, true, false, null, false);
            ExprNodeUtilityRich.getValidatedSubtree(ExprNodeOrigin.JOINON, equalsNode, validationContext);
        } catch (ExprValidationException ex) {
            log.debug("Validation exception for outer join node=" + ExprNodeUtilityCore.toExpressionStringMinPrecedenceSafe(equalsNode), ex);
            throw new EPStatementException("Error validating expression: " + ex.getMessage(), statementContext.getExpression());
        }

        // Make sure we have left-hand-side and right-hand-side refering to different streams
        int streamIdLeft = leftNode.getStreamId();
        int streamIdRight = rightNode.getStreamId();
        if (streamIdLeft == streamIdRight) {
            String message = "Outer join ON-clause cannot refer to properties of the same stream";
            throw new EPStatementException("Error validating expression: " + message, statementContext.getExpression());
        }

        // Make sure one of the properties refers to the acutual stream currently being joined
        int expectedStreamJoined = outerJoinCount + 1;
        if ((streamIdLeft != expectedStreamJoined) && (streamIdRight != expectedStreamJoined)) {
            String message = "Outer join ON-clause must refer to at least one property of the joined stream" +
                    " for stream " + expectedStreamJoined;
            throw new EPStatementException("Error validating expression: " + message, statementContext.getExpression());
        }

        // Make sure neither of the streams refer to a 'future' stream
        String badPropertyName = null;
        if (streamIdLeft > outerJoinCount + 1) {
            badPropertyName = leftNode.getResolvedPropertyName();
        }
        if (streamIdRight > outerJoinCount + 1) {
            badPropertyName = rightNode.getResolvedPropertyName();
        }
        if (badPropertyName != null) {
            String message = "Outer join ON-clause invalid scope for property" +
                    " '" + badPropertyName + "', expecting the current or a prior stream scope";
            throw new EPStatementException("Error validating expression: " + message, statementContext.getExpression());
        }

        return new UniformPair<Integer>(streamIdLeft, streamIdRight);
    }

    protected static ExprNode validateExprNoAgg(ExprNodeOrigin exprNodeOrigin, ExprNode exprNode, StreamTypeService streamTypeService, StatementContext statementContext, ExprEvaluatorContext exprEvaluatorContext, String errorMsg, boolean allowTableConsumption) throws ExprValidationException {
        ExprValidationContext validationContext = new ExprValidationContext(streamTypeService, statementContext.getEngineImportService(), statementContext.getStatementExtensionServicesContext(), null, statementContext.getSchedulingService(), statementContext.getVariableService(), statementContext.getTableService(), exprEvaluatorContext, statementContext.getEventAdapterService(), statementContext.getStatementName(), statementContext.getStatementId(), statementContext.getAnnotations(), statementContext.getContextDescriptor(), false, false, allowTableConsumption, false, null, false);
        ExprNode validated = ExprNodeUtilityRich.getValidatedSubtree(exprNodeOrigin, exprNode, validationContext);
        validateNoAggregations(validated, errorMsg);
        return validated;
    }

    protected static void validateNoAggregations(ExprNode exprNode, String errorMsg)
            throws ExprValidationException {
        // Make sure there is no aggregation in the where clause
        List<ExprAggregateNode> aggregateNodes = new LinkedList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(exprNode, aggregateNodes);
        if (!aggregateNodes.isEmpty()) {
            throw new ExprValidationException(errorMsg);
        }
    }

    // Special-case validation: When an on-merge query in the not-matched clause uses a subquery then
    // that subquery should not reference any of the stream's properties which are not-matched
    protected static void validateSubqueryExcludeOuterStream(ExprNode matchCondition) throws ExprValidationException {
        ExprNodeSubselectDeclaredDotVisitor visitorSubselects = new ExprNodeSubselectDeclaredDotVisitor();
        matchCondition.accept(visitorSubselects);
        if (visitorSubselects.getSubselects().isEmpty()) {
            return;
        }
        ExprNodeIdentifierCollectVisitor visitorProps = new ExprNodeIdentifierCollectVisitor();
        for (ExprSubselectNode node : visitorSubselects.getSubselects()) {
            if (node.getStatementSpecCompiled().getFilterRootNode() != null) {
                node.getStatementSpecCompiled().getFilterRootNode().accept(visitorProps);
            }
        }
        for (ExprIdentNode node : visitorProps.getExprProperties()) {
            if (node.getStreamId() == 1) {
                throw new ExprValidationException("On-Merge not-matched filter expression may not use properties that are provided by the named window event");
            }
        }
    }
}
