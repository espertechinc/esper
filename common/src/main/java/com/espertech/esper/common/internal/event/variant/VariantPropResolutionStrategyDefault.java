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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
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
        EPType commonType = null;
        boolean mustCoerce = false;
        for (int i = 0; i < variants.length; i++) {
            EPType propertyType = JavaClassHelper.getBoxedType(variants[i].getPropertyEPType(propertyName));
            if (propertyType == null) {
                existsInAll = false;
                continue;
            }

            if (commonType == null) {
                commonType = propertyType;
                continue;
            }

            // compare types
            if (propertyType.equals(commonType)) {
                continue;
            }
            if (commonType == EPTypeNull.INSTANCE) {
                continue;
            }

            EPTypeClass commonTypeClass = (EPTypeClass) commonType;
            // coercion
            if (propertyType instanceof EPTypeClass) {
                EPTypeClass typeClass = (EPTypeClass) propertyType;
                if (JavaClassHelper.isNumeric(typeClass)) {
                    if (JavaClassHelper.canCoerce(typeClass.getType(), commonTypeClass.getType())) {
                        mustCoerce = true;
                        continue;
                    }
                    if (JavaClassHelper.canCoerce(commonTypeClass.getType(), typeClass.getType())) {
                        mustCoerce = true;
                        commonType = typeClass;
                    }
                } else if (commonTypeClass.getType() == Object.class) {
                    continue;
                } else if (!JavaClassHelper.isJavaBuiltinDataType(typeClass)) {
                    // common interface or base class
                    Set<Class> supersForType = new LinkedHashSet<Class>();
                    JavaClassHelper.getSuper(typeClass.getType(), supersForType);
                    supersForType.remove(Object.class);

                    if (supersForType.contains(typeClass.getType())) {
                        continue;   // type implements or extends common type
                    }
                    if (JavaClassHelper.isSubclassOrImplementsInterface(commonTypeClass, typeClass.getType())) {
                        commonType = typeClass;  // common type implements type
                        continue;
                    }

                    // find common interface or type both implement
                    Set<Class> supersForCommonType = new LinkedHashSet<Class>();
                    JavaClassHelper.getSuper(commonTypeClass.getType(), supersForCommonType);
                    supersForCommonType.remove(Object.class);

                    // Take common classes first, ignoring interfaces
                    boolean found = false;
                    for (Class superClassType : supersForType) {
                        if (!superClassType.isInterface() && (supersForCommonType.contains(superClassType))) {
                            commonType = ClassHelperGenericType.getClassEPType(superClassType);
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
            }
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
            SimpleTypeCaster caster = SimpleTypeCasterFactory.getCaster(EPTypeNull.INSTANCE, (EPTypeClass) commonType);
            getter = new VariantEventPropertyGetterAnyWCast(variantEventType, propertyName, caster);
        } else {
            getter = new VariantEventPropertyGetterAny(variantEventType, propertyName);
        }
        return new VariantPropertyDesc(commonType, getter, true);
    }
}
