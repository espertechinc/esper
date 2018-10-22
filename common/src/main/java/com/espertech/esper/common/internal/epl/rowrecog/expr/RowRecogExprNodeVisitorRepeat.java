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
package com.espertech.esper.common.internal.epl.rowrecog.expr;

import com.espertech.esper.common.internal.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RowRecogExprNodeVisitorRepeat implements RowRecogExprNodeVisitor {
    private List<Pair<RowRecogExprNodeAtom, RowRecogExprNode>> atoms;
    private List<RowRegexNestedDesc> nesteds;
    private List<RowRegexPermuteDesc> permutes;

    public void visit(RowRecogExprNode node, RowRecogExprNode optionalParent, int level) {
        if (node instanceof RowRecogExprNodeAtom) {
            RowRecogExprNodeAtom atom = (RowRecogExprNodeAtom) node;
            if (atom.getOptionalRepeat() != null) {
                if (atoms == null) {
                    atoms = new ArrayList<Pair<RowRecogExprNodeAtom, RowRecogExprNode>>();
                }
                atoms.add(new Pair<RowRecogExprNodeAtom, RowRecogExprNode>(atom, optionalParent));
            }
        }
        if (node instanceof RowRecogExprNodeNested) {
            RowRecogExprNodeNested nested = (RowRecogExprNodeNested) node;
            if (nested.getOptionalRepeat() != null) {
                if (nesteds == null) {
                    nesteds = new ArrayList<RowRegexNestedDesc>();
                }
                nesteds.add(new RowRegexNestedDesc(nested, optionalParent, level));
            }
        }
        if (node instanceof RowRecogExprNodePermute) {
            RowRecogExprNodePermute permute = (RowRecogExprNodePermute) node;
            if (permutes == null) {
                permutes = new ArrayList<RowRegexPermuteDesc>();
            }
            permutes.add(new RowRegexPermuteDesc(permute, optionalParent, level));
        }
    }

    public List<Pair<RowRecogExprNodeAtom, RowRecogExprNode>> getAtoms() {
        if (atoms == null) {
            return Collections.emptyList();
        }
        return atoms;
    }

    public List<RowRegexNestedDesc> getNesteds() {
        if (nesteds == null) {
            return Collections.emptyList();
        }
        return nesteds;
    }

    public List<RowRegexPermuteDesc> getPermutes() {
        if (permutes == null) {
            return Collections.emptyList();
        }
        return permutes;
    }

    public static class RowRegexPermuteDesc {
        private final RowRecogExprNodePermute permute;
        private final RowRecogExprNode optionalParent;
        private final int level;

        public RowRegexPermuteDesc(RowRecogExprNodePermute permute, RowRecogExprNode optionalParent, int level) {
            this.permute = permute;
            this.optionalParent = optionalParent;
            this.level = level;
        }

        public RowRecogExprNodePermute getPermute() {
            return permute;
        }

        public RowRecogExprNode getOptionalParent() {
            return optionalParent;
        }

        public int getLevel() {
            return level;
        }
    }

    public static class RowRegexNestedDesc {
        private final RowRecogExprNodeNested nested;
        private final RowRecogExprNode optionalParent;
        private final int level;

        public RowRegexNestedDesc(RowRecogExprNodeNested nested, RowRecogExprNode optionalParent, int level) {
            this.nested = nested;
            this.optionalParent = optionalParent;
            this.level = level;
        }

        public RowRecogExprNodeNested getNested() {
            return nested;
        }

        public RowRecogExprNode getOptionalParent() {
            return optionalParent;
        }

        public int getLevel() {
            return level;
        }
    }
}
