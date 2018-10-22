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
package com.espertech.esper.runtime.internal.support;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.meta.EventTypeTypeClass;
import com.espertech.esper.common.client.util.AccessorStyle;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.client.util.PropertyResolutionStyle;
import com.espertech.esper.common.internal.event.bean.core.BeanEventType;
import com.espertech.esper.common.internal.event.bean.core.BeanEventTypeStemService;
import com.espertech.esper.common.internal.event.bean.introspect.BeanEventTypeStemBuilder;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactory;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactoryRuntime;
import com.espertech.esper.common.internal.event.eventtypefactory.EventTypeFactoryImpl;
import com.espertech.esper.common.internal.event.map.MapEventType;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.Map;
import java.util.function.Function;

public class SupportEventTypeFactory {
    final static BeanEventTypeStemService BEAN_STEM_SVC = new BeanEventTypeStemService(null, null, PropertyResolutionStyle.CASE_SENSITIVE, AccessorStyle.JAVABEAN);
    public final static BeanEventTypeFactory BEAN_EVENT_TYPE_FACTORY = new BeanEventTypeFactoryPrivate(new EventBeanTypedEventFactoryRuntime(null), EventTypeFactoryImpl.INSTANCE, BEAN_STEM_SVC);
    final static BeanEventTypeStemBuilder STEM_BUILDER = new BeanEventTypeStemBuilder(null, PropertyResolutionStyle.CASE_SENSITIVE);
    final static Function<String, EventTypeMetadata> METADATA_CLASS = name -> new EventTypeMetadata(name, null, EventTypeTypeClass.STREAM, EventTypeApplicationType.CLASS, NameAccessModifier.PROTECTED, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
    final static BeanEventType SUPPORTBEAN_EVENTTTPE = makeType(SupportBean.class);
    final static BeanEventType SUPPORTBEAN_S0_EVENTTTPE = makeType(SupportBean_S0.class);
    final static BeanEventType SUPPORTBEAN_S1_EVENTTTPE = makeType(SupportBean_S1.class);
    final static BeanEventType SUPPORTBEAN_S2_EVENTTTPE = makeType(SupportBean_S2.class);
    final static BeanEventType SUPPORTBEAN_A_EVENTTTPE = makeType(SupportBean_A.class);
    final static BeanEventType SUPPORTBEANCOMPLEXPROPS_EVENTTTPE = makeType(SupportBeanComplexProps.class);
    final static BeanEventType SUPPORTBEANSIMPLE_EVENTTTPE = makeType(SupportBeanSimple.class);

    public static BeanEventType createBeanType(Class clazz) {
        if (clazz == SupportBean.class) {
            return SUPPORTBEAN_EVENTTTPE;
        }
        if (clazz == SupportBean_S0.class) {
            return SUPPORTBEAN_S0_EVENTTTPE;
        }
        if (clazz == SupportBean_A.class) {
            return SUPPORTBEAN_A_EVENTTTPE;
        }
        if (clazz == SupportBeanComplexProps.class) {
            return SUPPORTBEANCOMPLEXPROPS_EVENTTTPE;
        }
        if (clazz == SupportBeanSimple.class) {
            return SUPPORTBEANSIMPLE_EVENTTTPE;
        }
        if (clazz == SupportBean_S1.class) {
            return SUPPORTBEAN_S1_EVENTTTPE;
        }
        if (clazz == SupportBean_S2.class) {
            return SUPPORTBEAN_S2_EVENTTTPE;
        }
        throw new UnsupportedOperationException("Unrecognized type " + clazz.getName());
    }

    public static EventType createMapType(Map<String, Object> map) {
        EventTypeMetadata metadata = new EventTypeMetadata(UuidGenerator.generate(), null, EventTypeTypeClass.STREAM, EventTypeApplicationType.MAP, NameAccessModifier.PROTECTED, EventTypeBusModifier.NONBUS, false, EventTypeIdPair.unassigned());
        return new MapEventType(metadata, map, null, null, null, null, BEAN_EVENT_TYPE_FACTORY);
    }

    private static BeanEventType makeType(Class clazz) {
        return new BeanEventType(STEM_BUILDER.make(clazz), METADATA_CLASS.apply(clazz.getSimpleName()), BEAN_EVENT_TYPE_FACTORY, null, null, null, null);
    }
}
