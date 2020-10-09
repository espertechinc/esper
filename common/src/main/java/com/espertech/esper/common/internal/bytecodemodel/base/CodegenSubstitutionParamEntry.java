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

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenSubstitutionParamEntry {
    private final CodegenField field;
    private final String name;
    private final EPTypeClass type;

    public CodegenSubstitutionParamEntry(CodegenField field, String name, EPTypeClass type) {
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

    public EPTypeClass getType() {
        return type;
    }

    public static void codegenSetterMethod(CodegenClassScope classScope, CodegenMethod method) {
        int targetMethodComplexity = Math.max(64, classScope.getPackageScope().getConfig().getInternalUseOnlyMaxMethodComplexity());
        List<CodegenSubstitutionParamEntry> numbered = classScope.getPackageScope().getSubstitutionParamsByNumber();
        LinkedHashMap<String, CodegenSubstitutionParamEntry> named = classScope.getPackageScope().getSubstitutionParamsByName();
        if (!numbered.isEmpty() && !named.isEmpty()) {
            throw new IllegalStateException("Both named and numbered substitution parameters are non-empty");
        }

        List<CodegenSubstitutionParamEntry> fields;
        if (!numbered.isEmpty()) {
            fields = numbered;
        } else {
            fields = new ArrayList<>(named.values());
        }

        if (fields.size() <= targetMethodComplexity) {
            populateSet(method, fields, 0);
            return;
        }

        List<List<CodegenSubstitutionParamEntry>> assignments = CollectionUtil.subdivide(fields, targetMethodComplexity);
        List<CodegenMethod> leafs = new ArrayList<>(assignments.size());
        for (int i = 0; i < assignments.size(); i++) {
            List<CodegenSubstitutionParamEntry> assignment = assignments.get(i);
            CodegenMethod leaf = method.makeChild(EPTypePremade.VOID.getEPType(), CodegenSubstitutionParamEntry.class, classScope).addParam(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index").addParam(EPTypePremade.OBJECT.getEPType(), "value");
            populateSet(leaf, assignment, i * targetMethodComplexity);
            leafs.add(leaf);
        }

        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "lidx", op(op(ref("index"), "-", constant(1)), "/", constant(targetMethodComplexity)));
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("lidx"), assignments.size(), false);
        for (int i = 0; i < blocks.length; i++) {
            blocks[i].localMethod(leafs.get(i), ref("index"), ref("value"));
        }
    }

    private static void populateSet(CodegenMethod method, List<CodegenSubstitutionParamEntry> fields, int offset) {
        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "zidx", op(ref("index"), "-", constant(1)));
        CodegenBlock[] blocks = method.getBlock().switchBlockOfLength(ref("zidx"), fields.size(), false, offset);
        for (int i = 0; i < blocks.length; i++) {
            CodegenSubstitutionParamEntry param = fields.get(i);
            blocks[i].assignRef(field(param.getField()), cast(JavaClassHelper.getBoxedType(param.getType()), ref("value")));
        }
    }
}
