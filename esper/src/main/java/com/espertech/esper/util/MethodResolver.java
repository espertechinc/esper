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
package com.espertech.esper.util;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used for retrieving static and instance method objects. It
 * provides two points of added functionality over the standard
 * java.lang.reflect mechanism of retrieving methods. First,
 * class names can be partial, and if the class name is partial
 * then java.lang is searched for the class. Second,
 * invocation parameter types don't have to match the declaration
 * parameter types exactly when the standard java conversion
 * mechanisms (currently autoboxing and widening conversions)
 * will make the invocation valid. Preference is given to those
 * methods that require the fewest widening conversions.
 */
public class MethodResolver {
    private static final Logger log = LoggerFactory.getLogger(MethodResolver.class);

    private static final Map<Class, Set<Class>> WIDENING_CONVERSIONS = new HashMap<Class, Set<Class>>();
    private static final Map<Class, Set<Class>> WRAPPING_CONVERSIONS = new HashMap<Class, Set<Class>>();

    static {
        // Initialize the map of wrapper conversions
        Set<Class> booleanWrappers = new HashSet<Class>();
        booleanWrappers.add(boolean.class);
        booleanWrappers.add(Boolean.class);
        WRAPPING_CONVERSIONS.put(boolean.class, booleanWrappers);
        WRAPPING_CONVERSIONS.put(Boolean.class, booleanWrappers);

        Set<Class> charWrappers = new HashSet<Class>();
        charWrappers.add(char.class);
        charWrappers.add(Character.class);
        WRAPPING_CONVERSIONS.put(char.class, charWrappers);
        WRAPPING_CONVERSIONS.put(Character.class, charWrappers);

        Set<Class> byteWrappers = new HashSet<Class>();
        byteWrappers.add(byte.class);
        byteWrappers.add(Byte.class);
        WRAPPING_CONVERSIONS.put(byte.class, byteWrappers);
        WRAPPING_CONVERSIONS.put(Byte.class, byteWrappers);

        Set<Class> shortWrappers = new HashSet<Class>();
        shortWrappers.add(short.class);
        shortWrappers.add(Short.class);
        WRAPPING_CONVERSIONS.put(short.class, shortWrappers);
        WRAPPING_CONVERSIONS.put(Short.class, shortWrappers);

        Set<Class> intWrappers = new HashSet<Class>();
        intWrappers.add(int.class);
        intWrappers.add(Integer.class);
        WRAPPING_CONVERSIONS.put(int.class, intWrappers);
        WRAPPING_CONVERSIONS.put(Integer.class, intWrappers);

        Set<Class> longWrappers = new HashSet<Class>();
        longWrappers.add(long.class);
        longWrappers.add(Long.class);
        WRAPPING_CONVERSIONS.put(long.class, longWrappers);
        WRAPPING_CONVERSIONS.put(Long.class, longWrappers);

        Set<Class> floatWrappers = new HashSet<Class>();
        floatWrappers.add(float.class);
        floatWrappers.add(Float.class);
        WRAPPING_CONVERSIONS.put(float.class, floatWrappers);
        WRAPPING_CONVERSIONS.put(Float.class, floatWrappers);

        Set<Class> doubleWrappers = new HashSet<Class>();
        doubleWrappers.add(double.class);
        doubleWrappers.add(Double.class);
        WRAPPING_CONVERSIONS.put(double.class, doubleWrappers);
        WRAPPING_CONVERSIONS.put(Double.class, doubleWrappers);

        // Initialize the map of widening conversions
        Set<Class> wideningConversions = new HashSet<Class>(byteWrappers);
        MethodResolver.WIDENING_CONVERSIONS.put(short.class, new HashSet<Class>(wideningConversions));
        MethodResolver.WIDENING_CONVERSIONS.put(Short.class, new HashSet<Class>(wideningConversions));

        wideningConversions.addAll(shortWrappers);
        wideningConversions.addAll(charWrappers);
        MethodResolver.WIDENING_CONVERSIONS.put(int.class, new HashSet<Class>(wideningConversions));
        MethodResolver.WIDENING_CONVERSIONS.put(Integer.class, new HashSet<Class>(wideningConversions));

        wideningConversions.addAll(intWrappers);
        MethodResolver.WIDENING_CONVERSIONS.put(long.class, new HashSet<Class>(wideningConversions));
        MethodResolver.WIDENING_CONVERSIONS.put(Long.class, new HashSet<Class>(wideningConversions));

        wideningConversions.addAll(longWrappers);
        MethodResolver.WIDENING_CONVERSIONS.put(float.class, new HashSet<Class>(wideningConversions));
        MethodResolver.WIDENING_CONVERSIONS.put(Float.class, new HashSet<Class>(wideningConversions));

        wideningConversions.addAll(floatWrappers);
        MethodResolver.WIDENING_CONVERSIONS.put(double.class, new HashSet<Class>(wideningConversions));
        MethodResolver.WIDENING_CONVERSIONS.put(Double.class, new HashSet<Class>(wideningConversions));
    }

    /**
     * Returns the allowable widening conversions.
     *
     * @return map where key is the class that we are asking to be widened into, and
     * a set of classes that can be widened from
     */
    public static Map<Class, Set<Class>> getWideningConversions() {
        return WIDENING_CONVERSIONS;
    }

    /**
     * Attempts to find the static or instance method described by the parameters,
     * or a method of the same name that will accept the same type of
     * parameters.
     *
     * @param declaringClass         - the class to search for the method
     * @param methodName             - the name of the method
     * @param paramTypes             - the parameter types for the method
     * @param allowInstance          - true to allow instance methods as well, false to allow only static method
     * @param allowEventBeanCollType whether event-bean-collection parameter type is allowed
     * @param allowEventBeanType     whether event-bean parameter type is allowed
     * @return - the Method object for this method
     * @throws MethodResolverNoSuchMethodException if the method could not be found
     */
    public static Method resolveMethod(Class declaringClass, String methodName, Class[] paramTypes, boolean allowInstance, boolean[] allowEventBeanType, boolean[] allowEventBeanCollType)
            throws MethodResolverNoSuchMethodException {
        // Get all the methods for this class
        Method[] methods = declaringClass.getMethods();

        Method bestMatch = null;
        int bestConversionCount = -1;

        // Examine each method, checking if the signature is compatible
        Method conversionFailedMethod = null;
        for (Method method : methods) {
            // Check the modifiers: we only want public and static, if required
            if (!isPublicAndStatic(method, allowInstance)) {
                continue;
            }

            // Check the name
            if (!method.getName().equals(methodName)) {
                continue;
            }

            // Check the parameter list
            int conversionCount = compareParameterTypesAllowContext(method.getParameterTypes(), paramTypes, allowEventBeanType, allowEventBeanCollType, method.getGenericParameterTypes(), method.isVarArgs());

            // Parameters don't match
            if (conversionCount == -1) {
                conversionFailedMethod = method;
                continue;
            }

            // Parameters match exactly
            if (conversionCount == 0) {
                bestMatch = method;
                break;
            }

            // No previous match
            if (bestMatch == null) {
                bestMatch = method;
                bestConversionCount = conversionCount;
            } else {
                // Current match is better
                if (conversionCount < bestConversionCount) {
                    bestMatch = method;
                    bestConversionCount = conversionCount;
                }
            }

        }

        if (bestMatch != null) {
            logWarnBoxedToPrimitiveType(declaringClass, methodName, bestMatch, paramTypes);
            return bestMatch;
        }

        StringBuilder parameters = new StringBuilder();
        if (paramTypes != null && paramTypes.length != 0) {
            String appendString = "";
            for (Object param : paramTypes) {
                parameters.append(appendString);
                if (param == null) {
                    parameters.append("(null)");
                } else {
                    parameters.append(param.toString());
                }
                appendString = ", ";
            }
        }
        throw new MethodResolverNoSuchMethodException("Unknown method " + declaringClass.getSimpleName() + '.' + methodName + '(' + parameters + ')', conversionFailedMethod);
    }

    private static void logWarnBoxedToPrimitiveType(Class declaringClass, String methodName, Method bestMatch, Class[] paramTypes) {
        Class[] parametersMethod = bestMatch.getParameterTypes();
        for (int i = 0; i < parametersMethod.length; i++) {
            if (!parametersMethod[i].isPrimitive()) {
                continue;
            }
            // if null-type parameter, or non-JDK class and boxed type matches
            if (paramTypes[i] == null ||
                    (!declaringClass.getClass().getName().startsWith("java") &&
                            (JavaClassHelper.getBoxedType(parametersMethod[i])) == paramTypes[i])) {
                String paramTypeStr = paramTypes[i] == null ? "null" : paramTypes[i].getSimpleName();
                log.info("Method '" + methodName + "' in class '" + declaringClass.getName() + "' expects primitive type '" + parametersMethod[i] +
                        "' as parameter " + i + ", but receives a nullable (boxed) type " + paramTypeStr +
                        ". This may cause null pointer exception at runtime if the actual value is null, please consider using boxed types for method parameters.");
                return;
            }
        }
    }

    private static boolean isWideningConversion(Class declarationType, Class invocationType) {
        if (WIDENING_CONVERSIONS.containsKey(declarationType)) {
            return WIDENING_CONVERSIONS.get(declarationType).contains(invocationType);
        } else {
            return false;
        }
    }

    private static boolean isPublicAndStatic(Method method, boolean allowInstance) {
        int modifiers = method.getModifiers();
        if (allowInstance) {
            return Modifier.isPublic(modifiers);
        } else {
            return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
        }
    }

    private static int compareParameterTypesAllowContext(Class[] declarationParameters,
                                                         Class[] invocationParameters,
                                                         boolean[] optionalAllowEventBeanType,
                                                         boolean[] optionalAllowEventBeanCollType,
                                                         Type[] genericParameterTypes,
                                                         boolean isVarargs) {

        // determine if the last parameter is EPLMethodInvocationContext (no varargs)
        Class[] declaredNoContext = declarationParameters;
        if (!isVarargs && declarationParameters.length > 0 &&
                declarationParameters[declarationParameters.length - 1] == EPLMethodInvocationContext.class) {
            declaredNoContext = JavaClassHelper.takeFirstN(declarationParameters, declarationParameters.length - 1);
        }

        // determine if the previous-to-last parameter is EPLMethodInvocationContext (varargs-only)
        if (isVarargs && declarationParameters.length > 1 &&
                declarationParameters[declarationParameters.length - 2] == EPLMethodInvocationContext.class) {
            Class[] rewritten = new Class[declarationParameters.length - 1];
            System.arraycopy(declarationParameters, 0, rewritten, 0, declarationParameters.length - 2);
            rewritten[rewritten.length - 1] = declarationParameters[declarationParameters.length - 1];
            declaredNoContext = rewritten;
        }

        return compareParameterTypesNoContext(declaredNoContext, invocationParameters,
                optionalAllowEventBeanType, optionalAllowEventBeanCollType, genericParameterTypes, isVarargs);
    }

    // Returns -1 if the invocation parameters aren't applicable
    // to the method. Otherwise returns the number of parameters
    // that have to be converted
    private static int compareParameterTypesNoContext(Class[] declarationParameters,
                                                      Class[] invocationParameters,
                                                      boolean[] optionalAllowEventBeanType,
                                                      boolean[] optionalAllowEventBeanCollType,
                                                      Type[] genericParameterTypes,
                                                      boolean isVarargs) {
        if (invocationParameters == null) {
            return declarationParameters.length == 0 ? 0 : -1;
        }

        // handle varargs
        if (isVarargs) {
            if (invocationParameters.length < declarationParameters.length - 1) {
                return -1;
            }
            if (invocationParameters.length == 0) {
                return 0;
            }

            AtomicInteger conversionCount = new AtomicInteger();

            // check declared types (non-vararg)
            for (int i = 0; i < declarationParameters.length - 1; i++) {
                boolean compatible = compareParameterTypeCompatible(invocationParameters[i],
                        declarationParameters[i],
                        optionalAllowEventBeanType == null ? null : optionalAllowEventBeanType[i],
                        optionalAllowEventBeanCollType == null ? null : optionalAllowEventBeanCollType[i],
                        genericParameterTypes[i],
                        conversionCount);
                if (!compatible) {
                    return -1;
                }
            }

            Class varargDeclarationParameter = declarationParameters[declarationParameters.length - 1].getComponentType();

            // handle array of compatible type passed into vararg
            if (invocationParameters.length == declarationParameters.length) {
                Class providedType = invocationParameters[invocationParameters.length - 1];
                if (providedType != null && providedType.isArray()) {
                    if (providedType.getComponentType() == varargDeclarationParameter) {
                        return conversionCount.get();
                    }
                    if (JavaClassHelper.isSubclassOrImplementsInterface(providedType.getComponentType(), varargDeclarationParameter)) {
                        conversionCount.incrementAndGet();
                        return conversionCount.get();
                    }
                }
            }

            // handle compatible types passed into vararg
            Type varargGenericParameterTypes = genericParameterTypes[genericParameterTypes.length - 1];
            for (int i = declarationParameters.length - 1; i < invocationParameters.length; i++) {
                boolean compatible = compareParameterTypeCompatible(invocationParameters[i],
                        varargDeclarationParameter,
                        optionalAllowEventBeanType == null ? null : optionalAllowEventBeanType[i],
                        optionalAllowEventBeanCollType == null ? null : optionalAllowEventBeanCollType[i],
                        varargGenericParameterTypes,
                        conversionCount);
                if (!compatible) {
                    return -1;
                }
            }
            return conversionCount.get();
        }

        // handle non-varargs
        if (declarationParameters.length != invocationParameters.length) {
            return -1;
        }

        AtomicInteger conversionCount = new AtomicInteger();
        for (int i = 0; i < declarationParameters.length; i++) {
            boolean compatible = compareParameterTypeCompatible(invocationParameters[i],
                    declarationParameters[i],
                    optionalAllowEventBeanType == null ? null : optionalAllowEventBeanType[i],
                    optionalAllowEventBeanCollType == null ? null : optionalAllowEventBeanCollType[i],
                    genericParameterTypes[i],
                    conversionCount);
            if (!compatible) {
                return -1;
            }
        }
        return conversionCount.get();
    }

    private static boolean compareParameterTypeCompatible(Class invocationParameter,
                                                          Class declarationParameter,
                                                          Boolean optionalAllowEventBeanType,
                                                          Boolean optionalAllowEventBeanCollType,
                                                          Type genericParameterType,
                                                          AtomicInteger conversionCount) {
        if (invocationParameter == null) {
            return !declarationParameter.isPrimitive();
        }
        if (optionalAllowEventBeanType != null && declarationParameter == EventBean.class && optionalAllowEventBeanType) {
            return true;
        }
        if (optionalAllowEventBeanCollType != null &&
                declarationParameter == Collection.class &&
                optionalAllowEventBeanCollType &&
                JavaClassHelper.getGenericType(genericParameterType, 0) == EventBean.class) {
            return true;
        }
        if (!isIdentityConversion(declarationParameter, invocationParameter)) {
            conversionCount.incrementAndGet();
            if (!isWideningConversion(declarationParameter, invocationParameter) && declarationParameter != Object.class) {
                return false;
            }
        }
        return true;
    }

    // Identity conversion means no conversion, wrapper conversion,
    // or conversion to a supertype
    private static boolean isIdentityConversion(Class declarationType, Class invocationType) {
        if (WRAPPING_CONVERSIONS.containsKey(declarationType)) {
            return WRAPPING_CONVERSIONS.get(declarationType).contains(invocationType) || declarationType.isAssignableFrom(invocationType);
        } else {
            if (invocationType == null) {
                return !declarationType.isPrimitive();
            }
            if (invocationType.isPrimitive()) {
                invocationType = JavaClassHelper.getBoxedType(invocationType);
            }
            return declarationType.isAssignableFrom(invocationType);
        }

    }

    public static Constructor resolveCtor(Class declaringClass, Class[] paramTypes) throws MethodResolverNoSuchCtorException {
        // Get all the methods for this class
        Constructor[] ctors = declaringClass.getConstructors();

        Constructor bestMatch = null;
        int bestConversionCount = -1;

        // Examine each method, checking if the signature is compatible
        Constructor conversionFailedCtor = null;
        for (Constructor ctor : ctors) {
            // Check the modifiers: we only want public
            if (!Modifier.isPublic(ctor.getModifiers())) {
                continue;
            }

            // Check the parameter list
            int conversionCount = compareParameterTypesNoContext(ctor.getParameterTypes(), paramTypes, null, null, ctor.getGenericParameterTypes(), ctor.isVarArgs());

            // Parameters don't match
            if (conversionCount == -1) {
                conversionFailedCtor = ctor;
                continue;
            }

            // Parameters match exactly
            if (conversionCount == 0) {
                bestMatch = ctor;
                break;
            }

            // No previous match
            if (bestMatch == null) {
                bestMatch = ctor;
                bestConversionCount = conversionCount;
            } else {
                // Current match is better
                if (conversionCount < bestConversionCount) {
                    bestMatch = ctor;
                    bestConversionCount = conversionCount;
                }
            }

        }

        if (bestMatch != null) {
            return bestMatch;
        } else {
            StringBuilder parameters = new StringBuilder();
            String message = "Constructor not found for " + declaringClass.getSimpleName() + " taking ";
            if (paramTypes != null && paramTypes.length != 0) {
                String appendString = "";
                for (Object param : paramTypes) {
                    parameters.append(appendString);
                    if (param == null) {
                        parameters.append("(null)");
                    } else {
                        parameters.append(param.toString());
                    }
                    appendString = ", ";
                }
                message += "('" + parameters + "')'";
            } else {
                message += "no parameters";
            }
            throw new MethodResolverNoSuchCtorException(message, conversionFailedCtor);
        }
    }
}
