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
package com.espertech.esper.common.internal.support;

import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Map;

public class SupportEventPropDesc {
    private String propertyName;
    private EPType propertyType;
    private boolean isRequiresIndex;
    private boolean isRequiresMapkey;
    private boolean isIndexed;
    private boolean isMapped;
    private boolean isFragment;
    private Class componentType;

    public SupportEventPropDesc(String name, Class clazz) {
        this.propertyName = name;
        this.propertyType = ClassHelperGenericType.getClassEPType(clazz);
        presets();
    }

    public SupportEventPropDesc(String name, EPType type) {
        this.propertyName = name;
        this.propertyType = type;
        presets();
    }

    private void presets() {
        if (propertyType == EPTypeNull.INSTANCE) {
            return;
        }
        EPTypeClass propertyClass = (EPTypeClass) propertyType;
        if (propertyClass.getType().isArray()) {
            indexed().componentType(propertyClass.getType().getComponentType());
        }
        if (JavaClassHelper.isSubclassOrImplementsInterface(propertyClass, Iterable.class)) {
            indexed().componentType(JavaClassHelper.getSingleParameterTypeOrObject((EPTypeClass) propertyType).getType());
        }
        if (JavaClassHelper.isSubclassOrImplementsInterface(propertyClass, Map.class)) {
            mapped().componentType(JavaClassHelper.getSecondParameterTypeOrObject((EPTypeClass) propertyType).getType());
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public EPType getPropertyType() {
        return propertyType;
    }

    public boolean isRequiresIndex() {
        return isRequiresIndex;
    }

    public boolean isRequiresMapkey() {
        return isRequiresMapkey;
    }

    public boolean isIndexed() {
        return isIndexed;
    }

    public boolean isMapped() {
        return isMapped;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public Class getComponentType() {
        return componentType;
    }

    public SupportEventPropDesc mapped() {
        this.isMapped = true;
        return this;
    }

    public SupportEventPropDesc mapped(boolean flag) {
        this.isMapped = flag;
        return this;
    }

    public SupportEventPropDesc mappedRequiresKey() {
        this.isMapped = true;
        this.isRequiresMapkey = true;
        return this;
    }

    public SupportEventPropDesc indexed() {
        this.isIndexed = true;
        return this;
    }

    public SupportEventPropDesc indexed(boolean flag) {
        this.isIndexed = flag;
        return this;
    }

    public SupportEventPropDesc indexedRequiresIndex() {
        this.isIndexed = true;
        this.isRequiresIndex = true;
        return this;
    }

    public SupportEventPropDesc componentType(Class componentType) {
        this.componentType = componentType;
        return this;
    }

    public SupportEventPropDesc fragment() {
        this.isFragment = true;
        return this;
    }

    public SupportEventPropDesc fragment(boolean flag) {
        this.isFragment = flag;
        return this;
    }
}
