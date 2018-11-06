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

import com.espertech.esper.common.client.annotation.*;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public abstract class ClasspathImportServiceBase implements ClasspathImportService {
    private static final Logger log = LoggerFactory.getLogger(ClasspathImportServiceBase.class);

    private final Map<String, Object> transientConfiguration;
    private final TimeAbacus timeAbacus;
    private final Set<String> eventTypeAutoNames;

    private final List<String> imports = new ArrayList<>();
    private final List<String> annotationImports = new ArrayList<>();

    public ClasspathImportServiceBase(Map<String, Object> transientConfiguration, TimeAbacus timeAbacus, Set<String> eventTypeAutoNames) {
        this.transientConfiguration = transientConfiguration;
        this.timeAbacus = timeAbacus;
        this.eventTypeAutoNames = eventTypeAutoNames;
    }

    public ClassLoader getClassLoader() {
        return TransientConfigurationResolver.resolveClassLoader(transientConfiguration).classloader();
    }

    public TimeAbacus getTimeAbacus() {
        return timeAbacus;
    }

    public void addImport(String importName) throws ClasspathImportException {
        validateImportAndAdd(importName, imports);
    }

    public void addAnnotationImport(String importName) throws ClasspathImportException {
        validateImportAndAdd(importName, annotationImports);
    }

    public Method resolveMethodOverloadChecked(String className, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType)
            throws ClasspathImportException {
        Class clazz;
        try {
            clazz = resolveClassInternal(className, false, false);
        } catch (ClassNotFoundException e) {
            throw new ClasspathImportException("Could not load class by name '" + className + "', please check imports", e);
        }

        try {
            return MethodResolver.resolveMethod(clazz, methodName, paramTypes, false, allowEventBeanType, allowEventBeanCollType);
        } catch (MethodResolverNoSuchMethodException e) {
            throw convert(clazz, methodName, paramTypes, e, false);
        }
    }

    public Class resolveClass(String className, boolean forAnnotation) throws ClasspathImportException {
        Class clazz;
        try {
            clazz = resolveClassInternal(className, false, forAnnotation);
        } catch (ClassNotFoundException e) {
            throw makeClassNotFoundEx(className, e);
        }

        return clazz;
    }

    public Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType)
            throws ClasspathImportException {
        try {
            return MethodResolver.resolveMethod(clazz, methodName, paramTypes, true, allowEventBeanType, allowEventBeanCollType);
        } catch (MethodResolverNoSuchMethodException e) {
            throw convert(clazz, methodName, paramTypes, e, true);
        }
    }

    public Constructor resolveCtor(Class clazz, Class[] paramTypes) throws ClasspathImportException {
        try {
            return MethodResolver.resolveCtor(clazz, paramTypes);
        } catch (MethodResolverNoSuchCtorException e) {
            throw convert(clazz, paramTypes, e);
        }
    }

    /**
     * Finds a class by class name using the auto-import information provided.
     *
     * @param className         is the class name to find
     * @param requireAnnotation whether the class must be an annotation
     * @param forAnnotationUse  whether resolving class for use with annotations
     * @return class
     * @throws ClassNotFoundException if the class cannot be loaded
     */
    protected Class resolveClassInternal(String className, boolean requireAnnotation, boolean forAnnotationUse) throws ClassNotFoundException {
        if (forAnnotationUse) {
            String lowercase = className.toLowerCase(Locale.ENGLISH);
            if (lowercase.equals("private")) {
                return Private.class;
            }
            if (lowercase.equals("protected")) {
                return Protected.class;
            }
            if (lowercase.equals("public")) {
                return Public.class;
            }
            if (lowercase.equals("buseventtype")) {
                return BusEventType.class;
            }
        }

        // Attempt to retrieve the class with the name as-is
        try {
            return getClassForNameProvider().classForName(className);
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Class not found for resolving from name as-is '" + className + "'");
            }
        }

        // check annotation-specific imports first
        if (forAnnotationUse) {
            Class clazz = checkImports(annotationImports, requireAnnotation, className);
            if (clazz != null) {
                return clazz;
            }
        }

        // check all imports
        Class clazz = checkImports(imports, requireAnnotation, className);
        if (clazz != null) {
            return clazz;
        }

        // No import worked, the class isn't resolved
        throw new ClassNotFoundException("Unknown class " + className);
    }

    public ClassForNameProvider getClassForNameProvider() {
        return TransientConfigurationResolver.resolveClassForNameProvider(transientConfiguration);
    }

    public Class resolveClassForBeanEventType(String fullyQualClassName) throws ClasspathImportException {
        try {
            return getClassForNameProvider().classForName(fullyQualClassName);
        } catch (ClassNotFoundException ex) {

            // Attempt to resolve from auto-name packages
            Class clazz = null;
            for (String javaPackageName : eventTypeAutoNames) {
                String generatedClassName = javaPackageName + "." + fullyQualClassName;
                try {
                    Class resolvedClass = getClassForNameProvider().classForName(generatedClassName);
                    if (clazz != null) {
                        throw new ClasspathImportException("Failed to resolve name '" + fullyQualClassName + "', the class was ambigously found both in " +
                                "package '" + clazz.getPackage().getName() + "' and in " +
                                "package '" + resolvedClass.getPackage().getName() + "'", ex);
                    }
                    clazz = resolvedClass;
                } catch (ClassNotFoundException ex1) {
                    // expected, class may not exists in all packages
                }
            }

            if (clazz != null) {
                return clazz;
            }

            return resolveClass(fullyQualClassName, false);
        }
    }

    private Class checkImports(List<String> imports, boolean requireAnnotation, String className) throws ClassNotFoundException {
        // Try all the imports
        for (String importName : imports) {
            boolean isClassName = isClassName(importName);
            boolean containsPackage = importName.indexOf('.') != -1;
            String classNameWithDot = "." + className;
            String classNameWithDollar = "$" + className;

            // Import is a class name
            if (isClassName) {
                if ((containsPackage && importName.endsWith(classNameWithDot)) ||
                        (containsPackage && importName.endsWith(classNameWithDollar)) ||
                        (!containsPackage && importName.equals(className)) ||
                        (!containsPackage && importName.endsWith(classNameWithDollar))) {
                    return getClassForNameProvider().classForName(importName);
                }

                String prefixedClassName = importName + '$' + className;
                try {
                    Class clazz = getClassForNameProvider().classForName(prefixedClassName);
                    if (!requireAnnotation || clazz.isAnnotation()) {
                        return clazz;
                    }
                } catch (ClassNotFoundException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Class not found for resolving from name '" + prefixedClassName + "'");
                    }
                }
            } else {
                if (requireAnnotation && importName.equals(ConfigurationCommon.ANNOTATION_IMPORT)) {
                    Class clazz = BuiltinAnnotation.BUILTIN.get(className.toLowerCase(Locale.ENGLISH));
                    if (clazz != null) {
                        return clazz;
                    }
                }

                // Import is a package name
                String prefixedClassName = getPackageName(importName) + '.' + className;
                try {
                    Class clazz = getClassForNameProvider().classForName(prefixedClassName);
                    if (!requireAnnotation || clazz.isAnnotation()) {
                        return clazz;
                    }
                } catch (ClassNotFoundException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Class not found for resolving from name '" + prefixedClassName + "'");
                    }
                }
            }
        }

        return null;
    }

    static boolean isClassName(String importName) {
        String classNameRegEx = "(\\w+\\.)*\\w+(\\$\\w+)?";
        return importName.matches(classNameRegEx);
    }

    static String getPackageName(String importName) {
        return importName.substring(0, importName.length() - 2);
    }

    private static boolean isPackageName(String importName) {
        String classNameRegEx = "(\\w+\\.)+\\*";
        return importName.matches(classNameRegEx);
    }

    protected void validateImportAndAdd(String importName, List<String> imports) throws ClasspathImportException {
        if (!isClassName(importName) && !isPackageName(importName)) {
            throw new ClasspathImportException("Invalid import name '" + importName + "'");
        }
        if (log.isDebugEnabled()) {
            log.debug("Adding import " + importName);
        }

        imports.add(importName);
    }

    protected ClasspathImportException makeClassNotFoundEx(String className, Exception e) {
        return new ClasspathImportException("Could not load class by name '" + className + "', please check imports", e);
    }

    protected ClasspathImportException convert(Class clazz, Class[] paramTypes, MethodResolverNoSuchCtorException e) {
        String expected = JavaClassHelper.getParameterAsString(paramTypes);
        String message = "Could not find constructor ";
        if (paramTypes.length > 0) {
            message += "in class '" + JavaClassHelper.getClassNameFullyQualPretty(clazz) + "' with matching parameter number and expected parameter type(s) '" + expected + "'";
        } else {
            message += "in class '" + JavaClassHelper.getClassNameFullyQualPretty(clazz) + "' taking no parameters";
        }

        if (e.getNearestMissCtor() != null) {
            message += " (nearest matching constructor ";
            if (e.getNearestMissCtor().getParameterTypes().length == 0) {
                message += "taking no parameters";
            } else {
                message += "taking type(s) '" + JavaClassHelper.getParameterAsString(e.getNearestMissCtor().getParameterTypes()) + "'";
            }
            message += ")";
        }
        return new ClasspathImportException(message, e);
    }

    protected ClasspathImportException convert(Class clazz, String methodName, Class[] paramTypes, MethodResolverNoSuchMethodException e, boolean isInstance) {
        String expected = JavaClassHelper.getParameterAsString(paramTypes);
        String message = "Could not find ";
        if (!isInstance) {
            message += "static ";
        } else {
            message += "enumeration method, date-time method or instance ";
        }

        if (paramTypes.length > 0) {
            message += "method named '" + methodName + "' in class '" + JavaClassHelper.getClassNameFullyQualPretty(clazz) + "' with matching parameter number and expected parameter type(s) '" + expected + "'";
        } else {
            message += "method named '" + methodName + "' in class '" + JavaClassHelper.getClassNameFullyQualPretty(clazz) + "' taking no parameters";
        }

        if (e.getNearestMissMethod() != null) {
            message += " (nearest match found was '" + e.getNearestMissMethod().getName();
            if (e.getNearestMissMethod().getParameterTypes().length == 0) {
                message += "' taking no parameters";
            } else {
                message += "' taking type(s) '" + JavaClassHelper.getParameterAsString(e.getNearestMissMethod().getParameterTypes()) + "'";
            }
            message += ")";
        }
        return new ClasspathImportException(message, e);
    }
}
