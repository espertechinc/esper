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
package com.espertech.esper.common.internal.epl.classprovided.compiletime;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.hook.aggfunc.ExtensionAggregationFunction;
import com.espertech.esper.common.client.hook.aggmultifunc.ExtensionAggregationMultiFunction;
import com.espertech.esper.common.client.hook.singlerowfunc.ExtensionSingleRowFunction;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

public class ClassProvidedClasspathExtensionImpl implements ClassProvidedClasspathExtension {
    private final ClassProvidedCompileTimeResolver resolver;
    private final List<Class> classes = new ArrayList<>(2);
    private final Map<String, byte[]> bytes = new LinkedHashMap<>();
    private Map<String, Pair<Class, ExtensionSingleRowFunction>> singleRowFunctionExtensions = Collections.emptyMap();
    private Map<String, Pair<Class, ExtensionAggregationFunction>> aggregationFunctionExtensions = Collections.emptyMap();
    private Map<String, Pair<Class, String[]>> aggregationMultiFunctionExtensions = Collections.emptyMap();

    public ClassProvidedClasspathExtensionImpl(ClassProvidedCompileTimeResolver resolver) {
        this.resolver = resolver;
    }

    public void add(List<Class> classes, Map<String, byte[]> bytes) throws ExprValidationException {
        this.classes.addAll(classes);
        this.bytes.putAll(bytes); // duplicate class names checked at compile-time

        try {
            JavaClassHelper.traverseAnnotations(classes, ExtensionSingleRowFunction.class, (clazz, annotation) -> {
                if (singleRowFunctionExtensions.isEmpty()) {
                    singleRowFunctionExtensions = new HashMap<>(2);
                }
                if (singleRowFunctionExtensions.containsKey(annotation.name())) {
                    throw new EPException("The plug-in single-row function '" + annotation.name() + "' occurs multiple times");
                }
                singleRowFunctionExtensions.put(annotation.name(), new Pair<>(clazz, annotation));
            });

            JavaClassHelper.traverseAnnotations(classes, ExtensionAggregationFunction.class, (clazz, annotation) -> {
                if (aggregationFunctionExtensions.isEmpty()) {
                    aggregationFunctionExtensions = new HashMap<>(2);
                }
                if (aggregationFunctionExtensions.containsKey(annotation.name())) {
                    throw new EPException("The plug-in aggregation function '" + annotation.name() + "' occurs multiple times");
                }
                aggregationFunctionExtensions.put(annotation.name(), new Pair<>(clazz, annotation));
            });

            JavaClassHelper.traverseAnnotations(classes, ExtensionAggregationMultiFunction.class, (clazz, annotation) -> {
                if (aggregationMultiFunctionExtensions.isEmpty()) {
                    aggregationMultiFunctionExtensions = new HashMap<>(2);
                }
                String[] names = annotation.names().split(",");
                Set<String> namesDeduplicated = new HashSet<>(names.length);
                for (String nameWithSpaces : names) {
                    String name = nameWithSpaces.trim();
                    namesDeduplicated.add(name);
                }
                String[] namesArray = namesDeduplicated.toArray(new String[0]);

                for (String name : namesDeduplicated) {
                    if (aggregationMultiFunctionExtensions.containsKey(name)) {
                        throw new EPException("The plug-in aggregation multi-function '" + name + "' occurs multiple times");
                    }
                    aggregationMultiFunctionExtensions.put(name, new Pair<>(clazz, namesArray));
                }
            });
        } catch (EPException ex) {
            throw new ExprValidationException(ex.getMessage(), ex);
        }
    }

    public Class findClassByName(String className) {
        // check inlined classes
        for (Class clazz : classes) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        // check same-module (create inlined_class) or path classes
        ClassProvided provided = resolver.resolveClass(className);
        if (provided != null) {
            for (Class clazz : provided.getClassesMayNull()) {
                if (clazz.getName().equals(className)) {
                    return clazz;
                }
            }
        }
        return null;
    }

    public Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) {
        // check local
        Pair<Class, ExtensionSingleRowFunction> pair = singleRowFunctionExtensions.get(name);
        if (pair != null) {
            return new Pair<>(pair.getFirst(), new ClasspathImportSingleRowDesc(pair.getFirst(), pair.getSecond()));
        }
        // check same-module (create inlined_class) or path classes
        return resolver.resolveSingleRow(name);
    }

    public Class resolveAggregationFunction(String name) {
        // check local
        Pair<Class, ExtensionAggregationFunction> pair = aggregationFunctionExtensions.get(name);
        if (pair != null) {
            return pair.getFirst();
        }
        // check same-module (create inlined_class) or path classes
        return resolver.resolveAggregationFunction(name);
    }

    public Pair<Class, String[]> resolveAggregationMultiFunction(String name) {
        // check local
        Pair<Class, String[]> pair = aggregationMultiFunctionExtensions.get(name);
        if (pair != null) {
            return pair;
        }
        // check same-module (create inlined_class) or path classes
        return resolver.resolveAggregationMultiFunction(name);
    }

    public Map<String, byte[]> getBytes() {
        return bytes;
    }

    public boolean isLocalInlinedClass(Class<?> declaringClass) {
        for (Class clazz : classes) {
            if (declaringClass == clazz) {
                return true;
            }
        }
        return false;
    }
}
