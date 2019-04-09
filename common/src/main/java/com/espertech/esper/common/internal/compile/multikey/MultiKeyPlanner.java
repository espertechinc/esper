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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.collection.*;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgable;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MultiKeyPlanner {
    public static boolean requiresDeepEquals(Class arrayComponentType) {
        return arrayComponentType == Object.class || arrayComponentType.isArray();
    }

    public static MultiKeyPlan planMultiKeyDistinct(boolean isDistinct, EventType eventType) {
        if (!isDistinct) {
            return new MultiKeyPlan(Collections.emptyList(), null);
        }
        String[] propertyNames = eventType.getPropertyNames();
        Class[] props = new Class[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            props[i] = eventType.getPropertyType(propertyNames[i]);
        }
        return planMultiKey(props, false);
    }

    public static MultiKeyPlan planMultiKey(ExprNode[] criteriaExpressions, boolean lenientEquals) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals);
    }

    public static MultiKeyPlan planMultiKey(ExprForge[] criteriaExpressions, boolean lenientEquals) {
        return planMultiKey(ExprNodeUtilityQuery.getExprResultTypes(criteriaExpressions), lenientEquals);
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

    public static MultiKeyPlan planMultiKey(Class[] criteriaExpressionTypes, boolean lenientEquals) {
        if (criteriaExpressionTypes == null || criteriaExpressionTypes.length == 0) {
            return new MultiKeyPlan(Collections.emptyList(), null);
        }

        if (criteriaExpressionTypes.length == 1) {
            Class paramType = criteriaExpressionTypes[0];
            if (paramType == null) {
                return new MultiKeyPlan(Collections.emptyList(), null);
            }
            if (!paramType.isArray()) {
                return new MultiKeyPlan(Collections.emptyList(), null);
            }
            Class mkClass = getMKClassForComponentType(paramType.getComponentType());
            return new MultiKeyPlan(Collections.emptyList(), new MultiKeyClassRefPredetermined(mkClass, criteriaExpressionTypes, null));
        }

        Class[] boxed = new Class[criteriaExpressionTypes.length];
        for (int i = 0; i < boxed.length; i++) {
            boxed[i] = JavaClassHelper.getBoxedType(criteriaExpressionTypes[i]);
        }
        MultiKeyClassRefUUIDBased classNames = new MultiKeyClassRefUUIDBased(boxed);
        StmtClassForgableFactory factoryMK = new StmtClassForgableFactory() {
            public StmtClassForgable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgableMultiKey(classNames.getClassNameMK(classPostfix), packageScope, criteriaExpressionTypes, lenientEquals);
            }
        };

        StmtClassForgableFactory factoryMKSerde = new StmtClassForgableFactory() {
            public StmtClassForgable make(CodegenPackageScope packageScope, String classPostfix) {
                return new StmtClassForgableMultiKeySerde(classNames.getClassNameMKSerde(classPostfix), packageScope, criteriaExpressionTypes, classNames.getClassNameMK(classPostfix));
            }
        };

        List<StmtClassForgableFactory> forgables = Arrays.asList(factoryMK, factoryMKSerde);
        return new MultiKeyPlan(forgables, classNames);
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
