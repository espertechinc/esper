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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.common.internal.epl.expression.ops.ExprOrNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterSpecCompilerIndexPlannerUnwindAndOr {
    public static List<ExprNode> unwindAndOr(List<ExprNode> nodes) {
        List<ExprNode> unwound = new ArrayList<>(nodes.size());
        for (ExprNode node : nodes) {
            ExprNode result = unwind(node);
            unwound.add(result);
        }
        return unwound;
    }

    private static ExprNode unwind(ExprNode node) {
        boolean isOr = node instanceof ExprOrNode;
        boolean isAnd = node instanceof ExprAndNode;
        if (!isOr && !isAnd) {
            return node;
        }

        boolean needsUnwind = false;
        for (ExprNode child : node.getChildNodes()) {
            if ((child instanceof ExprOrNode && isOr) || (child instanceof ExprAndNode && isAnd)) {
                needsUnwind = true;
                break;
            }
        }
        if (!needsUnwind) {
            return node;
        }

        if (isOr) {
            ExprOrNode unwound = new ExprOrNode();
            for (ExprNode child : node.getChildNodes()) {
                if (child instanceof ExprOrNode) {
                    for (ExprNode orChild : child.getChildNodes()) {
                        ExprNode unwoundChild = unwind(orChild);
                        if (unwoundChild instanceof ExprOrNode) {
                            unwound.addChildNodes(Arrays.asList(unwoundChild.getChildNodes()));
                        } else {
                            unwound.addChildNode(unwoundChild);
                        }
                    }
                } else {
                    unwound.addChildNode(unwind(child));
                }
            }
            return unwound;
        }

        ExprAndNode unwound = new ExprAndNodeImpl();
        for (ExprNode child : node.getChildNodes()) {
            if (child instanceof ExprAndNode) {
                for (ExprNode andChild : child.getChildNodes()) {
                    ExprNode unwoundChild = unwind(andChild);
                    if (unwoundChild instanceof ExprAndNode) {
                        unwound.addChildNodes(Arrays.asList(unwoundChild.getChildNodes()));
                    } else {
                        unwound.addChildNode(unwoundChild);
                    }
                }
            } else {
                unwound.addChildNode(unwind(child));
            }
        }
        return unwound;
    }
}
