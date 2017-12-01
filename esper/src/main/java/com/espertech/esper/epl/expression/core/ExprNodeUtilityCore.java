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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;

public class ExprNodeUtilityCore {
    private static final Logger log = LoggerFactory.getLogger(ExprNodeUtilityCore.class);

    public static final ExprNode[] EMPTY_EXPR_ARRAY = new ExprNode[0];
    public static final ExprForge[] EMPTY_FORGE_ARRAY = new ExprForge[0];

    public static Comparator<Object> getComparatorHashableMultiKeys(ExprNode[] sortCriteria, boolean isSortUsingCollator, boolean[] isDescendingValues) {
        // determine string-type sorting
        boolean hasStringTypes = false;
        boolean[] stringTypes = new boolean[sortCriteria.length];

        int count = 0;
        for (int i = 0; i < sortCriteria.length; i++) {
            if (sortCriteria[i].getForge().getEvaluationType() == String.class) {
                hasStringTypes = true;
                stringTypes[count] = true;
            }
            count++;
        }

        if (sortCriteria.length > 1) {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                HashableMultiKeyComparator comparatorMK = new HashableMultiKeyComparator(isDescendingValues);
                return new HashableMultiKeyCastingComparator(comparatorMK);
            } else {
                HashableMultiKeyCollatingComparator comparatorMk = new HashableMultiKeyCollatingComparator(isDescendingValues, stringTypes);
                return new HashableMultiKeyCastingComparator(comparatorMk);
            }
        } else {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                return new ObjectComparator(isDescendingValues[0]);
            } else {
                return new ObjectCollatingComparator(isDescendingValues[0]);
            }
        }
    }

    public static Object evaluateValidationTimeNoStreams(ExprEvaluator evaluator, ExprEvaluatorContext context, String expressionName) throws ExprValidationException {
        try {
            return evaluator.evaluate(null, true, context);
        } catch (EPException ex) {
            throw new ExprValidationException("Invalid " + expressionName + " expression: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            log.warn("Invalid " + expressionName + " expression evaluation: {}", ex.getMessage(), ex);
            throw new ExprValidationException("Invalid " + expressionName + " expression");
        }
    }

    public static boolean deepEqualsIsSubset(ExprNode[] subset, ExprNode[] superset) {
        for (ExprNode subsetNode : subset) {
            boolean found = false;
            for (ExprNode supersetNode : superset) {
                if (deepEquals(subsetNode, supersetNode, false)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsIgnoreDupAndOrder(ExprNode[] setOne, ExprNode[] setTwo) {
        if ((setOne.length == 0 && setTwo.length != 0) || (setOne.length != 0 && setTwo.length == 0)) {
            return false;
        }

        // find set-one expressions in set two
        boolean[] foundTwo = new boolean[setTwo.length];
        for (ExprNode one : setOne) {
            boolean found = false;
            for (int i = 0; i < setTwo.length; i++) {
                if (deepEquals(one, setTwo[i], false)) {
                    found = true;
                    foundTwo[i] = true;
                }
            }
            if (!found) {
                return false;
            }
        }

        // find any remaining set-two expressions in set one
        for (int i = 0; i < foundTwo.length; i++) {
            if (foundTwo[i]) {
                continue;
            }
            for (ExprNode one : setOne) {
                if (deepEquals(one, setTwo[i], false)) {
                    break;
                }
            }
            return false;
        }
        return true;
    }

    public static String toExpressionStringMinPrecedenceSafe(ExprNode node) {
        try {
            StringWriter writer = new StringWriter();
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            return writer.toString();
        } catch (RuntimeException ex) {
            log.debug("Failed to render expression text: " + ex.getMessage(), ex);
            return "";
        }
    }

    public static String[] toExpressionStringMinPrecedenceAsArray(ExprNode[] nodes) {
        String[] expressions = new String[nodes.length];
        for (int i = 0; i < expressions.length; i++) {
            StringWriter writer = new StringWriter();
            nodes[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            expressions[i] = writer.toString();
        }
        return expressions;
    }

    public static String toExpressionStringMinPrecedenceAsList(ExprNode[] nodes) {
        StringWriter writer = new StringWriter();
        toExpressionStringMinPrecedenceAsList(nodes, writer);
        return writer.toString();
    }

    public static void toExpressionStringMinPrecedenceAsList(ExprNode[] nodes, StringWriter writer) {
        String delimiter = "";
        for (ExprNode node : nodes) {
            writer.append(delimiter);
            node.toEPL(writer, ExprPrecedenceEnum.MINIMUM);
            delimiter = ",";
        }
    }

    public static void applyFilterExpressionsIterable(Iterable<EventBean> iterable, List<ExprNode> filterExpressions, ExprEvaluatorContext exprEvaluatorContext, Collection<EventBean> eventsInWindow) {
        ExprEvaluator[] evaluators = getEvaluatorsNoCompile(filterExpressions);
        EventBean[] events = new EventBean[1];
        for (EventBean theEvent : iterable) {
            events[0] = theEvent;
            boolean add = true;
            for (ExprEvaluator filter : evaluators) {
                Object result = filter.evaluate(events, true, exprEvaluatorContext);
                if ((result == null) || (!((Boolean) result))) {
                    add = false;
                    break;
                }
            }
            if (add) {
                eventsInWindow.add(events[0]);
            }
        }
    }

    public static void applyFilterExpressionIterable(Iterator<EventBean> iterator, ExprEvaluator filterExpression, ExprEvaluatorContext exprEvaluatorContext, Collection<EventBean> eventsInWindow) {
        EventBean[] events = new EventBean[1];
        for (; iterator.hasNext(); ) {
            events[0] = iterator.next();
            Object result = filterExpression.evaluate(events, true, exprEvaluatorContext);
            if ((result == null) || (!((Boolean) result))) {
                continue;
            }
            eventsInWindow.add(events[0]);
        }
    }

    public static boolean isConstantValueExpr(ExprNode exprNode) {
        if (!(exprNode instanceof ExprConstantNode)) {
            return false;
        }
        ExprConstantNode constantNode = (ExprConstantNode) exprNode;
        return constantNode.isConstantValue();
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

    public static String[] toExpressionStringsMinPrecedence(ExprNode[] expressions) {
        String[] texts = new String[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            texts[i] = toExpressionStringMinPrecedenceSafe(expressions[i]);
        }
        return texts;
    }

    public static List<Pair<ExprNode, ExprNode>> findExpression(ExprNode selectExpression, ExprNode searchExpression) {
        List<Pair<ExprNode, ExprNode>> pairs = new ArrayList<Pair<ExprNode, ExprNode>>();
        if (deepEquals(selectExpression, searchExpression, false)) {
            pairs.add(new Pair<ExprNode, ExprNode>(null, selectExpression));
            return pairs;
        }
        findExpressionChildRecursive(selectExpression, searchExpression, pairs);
        return pairs;
    }

    private static void findExpressionChildRecursive(ExprNode parent, ExprNode searchExpression, List<Pair<ExprNode, ExprNode>> pairs) {
        for (ExprNode child : parent.getChildNodes()) {
            if (deepEquals(child, searchExpression, false)) {
                pairs.add(new Pair<ExprNode, ExprNode>(parent, child));
                continue;
            }
            findExpressionChildRecursive(child, searchExpression, pairs);
        }
    }

    public static void toExpressionStringParameterList(ExprNode[] childNodes, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode childNode : childNodes) {
            buffer.append(delimiter);
            buffer.append(toExpressionStringMinPrecedenceSafe(childNode));
            delimiter = ",";
        }
    }

    public static void toExpressionStringWFunctionName(String functionName, ExprNode[] childNodes, StringWriter writer) {
        writer.append(functionName);
        writer.append("(");
        toExpressionStringParameterList(childNodes, writer);
        writer.append(')');
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

    public static ExprNode[] addExpression(ExprNode[] expressions, ExprNode expression) {
        ExprNode[] target = new ExprNode[expressions.length + 1];
        System.arraycopy(expressions, 0, target, 0, expressions.length);
        target[expressions.length] = expression;
        return target;
    }

    /**
     * Apply a filter expression.
     *
     * @param filter               expression
     * @param streamZeroEvent      the event that represents stream zero
     * @param streamOneEvents      all events thate are stream one events
     * @param exprEvaluatorContext context for expression evaluation
     * @return filtered stream one events
     */
    public static EventBean[] applyFilterExpression(ExprEvaluator filter, EventBean streamZeroEvent, EventBean[] streamOneEvents, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean[] eventsPerStream = new EventBean[2];
        eventsPerStream[0] = streamZeroEvent;

        EventBean[] filtered = new EventBean[streamOneEvents.length];
        int countPass = 0;

        for (EventBean eventBean : streamOneEvents) {
            eventsPerStream[1] = eventBean;

            Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if ((result != null) && result) {
                filtered[countPass] = eventBean;
                countPass++;
            }
        }

        if (countPass == streamOneEvents.length) {
            return streamOneEvents;
        }
        return EventBeanUtility.resizeArray(filtered, countPass);
    }

    /**
     * Apply a filter expression returning a pass indicator.
     *
     * @param filter               to apply
     * @param eventsPerStream      events per stream
     * @param exprEvaluatorContext context for expression evaluation
     * @return pass indicator
     */
    public static boolean applyFilterExpression(ExprEvaluator filter, EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        Boolean result = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
        return (result != null) && result;
    }

    /**
     * Compare two expression nodes and their children in exact child-node sequence,
     * returning true if the 2 expression nodes trees are equals, or false if they are not equals.
     * <p>
     * Recursive call since it uses this method to compare child nodes in the same exact sequence.
     * Nodes are compared using the equalsNode method.
     *
     * @param nodeOne            - first expression top node of the tree to compare
     * @param nodeTwo            - second expression top node of the tree to compare
     * @param ignoreStreamPrefix when the equals-comparison can ignore prefix of event properties
     * @return false if this or all child nodes are not equal, true if equal
     */
    public static boolean deepEquals(ExprNode nodeOne, ExprNode nodeTwo, boolean ignoreStreamPrefix) {
        if (nodeOne.getChildNodes().length != nodeTwo.getChildNodes().length) {
            return false;
        }
        if (!nodeOne.equalsNode(nodeTwo, ignoreStreamPrefix)) {
            return false;
        }
        for (int i = 0; i < nodeOne.getChildNodes().length; i++) {
            ExprNode childNodeOne = nodeOne.getChildNodes()[i];
            ExprNode childNodeTwo = nodeTwo.getChildNodes()[i];

            if (!deepEquals(childNodeOne, childNodeTwo, ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsNullChecked(ExprNode nodeOne, ExprNode nodeTwo, boolean ignoreStreamPrefix) {
        if (nodeOne == null) {
            return nodeTwo == null;
        }
        return nodeTwo != null && deepEquals(nodeOne, nodeTwo, ignoreStreamPrefix);
    }

    /**
     * Compares two expression nodes via deep comparison, considering all
     * child nodes of either side.
     *
     * @param one                array of expressions
     * @param two                array of expressions
     * @param ignoreStreamPrefix indicator whether we ignore stream prefixes and instead use resolved property name
     * @return true if the expressions are equal, false if not
     */
    public static boolean deepEquals(ExprNode[] one, ExprNode[] two, boolean ignoreStreamPrefix) {
        if (one.length != two.length) {
            return false;
        }
        for (int i = 0; i < one.length; i++) {
            if (!deepEquals(one[i], two[i], ignoreStreamPrefix)) {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEquals(List<ExprNode> one, List<ExprNode> two) {
        if (one.size() != two.size()) {
            return false;
        }
        for (int i = 0; i < one.size(); i++) {
            if (!deepEquals(one.get(i), two.get(i), false)) {
                return false;
            }
        }
        return true;
    }

    public static Object[] evaluateExpressions(ExprEvaluator[] parameters, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] results = new Object[parameters.length];
        int count = 0;
        for (ExprEvaluator expr : parameters) {
            try {
                results[count] = expr.evaluate(null, true, exprEvaluatorContext);
                count++;
            } catch (RuntimeException ex) {
                String message = "Failed expression evaluation in crontab timer-at for parameter " + count + ": " + ex.getMessage();
                log.error(message, ex);
                throw new IllegalArgumentException(message);
            }
        }
        return results;
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

    public static void toExpressionStringParams(StringWriter writer, ExprNode[] params) {
        writer.append('(');
        String delimiter = "";
        for (ExprNode childNode : params) {
            writer.append(delimiter);
            delimiter = ",";
            writer.append(toExpressionStringMinPrecedenceSafe(childNode));
        }
        writer.append(')');
    }

    public static Class[] getExprResultTypes(List<ExprNode> expressions) {
        Class[] returnTypes = new Class[expressions.size()];
        for (int i = 0; i < expressions.size(); i++) {
            returnTypes[i] = expressions.get(i).getForge().getEvaluationType();
        }
        return returnTypes;
    }

    public static void replaceChildNode(ExprNode parentNode, ExprNode nodeToReplace, ExprNode newNode) {
        int index = ExprNodeUtilityCore.findChildNode(parentNode, nodeToReplace);
        if (index == -1) {
            parentNode.replaceUnlistedChildNode(nodeToReplace, newNode);
        } else {
            parentNode.setChildNode(index, newNode);
        }
    }

    public static void toExpressionStringParameterList(List<ExprNode> parameters, StringWriter buffer) {
        String delimiter = "";
        for (ExprNode param : parameters) {
            buffer.append(delimiter);
            delimiter = ",";
            buffer.append(toExpressionStringMinPrecedenceSafe(param));
        }
    }

    public static void toExpressionString(ExprNode node, StringWriter buffer) {
        node.toEPL(buffer, ExprPrecedenceEnum.MINIMUM);
    }

    public static void toExpressionStringIncludeParen(List<ExprNode> parameters, StringWriter buffer) {
        buffer.append("(");
        toExpressionStringParameterList(parameters, buffer);
        buffer.append(")");
    }

    public static String printEvaluators(ExprEvaluator[] evaluators) {
        StringWriter writer = new StringWriter();
        String delimiter = "";
        for (ExprEvaluator evaluator : evaluators) {
            writer.append(delimiter);
            writer.append(evaluator.getClass().getSimpleName());
            delimiter = ", ";
        }
        return writer.toString();
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

    public static ExprIdentNode getExprIdentNode(EventType[] typesPerStream, int streamId, String property) {
        return new ExprIdentNodeImpl(typesPerStream[streamId], property, streamId);
    }

    private static int findChildNode(ExprNode parentNode, ExprNode childNode) {
        for (int i = 0; i < parentNode.getChildNodes().length; i++) {
            if (parentNode.getChildNodes()[i] == childNode) {
                return i;
            }
        }
        return -1;
    }
}
