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
package com.espertech.esper.common.internal.event.json.compiletime;

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.bytecodemodel.model.statement.CodegenStatementSwitch;

import java.util.Map;
import java.util.NoSuchElementException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class StmtClassForgeableJsonUtil {
    static void makeNoSuchElementDefault(CodegenStatementSwitch switchStmt, CodegenExpressionRef num) {
        switchStmt.getDefaultBlock().blockThrow(newInstance(NoSuchElementException.class, concat(constant("Field at number "), num)));
    }

    static CodegenExpression[] getCasesNumberNtoM(StmtClassForgeableJsonDesc desc) {
        CodegenExpression[] cases = new CodegenExpression[desc.getPropertiesThisType().size()];
        int index = 0;
        for (Map.Entry<String, Object> property : desc.getPropertiesThisType().entrySet()) {
            JsonUnderlyingField field = desc.getFieldDescriptorsInclSupertype().get(property.getKey());
            cases[index] = constant(field.getPropertyNumber());
            index++;
        }
        return cases;
    }

}
