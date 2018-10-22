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
package com.espertech.esper.common.internal.epl.lookup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.io.StringWriter;
import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AdvancedIndexIndexMultiKeyPart {
    private final String indexTypeName;
    private final String[] indexExpressions;
    private final String[] indexedProperties;

    public AdvancedIndexIndexMultiKeyPart(String indexTypeName, String[] indexExpressions, String[] indexedProperties) {
        this.indexTypeName = indexTypeName;
        this.indexExpressions = indexExpressions;
        this.indexedProperties = indexedProperties;
    }

    public String getIndexTypeName() {
        return indexTypeName;
    }

    public String[] getIndexExpressions() {
        return indexExpressions;
    }

    public String[] getIndexedProperties() {
        return indexedProperties;
    }

    public boolean equalsAdvancedIndex(AdvancedIndexIndexMultiKeyPart that) {
        return indexTypeName.equals(that.indexTypeName) && Arrays.equals(indexExpressions, that.indexExpressions);
    }

    public String toQueryPlan() {
        if (indexExpressions.length == 0) {
            return indexTypeName;
        }
        StringWriter writer = new StringWriter();
        writer.append(indexTypeName);
        writer.append("(");
        writer.append(String.join(",", indexExpressions));
        writer.append(")");
        return writer.toString();
    }

    public CodegenExpression codegenMake(CodegenMethodScope parent, CodegenClassScope classScope) {
        return newInstance(AdvancedIndexIndexMultiKeyPart.class, constant(indexTypeName), constant(indexExpressions), constant(indexedProperties));
    }
}
