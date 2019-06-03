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
import com.espertech.esper.common.internal.compile.stage1.spec.FilterStreamSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecRaw;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalUnderlyingEvaluator;
import com.espertech.esper.common.internal.epl.expression.etc.ExprEvalUnderlyingEvaluatorTable;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.util.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ExprNodeUtilityMake {
    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param sortCriteriaTypes   types
     * @param isSortUsingCollator flag
     * @param isDescendingValues  flags
     * @return comparator
     */
    public static Comparator<Object> getComparatorHashableMultiKeys(Class[] sortCriteriaTypes, boolean isSortUsingCollator, boolean[] isDescendingValues) {
        // determine string-type sorting
        boolean hasStringTypes = false;
        boolean[] stringTypes = new boolean[sortCriteriaTypes.length];

        int count = 0;
        for (int i = 0; i < sortCriteriaTypes.length; i++) {
            if (sortCriteriaTypes[i] == String.class) {
                hasStringTypes = true;
                stringTypes[count] = true;
            }
            count++;
        }

        if (sortCriteriaTypes.length > 1) {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                ComparatorHashableMultiKey comparatorMK = new ComparatorHashableMultiKey(isDescendingValues);
                return new ComparatorHashableMultiKeyCasting(comparatorMK);
            } else {
                ComparatorHashableMultiKeyCollating comparatorMk = new ComparatorHashableMultiKeyCollating(isDescendingValues, stringTypes);
                return new ComparatorHashableMultiKeyCasting(comparatorMk);
            }
        } else {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                return new ObjectComparator(isDescendingValues[0]);
            } else {
                return new ObjectCollatingComparator(isDescendingValues[0]);
            }
        }
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param sortCriteriaTypes   types
     * @param isSortUsingCollator flag
     * @param isDescendingValues  flags
     * @return comparator
     */
    public static Comparator<Object> getComparatorObjectArrayNonHashable(Class[] sortCriteriaTypes, boolean isSortUsingCollator, boolean[] isDescendingValues) {
        // determine string-type sorting
        boolean hasStringTypes = false;
        boolean[] stringTypes = new boolean[sortCriteriaTypes.length];

        int count = 0;
        for (int i = 0; i < sortCriteriaTypes.length; i++) {
            if (sortCriteriaTypes[i] == String.class) {
                hasStringTypes = true;
                stringTypes[count] = true;
            }
            count++;
        }

        if (sortCriteriaTypes.length > 1) {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                ComparatorObjectArray comparatorMK = new ComparatorObjectArray(isDescendingValues);
                return new ComparatorObjectArrayCasting(comparatorMK);
            } else {
                ComparatorObjectArrayCollating comparatorMk = new ComparatorObjectArrayCollating(isDescendingValues, stringTypes);
                return new ComparatorObjectArrayCasting(comparatorMk);
            }
        } else {
            if ((!hasStringTypes) || (!isSortUsingCollator)) {
                return new ObjectComparator(isDescendingValues[0]);
            } else {
                return new ObjectCollatingComparator(isDescendingValues[0]);
            }
        }
    }

    public static ExprForge makeUnderlyingForge(final int streamNum, final Class resultType, TableMetaData tableMetadata) {
        if (tableMetadata != null) {
            return new ExprEvalUnderlyingEvaluatorTable(streamNum, resultType, tableMetadata);
        }
        return new ExprEvalUnderlyingEvaluator(streamNum, resultType);
    }

    static ExprForge[] makeVarargArrayForges(Method method, final ExprForge[] childForges) {
        ExprForge[] forges = new ExprForge[method.getParameterTypes().length];
        Class varargClass = method.getParameterTypes()[method.getParameterTypes().length - 1].getComponentType();
        Class varargClassBoxed = JavaClassHelper.getBoxedType(varargClass);
        if (method.getParameterTypes().length > 1) {
            System.arraycopy(childForges, 0, forges, 0, forges.length - 1);
        }
        final int varargArrayLength = childForges.length - method.getParameterTypes().length + 1;

        // handle passing array along
        if (varargArrayLength == 1) {
            ExprForge lastForge = childForges[method.getParameterTypes().length - 1];
            Class lastReturns = lastForge.getEvaluationType();
            if (lastReturns != null && lastReturns.isArray()) {
                forges[method.getParameterTypes().length - 1] = lastForge;
                return forges;
            }
        }

        // handle parameter conversion to vararg parameter
        ExprForge[] varargForges = new ExprForge[varargArrayLength];
        SimpleNumberCoercer[] coercers = new SimpleNumberCoercer[varargForges.length];
        boolean needCoercion = false;
        for (int i = 0; i < varargArrayLength; i++) {
            int childIndex = i + method.getParameterTypes().length - 1;
            Class resultType = childForges[childIndex].getEvaluationType();
            varargForges[i] = childForges[childIndex];

            if (resultType == null && !varargClass.isPrimitive()) {
                continue;
            }

            if (JavaClassHelper.isSubclassOrImplementsInterface(resultType, varargClass)) {
                // no need to coerce
                continue;
            }

            if (JavaClassHelper.getBoxedType(resultType) != varargClassBoxed) {
                needCoercion = true;
                coercers[i] = SimpleNumberCoercerFactory.getCoercer(resultType, varargClassBoxed);
            }
        }

        ExprForge varargForge = new ExprNodeVarargOnlyArrayForge(varargForges, varargClass, needCoercion ? coercers : null);
        forges[method.getParameterTypes().length - 1] = varargForge;
        return forges;
    }

    public static ExprNode[] addExpression(ExprNode[] expressions, ExprNode expression) {
        ExprNode[] target = new ExprNode[expressions.length + 1];
        System.arraycopy(expressions, 0, target, 0, expressions.length);
        target[expressions.length] = expression;
        return target;
    }

    public static UnsupportedOperationException makeUnsupportedCompileTime() {
        return new UnsupportedOperationException("The operation is not available at compile time");
    }

    public static ExprIdentNode makeExprIdentNode(EventType[] typesPerStream, int streamId, String property) {
        return new ExprIdentNodeImpl(typesPerStream[streamId], property, streamId);
    }

    public static ExprNode connectExpressionsByLogicalAndWhenNeeded(Collection<ExprNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.iterator().next();
        }
        return connectExpressionsByLogicalAnd(nodes);
    }

    public static ExprNode connectExpressionsByLogicalAnd(List<ExprNode> nodes, ExprNode optionalAdditionalFilter) {
        if (nodes.isEmpty()) {
            return optionalAdditionalFilter;
        }
        if (optionalAdditionalFilter == null) {
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            return connectExpressionsByLogicalAnd(nodes);
        }
        if (nodes.size() == 1) {
            return connectExpressionsByLogicalAnd(Arrays.asList(nodes.get(0), optionalAdditionalFilter));
        }
        ExprAndNode andNode = connectExpressionsByLogicalAnd(nodes);
        andNode.addChildNode(optionalAdditionalFilter);
        return andNode;
    }

    public static ExprAndNode connectExpressionsByLogicalAnd(Collection<ExprNode> nodes) {
        if (nodes.size() < 2) {
            throw new IllegalArgumentException("Invalid empty or 1-element list of nodes");
        }
        ExprAndNode andNode = new ExprAndNodeImpl();
        for (ExprNode node : nodes) {
            andNode.addChildNode(node);
        }
        return andNode;
    }

    public static void setChildIdentNodesOptionalEvent(ExprNode exprNode) {
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        exprNode.accept(visitor);
        for (ExprIdentNode node : visitor.getExprProperties()) {
            node.setOptionalEvent(true);
        }
    }

    public static String getSubqueryInfoText(ExprSubselectNode subselect) {
        String text = "subquery number " + (subselect.getSubselectNumber() + 1);
        StreamSpecRaw streamRaw = subselect.getStatementSpecRaw().getStreamSpecs().get(0);
        if (streamRaw instanceof FilterStreamSpecRaw) {
            text += " querying " + ((FilterStreamSpecRaw) streamRaw).getRawFilterSpec().getEventTypeName();
        }
        return text;
    }
}
