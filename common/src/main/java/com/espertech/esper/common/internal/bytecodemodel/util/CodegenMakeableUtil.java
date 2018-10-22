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
package com.espertech.esper.common.internal.bytecodemodel.util;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenMakeableUtil {

    public static <T extends CodegenMakeable> CodegenExpression makeArray(String name, Class clazz, T[] forges, Class generator, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        Class arrayType = JavaClassHelper.getArrayType(clazz);
        if (forges == null || forges.length == 0) {
            return newArrayByLength(clazz, constant(0));
        }
        CodegenMethod method = parent.makeChild(arrayType, generator, classScope);
        method.getBlock().declareVar(arrayType, name, newArrayByLength(clazz, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            method.getBlock().assignArrayElement(ref(name), constant(i), forges[i] == null ? constantNull() : forges[i].make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref(name));
        return localMethod(method);
    }

    public static <K extends CodegenMakeable, V extends CodegenMakeable> CodegenExpression makeMap(String name, Class clazzKey, Class clazzValue, Map<K, V> map, Class generator, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        if (map.isEmpty()) {
            return staticMethod(Collections.class, "emptyMap");
        }
        CodegenMethod method = parent.makeChild(Map.class, generator, classScope);
        int count = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            String nameKey = "key" + count;
            String nameValue = "value" + count;
            method.getBlock()
                    .declareVar(clazzKey, nameKey, entry.getKey().make(method, symbols, classScope))
                    .declareVar(clazzValue, nameValue, entry.getValue().make(method, symbols, classScope));
            count++;
        }
        if (map.size() == 1) {
            method.getBlock().methodReturn(staticMethod(Collections.class, "singletonMap", ref("key0"), ref("value0")));
        } else {
            method.getBlock().declareVar(Map.class, name, newInstance(LinkedHashMap.class, constant(map.size())));
            for (int i = 0; i < map.size(); i++) {
                method.getBlock().exprDotMethod(ref(name), "put", ref("key" + i), ref("value" + i));
            }
            method.getBlock().methodReturn(ref(name));
        }
        return localMethod(method);
    }
}
