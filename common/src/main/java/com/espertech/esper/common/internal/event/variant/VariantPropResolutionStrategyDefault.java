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
package com.espertech.esper.common.internal.event.variant;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleTypeCaster;
import com.espertech.esper.common.internal.util.SimpleTypeCasterFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A property resolution strategy that allows only the preconfigured types, wherein all properties
 * that are common (name and type) to all properties are considered.
 */
public class VariantPropResolutionStrategyDefault implements VariantPropResolutionStrategy {
    private final VariantEventType variantEventType;

    public VariantPropResolutionStrategyDefault(VariantEventType variantEventType) {
        this.variantEventType = variantEventType;
    }

    public VariantPropertyDesc resolveProperty(String propertyName, EventType[] variants) {
        boolean existsInAll = true;
        Class commonType = null;
        boolean mustCoerce = false;
        for (int i = 0; i < variants.length; i++) {
            Class type = JavaClassHelper.getBoxedType(variants[i].getPropertyType(propertyName));
            if (type == null) {
                existsInAll = false;
                continue;
            }

            if (commonType == null) {
                commonType = type;
                continue;
            }

            // compare types
            if (type.equals(commonType)) {
                continue;
            }

            // coercion
            if (JavaClassHelper.isNumeric(type)) {
                if (JavaClassHelper.canCoerce(type, commonType)) {
                    mustCoerce = true;
                    continue;
                }
                if (JavaClassHelper.canCoerce(commonType, type)) {
                    mustCoerce = true;
                    commonType = type;
                }
            } else if (commonType == Object.class) {
                continue;
            } else if (!JavaClassHelper.isJavaBuiltinDataType(type)) {
                // common interface or base class
                Set<Class> supersForType = new LinkedHashSet<Class>();
                JavaClassHelper.getSuper(type, supersForType);
                supersForType.remove(Object.class);

                if (supersForType.contains(commonType)) {
                    continue;   // type implements or extends common type
                }
                if (JavaClassHelper.isSubclassOrImplementsInterface(commonType, type)) {
                    commonType = type;  // common type implements type
                    continue;
                }

                // find common interface or type both implement
                Set<Class> supersForCommonType = new LinkedHashSet<Class>();
                JavaClassHelper.getSuper(commonType, supersForCommonType);
                supersForCommonType.remove(Object.class);

                // Take common classes first, ignoring interfaces
                boolean found = false;
                for (Class superClassType : supersForType) {
                    if (!superClassType.isInterface() && (supersForCommonType.contains(superClassType))) {
                        commonType = superClassType;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                // Take common interfaces
                for (Class superClassType : supersForType) {
                    if (superClassType.isInterface() && (supersForCommonType.contains(superClassType))) {
                        break;
                    }
                }
            }

            commonType = Object.class;
        }

        if (!existsInAll) {
            return null;
        }

        if (commonType == null) {
            return null;
        }

        // property numbers should start at zero since the serve as array index
        VariantPropertyGetterCache propertyGetterCache = variantEventType.getVariantPropertyGetterCache();
        propertyGetterCache.addGetters(propertyName);

        EventPropertyGetterSPI getter;
        if (mustCoerce) {
            SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(null, commonType);
            getter = new VariantEventPropertyGetterAnyWCast(variantEventType, propertyName, caster);
        } else {
            getter = new VariantEventPropertyGetterAny(variantEventType, propertyName);
        }

        return new VariantPropertyDesc(commonType, getter, true);
    }
}
