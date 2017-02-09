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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;

import java.util.List;
import java.util.Map;

/**
 * Getter for one or more levels deep nested properties of maps.
 */
public class MapNestedPropertyGetterMixedType implements MapEventPropertyGetter {
    private final EventPropertyGetter[] getterChain;

    /**
     * Ctor.
     *
     * @param getterChain        is the chain of getters to retrieve each nested property
     * @param eventAdaperService is a factory for POJO bean event types
     */
    public MapNestedPropertyGetterMixedType(List<EventPropertyGetter> getterChain,
                                            EventAdapterService eventAdaperService) {
        this.getterChain = getterChain.toArray(new EventPropertyGetter[getterChain.size()]);
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleGetterTrailingChain(result);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        if (!((MapEventPropertyGetter) getterChain[0]).isMapExistsProperty(map)) {
            return false;
        }
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleIsExistsTrailingChain(result);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = getterChain[0].get(eventBean);
        return handleGetterTrailingChain(result);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        if (!getterChain[0].isExistsProperty(eventBean)) {
            return false;
        }
        Object result = getterChain[0].get(eventBean);
        return handleIsExistsTrailingChain(result);
    }

    private boolean handleIsExistsTrailingChain(Object result) {
        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return false;
            }

            EventPropertyGetter getter = getterChain[i];

            if (i == getterChain.length - 1) {
                if (getter instanceof BeanEventPropertyGetter) {
                    return ((BeanEventPropertyGetter) getter).isBeanExistsProperty(result);
                } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                    return ((MapEventPropertyGetter) getter).isMapExistsProperty((Map) result);
                } else if (result instanceof EventBean) {
                    return getter.isExistsProperty((EventBean) result);
                } else {
                    return false;
                }
            }

            if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else {
                return false;
            }
        }
        return false;
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }


    private Object handleGetterTrailingChain(Object result) {

        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return null;
            }
            EventPropertyGetter getter = getterChain[i];
            if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else {
                return null;
            }
        }
        return result;
    }
}
