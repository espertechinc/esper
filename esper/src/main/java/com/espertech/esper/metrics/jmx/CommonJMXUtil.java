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
package com.espertech.esper.metrics.jmx;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.modelmbean.*;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.*;

public class CommonJMXUtil {
    private final static Logger log = LoggerFactory.getLogger(CommonJMXUtil.class);

    private static final Object LOCK = new Object();

    public static void registerMbean(Object mbean, MetricName name) {
        ObjectName on;
        try {
            on = new ObjectName(name.getMBeanName());
        } catch (MalformedObjectNameException e) {
            log.error("Failed to obtain object name for '" + name.getMBeanName() + "': " + e.getMessage(), e);
            return;
        }
        registerMbean(mbean, on);
    }

    public static void registerMBeanNonModel(ObjectName objectName, Object internal) {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(internal, objectName);
        } catch (Exception e) {
            log.error("Error registering mbean: " + e.getMessage(), e);
        }
    }

    public static void unregisterMbean(MBeanServer server, ObjectName name) {
        try {
            server.unregisterMBean(name);
        } catch (Exception e) {
            log.error("Error unregistering mbean: " + e.getMessage(), e);
        }
    }

    public static void unregisterMbean(MetricName name) {
        ObjectName on;
        try {
            on = new ObjectName(name.getMBeanName());
        } catch (MalformedObjectNameException e) {
            log.error("Failed to obtain object name for '" + name.getMBeanName() + "': " + e.getMessage(), e);
            return;
        }
        unregisterMbean(on);
    }

    public static String buildDefaultURL(String host, int rmiport, int jmxport) {
        return "service:jmx:rmi://" + host + ":" + jmxport + "/jndi/rmi://" + host + ":" + rmiport + "/jmxrmi";
    }

    private static void unregisterMbean(ObjectName name) {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
        } catch (Exception e) {
            log.error("Error unregistering mbean: " + e.getMessage(), e);
        }
    }

    private static void registerMbean(Object mbean, ObjectName name) {
        try {
            registerMbean(ManagementFactory.getPlatformMBeanServer(), createModelMBean(mbean), name);
        } catch (Exception e) {
            log.error("Error registering mbean: " + e.getMessage(), e);
        }
    }

    private static void registerMbean(MBeanServer server, ModelMBean mbean, ObjectName name) {
        try {
            synchronized (LOCK) {
                if (server.isRegistered(name))
                    unregisterMbean(server, name);
                server.registerMBean(mbean, name);
            }
        } catch (Exception e) {
            log.error("Error registering mbean:" + e.getMessage(), e);
        }
    }

    private static ModelMBean createModelMBean(Object o) {
        try {
            ModelMBean mbean = new RequiredModelMBean();
            JmxManaged annotation = o.getClass().getAnnotation(JmxManaged.class);
            String description = annotation == null ? "" : annotation.description();
            ModelMBeanInfo info = new ModelMBeanInfoSupport(o.getClass().getName(),
                    description,
                    extractAttributeInfo(o),
                    new ModelMBeanConstructorInfo[0],
                    extractOperationInfo(o),
                    new ModelMBeanNotificationInfo[0]);
            mbean.setModelMBeanInfo(info);
            mbean.setManagedResource(o, "ObjectReference");

            return mbean;
        } catch (MBeanException e) {
            throw new RuntimeException(e);
        } catch (InvalidTargetObjectTypeException e) {
            throw new RuntimeException(e);
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static ModelMBeanOperationInfo[] extractOperationInfo(Object object) {
        ArrayList<ModelMBeanOperationInfo> infos = new ArrayList<ModelMBeanOperationInfo>();
        for (Method m : object.getClass().getMethods()) {
            JmxOperation jmxOperation = m.getAnnotation(JmxOperation.class);
            JmxGetter jmxGetter = m.getAnnotation(JmxGetter.class);
            JmxSetter jmxSetter = m.getAnnotation(JmxSetter.class);
            if (jmxOperation != null || jmxGetter != null || jmxSetter != null) {
                String description = "";
                int visibility = 1;
                int impact = MBeanOperationInfo.UNKNOWN;
                if (jmxOperation != null) {
                    description = jmxOperation.description();
                    impact = jmxOperation.impact();
                } else if (jmxGetter != null) {
                    description = jmxGetter.description();
                    impact = MBeanOperationInfo.INFO;
                    visibility = 4;
                } else if (jmxSetter != null) {
                    description = jmxSetter.description();
                    impact = MBeanOperationInfo.ACTION;
                    visibility = 4;
                }
                ModelMBeanOperationInfo info = new ModelMBeanOperationInfo(m.getName(),
                        description,
                        extractParameterInfo(m),
                        m.getReturnType()
                                .getName(), impact);
                info.getDescriptor().setField("visibility", Integer.toString(visibility));
                infos.add(info);
            }
        }

        return infos.toArray(new ModelMBeanOperationInfo[infos.size()]);
    }

    private static ModelMBeanAttributeInfo[] extractAttributeInfo(Object object) {
        Map<String, Method> getters = new HashMap<String, Method>();
        Map<String, Method> setters = new HashMap<String, Method>();
        Map<String, String> descriptions = new HashMap<String, String>();
        for (Method m : object.getClass().getMethods()) {
            JmxGetter getter = m.getAnnotation(JmxGetter.class);
            if (getter != null) {
                getters.put(getter.name(), m);
                descriptions.put(getter.name(), getter.description());
            }
            JmxSetter setter = m.getAnnotation(JmxSetter.class);
            if (setter != null) {
                setters.put(setter.name(), m);
                descriptions.put(setter.name(), setter.description());
            }
        }

        Set<String> attributes = new HashSet<String>(getters.keySet());
        attributes.addAll(setters.keySet());
        List<ModelMBeanAttributeInfo> infos = new ArrayList<ModelMBeanAttributeInfo>();
        for (String name : attributes) {
            try {
                Method getter = getters.get(name);
                Method setter = setters.get(name);
                ModelMBeanAttributeInfo info = new ModelMBeanAttributeInfo(name,
                        descriptions.get(name),
                        getter,
                        setter);
                Descriptor descriptor = info.getDescriptor();
                if (getter != null)
                    descriptor.setField("getMethod", getter.getName());
                if (setter != null)
                    descriptor.setField("setMethod", setter.getName());
                info.setDescriptor(descriptor);
                infos.add(info);
            } catch (IntrospectionException e) {
                throw new RuntimeException(e);
            }
        }

        return infos.toArray(new ModelMBeanAttributeInfo[infos.size()]);
    }

    private static MBeanParameterInfo[] extractParameterInfo(Method m) {
        Class<?>[] types = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();
        MBeanParameterInfo[] params = new MBeanParameterInfo[types.length];
        for (int i = 0; i < params.length; i++) {
            boolean hasAnnotation = false;
            for (int j = 0; j < annotations[i].length; j++) {
                if (annotations[i][j] instanceof JmxParam) {
                    JmxParam param = (JmxParam) annotations[i][j];
                    params[i] = new MBeanParameterInfo(param.name(),
                            types[i].getName(),
                            param.description());
                    hasAnnotation = true;
                    break;
                }
            }
            if (!hasAnnotation) {
                params[i] = new MBeanParameterInfo("", types[i].getName(), "");
            }
        }

        return params;
    }
}
