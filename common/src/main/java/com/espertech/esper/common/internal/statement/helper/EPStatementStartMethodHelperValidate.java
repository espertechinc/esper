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
package com.espertech.esper.common.internal.statement.helper;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.compile.stage1.spec.OnTriggerSetAssignment;
import com.espertech.esper.common.internal.compile.stage1.spec.OuterJoinDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.OutputLimitRateType;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowManagementService;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionExpressionTypeUtil;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeServiceImpl;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.view.access.ViewResourceDelegateExpr;
import com.espertech.esper.common.internal.view.core.DataWindowViewForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewForgeVisitor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class EPStatementStartMethodHelperValidate {
    // Special-case validation: When an on-merge query in the not-matched clause uses a subquery then
    // that subquery should not reference any of the stream's properties which are not-matched
    public static void validateSubqueryExcludeOuterStream(ExprNode matchCondition) throws ExprValidationException {
        ExprNodeSubselectDeclaredDotVisitor visitorSubselects = new ExprNodeSubselectDeclaredDotVisitor();
        matchCondition.accept(visitorSubselects);
        if (visitorSubselects.getSubselects().isEmpty()) {
            return;
        }
        ExprNodeIdentifierCollectVisitor visitorProps = new ExprNodeIdentifierCollectVisitor();
        for (ExprSubselectNode node : visitorSubselects.getSubselects()) {
            if (node.getStatementSpecCompiled().getRaw().getWhereClause() != null) {
                node.getStatementSpecCompiled().getRaw().getWhereClause().accept(visitorProps);
            }
        }
        for (ExprIdentNode node : visitorProps.getExprProperties()) {
            if (node.getStreamId() == 1) {
                throw new ExprValidationException("On-Merge not-matched filter expression may not use properties that are provided by the named window event");
            }
        }
    }

    public static ExprNode validateExprNoAgg(ExprNodeOrigin exprNodeOrigin, ExprNode exprNode, StreamTypeService streamTypeService, String errorMsg, boolean allowTableConsumption, boolean allowTableAggReset, StatementRawInfo raw, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        ExprValidationContext validationContext = new ExprValidationContextBuilder(streamTypeService, raw, compileTimeServices)
                .withAllowBindingConsumption(allowTableConsumption).withAllowTableAggReset(allowTableAggReset).build();
        ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(exprNodeOrigin, exprNode, validationContext);
        validateNoAggregations(validated, errorMsg);
        return validated;
    }

    public static void validateNoDataWindowOnNamedWindow(List<ViewFactoryForge> forges) throws ExprValidationException {
        final AtomicBoolean hasDataWindow = new AtomicBoolean();
        ViewForgeVisitor visitor = forge -> {
            if (forge instanceof DataWindowViewForge) {
                hasDataWindow.set(true);
            }
        };
        for (ViewFactoryForge forge : forges) {
            forge.accept(visitor);
        }
        if (hasDataWindow.get()) {
            throw new ExprValidationException(NamedWindowManagementService.ERROR_MSG_NO_DATAWINDOW_ALLOWED);
        }
    }

    public static ExprNode validateNodes(StatementSpecRaw statementSpec, StreamTypeService typeService, ViewResourceDelegateExpr viewResourceDelegate, StatementRawInfo statementRawInfo,
                                         StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        String intoTableName = statementSpec.getIntoTableSpec() == null ? null : statementSpec.getIntoTableSpec().getName();

        ExprNode whereClauseValidated = null;
        if (statementSpec.getWhereClause() != null) {
            ExprNode whereClause = statementSpec.getWhereClause();

            // Validate where clause, initializing nodes to the stream ids used
            try {
                ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, statementRawInfo, compileTimeServices)
                        .withViewResourceDelegate(viewResourceDelegate)
                        .withAllowBindingConsumption(true)
                        .withIntoTableName(intoTableName)
                        .build();
                whereClause = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.FILTER, whereClause, validationContext);
                if (whereClause.getForge().getEvaluationType() != boolean.class && whereClause.getForge().getEvaluationType() != Boolean.class) {
                    throw new ExprValidationException("The where-clause filter expression must return a boolean value");
                }
                whereClauseValidated = whereClause;

                // Make sure there is no aggregation in the where clause
                List<ExprAggregateNode> aggregateNodes = new LinkedList<ExprAggregateNode>();
                ExprAggregateNodeUtil.getAggregatesBottomUp(whereClause, aggregateNodes);
                if (!aggregateNodes.isEmpty()) {
                    throw new ExprValidationException("An aggregate function may not appear in a WHERE clause (use the HAVING clause)");
                }
            } catch (ExprValidationException ex) {
                throw new ExprValidationException("Error validating expression: " + ex.getMessage(), ex);
            }
        }

        if ((statementSpec.getOutputLimitSpec() != null) && ((statementSpec.getOutputLimitSpec().getWhenExpressionNode() != null) || (statementSpec.getOutputLimitSpec().getAndAfterTerminateExpr() != null))) {
            // Validate where clause, initializing nodes to the stream ids used
            EventType outputLimitType = OutputConditionExpressionTypeUtil.getBuiltInEventType(statementRawInfo.getModuleName(), compileTimeServices.getBeanEventTypeFactoryPrivate());
            StreamTypeService typeServiceOutputWhen = new StreamTypeServiceImpl(new EventType[]{outputLimitType}, new String[]{null}, new boolean[]{true}, false, false);
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeServiceOutputWhen, statementRawInfo, compileTimeServices)
                    .withIntoTableName(intoTableName).build();

            ExprNode outputLimitWhenNode = statementSpec.getOutputLimitSpec().getWhenExpressionNode();
            if (outputLimitWhenNode != null) {
                outputLimitWhenNode = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, outputLimitWhenNode, validationContext);
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
                ExprNode validated = ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.OUTPUTLIMIT, statementSpec.getOutputLimitSpec().getAndAfterTerminateExpr(), validationContext);
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
        }

        for (int outerJoinCount = 0; outerJoinCount < statementSpec.getOuterJoinDescList().size(); outerJoinCount++) {
            OuterJoinDesc outerJoinDesc = statementSpec.getOuterJoinDescList().get(outerJoinCount);

            // validate on-expression nodes, if provided
            if (outerJoinDesc.getOptLeftNode() != null) {
                UniformPair<Integer> streamIdPair = validateOuterJoinPropertyPair(outerJoinDesc.getOptLeftNode(), outerJoinDesc.getOptRightNode(), outerJoinCount, typeService, viewResourceDelegate, statementRawInfo, compileTimeServices
                );

                if (outerJoinDesc.getAdditionalLeftNodes() != null) {
                    Set<Integer> streamSet = new HashSet<Integer>();
                    streamSet.add(streamIdPair.getFirst());
                    streamSet.add(streamIdPair.getSecond());
                    for (int i = 0; i < outerJoinDesc.getAdditionalLeftNodes().length; i++) {
                        UniformPair<Integer> streamIdPairAdd = validateOuterJoinPropertyPair(outerJoinDesc.getAdditionalLeftNodes()[i], outerJoinDesc.getAdditionalRightNodes()[i], outerJoinCount, typeService, viewResourceDelegate, statementRawInfo, compileTimeServices
                        );

                        // make sure all additional properties point to the same two streams
                        if (!streamSet.contains(streamIdPairAdd.getFirst()) || (!streamSet.contains(streamIdPairAdd.getSecond()))) {
                            String message = "Outer join ON-clause columns must refer to properties of the same joined streams" +
                                    " when using multiple columns in the on-clause";
                            throw new ExprValidationException("Error validating outer-join expression: " + message);
                        }

                    }
                }
            }
        }

        return whereClauseValidated;
    }

    protected static UniformPair<Integer> validateOuterJoinPropertyPair(
            ExprIdentNode leftNode, ExprIdentNode rightNode, int outerJoinCount, StreamTypeService typeService,
            ViewResourceDelegateExpr viewResourceDelegate, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) throws ExprValidationException {
        // Validate the outer join clause using an artificial equals-node on top.
        // Thus types are checked via equals.
        // Sets stream ids used for validated nodes.
        ExprNode equalsNode = new ExprEqualsNodeImpl(false, false);
        equalsNode.addChildNode(leftNode);
        equalsNode.addChildNode(rightNode);
        try {
            ExprValidationContext validationContext = new ExprValidationContextBuilder(typeService, statementRawInfo, compileTimeServices)
                    .withViewResourceDelegate(viewResourceDelegate).withAllowBindingConsumption(true).withIsFilterExpression(true).build();
            ExprNodeUtilityValidate.getValidatedSubtree(ExprNodeOrigin.JOINON, equalsNode, validationContext);
        } catch (ExprValidationException ex) {
            throw new ExprValidationException("Error validating outer-join expression: " + ex.getMessage(), ex);
        }

        // Make sure we have left-hand-side and right-hand-side refering to different streams
        int streamIdLeft = leftNode.getStreamId();
        int streamIdRight = rightNode.getStreamId();
        if (streamIdLeft == streamIdRight) {
            String message = "Outer join ON-clause cannot refer to properties of the same stream";
            throw new ExprValidationException("Error validating outer-join expression: " + message);
        }

        // Make sure one of the properties refers to the acutual stream currently being joined
        int expectedStreamJoined = outerJoinCount + 1;
        if ((streamIdLeft != expectedStreamJoined) && (streamIdRight != expectedStreamJoined)) {
            String message = "Outer join ON-clause must refer to at least one property of the joined stream" +
                    " for stream " + expectedStreamJoined;
            throw new ExprValidationException("Error validating outer-join expression: " + message);
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
            throw new ExprValidationException("Error validating outer-join expression: " + message);
        }

        return new UniformPair<Integer>(streamIdLeft, streamIdRight);
    }

    public static void validateNoAggregations(ExprNode exprNode, String errorMsg)
            throws ExprValidationException {
        // Make sure there is no aggregation in the where clause
        List<ExprAggregateNode> aggregateNodes = new LinkedList<ExprAggregateNode>();
        ExprAggregateNodeUtil.getAggregatesBottomUp(exprNode, aggregateNodes);
        if (!aggregateNodes.isEmpty()) {
            throw new ExprValidationException(errorMsg);
        }
    }

    private static void validateThenSetAssignments(List<OnTriggerSetAssignment> assignments, ExprValidationContext validationContext)
            throws ExprValidationException {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }
        for (OnTriggerSetAssignment assign : assignments) {
            ExprNode node = ExprNodeUtilityValidate.getValidatedAssignment(assign, validationContext);
            assign.setExpression(node);
            EPStatementStartMethodHelperValidate.validateNoAggregations(node, "An aggregate function may not appear in a OUTPUT LIMIT clause");
        }
    }
}
