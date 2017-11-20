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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.spec.ContextDetailCondition;
import com.espertech.esper.epl.spec.ContextDetailConditionFilter;
import com.espertech.esper.epl.spec.ContextDetailConditionPattern;
import com.espertech.esper.epl.spec.ContextDetailPartitioned;

import java.util.*;

public class ContextPropertyEventType {
    public static final String PROP_CTX_NAME = "name";
    public static final String PROP_CTX_ID = "id";
    public static final String PROP_CTX_LABEL = "label";
    public static final String PROP_CTX_STARTTIME = "startTime";
    public static final String PROP_CTX_ENDTIME = "endTime";
    public static final String PROP_CTX_KEY_PREFIX = "key";

    private final static List<ContextProperty> LIST_INITIATEDTERM_PROPS;
    private final static List<ContextProperty> LIST_CATEGORY_PROPS;
    private final static List<ContextProperty> LIST_PARTITION_PROPS;
    private final static List<ContextProperty> LIST_HASH_PROPS;
    private final static List<ContextProperty> LIST_NESTED_PROPS;

    static {
        LIST_INITIATEDTERM_PROPS = new ArrayList<ContextProperty>();
        LIST_INITIATEDTERM_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ID, Integer.class));
        LIST_INITIATEDTERM_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_NAME, String.class));
        LIST_INITIATEDTERM_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_STARTTIME, Long.class));
        LIST_INITIATEDTERM_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ENDTIME, Long.class));

        LIST_CATEGORY_PROPS = new ArrayList<ContextProperty>();
        LIST_CATEGORY_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_NAME, String.class));
        LIST_CATEGORY_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ID, Integer.class));
        LIST_CATEGORY_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_LABEL, String.class));

        LIST_PARTITION_PROPS = new ArrayList<ContextProperty>();
        LIST_PARTITION_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_NAME, String.class));
        LIST_PARTITION_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ID, Integer.class));

        LIST_HASH_PROPS = new ArrayList<ContextProperty>();
        LIST_HASH_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_NAME, String.class));
        LIST_HASH_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ID, Integer.class));

        LIST_NESTED_PROPS = new ArrayList<ContextProperty>();
        LIST_NESTED_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_NAME, String.class));
        LIST_NESTED_PROPS.add(new ContextProperty(ContextPropertyEventType.PROP_CTX_ID, Integer.class));
    }

    public static Map<String, Object> getCategorizedType() {
        return makeEventType(LIST_CATEGORY_PROPS, Collections.<String, Object>emptyMap());
    }

    public static Map<String, Object> getCategorizedBean(String contextName, int agentInstanceId, String label) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_CTX_NAME, contextName);
        props.put(PROP_CTX_ID, agentInstanceId);
        props.put(PROP_CTX_LABEL, label);
        return props;
    }

    public static Map<String, Object> getInitiatedTerminatedType() {
        return makeEventType(LIST_INITIATEDTERM_PROPS, Collections.EMPTY_MAP);
    }

    public static void addEndpointTypes(String contextName, ContextDetailCondition endpoint, Map<String, Object> properties, Set<String> allTags) throws ExprValidationException {
        if (endpoint instanceof ContextDetailConditionFilter) {
            ContextDetailConditionFilter filter = (ContextDetailConditionFilter) endpoint;
            if (filter.getOptionalFilterAsName() != null) {
                if (properties.containsKey(filter.getOptionalFilterAsName())) {
                    throw new ExprValidationException("For context '" + contextName + "' the stream or tag name '" + filter.getOptionalFilterAsName() + "' is already declared");
                }
                allTags.add(filter.getOptionalFilterAsName());
                properties.put(filter.getOptionalFilterAsName(), filter.getFilterSpecCompiled().getFilterForEventType());
            }
        }
        if (endpoint instanceof ContextDetailConditionPattern) {
            ContextDetailConditionPattern pattern = (ContextDetailConditionPattern) endpoint;
            for (Map.Entry<String, Pair<EventType, String>> entry : pattern.getPatternCompiled().getTaggedEventTypes().entrySet()) {
                if (properties.containsKey(entry.getKey()) && !properties.get(entry.getKey()).equals(entry.getValue().getFirst())) {
                    throw new ExprValidationException("For context '" + contextName + "' the stream or tag name '" + entry.getKey() + "' is already declared");
                }
                allTags.add(entry.getKey());
                properties.put(entry.getKey(), entry.getValue().getFirst());
            }
        }
    }

    public static Map<String, Object> getTempOverlapBean(String contextName, int agentInstanceId, Map<String, Object> matchEvent, EventBean theEvent, String filterAsName) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_CTX_NAME, contextName);
        props.put(PROP_CTX_ID, agentInstanceId);
        if (matchEvent != null) {
            props.putAll(matchEvent);
        } else {
            props.put(filterAsName, theEvent);
        }
        return props;
    }

    public static Map<String, Object> getPartitionType(ContextDetailPartitioned segmentedSpec, Class[] propertyTypes) {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        for (int i = 0; i < segmentedSpec.getItems().get(0).getPropertyNames().size(); i++) {
            String propertyName = ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
            props.put(propertyName, propertyTypes[i]);
        }
        return makeEventType(ContextPropertyEventType.LIST_PARTITION_PROPS, props);
    }

    public static Map<String, Object> getPartitionBean(String contextName, int agentInstanceId, Object keyValue, List<String> propertyNames, Map<String, Object> initEvents) {
        Object[] agentInstanceProperties;
        if (propertyNames.size() == 1) {
            agentInstanceProperties = new Object[]{keyValue};
        } else {
            agentInstanceProperties = ((MultiKeyUntyped) keyValue).getKeys();
        }

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_CTX_NAME, contextName);
        props.put(PROP_CTX_ID, agentInstanceId);
        for (int i = 0; i < agentInstanceProperties.length; i++) {
            String propertyName = ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
            props.put(propertyName, agentInstanceProperties[i]);
        }
        if (!initEvents.isEmpty()) {
            for (Map.Entry<String, Object> entry : initEvents.entrySet()) {
                props.put(entry.getKey(), entry.getValue());
            }
        }
        return props;
    }

    public static Map<String, Object> getNestedTypeBase() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        return makeEventType(ContextPropertyEventType.LIST_NESTED_PROPS, props);
    }

    public static Map<String, Object> getNestedBeanBase(String contextName, int contextPartitionId) {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put(PROP_CTX_NAME, contextName);
        props.put(PROP_CTX_ID, contextPartitionId);
        return props;
    }

    public static Map<String, Object> getHashType() {
        return makeEventType(ContextPropertyEventType.LIST_HASH_PROPS, Collections.EMPTY_MAP);
    }

    public static Map<String, Object> getHashBean(String contextName, int agentInstanceId) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(PROP_CTX_NAME, contextName);
        props.put(PROP_CTX_ID, agentInstanceId);
        return props;
    }

    private static Map<String, Object> makeEventType(List<ContextProperty> builtin, Map<String, Object> additionalProperties) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>(additionalProperties);
        for (ContextProperty prop : builtin) {
            properties.put(prop.getPropertyName(), prop.getPropertyType());
        }
        return properties;
    }

    public static class ContextProperty {

        private final String propertyName;
        private final Class propertyType;

        public ContextProperty(String propertyName, Class propertyType) {
            this.propertyName = propertyName;
            this.propertyType = propertyType;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Class getPropertyType() {
            return propertyType;
        }
    }
}
