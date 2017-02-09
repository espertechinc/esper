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
package com.espertech.esper.event.bean;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;

import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for getters for a dynamic property (syntax field.inner?), caches methods to use for classes.
 */
public abstract class DynamicPropertyGetterBase implements BeanEventPropertyGetter {
    private final EventAdapterService eventAdapterService;
    private final CopyOnWriteArrayList<DynamicPropertyDescriptor> cache;

    /**
     * To be implemented to return the method required, or null to indicate an appropriate method could not be found.
     *
     * @param clazz to search for a matching method
     * @return method if found, or null if no matching method exists
     */
    protected abstract Method determineMethod(Class clazz);

    /**
     * Call the getter to obtains the return result object, or null if no such method exists.
     *
     * @param descriptor provides method information for the class
     * @param underlying is the underlying object to ask for the property value
     * @return underlying
     */
    protected abstract Object call(DynamicPropertyDescriptor descriptor, Object underlying);

    /**
     * Ctor.
     *
     * @param eventAdapterService factory for event beans and event types
     */
    public DynamicPropertyGetterBase(EventAdapterService eventAdapterService) {
        this.cache = new CopyOnWriteArrayList<DynamicPropertyDescriptor>();
        this.eventAdapterService = eventAdapterService;
    }

    public Object getBeanProp(Object object) throws PropertyAccessException {
        DynamicPropertyDescriptor desc = getPopulateCache(object);
        if (desc.getMethod() == null) {
            return null;
        }
        return call(desc, object);
    }

    public boolean isBeanExistsProperty(Object object) {
        DynamicPropertyDescriptor desc = getPopulateCache(object);
        if (desc.getMethod() == null) {
            return false;
        }
        return true;
    }

    public final Object get(EventBean obj) throws PropertyAccessException {
        DynamicPropertyDescriptor desc = getPopulateCache(obj.getUnderlying());
        if (desc.getMethod() == null) {
            return null;
        }
        return call(desc, obj.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        DynamicPropertyDescriptor desc = getPopulateCache(eventBean.getUnderlying());
        if (desc.getMethod() == null) {
            return false;
        }
        return true;
    }

    private DynamicPropertyDescriptor getPopulateCache(Object obj) {
        // Check if the method is already there
        Class target = obj.getClass();
        for (DynamicPropertyDescriptor desc : cache) {
            if (desc.getClazz() == target) {
                return desc;
            }
        }

        // need to add it
        synchronized (this) {
            for (DynamicPropertyDescriptor desc : cache) {
                if (desc.getClazz() == target) {
                    return desc;
                }
            }

            // Lookup method to use
            Method method = determineMethod(target);

            // Cache descriptor and create fast method
            DynamicPropertyDescriptor propertyDescriptor;
            if (method == null) {
                propertyDescriptor = new DynamicPropertyDescriptor(target, null, false);
            } else {
                FastClass fastClass = FastClass.create(eventAdapterService.getEngineImportService().getFastClassClassLoader(target), target);
                FastMethod fastMethod = fastClass.getMethod(method);
                propertyDescriptor = new DynamicPropertyDescriptor(target, fastMethod, fastMethod.getParameterTypes().length > 0);
            }
            cache.add(propertyDescriptor);
            return propertyDescriptor;
        }
    }

    public Object getFragment(EventBean eventBean) {
        Object result = get(eventBean);
        return BaseNativePropertyGetter.getFragmentDynamic(result, eventAdapterService);
    }
}
