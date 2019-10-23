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
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.visitor.*;

import java.util.*;

public class ExprNodeUtilityQuery {
    public static final ExprNode[] EMPTY_EXPR_ARRAY = new ExprNode[0];
    public static final ExprForge[] EMPTY_FORGE_ARRAY = new ExprForge[0];

    public static ExprForge[] forgesForProperties(EventType[] eventTypes, String[] propertyNames, int[] keyStreamNums) {
        ExprForge[] forges = new ExprForge[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            ExprIdentNodeImpl node = new ExprIdentNodeImpl(eventTypes[keyStreamNums[i]], propertyNames[i], keyStreamNums[i]);
            forges[i] = node.getForge();
        }
        return forges;
    }

    public static boolean isConstant(ExprNode exprNode) {
        return exprNode.getForge().getForgeConstantType().isConstant();
    }

    public static Set<String> getPropertyNamesIfAllProps(ExprNode[] expressions) {
        for (ExprNode expression : expressions) {
            if (!(expression instanceof ExprIdentNode)) {
                return null;
            }
        }
        Set<String> uniquePropertyNames = new HashSet<String>();
        for (ExprNode expression : expressions) {
            ExprIdentNode identNode = (ExprIdentNode) expression;
            uniquePropertyNames.add(identNode.getUnresolvedPropertyName());
        }
        return uniquePropertyNames;
    }

    public static List<Pair<ExprNode, ExprNode>> findExpression(ExprNode selectExpression, ExprNode searchExpression) {
        List<Pair<ExprNode, ExprNode>> pairs = new ArrayList<Pair<ExprNode, ExprNode>>();
        if (ExprNodeUtilityCompare.deepEquals(selectExpression, searchExpression, false)) {
            pairs.add(new Pair<ExprNode, ExprNode>(null, selectExpression));
            return pairs;
        }
        findExpressionChildRecursive(selectExpression, searchExpression, pairs);
        return pairs;
    }

    private static void findExpressionChildRecursive(ExprNode parent, ExprNode searchExpression, List<Pair<ExprNode, ExprNode>> pairs) {
        for (ExprNode child : parent.getChildNodes()) {
            if (ExprNodeUtilityCompare.deepEquals(child, searchExpression, false)) {
                pairs.add(new Pair<ExprNode, ExprNode>(parent, child));
                continue;
            }
            findExpressionChildRecursive(child, searchExpression, pairs);
        }
    }

    public static String[] getIdentResolvedPropertyNames(ExprNode[] nodes) {
        String[] propertyNames = new String[nodes.length];
        for (int i = 0; i < propertyNames.length; i++) {
            if (!(nodes[i] instanceof ExprIdentNode)) {
                throw new IllegalArgumentException("Expressions are not ident nodes");
            }
            propertyNames[i] = ((ExprIdentNode) nodes[i]).getResolvedPropertyName();
        }
        return propertyNames;
    }

    public static Class[] getExprResultTypes(ExprNode[] nodes) {
        Class[] types = new Class[nodes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = nodes[i].getForge().getEvaluationType();
        }
        return types;
    }

    public static Class[] getExprResultTypes(ExprForge[] nodes) {
        Class[] types = new Class[nodes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = nodes[i].getEvaluationType();
        }
        return types;
    }

    public static ExprNode[] toArray(Collection<ExprNode> expressions) {
        if (expressions.isEmpty()) {
            return EMPTY_EXPR_ARRAY;
        }
        return expressions.toArray(new ExprNode[expressions.size()]);
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(ExprNode[] exprNodes) {
        if (exprNodes == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNode node = exprNodes[i];
            if (node != null) {
                eval[i] = node.getForge().getExprEvaluator();
            }
        }
        return eval;
    }

    public static ExprForge[] getForges(ExprNode[] exprNodes) {
        if (exprNodes == null) {
            return null;
        }
        ExprForge[] forge = new ExprForge[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNode node = exprNodes[i];
            if (node != null) {
                forge[i] = node.getForge();
            }
        }
        return forge;
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(ExprForge[] forges) {
        if (forges == null) {
            return null;
        }
        ExprEvaluator[] eval = new ExprEvaluator[forges.length];
        for (int i = 0; i < forges.length; i++) {
            ExprForge forge = forges[i];
            if (forge != null) {
                eval[i] = forge.getExprEvaluator();
            }
        }
        return eval;
    }

    public static ExprEvaluator[] getEvaluatorsNoCompile(List<ExprNode> childNodes) {
        ExprEvaluator[] eval = new ExprEvaluator[childNodes.size()];
        for (int i = 0; i < childNodes.size(); i++) {
            eval[i] = childNodes.get(i).getForge().getExprEvaluator();
        }
        return eval;
    }

    public static Class[] getExprResultTypes(List<ExprNode> expressions) {
        Class[] returnTypes = new Class[expressions.size()];
        for (int i = 0; i < expressions.size(); i++) {
            returnTypes[i] = expressions.get(i).getForge().getEvaluationType();
        }
        return returnTypes;
    }

    public static void acceptParams(ExprNodeVisitor visitor, List<ExprNode> params) {
        for (ExprNode param : params) {
            param.accept(visitor);
        }
    }

    public static void acceptParams(ExprNodeVisitorWithParent visitor, List<ExprNode> params) {
        for (ExprNode param : params) {
            param.accept(visitor);
        }
    }

    public static void acceptParams(ExprNodeVisitorWithParent visitor, List<ExprNode> params, ExprNode parent) {
        for (ExprNode param : params) {
            param.acceptChildnodes(visitor, parent);
        }
    }


    public static String[] getPropertiesPerExpressionExpectSingle(ExprNode[] exprNodes) {
        String[] indexedProperties = new String[exprNodes.length];
        for (int i = 0; i < exprNodes.length; i++) {
            ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(true);
            exprNodes[i].accept(visitor);
            if (visitor.getExprProperties().size() != 1) {
                throw new IllegalStateException("Failed to find indexed property");
            }
            indexedProperties[i] = visitor.getExprProperties().iterator().next().getSecond();
        }
        return indexedProperties;
    }

    public static boolean isExpressionsAllPropsOnly(ExprNode[] exprNodes) {
        for (int i = 0; i < exprNodes.length; i++) {
            if (!(exprNodes[i] instanceof ExprIdentNode)) {
                return false;
            }
        }
        return true;
    }

    public static Set<Integer> getIdentStreamNumbers(ExprNode child) {
        Set<Integer> streams = new HashSet<>();
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        child.accept(visitor);
        for (ExprIdentNode node : visitor.getExprProperties()) {
            streams.add(node.getStreamId());
        }
        return streams;
    }

    public static List<Pair<Integer, String>> getExpressionProperties(ExprNode exprNode, boolean visitAggregateNodes) {
        ExprNodeIdentifierVisitor visitor = new ExprNodeIdentifierVisitor(visitAggregateNodes);
        exprNode.accept(visitor);
        return visitor.getExprProperties();
    }

    public static boolean isAllConstants(List<ExprNode> parameters) {
        for (ExprNode node : parameters) {
            if (!node.getForge().getForgeConstantType().isCompileTimeConstant()) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasStreamSelect(List<ExprNode> exprNodes) {
        ExprNodeStreamSelectVisitor visitor = new ExprNodeStreamSelectVisitor(false);
        for (ExprNode node : exprNodes) {
            node.accept(visitor);
            if (visitor.hasStreamSelect()) {
                return true;
            }
        }
        return false;
    }

    public static List<ExprNode> collectChainParameters(List<ExprChainedSpec> chainSpec) {
        List<ExprNode> result = new ArrayList<>();
        for (ExprChainedSpec chainElement : chainSpec) {
            result.addAll(chainElement.getParameters());
        }
        return result;
    }

    public static void acceptChain(ExprNodeVisitor visitor, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chain : chainSpec) {
            acceptParams(visitor, chain.getParameters());
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec) {
        for (ExprChainedSpec chain : chainSpec) {
            acceptParams(visitor, chain.getParameters());
        }
    }

    public static void acceptChain(ExprNodeVisitorWithParent visitor, List<ExprChainedSpec> chainSpec, ExprNode parent) {
        for (ExprChainedSpec chain : chainSpec) {
            acceptParams(visitor, chain.getParameters(), parent);
        }
    }

    public static Map<ExprDeclaredNode, List<ExprDeclaredNode>> getDeclaredExpressionCallHierarchy(ExprDeclaredNode[] declaredExpressions) {
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        Map<ExprDeclaredNode, List<ExprDeclaredNode>> calledToCallerMap = new HashMap<ExprDeclaredNode, List<ExprDeclaredNode>>();
        for (ExprDeclaredNode node : declaredExpressions) {
            visitor.reset();
            node.acceptNoVisitParams(visitor);
            for (ExprDeclaredNode called : visitor.getDeclaredExpressions()) {
                if (called == node) {
                    continue;
                }
                List<ExprDeclaredNode> callers = calledToCallerMap.get(called);
                if (callers == null) {
                    callers = new ArrayList<ExprDeclaredNode>(2);
                    calledToCallerMap.put(called, callers);
                }
                callers.add(node);
            }
            if (!calledToCallerMap.containsKey(node)) {
                calledToCallerMap.put(node, Collections.<ExprDeclaredNode>emptyList());
            }
        }
        return calledToCallerMap;
    }
}
