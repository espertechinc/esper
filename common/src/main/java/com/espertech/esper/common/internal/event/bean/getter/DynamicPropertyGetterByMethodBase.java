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
package com.espertech.esper.common.internal.event.bean.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenFieldSharable;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.bean.core.BeanEventPropertyGetter;
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByMethod;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Base class for getters for a dynamic property (syntax field.inner?), caches methods to use for classes.
 */
public abstract class DynamicPropertyGetterByMethodBase implements BeanEventPropertyGetter {
    protected final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    protected final BeanEventTypeFactory beanEventTypeFactory;
    protected final CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache;
    protected final CodegenFieldSharable sharableCode = new CodegenFieldSharable() {
        public Class type() {
            return CopyOnWriteArrayList.class;
        }

        public CodegenExpression initCtorScoped() {
            return newInstance(CopyOnWriteArrayList.class);
        }
    };

    /**
     * To be implemented to return the method required, or null to indicate an appropriate method could not be found.
     *
     * @param clazz to search for a matching method
     * @return method if found, or null if no matching method exists
     */
    protected abstract Method determineMethod(Class clazz);

    protected abstract CodegenExpression determineMethodCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope);

    /**
     * Call the getter to obtains the return result object, or null if no such method exists.
     *
     * @param descriptor provides method information for the class
     * @param underlying is the underlying object to ask for the property value
     * @return underlying
     */
    protected abstract Object call(DynamicPropertyDescriptorByMethod descriptor, Object underlying);

    protected abstract CodegenExpression callCodegen(CodegenExpressionRef desc, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope);

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param cache                      cache
     * @param getter                     getter
     * @param object                     object
     * @param eventBeanTypedEventFactory event server
     * @param beanEventTypeFactory       bean factory
     * @return property
     */
    public static Object cacheAndCall(CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache, DynamicPropertyGetterByMethodBase getter, Object object, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        DynamicPropertyDescriptorByMethod desc = getPopulateCache(cache, getter, object, eventBeanTypedEventFactory);
        if (desc.getMethod() == null) {
            return null;
        }
        return getter.call(desc, object);
    }

    private CodegenExpression cacheAndCallCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(Object.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
                .declareVar(DynamicPropertyDescriptorByMethod.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
                .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getMethod"))).blockReturn(constantNull())
                .methodReturn(callCodegen(ref("desc"), ref("object"), method, codegenClassScope));
        return localMethod(method, underlyingExpression);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param cache                      cache
     * @param getter                     getter
     * @param object                     object
     * @param eventBeanTypedEventFactory event server
     * @return exists-flag
     */
    public static boolean cacheAndExists(CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache, DynamicPropertyGetterByMethodBase getter, Object object, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        DynamicPropertyDescriptorByMethod desc = getPopulateCache(cache, getter, object, eventBeanTypedEventFactory);
        if (desc.getMethod() == null) {
            return false;
        }
        return true;
    }

    protected CodegenExpression cacheAndExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
                .declareVar(DynamicPropertyDescriptorByMethod.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
                .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getMethod"))).blockReturn(constantFalse())
                .methodReturn(constant(true));
        return localMethod(method, underlyingExpression);
    }

    public DynamicPropertyGetterByMethodBase(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.cache = new CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod>();
        this.eventBeanTypedEventFactory = eventBeanTypedEventFactory;
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        return cacheAndCall(cache, this, object, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public Class getTargetType() {
        return Object.class;
    }

    public boolean isBeanExistsProperty(Object object) {
        return cacheAndExists(cache, this, object, eventBeanTypedEventFactory);
    }

    public final Object get(EventBean event) throws PropertyAccessException {
        return cacheAndCall(cache, this, event.getUnderlying(), eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    public Class getBeanPropType() {
        return Object.class;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(exprDotUnderlying(beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(exprDotUnderlying(beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(exprDotUnderlying(beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return cacheAndCallCodegen(underlyingExpression, codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public Object getFragment(EventBean eventBean) {
        Object result = get(eventBean);
        return BaseNativePropertyGetter.getFragmentDynamic(result, eventBeanTypedEventFactory, beanEventTypeFactory);
    }

    protected static DynamicPropertyDescriptorByMethod getPopulateCache(CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache, DynamicPropertyGetterByMethodBase dynamicPropertyGetterBase, Object obj, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        DynamicPropertyDescriptorByMethod desc = dynamicPropertyCacheCheck(cache, obj);
        if (desc != null) {
            return desc;
        }

        // need to add it
        synchronized (dynamicPropertyGetterBase) {
            desc = dynamicPropertyCacheCheck(cache, obj);
            if (desc != null) {
                return desc;
            }

            // Lookup method to use
            Method method = dynamicPropertyGetterBase.determineMethod(obj.getClass());

            // Cache descriptor and create fast method
            desc = dynamicPropertyCacheAdd(obj.getClass(), method, cache);
            return desc;
        }
    }

    protected CodegenExpression getPopulateCacheCodegen(CodegenExpression memberCache, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(DynamicPropertyDescriptorByMethod.class, DynamicPropertyGetterByMethodBase.class, codegenClassScope).addParam(CopyOnWriteArrayList.class, "cache").addParam(Object.class, "obj");
        method.getBlock()
                .declareVar(DynamicPropertyDescriptorByMethod.class, "desc", staticMethod(DynamicPropertyGetterByMethodBase.class, "dynamicPropertyCacheCheck", ref("cache"), ref("obj")))
                .ifRefNotNull("desc").blockReturn(ref("desc"))
                .declareVar(Class.class, "clazz", exprDotMethod(ref("obj"), "getClass"))
                .declareVar(Method.class, "method", determineMethodCodegen(ref("clazz"), method, codegenClassScope))
                .assignRef("desc", staticMethod(DynamicPropertyGetterByMethodBase.class, "dynamicPropertyCacheAdd", ref("clazz"), ref("method"), ref("cache")))
                .methodReturn(ref("desc"));
        return localMethod(method, memberCache, object);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param obj   target
     * @param cache cache
     * @return descriptor
     */
    public static DynamicPropertyDescriptorByMethod dynamicPropertyCacheCheck(CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache, Object obj) {
        // Check if the method is already there
        Class target = obj.getClass();
        for (DynamicPropertyDescriptorByMethod desc : cache) {
            if (desc.getClazz() == target) {
                return desc;
            }
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz  class
     * @param method method
     * @param cache  cache
     * @return descriptor
     */
    public static DynamicPropertyDescriptorByMethod dynamicPropertyCacheAdd(Class clazz, Method method, CopyOnWriteArrayList<DynamicPropertyDescriptorByMethod> cache) {
        DynamicPropertyDescriptorByMethod propertyDescriptor;
        if (method == null) {
            propertyDescriptor = new DynamicPropertyDescriptorByMethod(clazz, null, false);
        } else {
            propertyDescriptor = new DynamicPropertyDescriptorByMethod(clazz, method, method.getParameterTypes().length > 0);
        }
        cache.add(propertyDescriptor);
        return propertyDescriptor;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param descriptor descriptor
     * @param underlying underlying
     * @param t          throwable
     * @return exception
     */
    public static PropertyAccessException handleException(DynamicPropertyDescriptorByMethod descriptor, Object underlying, Throwable t) {
        if (t instanceof ClassCastException) {
            throw PropertyUtility.getMismatchException(descriptor.getMethod(), underlying, (ClassCastException) t);
        }
        if (t instanceof InvocationTargetException) {
            throw PropertyUtility.getInvocationTargetException(descriptor.getMethod(), (InvocationTargetException) t);
        }
        if (t instanceof IllegalArgumentException) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getMethod(), (IllegalArgumentException) t);
        }
        if (t instanceof IllegalAccessException) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getMethod(), (IllegalAccessException) t);
        }
        throw PropertyUtility.getGeneralException(descriptor.getMethod(), t);
    }
}
