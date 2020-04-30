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

public class ExprNodeWithParentPair {
    private final ExprNode node;
    private final ExprNode parent;

    public ExprNodeWithParentPair(ExprNode node, ExprNode parent) {
        this.node = node;
        this.parent = parent;
    }

    public ExprNode getNode() {
        return node;
    }

    public ExprNode getParent() {
        return parent;
    }
}
