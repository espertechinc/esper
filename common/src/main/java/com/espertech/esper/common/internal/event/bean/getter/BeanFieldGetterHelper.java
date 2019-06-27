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

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.util.PropertyUtility;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static com.espertech.esper.common.internal.util.CollectionUtil.getMapValueChecked;

public class BeanFieldGetterHelper {
    public static Object getFieldSimple(Field field, Object object) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(field, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(field, e);
        }
    }

    public static Object getFieldMap(Field field, Object object, Object key) throws PropertyAccessException {
        try {
            Object result = field.get(object);
            return getMapValueChecked(result, key);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(field, object, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(field, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(field, e);
        }
    }

    public static Object getFieldArray(Field field, Object object, int index) throws PropertyAccessException {
        try {
            Object value = field.get(object);
            if (Array.getLength(value) <= index) {
                return null;
            }
            return Array.get(value, index);
        } catch (ClassCastException e) {
            throw PropertyUtility.getMismatchException(field, object, e);
        } catch (IllegalAccessException e) {
            throw PropertyUtility.getIllegalAccessException(field, e);
        } catch (IllegalArgumentException e) {
            throw PropertyUtility.getIllegalArgumentException(field, e);
        }
    }
}
