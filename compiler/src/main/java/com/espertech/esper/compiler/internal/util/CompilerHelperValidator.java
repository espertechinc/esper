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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprSubstitutionNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilerHelperValidator {
    public static void verifySubstitutionParams(List<ExprSubstitutionNode> substitutionParameters) throws ExprValidationException {
        if (substitutionParameters.isEmpty()) {
            return;
        }
        Map<String, EPTypeClass> named = new HashMap<>();
        List<EPTypeClass> unnamed = new ArrayList<>();

        for (ExprSubstitutionNode node : substitutionParameters) {
            if (node.getOptionalName() != null) {
                String name = node.getOptionalName();
                EPTypeClass existing = named.get(name);
                if (existing == null) {
                    named.put(name, node.getResolvedType());
                } else {
                    if (!JavaClassHelper.isSubclassOrImplementsInterface(node.getResolvedType(), existing)) {
                        throw new ExprValidationException("Substitution parameter '" + name + "' incompatible type assignment between types '" + existing + "' and '" + node.getResolvedType() + "'");
                    }
                }
            } else {
                unnamed.add(node.getResolvedType());
            }
        }

        if (!unnamed.isEmpty() && !named.isEmpty()) {
            throw new ExprValidationException("Inconsistent use of substitution parameters, expecting all substitutions to either all provide a name or provide no name");
        }
    }

}
