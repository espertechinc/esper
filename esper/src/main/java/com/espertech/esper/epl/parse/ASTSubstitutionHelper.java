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
package com.espertech.esper.epl.parse;

import com.espertech.esper.epl.expression.core.ExprSubstitutionNode;

import java.util.List;

public class ASTSubstitutionHelper {
    public static void validateNewSubstitution(List<ExprSubstitutionNode> substitutionParamNodes, ExprSubstitutionNode substitutionNode) {
        if (substitutionParamNodes.isEmpty()) {
            return;
        }
        ExprSubstitutionNode first = substitutionParamNodes.get(0);
        if (substitutionNode.getIndex() != null && first.getIndex() == null) {
            throw getException();
        }
        if (substitutionNode.getName() != null && first.getName() == null) {
            throw getException();
        }
    }

    private static ASTWalkException getException() {
        return ASTWalkException.from("Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");
    }
}

