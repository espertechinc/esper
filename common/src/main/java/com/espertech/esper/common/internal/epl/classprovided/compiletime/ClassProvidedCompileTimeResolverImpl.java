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
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.collection.PathException;
import com.espertech.esper.common.internal.collection.PathRegistry;
import com.espertech.esper.common.internal.collection.PathRegistryObjectType;
import com.espertech.esper.common.internal.context.module.ModuleDependenciesCompileTime;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.util.CompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClassProvidedCompileTimeResolverImpl implements ClassProvidedCompileTimeResolver {
    private final String moduleName;
    private final Set<String> moduleUses;
    private final ClassProvidedCompileTimeRegistry locals;
    private final PathRegistry<String, ClassProvided> path;
    private final ModuleDependenciesCompileTime moduleDependencies;
    private final boolean isFireAndForget;

    public ClassProvidedCompileTimeResolverImpl(String moduleName, Set<String> moduleUses, ClassProvidedCompileTimeRegistry locals, PathRegistry<String, ClassProvided> path, ModuleDependenciesCompileTime moduleDependencies, boolean isFireAndForget) {
        this.moduleName = moduleName;
        this.moduleUses = moduleUses;
        this.locals = locals;
        this.path = path;
        this.moduleDependencies = moduleDependencies;
        this.isFireAndForget = isFireAndForget;
    }

    public ClassProvided resolveClass(String name) {
        // try self-originated protected types first
        ClassProvided localExpr = locals.getClasses().get(name);
        if (localExpr != null) {
            return localExpr;
        }
        try {
            Pair<ClassProvided, String> expression = path.getAnyModuleExpectSingle(name, moduleUses);
            if (expression != null) {
                if (!isFireAndForget && !NameAccessModifier.visible(expression.getFirst().getVisibility(), expression.getFirst().getModuleName(), moduleName)) {
                    return null;
                }

                moduleDependencies.addPathClass(name, expression.getSecond());
                return expression.getFirst();
            }
        } catch (PathException e) {
            throw CompileTimeResolver.makePathAmbiguous(PathRegistryObjectType.CLASSPROVIDED, name, e);
        }
        return null;
    }

    public Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) {
        Pair<Class, ExtensionSingleRowFunction> pair = resolveFromLocalAndPath(name, locals, path, ExtensionSingleRowFunction.class, "single-row function", moduleUses, moduleDependencies, anno -> Collections.singleton(anno.name()));
        return pair == null ? null : new Pair<>(pair.getFirst(), new ClasspathImportSingleRowDesc(pair.getFirst(), pair.getSecond()));
    }

    public Class resolveAggregationFunction(String name) {
        Pair<Class, ExtensionAggregationFunction> pair = resolveFromLocalAndPath(name, locals, path, ExtensionAggregationFunction.class, "aggregation function", moduleUses, moduleDependencies, anno -> Collections.singleton(anno.name()));
        return pair == null ? null : pair.getFirst();
    }

    public Pair<Class, String[]> resolveAggregationMultiFunction(String name) {
        Function<ExtensionAggregationMultiFunction, Set<String>> nameProvision = anno -> {
            Set<String> names = new HashSet<>(2);
            String[] split = anno.names().split(",");
            for (String nameprovided : split) {
                names.add(nameprovided.trim());
            }
            return names;
        };
        Pair<Class, ExtensionAggregationMultiFunction> pair = resolveFromLocalAndPath(name, locals, path, ExtensionAggregationMultiFunction.class, "aggregation multi-function", moduleUses, moduleDependencies, nameProvision);
        return pair == null ? null : new Pair<>(pair.getFirst(), pair.getSecond().names().split(","));
    }

    public boolean isEmpty() {
        return path.isEmpty() && locals.getClasses().isEmpty();
    }

    public void addTo(ClassProvidedClassesAdd additionalClasses) {
        path.traverse(cp -> additionalClasses.add(cp.getBytes()));
    }

    public void addTo(Map<String, byte[]> bytes) {
        path.traverse(cp -> bytes.putAll(cp.getBytes()));
    }

    public void removeFrom(ClassProvidedClassRemove moduleBytes) {
        Consumer<ClassProvided> classProvidedByteCodeRemover = item -> {
            for (Map.Entry<String, byte[]> entry : item.getBytes().entrySet()) {
                moduleBytes.remove(entry.getKey());
            }
        };
        path.traverse(classProvidedByteCodeRemover);
    }

    private static <T> Pair<Class, T> resolveFromLocalAndPath(String soughtName, ClassProvidedCompileTimeRegistry locals, PathRegistry<String, ClassProvided> path, Class<T> annotationType, String objectName, Set<String> moduleUses, ModuleDependenciesCompileTime moduleDependencies, Function<T, Set<String>> namesProvider) {
        if (locals.getClasses().isEmpty() && path.isEmpty()) {
            return null;
        }

        try {
            // try resolve from local
            Pair<Class, T> localPair = resolveFromLocal(soughtName, locals, annotationType, objectName, namesProvider);
            if (localPair != null) {
                return localPair;
            }

            // try resolve from path, using module-uses when necessary
            return resolveFromPath(soughtName, path, annotationType, objectName, moduleUses, moduleDependencies, namesProvider);
        } catch (ExprValidationException ex) {
            throw new EPException(ex.getMessage(), ex);
        }
    }

    private static <T> Pair<Class, T> resolveFromLocal(String soughtName, ClassProvidedCompileTimeRegistry locals, Class annotationType, String objectName, Function<T, Set<String>> namesProvider) throws ExprValidationException {
        List<Pair<Class, T>> foundLocal = new ArrayList<>(2);
        for (Map.Entry<String, ClassProvided> entry : locals.getClasses().entrySet()) {
            JavaClassHelper.traverseAnnotations(entry.getValue().getClassesMayNull(), annotationType, (clazz, annotation) -> {
                T t = (T) annotation;
                Set<String> names = namesProvider.apply(t);
                for (String name : names) {
                    if (soughtName.equals(name)) {
                        foundLocal.add(new Pair<>(clazz, t));
                    }
                }
            });
        }
        if (foundLocal.size() > 1) {
            throw getDuplicateSingleRow(soughtName, objectName);
        }
        if (foundLocal.size() == 1) {
            return foundLocal.get(0);
        }
        return null;
    }

    private static <T> Pair<Class, T> resolveFromPath(String soughtName, PathRegistry<String, ClassProvided> path, Class annotationType, String objectName, Set<String> moduleUses, ModuleDependenciesCompileTime moduleDependencies, Function<T, Set<String>> namesProvider) throws ExprValidationException {
        List<PathFunc<T>> foundPath = new ArrayList<>(2);
        path.traverseWithModule((moduleName, classProvided) -> {
            JavaClassHelper.traverseAnnotations(classProvided.getClassesMayNull(), annotationType, (clazz, annotation) -> {
                T t = (T) annotation;
                Set<String> names = namesProvider.apply(t);
                for (String name : names) {
                    if (soughtName.equals(name)) {
                        foundPath.add(new PathFunc<T>(moduleName, clazz, t));
                    }
                }
            });
        });

        PathFunc<T> foundPathFunc;
        if (foundPath.isEmpty()) {
            return null;
        } else if (foundPath.size() == 1) {
            foundPathFunc = foundPath.get(0);
        } else {
            if (moduleUses == null || moduleUses.isEmpty()) {
                throw getDuplicateSingleRow(soughtName, objectName);
            }
            List<PathFunc<T>> matchesUses = new ArrayList<>(2);
            for (PathFunc<T> func : foundPath) {
                if (moduleUses.contains(func.optionalModuleName)) {
                    matchesUses.add(func);
                }
            }
            if (matchesUses.size() > 1) {
                throw getDuplicateSingleRow(soughtName, objectName);
            }
            if (matchesUses.isEmpty()) {
                return null;
            }
            foundPathFunc = matchesUses.get(0);
        }

        moduleDependencies.addPathClass(foundPathFunc.getClazz().getName(), foundPathFunc.getOptionalModuleName());
        return new Pair<>(foundPathFunc.getClazz(), foundPathFunc.annotation);
    }

    private static ExprValidationException getDuplicateSingleRow(String name, String objectName) {
        return new ExprValidationException("The plug-in " + objectName + " '" + name + "' occurs multiple times");
    }

    private static class PathFunc<T> {
        private final String optionalModuleName;
        private final Class clazz;
        private final T annotation;

        public PathFunc(String optionalModuleName, Class clazz, T annotation) {
            this.optionalModuleName = optionalModuleName;
            this.clazz = clazz;
            this.annotation = annotation;
        }

        public String getOptionalModuleName() {
            return optionalModuleName;
        }

        public Class getClazz() {
            return clazz;
        }

        public T getAnnotation() {
            return annotation;
        }
    }
}
