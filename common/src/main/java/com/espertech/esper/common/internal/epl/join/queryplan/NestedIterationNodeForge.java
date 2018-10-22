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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanNodeForgeVisitor;
import com.espertech.esper.common.internal.util.IndentWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Plan to perform a nested iteration over child nodes.
 */
public class NestedIterationNodeForge extends QueryPlanNodeForge {
    private final LinkedList<QueryPlanNodeForge> childNodes;
    private final int[] nestingOrder;

    /**
     * Ctor.
     *
     * @param nestingOrder - order of streams in nested iteration
     */
    public NestedIterationNodeForge(int[] nestingOrder) {
        this.nestingOrder = nestingOrder;
        this.childNodes = new LinkedList<>();

        if (nestingOrder.length == 0) {
            throw new IllegalArgumentException("Invalid empty nesting order");
        }
    }

    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    public final void addChildNode(QueryPlanNodeForge childNode) {
        childNodes.add(childNode);
    }

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public final LinkedList<QueryPlanNodeForge> getChildNodes() {
        return childNodes;
    }

    public void addIndexes(HashSet<TableLookupIndexReqKey> usedIndexes) {
        for (QueryPlanNodeForge child : childNodes) {
            child.addIndexes(usedIndexes);
        }
    }

    public void print(IndentWriter indentWriter) {
        indentWriter.println("NestedIterationNode with nesting order " + Arrays.toString(nestingOrder));
        indentWriter.incrIndent();
        for (QueryPlanNodeForge child : childNodes) {
            child.print(indentWriter);
        }
        indentWriter.decrIndent();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression childNodeArray = CodegenMakeableUtil.makeArray("childNodes", QueryPlanNode.class, childNodes.toArray(new QueryPlanNodeForge[childNodes.size()]),
                this.getClass(), parent, symbols, classScope);
        return newInstance(NestedIterationNode.class, childNodeArray, constant(nestingOrder));
    }

    public void accept(QueryPlanNodeForgeVisitor visitor) {
        visitor.visit(this);
        for (QueryPlanNodeForge child : childNodes) {
            child.accept(visitor);
        }
    }
}
