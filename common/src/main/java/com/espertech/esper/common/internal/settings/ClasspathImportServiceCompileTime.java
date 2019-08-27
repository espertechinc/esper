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

import com.espertech.esper.common.client.configuration.compiler.*;
import com.espertech.esper.common.client.hook.aggfunc.AggregationFunctionForge;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.epl.agg.access.linear.AggregationAccessorLinearType;
import com.espertech.esper.common.internal.epl.approx.countminsketch.CountMinSketchAggType;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionCountMinSketchNode;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.common.internal.epl.expression.agg.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.common.internal.epl.expression.agg.method.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprCurrentEvaluationContextNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.MinMaxTypeEnum;
import com.espertech.esper.common.internal.epl.expression.funcs.ExprEventIdentityEqualsNode;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexFactoryProviderMXCIFQuadTree;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.AdvancedIndexFactoryProviderPointRegionQuadTree;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.AdvancedIndexFactoryProvider;
import com.espertech.esper.common.internal.util.MethodResolver;
import com.espertech.esper.common.internal.util.MethodResolverNoSuchMethodException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.MathContext;
import java.util.*;

public class ClasspathImportServiceCompileTime extends ClasspathImportServiceBase {
    public final static String EXT_SINGLEROW_FUNCTION_TRANSPOSE = "transpose";

    private final MathContext mathContext;
    private final boolean allowExtendedAggregationFunc;
    private final boolean sortUsingCollator;

    private final Map<String, ConfigurationCompilerPlugInAggregationFunction> aggregationFunctions;
    private final List<Pair<Set<String>, ConfigurationCompilerPlugInAggregationMultiFunction>> aggregationAccess;
    private final Map<String, ClasspathImportSingleRowDesc> singleRowFunctions = new HashMap<>();
    private final LinkedHashMap<String, AdvancedIndexFactoryProvider> advancedIndexProviders = new LinkedHashMap<>(8);
    private final Map<String, ConfigurationCompilerPlugInDateTimeMethod> dateTimeMethods;
    private final Map<String, ConfigurationCompilerPlugInEnumMethod> enumMethods;

    public ClasspathImportServiceCompileTime(Map<String, Object> transientConfiguration, TimeAbacus timeAbacus, Set<String> eventTypeAutoNames, MathContext mathContext, boolean allowExtendedAggregationFunc, boolean sortUsingCollator) {
        super(transientConfiguration, timeAbacus, eventTypeAutoNames);
        aggregationFunctions = new HashMap<>();
        this.aggregationAccess = new ArrayList<>();
        this.mathContext = mathContext;
        this.allowExtendedAggregationFunc = allowExtendedAggregationFunc;
        this.sortUsingCollator = sortUsingCollator;
        this.advancedIndexProviders.put("pointregionquadtree", new AdvancedIndexFactoryProviderPointRegionQuadTree());
        this.advancedIndexProviders.put("mxcifquadtree", new AdvancedIndexFactoryProviderMXCIFQuadTree());
        this.dateTimeMethods = new HashMap<>();
        this.enumMethods = new HashMap<>();
    }

    public void addSingleRow(String functionName, String singleRowFuncClass, String methodName, ConfigurationCompilerPlugInSingleRowFunction.ValueCache valueCache, ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable filterOptimizable, boolean rethrowExceptions, String optionalEventTypeName) throws ClasspathImportException {
        validateFunctionName("single-row", functionName);

        if (!isClassName(singleRowFuncClass)) {
            throw new ClasspathImportException("Invalid class name for aggregation '" + singleRowFuncClass + "'");
        }
        singleRowFunctions.put(functionName.toLowerCase(Locale.ENGLISH), new ClasspathImportSingleRowDesc(singleRowFuncClass, methodName, valueCache, filterOptimizable, rethrowExceptions, optionalEventTypeName));
    }

    public void addPlugInDateTimeMethod(String dtmMethodName, ConfigurationCompilerPlugInDateTimeMethod config) throws ClasspathImportException {
        validateFunctionName("date-time-method", dtmMethodName);

        if (!isClassName(config.getForgeClassName())) {
            throw new ClasspathImportException("Invalid class name for date-time-method '" + config.getForgeClassName() + "'");
        }
        dateTimeMethods.put(dtmMethodName.toLowerCase(Locale.ENGLISH), config);
    }

    public void addPlugInEnumMethod(String dtmMethodName, ConfigurationCompilerPlugInEnumMethod config) throws ClasspathImportException {
        validateFunctionName("enum-method", dtmMethodName);

        if (!isClassName(config.getForgeClassName())) {
            throw new ClasspathImportException("Invalid class name for enum-method '" + config.getForgeClassName() + "'");
        }
        enumMethods.put(dtmMethodName.toLowerCase(Locale.ENGLISH), config);
    }

    public Pair<Class, ClasspathImportSingleRowDesc> resolveSingleRow(String name) throws ClasspathImportException, ClasspathImportUndefinedException {
        ClasspathImportSingleRowDesc pair = singleRowFunctions.get(name);
        if (pair == null) {
            pair = singleRowFunctions.get(name.toLowerCase(Locale.ENGLISH));
        }
        if (pair == null) {
            throw new ClasspathImportUndefinedException("A function named '" + name + "' is not defined");
        }

        Class clazz;
        try {
            clazz = getClassForNameProvider().classForName(pair.getClassName());
        } catch (ClassNotFoundException ex) {
            throw new ClasspathImportException("Could not load single-row function class by name '" + pair.getClassName() + "'", ex);
        }
        return new Pair<>(clazz, pair);
    }

    public Class resolveAnnotation(String className) throws ClasspathImportException {
        Class clazz;
        try {
            clazz = resolveClassInternal(className, true, true);
        } catch (ClassNotFoundException e) {
            throw new ClasspathImportException("Could not load annotation class by name '" + className + "', please check imports", e);
        }
        return clazz;
    }

    public Method resolveMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allowEventBeanType) throws ClasspathImportException {
        try {
            return MethodResolver.resolveMethod(clazz, methodName, paramTypes, true, allowEventBeanType, allowEventBeanType);
        } catch (MethodResolverNoSuchMethodException e) {
            throw convert(clazz, methodName, paramTypes, e, true);
        }
    }

    public Method resolveMethodOverloadChecked(Class clazz, String methodName) throws ClasspathImportException {
        return resolveMethodInternalCheckOverloads(clazz, methodName, MethodModifiers.REQUIRE_STATIC_AND_PUBLIC);
    }

    public Method resolveMethodOverloadChecked(String className, String methodName) throws ClasspathImportException {
        Class clazz;
        try {
            clazz = resolveClassInternal(className, false, false);
        } catch (ClassNotFoundException e) {
            throw new ClasspathImportException("Could not load class by name '" + className + "', please check imports", e);
        }
        return resolveMethodInternalCheckOverloads(clazz, methodName, MethodModifiers.REQUIRE_STATIC_AND_PUBLIC);
    }

    public ExprNode resolveSingleRowExtendedBuiltin(String name) {
        String nameLowerCase = name.toLowerCase(Locale.ENGLISH);
        if (nameLowerCase.equals("current_evaluation_context")) {
            return new ExprCurrentEvaluationContextNode();
        }
        if (nameLowerCase.equals(ExprEventIdentityEqualsNode.NAME)) {
            return new ExprEventIdentityEqualsNode();
        }
        return null;
    }

    public MathContext getDefaultMathContext() {
        return mathContext;
    }

    private void validateFunctionName(String functionType, String functionName) throws ClasspathImportException {
        String functionNameLower = functionName.toLowerCase(Locale.ENGLISH);
        if (aggregationFunctions.containsKey(functionNameLower)) {
            throw new ClasspathImportException("Aggregation function by name '" + functionName + "' is already defined");
        }
        if (singleRowFunctions.containsKey(functionNameLower)) {
            throw new ClasspathImportException("Single-row function by name '" + functionName + "' is already defined");
        }
        for (Pair<Set<String>, ConfigurationCompilerPlugInAggregationMultiFunction> pairs : aggregationAccess) {
            if (pairs.getFirst().contains(functionNameLower)) {
                throw new ClasspathImportException("Aggregation multi-function by name '" + functionName + "' is already defined");
            }
        }
        if (!isFunctionName(functionName)) {
            throw new ClasspathImportException("Invalid " + functionType + " name '" + functionName + "'");
        }
    }

    private static boolean isFunctionName(String functionName) {
        String classNameRegEx = "\\w+";
        return functionName.matches(classNameRegEx);
    }

    public ExprNode resolveAggExtendedBuiltin(String name, boolean isDistinct) {
        if (!allowExtendedAggregationFunc) {
            return null;
        }
        String nameLowerCase = name.toLowerCase(Locale.ENGLISH);
        if (nameLowerCase.equals("first")) {
            return new ExprAggMultiFunctionLinearAccessNode(AggregationAccessorLinearType.FIRST);
        }
        if (nameLowerCase.equals("last")) {
            return new ExprAggMultiFunctionLinearAccessNode(AggregationAccessorLinearType.LAST);
        }
        if (nameLowerCase.equals("window")) {
            return new ExprAggMultiFunctionLinearAccessNode(AggregationAccessorLinearType.WINDOW);
        }
        if (nameLowerCase.equals("firstever")) {
            return new ExprFirstLastEverNode(isDistinct, true);
        }
        if (nameLowerCase.equals("lastever")) {
            return new ExprFirstLastEverNode(isDistinct, false);
        }
        if (nameLowerCase.equals("countever")) {
            return new ExprCountEverNode(isDistinct);
        }
        if (nameLowerCase.equals("minever")) {
            return new ExprMinMaxAggrNode(isDistinct, MinMaxTypeEnum.MIN, false, true);
        }
        if (nameLowerCase.equals("maxever")) {
            return new ExprMinMaxAggrNode(isDistinct, MinMaxTypeEnum.MAX, false, true);
        }
        if (nameLowerCase.equals("fminever")) {
            return new ExprMinMaxAggrNode(isDistinct, MinMaxTypeEnum.MIN, true, true);
        }
        if (nameLowerCase.equals("fmaxever")) {
            return new ExprMinMaxAggrNode(isDistinct, MinMaxTypeEnum.MAX, true, true);
        }
        if (nameLowerCase.equals("rate")) {
            return new ExprRateAggNode(isDistinct);
        }
        if (nameLowerCase.equals("nth")) {
            return new ExprNthAggNode(isDistinct);
        }
        if (nameLowerCase.equals("leaving")) {
            return new ExprLeavingAggNode(isDistinct);
        }
        if (nameLowerCase.equals("maxby")) {
            return new ExprAggMultiFunctionSortedMinMaxByNode(true, false, false);
        }
        if (nameLowerCase.equals("maxbyever")) {
            return new ExprAggMultiFunctionSortedMinMaxByNode(true, true, false);
        }
        if (nameLowerCase.equals("minby")) {
            return new ExprAggMultiFunctionSortedMinMaxByNode(false, false, false);
        }
        if (nameLowerCase.equals("minbyever")) {
            return new ExprAggMultiFunctionSortedMinMaxByNode(false, true, false);
        }
        if (nameLowerCase.equals("sorted")) {
            return new ExprAggMultiFunctionSortedMinMaxByNode(false, false, true);
        }
        CountMinSketchAggType cmsType = CountMinSketchAggType.fromNameMayMatch(nameLowerCase);
        if (cmsType != null) {
            return new ExprAggMultiFunctionCountMinSketchNode(isDistinct, cmsType);
        }
        return null;
    }

    public boolean isSortUsingCollator() {
        return sortUsingCollator;
    }

    public Method resolveNonStaticMethodOverloadChecked(Class clazz, String methodName) throws ClasspathImportException {
        return resolveMethodInternalCheckOverloads(clazz, methodName, MethodModifiers.REQUIRE_NONSTATIC_AND_PUBLIC);
    }

    private Method resolveMethodInternalCheckOverloads(Class clazz, String methodName, MethodModifiers methodModifiers)
            throws ClasspathImportException {
        Method[] methods = clazz.getMethods();
        Set<Method> overloadeds = null;
        Method methodByName = null;

        // check each method by name, add to overloads when multiple methods for the same name
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                int modifiers = method.getModifiers();
                boolean isPublic = Modifier.isPublic(modifiers);
                boolean isStatic = Modifier.isStatic(modifiers);
                if (methodModifiers.acceptsPublicFlag(isPublic) && methodModifiers.acceptsStaticFlag(isStatic)) {
                    if (methodByName != null) {
                        if (overloadeds == null) {
                            overloadeds = new HashSet<>();
                        }
                        overloadeds.add(method);
                    } else {
                        methodByName = method;
                    }
                }
            }
        }
        if (methodByName == null) {
            throw new ClasspathImportException("Could not find " + methodModifiers.getText() + " method named '" + methodName + "' in class '" + clazz.getName() + "'");
        }
        if (overloadeds == null) {
            return methodByName;
        }

        // determine that all overloads have the same result type
        for (Method overloaded : overloadeds) {
            if (!overloaded.getReturnType().equals(methodByName.getReturnType())) {
                throw new ClasspathImportException("Method by name '" + methodName + "' is overloaded in class '" + clazz.getName() + "' and overloaded methods do not return the same type");
            }
        }

        return methodByName;
    }

    public AdvancedIndexFactoryProvider resolveAdvancedIndexProvider(String indexTypeName) throws ClasspathImportException {
        AdvancedIndexFactoryProvider provider = advancedIndexProviders.get(indexTypeName);
        if (provider == null) {
            throw new ClasspathImportException("Unrecognized advanced-type index '" + indexTypeName + "'");
        }
        return provider;
    }

    public AggregationFunctionForge resolveAggregationFunction(String functionName) throws ClasspathImportUndefinedException, ClasspathImportException {
        ConfigurationCompilerPlugInAggregationFunction desc = aggregationFunctions.get(functionName);
        if (desc == null) {
            desc = aggregationFunctions.get(functionName.toLowerCase(Locale.ENGLISH));
        }
        if (desc == null || desc.getForgeClassName() == null) {
            throw new ClasspathImportUndefinedException("A function named '" + functionName + "' is not defined");
        }

        String className = desc.getForgeClassName();
        Class clazz;
        try {
            clazz = getClassForNameProvider().classForName(className);
        } catch (ClassNotFoundException ex) {
            throw new ClasspathImportException("Could not load aggregation factory class by name '" + className + "'", ex);
        }

        Object object;
        try {
            object = clazz.newInstance();
        } catch (InstantiationException e) {
            throw new ClasspathImportException("Error instantiating aggregation factory class by name '" + className + "'", e);
        } catch (IllegalAccessException e) {
            throw new ClasspathImportException("Illegal access instantiating aggregation factory class by name '" + className + "'", e);
        }

        if (!(object instanceof AggregationFunctionForge)) {
            throw new ClasspathImportException("Class by name '" + className + "' does not implement the " + AggregationFunctionForge.class.getSimpleName() + " interface");
        }
        return (AggregationFunctionForge) object;
    }

    public void addAggregation(String functionName, ConfigurationCompilerPlugInAggregationFunction aggregationDesc) throws ClasspathImportException {
        validateFunctionName("aggregation function", functionName);
        if (aggregationDesc.getForgeClassName() == null || !isClassName(aggregationDesc.getForgeClassName())) {
            throw new ClasspathImportException("Invalid class name for aggregation function forge '" + aggregationDesc.getForgeClassName() + "'");
        }
        aggregationFunctions.put(functionName.toLowerCase(Locale.ENGLISH), aggregationDesc);
    }

    public ConfigurationCompilerPlugInAggregationMultiFunction resolveAggregationMultiFunction(String name) {
        for (Pair<Set<String>, ConfigurationCompilerPlugInAggregationMultiFunction> config : aggregationAccess) {
            if (config.getFirst().contains(name.toLowerCase(Locale.ENGLISH))) {
                return config.getSecond();
            }
        }
        return null;
    }

    public void addAggregationMultiFunction(ConfigurationCompilerPlugInAggregationMultiFunction desc) throws ClasspathImportException {
        LinkedHashSet<String> orderedImmutableFunctionNames = new LinkedHashSet<String>();
        for (String functionName : desc.getFunctionNames()) {
            orderedImmutableFunctionNames.add(functionName.toLowerCase(Locale.ENGLISH));
            validateFunctionName("aggregation multi-function", functionName.toLowerCase(Locale.ENGLISH));
        }
        if (!isClassName(desc.getMultiFunctionForgeClassName())) {
            throw new ClasspathImportException("Invalid class name for aggregation multi-function factory '" + desc.getMultiFunctionForgeClassName() + "'");
        }
        aggregationAccess.add(new Pair<Set<String>, ConfigurationCompilerPlugInAggregationMultiFunction>(orderedImmutableFunctionNames, desc));
    }

    public Class resolveDateTimeMethod(String name) throws ClasspathImportException {
        ConfigurationCompilerPlugInDateTimeMethod dtm = dateTimeMethods.get(name);
        if (dtm == null) {
            dtm = dateTimeMethods.get(name.toLowerCase(Locale.ENGLISH));
        }
        if (dtm == null) {
            return null;
        }

        Class clazz;
        try {
            clazz = getClassForNameProvider().classForName(dtm.getForgeClassName());
        } catch (ClassNotFoundException ex) {
            throw new ClasspathImportException("Could not load date-time-method forge class by name '" + dtm.getForgeClassName() + "'", ex);
        }
        return clazz;
    }

    public Class resolveEnumMethod(String name) throws ClasspathImportException {
        ConfigurationCompilerPlugInEnumMethod enumMethod = enumMethods.get(name);
        if (enumMethod == null) {
            enumMethod = enumMethods.get(name.toLowerCase(Locale.ENGLISH));
        }
        if (enumMethod == null) {
            return null;
        }

        Class clazz;
        try {
            clazz = getClassForNameProvider().classForName(enumMethod.getForgeClassName());
        } catch (ClassNotFoundException ex) {
            throw new ClasspathImportException("Could not load enum-method forge class by name '" + enumMethod.getForgeClassName() + "'", ex);
        }
        return clazz;
    }

    private enum MethodModifiers {
        REQUIRE_STATIC_AND_PUBLIC("public static", true),
        REQUIRE_NONSTATIC_AND_PUBLIC("public non-static", false);

        private final String text;
        private final boolean requiredStaticFlag;

        MethodModifiers(String text, boolean requiredStaticFlag) {
            this.text = text;
            this.requiredStaticFlag = requiredStaticFlag;
        }

        public boolean acceptsPublicFlag(boolean isPublic) {
            return isPublic;
        }

        public boolean acceptsStaticFlag(boolean isStatic) {
            return requiredStaticFlag == isStatic;
        }

        public String getText() {
            return text;
        }
    }
}
