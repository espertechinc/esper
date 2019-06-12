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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprEqualsNodeImpl;
import com.espertech.esper.common.internal.type.OuterJoinType;

import java.util.Collection;

/**
 * Contains the ON-clause criteria in an outer join.
 */
public class OuterJoinDesc {
    public static final OuterJoinDesc[] EMPTY_OUTERJOIN_ARRAY = new OuterJoinDesc[0];

    private OuterJoinType outerJoinType;
    private ExprIdentNode optLeftNode;
    private ExprIdentNode optRightNode;
    private ExprIdentNode[] optAddLeftNode;
    private ExprIdentNode[] optAddRightNode;

    /**
     * Ctor.
     *
     * @param outerJoinType   - type of the outer join
     * @param optLeftNode     - left hand identifier node
     * @param optRightNode    - right hand identifier node
     * @param optAddLeftNode  - additional optional left hand identifier nodes for the on-clause in a logical-and
     * @param optAddRightNode - additional optional right hand identifier nodes for the on-clause in a logical-and
     */
    public OuterJoinDesc(OuterJoinType outerJoinType, ExprIdentNode optLeftNode, ExprIdentNode optRightNode, ExprIdentNode[] optAddLeftNode, ExprIdentNode[] optAddRightNode) {
        this.outerJoinType = outerJoinType;
        this.optLeftNode = optLeftNode;
        this.optRightNode = optRightNode;
        this.optAddLeftNode = optAddLeftNode;
        this.optAddRightNode = optAddRightNode;
    }

    /**
     * Returns the type of outer join (left/right/full).
     *
     * @return outer join type
     */
    public OuterJoinType getOuterJoinType() {
        return outerJoinType;
    }

    /**
     * Returns left hand identifier node.
     *
     * @return left hand
     */
    public ExprIdentNode getOptLeftNode() {
        return optLeftNode;
    }

    /**
     * Returns right hand identifier node.
     *
     * @return right hand
     */
    public ExprIdentNode getOptRightNode() {
        return optRightNode;
    }

    /**
     * Returns additional properties in the on-clause, if any, that are connected via logical-and
     *
     * @return additional properties
     */
    public ExprIdentNode[] getAdditionalLeftNodes() {
        return optAddLeftNode;
    }

    /**
     * Returns additional properties in the on-clause, if any, that are connected via logical-and
     *
     * @return additional properties
     */
    public ExprIdentNode[] getAdditionalRightNodes() {
        return optAddRightNode;
    }

    public ExprNode makeExprNode(StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        ExprNode representativeNode = new ExprEqualsNodeImpl(false, false);
        representativeNode.addChildNode(optLeftNode);
        representativeNode.addChildNode(optRightNode);

        if (optAddLeftNode == null) {
            topValidate(representativeNode, statementRawInfo, compileTimeServices);
            return representativeNode;
        }

        ExprAndNode andNode = new ExprAndNodeImpl();
        topValidate(representativeNode, statementRawInfo, compileTimeServices);
        andNode.addChildNode(representativeNode);
        representativeNode = andNode;

        for (int i = 0; i < optAddLeftNode.length; i++) {
            ExprEqualsNode eqNode = new ExprEqualsNodeImpl(false, false);
            eqNode.addChildNode(optAddLeftNode[i]);
            eqNode.addChildNode(optAddRightNode[i]);
            topValidate(eqNode, statementRawInfo, compileTimeServices);
            andNode.addChildNode(eqNode);
        }

        topValidate(andNode, statementRawInfo, compileTimeServices);
        return representativeNode;
    }

    public static boolean consistsOfAllInnerJoins(OuterJoinDesc[] outerJoinDescList) {
        for (OuterJoinDesc desc : outerJoinDescList) {
            if (desc.getOuterJoinType() != OuterJoinType.INNER) {
                return false;
            }
        }
        return true;
    }

    public static OuterJoinDesc[] toArray(Collection<OuterJoinDesc> expressions) {
        if (expressions.isEmpty()) {
            return EMPTY_OUTERJOIN_ARRAY;
        }
        return expressions.toArray(new OuterJoinDesc[expressions.size()]);
    }

    private void topValidate(ExprNode exprNode, StatementRawInfo statementRawInfo, StatementCompileTimeServices compileTimeServices) {
        try {
            ExprValidationContext validationContext = new ExprValidationContextBuilder(null, statementRawInfo, compileTimeServices).build();
            exprNode.validate(validationContext);
        } catch (ExprValidationException e) {
            throw new IllegalStateException("Failed to make representative node for outer join criteria");
        }
    }

    public static boolean hasOnClauses(OuterJoinDesc[] outerJoinDescList) {
        for (OuterJoinDesc desc : outerJoinDescList) {
            if (desc.getOptLeftNode() != null) {
                return true;
            }
        }
        return false;
    }
}
