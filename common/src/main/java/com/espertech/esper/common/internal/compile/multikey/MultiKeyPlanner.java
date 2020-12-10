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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.collection.*;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeSingleton;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.serdeset.multikey.DIOMultiKeyArraySerde;
import com.espertech.esper.common.internal.serde.serdeset.multikey.DIOMultiKeyArraySerdeFactory;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiKeyPlanner {
    public static boolean requiresDeepEquals(Class arrayComponentType) {
        return arrayComponentType == Object.class || arrayComponentType.isArray();
    }

    public static MultiKeyPlan planMultiKeyDistinct(boolean isDistinct, EventType eventType, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        if (!isDistinct) {
            return new MultiKeyPlan(Collections.emptyList(), MultiKeyClassRefEmpty.INSTANCE);
        }
        String[] propertyNames = eventType.getPropertyNames();
        EPType[] props = new EPType[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            props[i] = eventType.getPropertyEPType(propertyNames[i]);
        }
        return planMultiKey(props, false, raw, serdeResolver);
    }

    public static MultiKeyPlan planMultiKey(ExprNode[] criteriaExpressions, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals, raw, serdeResolver);
    }

    public static MultiKeyPlan planMultiKey(ExprForge[] criteriaExpressions, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals, raw, serdeResolver);
    }

    public static EPTypeClass getMKClassForComponentType(EPTypeClass componentType) {
        Class componentClass = componentType.getType();
        if (componentClass == boolean.class) {
            return MultiKeyArrayBoolean.EPTYPE;
        } else if (componentClass == byte.class) {
            return MultiKeyArrayByte.EPTYPE;
        } else if (componentClass == char.class) {
            return MultiKeyArrayChar.EPTYPE;
        } else if (componentClass == short.class) {
            return MultiKeyArrayShort.EPTYPE;
        } else if (componentClass == int.class) {
            return MultiKeyArrayInt.EPTYPE;
        } else if (componentClass == long.class) {
            return MultiKeyArrayLong.EPTYPE;
        } else if (componentClass == float.class) {
            return MultiKeyArrayFloat.EPTYPE;
        } else if (componentClass == double.class) {
            return MultiKeyArrayDouble.EPTYPE;
        }
        return MultiKeyArrayObject.EPTYPE;
    }

    public static DIOMultiKeyArraySerde getMKSerdeClassForComponentType(EPTypeClass componentType) {
        Class componentClass = componentType.getType();
        if (componentClass.isPrimitive()) {
            return DIOMultiKeyArraySerdeFactory.getSerde(componentClass);
        }
        return DIOMultiKeyArraySerdeFactory.getSerde(Object.class);
    }

    public static MultiKeyPlan planMultiKey(EPType[] types, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        if (types == null || types.length == 0) {
            return new MultiKeyPlan(Collections.emptyList(), MultiKeyClassRefEmpty.INSTANCE);
        }

        if (types.length == 1) {
            EPType paramType = types[0];
            if (paramType == null || paramType == EPTypeNull.INSTANCE || !((EPTypeClass) paramType).getType().isArray()) {
                DataInputOutputSerdeForge serdeForge = serdeResolver.serdeForKeyNonArray(paramType, raw);
                return new MultiKeyPlan(Collections.emptyList(), new MultiKeyClassRefWSerde(serdeForge, types));
            }
            EPTypeClass componentType = JavaClassHelper.getArrayComponentType((EPTypeClass) paramType);
            EPTypeClass mkClass = getMKClassForComponentType(componentType);
            DIOMultiKeyArraySerde mkSerde = getMKSerdeClassForComponentType(componentType);
            return new MultiKeyPlan(Collections.emptyList(), new MultiKeyClassRefPredetermined(mkClass, types, new DataInputOutputSerdeForgeSingleton(mkSerde.getClass()), mkSerde));
        }

        EPType[] boxed = new EPType[types.length];
        for (int i = 0; i < boxed.length; i++) {
            boxed[i] = JavaClassHelper.getBoxedType(types[i]);
        }
        DataInputOutputSerdeForge[] forges = serdeResolver.serdeForMultiKey(boxed, raw);

        MultiKeyClassRefUUIDBased classNames = new MultiKeyClassRefUUIDBased(boxed, forges);
        StmtClassForgeableFactory factoryMK = (packageScope, classPostfix) -> new StmtClassForgeableMultiKey(classNames.getClassNameMK(classPostfix), packageScope, types, lenientEquals);
        StmtClassForgeableFactory factoryMKSerde = (packageScope, classPostfix) -> new StmtClassForgeableMultiKeySerde(classNames.getClassNameMKSerde(classPostfix), packageScope, types, classNames.getClassNameMK(classPostfix), forges);

        List<StmtClassForgeableFactory> forgeables = Arrays.asList(factoryMK, factoryMKSerde);
        return new MultiKeyPlan(forgeables, classNames);
    }

    public static Object toMultiKey(Object keyValue) {
        Class componentType = keyValue.getClass().getComponentType();
        if (componentType == boolean.class) {
            return new MultiKeyArrayBoolean((boolean[]) keyValue);
        } else if (componentType == byte.class) {
            return new MultiKeyArrayByte((byte[]) keyValue);
        } else if (componentType == char.class) {
            return new MultiKeyArrayChar((char[]) keyValue);
        } else if (componentType == short.class) {
            return new MultiKeyArrayShort((short[]) keyValue);
        } else if (componentType == int.class) {
            return new MultiKeyArrayInt((int[]) keyValue);
        } else if (componentType == long.class) {
            return new MultiKeyArrayLong((long[]) keyValue);
        } else if (componentType == float.class) {
            return new MultiKeyArrayFloat((float[]) keyValue);
        } else if (componentType == double.class) {
            return new MultiKeyArrayDouble((double[]) keyValue);
        }
        return new MultiKeyArrayObject((Object[]) keyValue);
    }
}
