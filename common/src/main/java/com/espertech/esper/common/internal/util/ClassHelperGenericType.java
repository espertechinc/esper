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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;

import java.lang.reflect.*;

public class ClassHelperGenericType {
    public static EPTypeClass getFieldEPType(Field field) {
        return topLevel(field.getType(), field.getGenericType(), null);
    }

    public static EPTypeClass getFieldEPType(Field field, EPTypeClass type) {
        return topLevel(field.getType(), field.getGenericType(), type);
    }

    public static EPTypeClass getMethodReturnEPType(Method method) {
        return topLevel(method.getReturnType(), method.getGenericReturnType(), null);
    }

    public static EPTypeClass getMethodReturnEPType(Method method, EPTypeClass typeClass) {
        return topLevel(method.getReturnType(), method.getGenericReturnType(), typeClass);
    }

    private static EPTypeClass topLevel(Class rt, Type t, EPTypeClass typeClass) {
        if (t instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) t;
            return parameterizedTypeEPType(ptype, typeClass);
        }
        if (t instanceof GenericArrayType) {
            GenericArrayType ga = (GenericArrayType) t;
            return genericArrayEPType(ga, typeClass);
        }
        if (t instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) t;
            return typeVariableEPType(rt, tv, typeClass);
        }
        return EPTypePremade.getOrCreate(rt);
    }

    private static EPTypeClass typeVariableEPType(Class rt, TypeVariable tv, EPTypeClass typeClass) {
        if (!(typeClass instanceof EPTypeClassParameterized)) {
            return EPTypePremade.getOrCreate(rt);
        }
        TypeVariable[] declared = tv.getGenericDeclaration().getTypeParameters();
        int found = -1;
        for (int i = 0; i < declared.length; i++) {
            if (tv.getName().equals(declared[i].getName())) {
                if (found != -1) {
                    throw new IllegalStateException("Found type variable '" + tv.getName() + "' multiple times");
                }
                found = i;
            }
        }
        if (found == -1) {
            throw new IllegalStateException("Could not find type variable '" + tv.getName() + "'");
        }
        EPTypeClassParameterized parameterized = (EPTypeClassParameterized) typeClass;
        if (found >= parameterized.getParameters().length) {
            throw new IllegalStateException("Encountered type variable '" + tv.getName() + "' for index " + found + " but type '" + typeClass.toFullName() + "' has only " + parameterized.getParameters().length + " parameters");
        }
        return parameterized.getParameters()[found];
    }

    private static EPTypeClass genericArrayEPType(GenericArrayType ga, EPTypeClass typeClass) {
        Type component = ga.getGenericComponentType();
        EPTypeClass type = typeEPType(component, typeClass);
        Class arrayType = JavaClassHelper.getArrayType(type.getType());
        if (type instanceof EPTypeClassParameterized) {
            EPTypeClassParameterized parameterized = (EPTypeClassParameterized) type;
            return new EPTypeClassParameterized(arrayType, parameterized.getParameters());
        }
        return new EPTypeClass(arrayType);
    }

    private static EPTypeClass parameterizedTypeEPType(ParameterizedType ptype, EPTypeClass typeClass) {
        if (!(ptype.getRawType() instanceof Class)) {
            return EPTypePremade.OBJECT.getEPType();
        }

        Class raw = (Class) ptype.getRawType();
        Type[] typeArgs = ptype.getActualTypeArguments();
        if (typeArgs == null || typeArgs.length == 0) {
            return EPTypePremade.getOrCreate(raw);
        }

        EPTypeClass[] parameters = new EPTypeClass[typeArgs.length];
        for (int i = 0; i < typeArgs.length; i++) {
            EPTypeClass epType = typeEPType(typeArgs[i], typeClass);
            parameters[i] = epType;
        }
        return new EPTypeClassParameterized(raw, parameters);
    }

    private static EPTypeClass typeEPType(Type type, EPTypeClass typeClass) {
        if (type instanceof Class) {
            return EPTypePremade.getOrCreate((Class) type);
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            if (wt.getUpperBounds() != null && wt.getUpperBounds().length == 1 && (wt.getLowerBounds() == null || wt.getLowerBounds().length == 0)) {
                return typeEPType(wt.getUpperBounds()[0], typeClass);
            }
            return EPTypePremade.OBJECT.getEPType();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return parameterizedTypeEPType(pt, typeClass);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType ga = (GenericArrayType) type;
            return genericArrayEPType(ga, typeClass);
        } else if (type instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) type;
            return typeVariableEPType(Object.class, tv, typeClass);
        } else {
            return EPTypePremade.OBJECT.getEPType();
        }
    }

    public static EPTypeClass getParameterType(Parameter param) {
        return EPTypePremade.getOrCreate(param.getType());
    }

    public static EPTypeClass getClassEPType(Class clazz) {
        return EPTypePremade.getOrCreate(clazz);
    }

    public static EPTypeClass[] getParameterTypes(Parameter[] parameters, int offset) {
        EPTypeClass[] classes = new EPTypeClass[parameters.length - offset];
        int index = 0;
        for (int i = offset; i < parameters.length; i++) {
            classes[index++] = getParameterType(parameters[i]);
        }
        return classes;
    }
}
