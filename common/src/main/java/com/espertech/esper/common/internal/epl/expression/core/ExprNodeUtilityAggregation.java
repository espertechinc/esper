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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierAndStreamRefVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.common.internal.epl.streamtype.StreamTypeService;

import java.util.List;

public class ExprNodeUtilityAggregation {

    /**
     * Returns true if all properties within the expression are witin data window'd streams.
     *
     * @param child              expression to interrogate
     * @param streamTypeService  streams
     * @param unidirectionalJoin indicator unidirection join
     * @return indicator
     */
    public static boolean hasRemoveStreamForAggregations(ExprNode child, StreamTypeService streamTypeService, boolean unidirectionalJoin) {

        // Determine whether all streams are istream-only or irstream
        boolean[] isIStreamOnly = streamTypeService.getIStreamOnly();
        boolean isAllIStream = true;    // all true?
        boolean isAllIRStream = true;   // all false?
        for (boolean anIsIStreamOnly : isIStreamOnly) {
            if (!anIsIStreamOnly) {
                isAllIStream = false;
            } else {
                isAllIRStream = false;
            }
        }

        // determine if a data-window applies to this max function
        boolean hasDataWindows = true;
        if (isAllIStream) {
            hasDataWindows = false;
        } else if (!isAllIRStream) {
            if (streamTypeService.getEventTypes().length > 1) {
                if (unidirectionalJoin) {
                    return false;
                }
                // In a join we assume that a data window is present or implicit via unidirectional
            } else {
                hasDataWindows = false;
                // get all aggregated properties to determine if any is from a windowed stream
                ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
                child.accept(visitor);
                for (ExprIdentNode node : visitor.getExprProperties()) {
                    if (!isIStreamOnly[node.getStreamId()]) {
                        hasDataWindows = true;
                        break;
                    }
                }
            }
        }

        return hasDataWindows;
    }

    public static ExprNodePropOrStreamSet getAggregatedProperties(List<ExprAggregateNode> aggregateNodes) {
        // Get a list of properties being aggregated in the clause.
        ExprNodePropOrStreamSet propertiesAggregated = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);
        for (ExprNode selectAggExprNode : aggregateNodes) {
            visitor.reset();
            selectAggExprNode.accept(visitor);
            List<ExprNodePropOrStreamDesc> properties = visitor.getRefs();
            propertiesAggregated.addAll(properties);
        }

        return propertiesAggregated;
    }

    public static void addNonAggregatedProps(ExprNode exprNode, ExprNodePropOrStreamSet set, EventType[] types, ContextPropertyRegistry contextPropertyRegistry) {
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        exprNode.accept(visitor);
        addNonAggregatedProps(set, visitor.getRefs(), types, contextPropertyRegistry);
    }

    public static ExprNodePropOrStreamSet getNonAggregatedProps(EventType[] types, List<ExprNode> exprNodes, ContextPropertyRegistry contextPropertyRegistry) {
        // Determine all event properties in the clause
        ExprNodePropOrStreamSet nonAggProps = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(false);
        for (ExprNode node : exprNodes) {
            visitor.reset();
            node.accept(visitor);
            addNonAggregatedProps(nonAggProps, visitor.getRefs(), types, contextPropertyRegistry);
        }

        return nonAggProps;
    }

    public static ExprNodePropOrStreamSet getGroupByPropertiesValidateHasOne(ExprNode[] groupByNodes)
            throws ExprValidationException {
        // Get the set of properties refered to by all group-by expression nodes.
        ExprNodePropOrStreamSet propertiesGroupBy = new ExprNodePropOrStreamSet();
        ExprNodeIdentifierAndStreamRefVisitor visitor = new ExprNodeIdentifierAndStreamRefVisitor(true);

        for (ExprNode groupByNode : groupByNodes) {
            visitor.reset();
            groupByNode.accept(visitor);
            List<ExprNodePropOrStreamDesc> propertiesNode = visitor.getRefs();
            propertiesGroupBy.addAll(propertiesNode);

            // For each group-by expression node, require at least one property.
            if (propertiesNode.isEmpty()) {
                throw new ExprValidationException("Group-by expressions must refer to property names");
            }
        }

        return propertiesGroupBy;
    }

    private static void addNonAggregatedProps(ExprNodePropOrStreamSet nonAggProps, List<ExprNodePropOrStreamDesc> refs, EventType[] types, ContextPropertyRegistry contextPropertyRegistry) {
        for (ExprNodePropOrStreamDesc pair : refs) {
            if (pair instanceof ExprNodePropOrStreamPropDesc) {
                ExprNodePropOrStreamPropDesc propDesc = (ExprNodePropOrStreamPropDesc) pair;
                EventType originType = types.length > pair.getStreamNum() ? types[pair.getStreamNum()] : null;
                if (originType == null || contextPropertyRegistry == null || !contextPropertyRegistry.isPartitionProperty(originType, propDesc.getPropertyName())) {
                    nonAggProps.add(pair);
                }
            } else {
                nonAggProps.add(pair);
            }
        }
    }

}
