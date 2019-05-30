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
package com.espertech.esper.common.internal.bytecodemodel.base;

import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenSubstitutionParamEntry {
    private final CodegenField field;
    private final String name;
    private final Class type;

    public CodegenSubstitutionParamEntry(CodegenField field, String name, Class type) {
        this.field = field;
        this.name = name;
        this.type = type;
    }

    public CodegenField getField() {
        return field;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
    }

    public static void codegenSetterMethod(CodegenClassScope classScope, CodegenMethod method) {
        List<CodegenSubstitutionParamEntry> numbered = classScope.getPackageScope().getSubstitutionParamsByNumber();
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = classScope.getPackageScope().getSubstitutionParamsByName();
        if (numbered.isEmpty() && named.isEmpty()) {
            return;
        }
        if (!numbered.isEmpty() && !named.isEmpty()) {
            throw new IllegalStateException("Both named and numbered substitution parameters are non-empty");
        }

        List<CodegenSubstitutionParamEntry> fields;
        if (!numbered.isEmpty()) {
            fields = numbered;
        } else {
            fields = new ArrayList<>(named.values());
        }

        method.getBlock().declareVar(int.class, "zidx", op(ref("index"), "-", constant(1)));
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("zidx"), fields.size(), false);
        for (int i = 0; i < blocks.length; i++) {
            CodegenSubstitutionParamEntry param = fields.get(i);
            blocks[i].assignRef(field(param.getField()), cast(JavaClassHelper.getBoxedType(param.getType()), ref("value")));
        }
    }
}
