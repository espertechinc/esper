/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.entities;

import com.espertech.esper.client.EventPropertyDescriptor;
import com.espertech.esper.client.FragmentEventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventTypeSPI;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PropertyDetailFactory {

    private static final Logger log = LoggerFactory.getLogger(PropertyDetailFactory.class);

    public static PropertyDetail[] getProperties(Class clazz, PropertySerializerClassNameProvider serializerClassNameProvider) {
        EventTypeSPI type = (EventTypeSPI) SupportEventAdapterService.getService().addBeanType(clazz.getName(), clazz, false, false, false);
        return getProperties(type, serializerClassNameProvider);
    }

    public static PropertyDetail[] getProperties(EventTypeSPI type, PropertySerializerClassNameProvider serializerClassNameProvider) {
        if (type == null) {
            return new PropertyDetail[0];
        }
        List<PropertyDetail> coll = new ArrayList<PropertyDetail>();
        for (EventPropertyDescriptor prop : type.getPropertyDescriptors()) {
            String fragmentTypeName = null;
            String fragmentTypeUnd = null;
            Boolean fragmentIsNative = null;
            Boolean fragmentIsIndexed = null;
            if (prop.isFragment()) {
                FragmentEventType fragmentType;
                try {
                    fragmentType = type.getFragmentType(prop.getPropertyName());
                } catch (PropertyAccessException ex) {
                    continue;
                }

                if (fragmentType == null) {
                    log.warn("Failed to obtain fragment type for property '" + prop.getPropertyName() + "' of type '" + type.getName() + "'");
                } else {
                    fragmentTypeName = fragmentType.getFragmentType().getName();
                    fragmentIsNative = fragmentType.isNative();
                    fragmentIsIndexed = fragmentType.isIndexed();
                    fragmentTypeUnd = fragmentType.getFragmentType().getUnderlyingType().getName();
                }
            }

            String propertyType;
            if (prop.getPropertyType() == null) {
                propertyType = "null";
            } else {
                propertyType = JavaClassHelper.getClassNameFullyQualPretty(prop.getPropertyType());
            }

            String componentType = null;
            if (prop.getPropertyComponentType() != null) {
                componentType = prop.getPropertyComponentType().getName();
            }

            PropertyDetail[] fragmentProp = null;
            if (fragmentTypeName != null) {
                FragmentEventType fragment = type.getFragmentType(prop.getPropertyName());
                if (fragment != null) {
                    EventTypeSPI fragmentType = (EventTypeSPI) fragment.getFragmentType();
                    fragmentProp = getProperties(fragmentType, serializerClassNameProvider);
                }
            }

            // determine serializer; this is simply send along with the property description for deserialization (except fragments)
            String serializer;
            if (fragmentTypeName == null) {
                if (prop.getPropertyComponentType() != null) {
                    serializer = serializerClassNameProvider.serializerClassFor(prop.getPropertyComponentType());
                } else {
                    serializer = serializerClassNameProvider.serializerClassFor(prop.getPropertyType());
                }
            } else {
                serializer = serializerClassNameProvider.serializerClassFor(prop.getPropertyComponentType());
            }

            boolean writable = false;
            try {
                writable = type.getWritableProperty(prop.getPropertyName()) != null;
            } catch (Exception ex) {
                // expected, property name may not be parse-able
            }

            PropertyDetail json = new PropertyDetail(prop.getPropertyName(),
                    propertyType,
                    componentType,
                    writable,
                    prop.isRequiresIndex(),
                    prop.isRequiresMapkey(),
                    prop.isIndexed(),
                    prop.isMapped(),
                    fragmentTypeName,
                    fragmentTypeUnd,
                    fragmentIsIndexed,
                    fragmentIsNative,
                    fragmentProp,
                    serializer);
            coll.add(json);
        }
        return coll.toArray(new PropertyDetail[coll.size()]);
    }
}
