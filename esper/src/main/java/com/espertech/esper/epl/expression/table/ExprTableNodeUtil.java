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
package com.espertech.esper.epl.expression.table;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.util.JavaClassHelper;

public class ExprTableNodeUtil {
    public static void validateExpressions(String tableName,
                                           Class[] providedTypes,
                                           String providedName,
                                           ExprNode[] providedExpr,
                                           Class[] expectedTypes,
                                           String expectedName
    ) throws ExprValidationException {
        if (expectedTypes.length != providedTypes.length) {
            String actual = (providedTypes.length == 0 ?
                    "no" : "" + providedTypes.length) + " " + providedName + " expressions";
            String expected = (expectedTypes.length == 0 ?
                    "no" : "" + expectedTypes.length) + " " + expectedName + " expressions";
            throw new ExprValidationException("Incompatible number of " +
                    providedName +
                    " expressions for use with table '" +
                    tableName +
                    "', the table expects " +
                    expected +
                    " and provided are " +
                    actual);
        }

        for (int i = 0; i < expectedTypes.length; i++) {
            Class actual = JavaClassHelper.getBoxedType(providedTypes[i]);
            Class expected = JavaClassHelper.getBoxedType(expectedTypes[i]);
            if (!JavaClassHelper.isSubclassOrImplementsInterface(actual, expected)) {
                throw new ExprValidationException("Incompatible type returned by a " +
                        providedName +
                        " expression for use with table '" +
                        tableName +
                        "', the " + providedName + " expression '" +
                        ExprNodeUtilityCore.toExpressionStringMinPrecedenceAsList(providedExpr) + "' returns '" +
                        JavaClassHelper.getClassNameFullyQualPretty(actual) + "' but the table expects '" +
                        JavaClassHelper.getClassNameFullyQualPretty(expected) + "'");
            }
        }
    }
}
