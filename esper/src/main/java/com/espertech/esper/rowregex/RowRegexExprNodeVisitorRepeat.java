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
package com.espertech.esper.rowregex;

import com.espertech.esper.collection.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RowRegexExprNodeVisitorRepeat implements RowRegexExprNodeVisitor {
    private List<Pair<RowRegexExprNodeAtom, RowRegexExprNode>> atoms;
    private List<RowRegexNestedDesc> nesteds;
    private List<RowRegexPermuteDesc> permutes;

    public void visit(RowRegexExprNode node, RowRegexExprNode optionalParent, int level) {
        if (node instanceof RowRegexExprNodeAtom) {
            RowRegexExprNodeAtom atom = (RowRegexExprNodeAtom) node;
            if (atom.getOptionalRepeat() != null) {
                if (atoms == null) {
                    atoms = new ArrayList<Pair<RowRegexExprNodeAtom, RowRegexExprNode>>();
                }
                atoms.add(new Pair<RowRegexExprNodeAtom, RowRegexExprNode>(atom, optionalParent));
            }
        }
        if (node instanceof RowRegexExprNodeNested) {
            RowRegexExprNodeNested nested = (RowRegexExprNodeNested) node;
            if (nested.getOptionalRepeat() != null) {
                if (nesteds == null) {
                    nesteds = new ArrayList<RowRegexNestedDesc>();
                }
                nesteds.add(new RowRegexNestedDesc(nested, optionalParent, level));
            }
        }
        if (node instanceof RowRegexExprNodePermute) {
            RowRegexExprNodePermute permute = (RowRegexExprNodePermute) node;
            if (permutes == null) {
                permutes = new ArrayList<RowRegexPermuteDesc>();
            }
            permutes.add(new RowRegexPermuteDesc(permute, optionalParent, level));
        }
    }

    public List<Pair<RowRegexExprNodeAtom, RowRegexExprNode>> getAtoms() {
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
        private final RowRegexExprNodePermute permute;
        private final RowRegexExprNode optionalParent;
        private final int level;

        public RowRegexPermuteDesc(RowRegexExprNodePermute permute, RowRegexExprNode optionalParent, int level) {
            this.permute = permute;
            this.optionalParent = optionalParent;
            this.level = level;
        }

        public RowRegexExprNodePermute getPermute() {
            return permute;
        }

        public RowRegexExprNode getOptionalParent() {
            return optionalParent;
        }

        public int getLevel() {
            return level;
        }
    }

    public static class RowRegexNestedDesc {
        private final RowRegexExprNodeNested nested;
        private final RowRegexExprNode optionalParent;
        private final int level;

        public RowRegexNestedDesc(RowRegexExprNodeNested nested, RowRegexExprNode optionalParent, int level) {
            this.nested = nested;
            this.optionalParent = optionalParent;
            this.level = level;
        }

        public RowRegexExprNodeNested getNested() {
            return nested;
        }

        public RowRegexExprNode getOptionalParent() {
            return optionalParent;
        }

        public int getLevel() {
            return level;
        }
    }
}
