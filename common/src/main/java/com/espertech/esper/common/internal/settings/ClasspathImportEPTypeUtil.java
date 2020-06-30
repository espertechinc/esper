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
package com.espertech.esper.common.internal.settings;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedClasspathExtension;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.type.ClassDescriptor;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClasspathImportEPTypeUtil {
    public static EPTypeClass resolveClassIdentifierToEPType(ClassDescriptor classIdent, boolean allowObjectType, ClasspathImportService classpathImportService, ClasspathExtensionClass classpathExtension) throws ExprValidationException {
        String typeName = classIdent.getClassIdentifier();

        if (classIdent.isArrayOfPrimitive()) {
            Class primitive = JavaClassHelper.getPrimitiveClassForName(typeName);
            if (primitive != null) {
                EPTypeClass type = ClassHelperGenericType.getClassEPType(primitive);
                return JavaClassHelper.getArrayType(type, classIdent.getArrayDimensions());
            }
            throw new ExprValidationException("Type '" + typeName + "' is not a primitive type");
        }

        Class plain = JavaClassHelper.getClassForSimpleName(typeName, classpathImportService.getClassForNameProvider());
        if (plain != null) {
            return parameterizeType(plain, classIdent.getTypeParameters(), classIdent.getArrayDimensions(), classpathImportService, classpathExtension);
        }

        if (allowObjectType && typeName.toLowerCase(Locale.ENGLISH).equals("object")) {
            return EPTypePremade.OBJECT.getEPType();
        }

        // try imports first
        Class resolved = null;
        try {
            resolved = classpathImportService.resolveClass(typeName, false, classpathExtension);
        } catch (ClasspathImportException e) {
            // expected
        }

        String lowercase = typeName.toLowerCase(Locale.ENGLISH);
        if (lowercase.equals("biginteger")) {
            return JavaClassHelper.getArrayType(EPTypePremade.BIGINTEGER.getEPType(), classIdent.getArrayDimensions());
        }
        if (lowercase.equals("bigdecimal")) {
            return JavaClassHelper.getArrayType(EPTypePremade.BIGDECIMAL.getEPType(), classIdent.getArrayDimensions());
        }

        // resolve from classpath when not found
        if (resolved == null) {
            try {
                resolved = JavaClassHelper.getClassForName(typeName, classpathImportService.getClassForNameProvider());
            } catch (ClassNotFoundException e) {
                // expected
            }
        }

        // Handle resolved classes here
        if (resolved != null) {
            return parameterizeType(resolved, classIdent.getTypeParameters(), classIdent.getArrayDimensions(), classpathImportService, classpathExtension);
        }

        return null;
    }

    public static EPTypeClass parameterizeType(boolean allowArrayDimensions, Class clazz, ClassDescriptor descriptor, ClasspathImportService classpathImportService, ClassProvidedClasspathExtension classpathExtension) throws ExprValidationException {
        if (descriptor.getArrayDimensions() != 0 && !allowArrayDimensions) {
            throw new ExprValidationException("Array dimensions are not allowed");
        }
        Class classArrayed = clazz;
        if (descriptor.getArrayDimensions() > 0) {
            classArrayed = JavaClassHelper.getArrayType(clazz, descriptor.getArrayDimensions());
        }
        if (descriptor.getTypeParameters().isEmpty()) {
            return ClassHelperGenericType.getClassEPType(classArrayed);
        }
        TypeVariable[] variables = clazz.getTypeParameters();
        if (variables.length != descriptor.getTypeParameters().size()) {
            throw new ExprValidationException("Number of type parameters mismatch, the class '" + clazz.getName() + "' has " + variables.length + " type parameters but specified are " + descriptor.getTypeParameters().size() + " type parameters");
        }
        EPTypeClass[] parameters = new EPTypeClass[variables.length];
        for (int i = 0; i < descriptor.getTypeParameters().size(); i++) {
            ClassDescriptor desc = descriptor.getTypeParameters().get(i);
            Class inner;
            try {
                inner = classpathImportService.resolveClass(desc.getClassIdentifier(), false, classpathExtension);
            } catch (ClasspathImportException e) {
                throw new ExprValidationException("Failed to resolve type parameter " + i + " of type '" + desc.toEPL() + "': " + e.getMessage(), e);
            }
            TypeVariable tv = variables[i];
            Type[] bounds = tv.getBounds();
            if (bounds != null && bounds.length > 0) {
                for (Type bound : bounds) {
                    if (bound instanceof Class) {
                        Class boundClass = (Class) bound;
                        if (!JavaClassHelper.isSubclassOrImplementsInterface(inner, boundClass)) {
                            throw new ExprValidationException("Bound type parameters " + i + " named '" + tv.getName() + "' expects '" + boundClass.getName() + "' but receives '" + inner.getName() + "'");
                        }
                    } else {
                        throw new ExprValidationException("Bound type parameters are not supported for type parameter " + i + " named '" + tv.getName() + "' bound by '" + bound.toString() + "'");
                    }
                }
            }
            EPTypeClass parameterized = parameterizeType(true, inner, desc, classpathImportService, classpathExtension);
            parameters[i] = parameterized;
        }
        return new EPTypeClassParameterized(classArrayed, parameters);
    }

    public static EPTypeClass parameterizeType(Class plain, List<ClassDescriptor> typeParameters, int arrayDimensions, ClasspathImportService classpathImportService, ClasspathExtensionClass classpathExtension)
            throws ExprValidationException {
        if (typeParameters.isEmpty()) {
            Class clazz = JavaClassHelper.getArrayType(plain, arrayDimensions);
            return new EPTypeClass(clazz);
        }

        List<EPTypeClass> types = new ArrayList<>(typeParameters.size());
        for (int i = 0; i < typeParameters.size(); i++) {
            ClassDescriptor typeParam = typeParameters.get(i);
            EPTypeClass type = resolveClassIdentifierToEPType(typeParam, false, classpathImportService, classpathExtension);
            if (type == null) {
                throw new ExprValidationException("Failed to resolve type parameter '" + typeParam.toEPL() + "'");
            }
            types.add(type);
        }
        plain = JavaClassHelper.getArrayType(plain, arrayDimensions);
        return new EPTypeClassParameterized(plain, types.toArray(new EPTypeClass[0]));
    }
}
