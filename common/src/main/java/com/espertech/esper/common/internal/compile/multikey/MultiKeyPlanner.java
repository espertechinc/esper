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
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.collection.*;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForgeSingleton;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.serde.serdeset.multikey.*;
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
        Class[] props = new Class[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            props[i] = eventType.getPropertyType(propertyNames[i]);
        }
        return planMultiKey(props, false, raw, serdeResolver);
    }

    public static MultiKeyPlan planMultiKey(ExprNode[] criteriaExpressions, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals, raw, serdeResolver);
    }

    public static MultiKeyPlan planMultiKey(ExprForge[] criteriaExpressions, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals, raw, serdeResolver);
    }

    public static Class getMKClassForComponentType(Class componentType) {
        if (componentType == boolean.class) {
            return MultiKeyArrayBoolean.class;
        } else if (componentType == byte.class) {
            return MultiKeyArrayByte.class;
        } else if (componentType == char.class) {
            return MultiKeyArrayChar.class;
        } else if (componentType == short.class) {
            return MultiKeyArrayShort.class;
        } else if (componentType == int.class) {
            return MultiKeyArrayInt.class;
        } else if (componentType == long.class) {
            return MultiKeyArrayLong.class;
        } else if (componentType == float.class) {
            return MultiKeyArrayFloat.class;
        } else if (componentType == double.class) {
            return MultiKeyArrayDouble.class;
        }
        return MultiKeyArrayObject.class;
    }

    public static DataInputOutputSerde getMKSerdeClassForComponentType(Class componentType) {
        if (componentType == boolean.class) {
            return DIOMultiKeyArrayBooleanSerde.INSTANCE;
        } else if (componentType == byte.class) {
            return DIOMultiKeyArrayByteSerde.INSTANCE;
        } else if (componentType == char.class) {
            return DIOMultiKeyArrayCharSerde.INSTANCE;
        } else if (componentType == short.class) {
            return DIOMultiKeyArrayShortSerde.INSTANCE;
        } else if (componentType == int.class) {
            return DIOMultiKeyArrayIntSerde.INSTANCE;
        } else if (componentType == long.class) {
            return DIOMultiKeyArrayLongSerde.INSTANCE;
        } else if (componentType == float.class) {
            return DIOMultiKeyArrayFloatSerde.INSTANCE;
        } else if (componentType == double.class) {
            return DIOMultiKeyArrayDoubleSerde.INSTANCE;
        }
        return DIOMultiKeyArrayObjectSerde.INSTANCE;
    }

    public static MultiKeyPlan planMultiKey(Class[] types, boolean lenientEquals, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        if (types == null || types.length == 0) {
            return new MultiKeyPlan(Collections.emptyList(), MultiKeyClassRefEmpty.INSTANCE);
        }

        if (types.length == 1) {
            Class paramType = types[0];
            if (paramType == null || !paramType.isArray()) {
                DataInputOutputSerdeForge serdeForge = serdeResolver.serdeForKeyNonArray(paramType, raw);
                return new MultiKeyPlan(Collections.emptyList(), new MultiKeyClassRefWSerde(serdeForge, types));
            }
            Class mkClass = getMKClassForComponentType(paramType.getComponentType());
            DataInputOutputSerde mkSerde = getMKSerdeClassForComponentType(paramType.getComponentType());
            return new MultiKeyPlan(Collections.emptyList(), new MultiKeyClassRefPredetermined(mkClass, types, new DataInputOutputSerdeForgeSingleton(mkSerde.getClass())));
        }

        Class[] boxed = new Class[types.length];
        for (int i = 0; i < boxed.length; i++) {
            boxed[i] = JavaClassHelper.getBoxedType(types[i]);
        }
        MultiKeyClassRefUUIDBased classNames = new MultiKeyClassRefUUIDBased(boxed);
        StmtClassForgeableFactory factoryMK = new StmtClassForgeableFactory() {

            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableMultiKey(classNames.getClassNameMK(classPostfix), packageScope, types, lenientEquals);
            }
        };

        DataInputOutputSerdeForge[] forges = serdeResolver.serdeForMultiKey(types, raw);
        StmtClassForgeableFactory factoryMKSerde = new StmtClassForgeableFactory() {

            public StmtClassForgeable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgeableMultiKeySerde(classNames.getClassNameMKSerde(classPostfix), packageScope, types, classNames.getClassNameMK(classPostfix), forges);
            }
        };

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
