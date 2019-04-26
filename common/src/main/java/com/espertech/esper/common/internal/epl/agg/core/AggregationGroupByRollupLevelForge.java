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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;

import java.util.Arrays;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationGroupByRollupLevelForge {
    private final int levelNumber;
    private final int levelOffset;
    private final int[] rollupKeys;
    private final Class[] allGroupKeyTypes;
    private final MultiKeyClassRef allKeysMultikey;
    private final MultiKeyClassRef subKeyMultikey;

    public AggregationGroupByRollupLevelForge(int levelNumber, int levelOffset, int[] rollupKeys, Class[] allGroupKeyTypes, MultiKeyClassRef allKeysMultikey, MultiKeyClassRef subKeyMultikey) {
        this.levelNumber = levelNumber;
        this.levelOffset = levelOffset;
        this.rollupKeys = rollupKeys;
        this.allGroupKeyTypes = allGroupKeyTypes;
        this.allKeysMultikey = allKeysMultikey;
        this.subKeyMultikey = subKeyMultikey;
    }

    public int getAggregationOffset() {
        if (isAggregationTop()) {
            throw new IllegalArgumentException();
        }
        return levelOffset;
    }

    public CodegenExpression codegen(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationGroupByRollupLevel.class, this.getClass(), classScope);

        CodegenExpression serde = constantNull();
        if (rollupKeys != null) {
            if (allGroupKeyTypes.length == rollupKeys.length) {
                serde = allKeysMultikey.getExprMKSerde(method, classScope);
            } else {
                serde = subKeyMultikey.getExprMKSerde(method, classScope);
            }
        }

        CodegenExpressionNewAnonymousClass clazz = newAnonymousClass(method.getBlock(), AggregationGroupByRollupLevel.class,
            Arrays.asList(constant(levelNumber), constant(levelOffset), constant(rollupKeys), serde));

        CodegenMethod computeSubkey = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(Object.class, "groupKey");
        clazz.addMethod("computeSubkey", computeSubkey);

        if (isAggregationTop()) {
            computeSubkey.getBlock().methodReturn(constantNull());
        } else if (allKeysMultikey == null || allGroupKeyTypes.length == rollupKeys.length) {
            computeSubkey.getBlock().methodReturn(ref("groupKey"));
        } else {
            computeSubkey.getBlock()
                .declareVar(allKeysMultikey.getClassNameMK(), "mk", cast(allKeysMultikey.getClassNameMK(), ref("groupKey")));
            if (rollupKeys.length == 1 && (subKeyMultikey == null || subKeyMultikey.getClassNameMK() == null)) {
                computeSubkey.getBlock().methodReturn(exprDotMethod(ref("mk"), "getKey", constant(rollupKeys[0])));
            } else {
                CodegenExpression[] expressions = new CodegenExpression[rollupKeys.length];
                for (int i = 0; i < rollupKeys.length; i++) {
                    int index = rollupKeys[i];
                    CodegenExpression keyExpr = exprDotMethod(ref("mk"), "getKey", constant(index));
                    expressions[i] = cast(allGroupKeyTypes[index], keyExpr);
                }
                computeSubkey.getBlock().methodReturn(newInstance(subKeyMultikey.getClassNameMK(), expressions));
            }
        }

        method.getBlock().methodReturn(clazz);
        return localMethod(method);
    }

    public boolean isAggregationTop() {
        return levelOffset == -1;
    }

    public int[] getRollupKeys() {
        return rollupKeys;
    }
}
