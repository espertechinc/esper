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
import com.espertech.esper.common.internal.event.bean.core.DynamicPropertyDescriptorByField;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Base class for getters for a dynamic property (syntax field.inner?), caches methods to use for classes.
 */
public abstract class DynamicPropertyGetterByFieldBase implements BeanEventPropertyGetter {
    protected final EventBeanTypedEventFactory eventBeanTypedEventFactory;
    protected final BeanEventTypeFactory beanEventTypeFactory;
    protected final CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache;
    protected final CodegenFieldSharable sharableCode = new CodegenFieldSharable() {
        public Class type() {
            return CopyOnWriteArrayList.class;
        }

        public CodegenExpression initCtorScoped() {
            return newInstance(CopyOnWriteArrayList.class);
        }
    };

    /**
     * To be implemented to return the field required, or null to indicate an appropriate field could not be found.
     *
     * @param clazz to search for a matching field
     * @return field if found, or null if no matching field exists
     */
    protected abstract Field determineField(Class clazz);

    protected abstract CodegenExpression determineFieldCodegen(CodegenExpressionRef clazz, CodegenMethodScope parent, CodegenClassScope codegenClassScope);

    /**
     * Call the getter to obtains the return result object, or null if no such field exists.
     *
     * @param descriptor provides field information for the class
     * @param underlying is the underlying object to ask for the property value
     * @return underlying
     */
    protected abstract Object call(DynamicPropertyDescriptorByField descriptor, Object underlying);

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
    public static Object cacheAndCall(CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache, DynamicPropertyGetterByFieldBase getter, Object object, EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        DynamicPropertyDescriptorByField desc = getPopulateCache(cache, getter, object, eventBeanTypedEventFactory);
        if (desc.getField() == null) {
            return null;
        }
        return getter.call(desc, object);
    }

    private CodegenExpression cacheAndCallCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(Object.class, DynamicPropertyGetterByFieldBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByField.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getField"))).blockReturn(constantNull())
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
    public static boolean cacheAndExists(CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache, DynamicPropertyGetterByFieldBase getter, Object object, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        DynamicPropertyDescriptorByField desc = getPopulateCache(cache, getter, object, eventBeanTypedEventFactory);
        if (desc.getField() == null) {
            return false;
        }
        return true;
    }

    protected CodegenExpression cacheAndExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenExpression memberCache = codegenClassScope.addOrGetFieldSharable(sharableCode);
        CodegenMethod method = parent.makeChild(boolean.class, DynamicPropertyGetterByFieldBase.class, codegenClassScope).addParam(Object.class, "object");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByField.class, "desc", getPopulateCacheCodegen(memberCache, ref("object"), method, codegenClassScope))
            .ifCondition(equalsNull(exprDotMethod(ref("desc"), "getField"))).blockReturn(constantFalse())
            .methodReturn(constant(true));
        return localMethod(method, underlyingExpression);
    }

    public DynamicPropertyGetterByFieldBase(EventBeanTypedEventFactory eventBeanTypedEventFactory, BeanEventTypeFactory beanEventTypeFactory) {
        this.beanEventTypeFactory = beanEventTypeFactory;
        this.cache = new CopyOnWriteArrayList<DynamicPropertyDescriptorByField>();
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

    protected static DynamicPropertyDescriptorByField getPopulateCache(CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache, DynamicPropertyGetterByFieldBase dynamicPropertyGetterBase, Object obj, EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        DynamicPropertyDescriptorByField desc = dynamicPropertyCacheCheck(cache, obj);
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
            Field field = dynamicPropertyGetterBase.determineField(obj.getClass());

            // Cache descriptor and create field
            desc = dynamicPropertyCacheAdd(obj.getClass(), field, cache);
            return desc;
        }
    }

    protected CodegenExpression getPopulateCacheCodegen(CodegenExpression memberCache, CodegenExpressionRef object, CodegenMethodScope parent, CodegenClassScope codegenClassScope) {
        CodegenMethod method = parent.makeChild(DynamicPropertyDescriptorByField.class, DynamicPropertyGetterByFieldBase.class, codegenClassScope).addParam(CopyOnWriteArrayList.class, "cache").addParam(Object.class, "obj");
        method.getBlock()
            .declareVar(DynamicPropertyDescriptorByField.class, "desc", staticMethod(DynamicPropertyGetterByFieldBase.class, "dynamicPropertyCacheCheck", ref("cache"), ref("obj")))
            .ifRefNotNull("desc").blockReturn(ref("desc"))
            .declareVar(Class.class, "clazz", exprDotMethod(ref("obj"), "getClass"))
            .declareVar(Field.class, "field", determineFieldCodegen(ref("clazz"), method, codegenClassScope))
            .assignRef("desc", staticMethod(DynamicPropertyGetterByFieldBase.class, "dynamicPropertyCacheAdd", ref("clazz"), ref("field"), ref("cache")))
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
    public static DynamicPropertyDescriptorByField dynamicPropertyCacheCheck(CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache, Object obj) {
        // Check if the method is already there
        Class target = obj.getClass();
        for (DynamicPropertyDescriptorByField desc : cache) {
            if (desc.getClazz() == target) {
                return desc;
            }
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param clazz class
     * @param field field
     * @param cache cache
     * @return descriptor
     */
    public static DynamicPropertyDescriptorByField dynamicPropertyCacheAdd(Class clazz, Field field, CopyOnWriteArrayList<DynamicPropertyDescriptorByField> cache) {
        DynamicPropertyDescriptorByField propertyDescriptor = new DynamicPropertyDescriptorByField(clazz, field);
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
    public static PropertyAccessException handleException(DynamicPropertyDescriptorByField descriptor, Object underlying, Throwable t) {
        if (t instanceof ClassCastException) {
            throw PropertyUtility.getMismatchException(descriptor.getField(), underlying, (ClassCastException) t);
        }
        if (t instanceof IllegalArgumentException) {
            throw PropertyUtility.getIllegalArgumentException(descriptor.getField(), (IllegalArgumentException) t);
        }
        if (t instanceof IllegalAccessException) {
            throw PropertyUtility.getIllegalAccessException(descriptor.getField(), (IllegalAccessException) t);
        }
        throw PropertyUtility.getGeneralException(descriptor.getField(), t);
    }
}
