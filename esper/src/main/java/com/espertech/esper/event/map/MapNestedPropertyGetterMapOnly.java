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

import java.util.List;
import java.util.Map;

/**
 * Getter for one or more levels deep nested properties of maps.
 */
public class MapNestedPropertyGetterMapOnly implements MapEventPropertyGetter {
    private final MapEventPropertyGetter[] mapGetterChain;

    /**
     * Ctor.
     *
     * @param getterChain        is the chain of getters to retrieve each nested property
     * @param eventAdaperService is a factory for POJO bean event types
     */
    public MapNestedPropertyGetterMapOnly(List<EventPropertyGetter> getterChain,
                                          EventAdapterService eventAdaperService) {
        this.mapGetterChain = new MapEventPropertyGetter[getterChain.size()];
        for (int i = 0; i < getterChain.size(); i++) {
            mapGetterChain[i] = (MapEventPropertyGetter) getterChain.get(i);
        }
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object result = mapGetterChain[0].getMap(map);
        return handleGetterTrailingChain(result);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        if (!mapGetterChain[0].isMapExistsProperty(map)) {
            return false;
        }
        Object result = mapGetterChain[0].getMap(map);
        return handleIsExistsTrailingChain(result);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = mapGetterChain[0].get(eventBean);
        return handleGetterTrailingChain(result);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        if (!mapGetterChain[0].isExistsProperty(eventBean)) {
            return false;
        }
        Object result = mapGetterChain[0].get(eventBean);
        return handleIsExistsTrailingChain(result);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    private boolean handleIsExistsTrailingChain(Object result) {
        for (int i = 1; i < mapGetterChain.length; i++) {
            if (result == null) {
                return false;
            }

            MapEventPropertyGetter getter = mapGetterChain[i];

            if (i == mapGetterChain.length - 1) {
                if (!(result instanceof Map)) {
                    if (result instanceof EventBean) {
                        return getter.isExistsProperty((EventBean) result);
                    }
                    return false;
                } else {
                    return getter.isMapExistsProperty((Map<String, Object>) result);
                }
            }

            if (!(result instanceof Map)) {
                if (result instanceof EventBean) {
                    result = getter.get((EventBean) result);
                } else {
                    return false;
                }
            } else {
                result = getter.getMap((Map<String, Object>) result);
            }
        }
        return true;
    }

    private Object handleGetterTrailingChain(Object result) {
        for (int i = 1; i < mapGetterChain.length; i++) {
            if (result == null) {
                return null;
            }

            MapEventPropertyGetter getter = mapGetterChain[i];
            if (!(result instanceof Map)) {
                if (result instanceof EventBean) {
                    result = getter.get((EventBean) result);
                } else {
                    return null;
                }
            } else {
                result = getter.getMap((Map<String, Object>) result);
            }
        }
        return result;
    }
}
