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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSubstitutionParamEntry;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenRepetitiveValueBuilder;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class FAFQueryInformationals {
    public final static EPTypeClass EPTYPE = new EPTypeClass(FAFQueryInformationals.class);

    private final Class[] substitutionParamsTypes;
    private final Map<String, Integer> substitutionParamsNames;

    public FAFQueryInformationals(Class[] substitutionParamsTypes, Map<String, Integer> substitutionParamsNames) {
        this.substitutionParamsTypes = substitutionParamsTypes;
        this.substitutionParamsNames = substitutionParamsNames;
    }

    public static FAFQueryInformationals from(List<CodegenSubstitutionParamEntry> paramsByNumber, LinkedHashMap<String, CodegenSubstitutionParamEntry> paramsByName) {
        Class[] types;
        Map<String, Integer> names;
        if (!paramsByNumber.isEmpty()) {
            types = new Class[paramsByNumber.size()];
            for (int i = 0; i < paramsByNumber.size(); i++) {
                types[i] = paramsByNumber.get(i).getType().getType();
            }
            names = null;
        } else if (!paramsByName.isEmpty()) {
            types = new Class[paramsByName.size()];
            names = new HashMap<>();
            int index = 0;
            for (Map.Entry<String, CodegenSubstitutionParamEntry> entry : paramsByName.entrySet()) {
                types[index] = entry.getValue().getType().getType();
                names.put(entry.getKey(), index + 1);
                index++;
            }
        } else {
            types = null;
            names = null;
        }
        return new FAFQueryInformationals(types, names);
    }

    public Class[] getSubstitutionParamsTypes() {
        return substitutionParamsTypes;
    }

    public Map<String, Integer> getSubstitutionParamsNames() {
        return substitutionParamsNames;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        return newInstance(FAFQueryInformationals.EPTYPE, constant(substitutionParamsTypes), makeNames(parent, classScope));
    }

    private CodegenExpression makeNames(CodegenMethodScope parent, CodegenClassScope classScope) {
        if (substitutionParamsNames == null) {
            return constantNull();
        }
        CodegenMethod method = parent.makeChild(EPTypePremade.MAP.getEPType(), this.getClass(), classScope);
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "names", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(CollectionUtil.capacityHashMap(substitutionParamsNames.size()))));

        new CodegenRepetitiveValueBuilder<>(substitutionParamsNames.entrySet(), method, classScope, this.getClass())
                .addParam(EPTypePremade.MAP.getEPType(), "names")
                .setConsumer((entry, index, leaf)  -> {
                    leaf.getBlock().exprDotMethod(ref("names"), "put", constant(entry.getKey()), constant(entry.getValue()));
                })
                .build();
        method.getBlock().methodReturn(ref("names"));
        return localMethod(method);
    }
}
