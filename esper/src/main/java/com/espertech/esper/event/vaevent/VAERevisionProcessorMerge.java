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
package com.espertech.esper.event.vaevent;

import com.espertech.esper.client.ConfigurationRevisionEventType;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.EPStatementAgentInstanceHandle;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.lookup.EventTableIndexRepository;
import com.espertech.esper.epl.named.NamedWindowRootViewInstance;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.EventTypeIdGenerator;
import com.espertech.esper.event.EventTypeMetadata;
import com.espertech.esper.view.StatementStopCallback;
import com.espertech.esper.view.StatementStopService;
import com.espertech.esper.view.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Provides a set of merge-strategies for merging individual properties (rather then overlaying groups).
 */
public class VAERevisionProcessorMerge extends VAERevisionProcessorBase implements ValueAddEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(VAERevisionProcessorMerge.class);

    private final RevisionTypeDesc infoFullType;
    private final Map<Object, RevisionStateMerge> statePerKey;
    private final UpdateStrategy updateStrategy;

    /**
     * Ctor.
     *
     * @param revisioneventTypeName name
     * @param spec                  specification
     * @param statementStopService  for stop handling
     * @param eventAdapterService   for nested property handling
     * @param eventTypeIdGenerator  type id gen
     */
    public VAERevisionProcessorMerge(String revisioneventTypeName, RevisionSpec spec, StatementStopService statementStopService, EventAdapterService eventAdapterService, EventTypeIdGenerator eventTypeIdGenerator) {
        super(spec, revisioneventTypeName, eventAdapterService);

        // on statement stop, remove versions
        statementStopService.addSubscriber(new StatementStopCallback() {
            public void statementStopped() {
                statePerKey.clear();
            }
        });

        this.statePerKey = new HashMap<Object, RevisionStateMerge>();

        // For all changeset properties, add type descriptors (property number, getter etc)
        Map<String, RevisionPropertyTypeDesc> propertyDesc = new HashMap<String, RevisionPropertyTypeDesc>();
        int count = 0;

        for (String property : spec.getChangesetPropertyNames()) {
            EventPropertyGetter fullGetter = spec.getBaseEventType().getGetter(property);
            int propertyNumber = count;
            final RevisionGetterParameters parameters = new RevisionGetterParameters(property, propertyNumber, fullGetter, null);

            // if there are no groups (full event property only), then simply use the full event getter
            EventPropertyGetterSPI revisionGetter = new VAERevisionEventPropertyGetterMerge(parameters);
            Class type = spec.getBaseEventType().getPropertyType(property);
            if (type == null) {
                for (EventType deltaType : spec.getDeltaTypes()) {
                    Class dtype = deltaType.getPropertyType(property);
                    if (dtype != null) {
                        type = dtype;
                        break;
                    }
                }
            }
            RevisionPropertyTypeDesc propertyTypeDesc = new RevisionPropertyTypeDesc(revisionGetter, parameters, type);
            propertyDesc.put(property, propertyTypeDesc);
            count++;
        }

        count = 0;
        for (String property : spec.getKeyPropertyNames()) {
            final int keyPropertyNumber = count;

            EventPropertyGetterSPI revisionGetter;
            if (spec.getKeyPropertyNames().length == 1) {
                revisionGetter = new VAERevisionEventPropertyGetterMergeOneKey();
            } else {
                revisionGetter = new VAERevisionEventPropertyGetterMergeNKey(keyPropertyNumber);
            }

            Class type = spec.getBaseEventType().getPropertyType(property);
            if (type == null) {
                for (EventType deltaType : spec.getDeltaTypes()) {
                    Class dtype = deltaType.getPropertyType(property);
                    if (dtype != null) {
                        type = dtype;
                        break;
                    }
                }
            }
            RevisionPropertyTypeDesc propertyTypeDesc = new RevisionPropertyTypeDesc(revisionGetter, null, type);
            propertyDesc.put(property, propertyTypeDesc);
            count++;
        }

        // compile for each event type a list of getters and indexes within the overlay
        for (EventType deltaType : spec.getDeltaTypes()) {
            RevisionTypeDesc typeDesc = makeTypeDesc(deltaType, spec.getPropertyRevision());
            typeDescriptors.put(deltaType, typeDesc);
        }
        infoFullType = makeTypeDesc(spec.getBaseEventType(), spec.getPropertyRevision());

        // how to handle updates to a full event
        if (spec.getPropertyRevision() == ConfigurationRevisionEventType.PropertyRevision.MERGE_DECLARED) {
            updateStrategy = new UpdateStrategyDeclared(spec);
        } else if (spec.getPropertyRevision() == ConfigurationRevisionEventType.PropertyRevision.MERGE_NON_NULL) {
            updateStrategy = new UpdateStrategyNonNull(spec);
        } else if (spec.getPropertyRevision() == ConfigurationRevisionEventType.PropertyRevision.MERGE_EXISTS) {
            updateStrategy = new UpdateStrategyExists(spec);
        } else {
            throw new IllegalArgumentException("Unknown revision type '" + spec.getPropertyRevision() + "'");
        }

        EventTypeMetadata metadata = EventTypeMetadata.createValueAdd(revisioneventTypeName, EventTypeMetadata.TypeClass.REVISION);
        revisionEventType = new RevisionEventType(metadata, eventTypeIdGenerator.getTypeId(revisioneventTypeName), propertyDesc, eventAdapterService);
    }

    public EventBean getValueAddEventBean(EventBean theEvent) {
        return new RevisionEventBeanMerge(revisionEventType, theEvent);
    }

    public void onUpdate(EventBean[] newData, EventBean[] oldData, NamedWindowRootViewInstance namedWindowRootView, EventTableIndexRepository indexRepository) {
        // If new data is filled, it is not a delete
        if ((newData == null) || (newData.length == 0)) {
            // we are removing an event
            RevisionEventBeanMerge revisionEvent = (RevisionEventBeanMerge) oldData[0];
            Object key = revisionEvent.getKey();
            statePerKey.remove(key);

            // Insert into indexes for fast deletion, if there are any
            for (EventTable table : indexRepository.getTables()) {
                table.remove(oldData, namedWindowRootView.getAgentInstanceContext());
            }

            // make as not the latest event since its due for removal
            revisionEvent.setLatest(false);

            namedWindowRootView.updateChildren(null, oldData);
            return;
        }

        RevisionEventBeanMerge revisionEvent = (RevisionEventBeanMerge) newData[0];
        EventBean underlyingEvent = revisionEvent.getUnderlyingFullOrDelta();
        EventType underyingEventType = underlyingEvent.getEventType();

        // obtain key values
        Object key = null;
        RevisionTypeDesc typesDesc;
        boolean isBaseEventType = false;
        if (underyingEventType == revisionSpec.getBaseEventType()) {
            typesDesc = infoFullType;
            key = PropertyUtility.getKeys(underlyingEvent, infoFullType.getKeyPropertyGetters());
            isBaseEventType = true;
        } else {
            typesDesc = typeDescriptors.get(underyingEventType);

            // if this type cannot be found, check all supertypes, if any
            if (typesDesc == null) {
                Iterator<EventType> superTypes = underyingEventType.getDeepSuperTypes();
                if (superTypes != null) {
                    EventType superType;
                    for (; superTypes.hasNext(); ) {
                        superType = superTypes.next();
                        if (superType == revisionSpec.getBaseEventType()) {
                            typesDesc = infoFullType;
                            key = PropertyUtility.getKeys(underlyingEvent, infoFullType.getKeyPropertyGetters());
                            isBaseEventType = true;
                            break;
                        }
                        typesDesc = typeDescriptors.get(superType);
                        if (typesDesc != null) {
                            typeDescriptors.put(underyingEventType, typesDesc);
                            key = PropertyUtility.getKeys(underlyingEvent, typesDesc.getKeyPropertyGetters());
                            break;
                        }
                    }
                }
            } else {
                key = PropertyUtility.getKeys(underlyingEvent, typesDesc.getKeyPropertyGetters());
            }
        }

        // get the state for this key value
        RevisionStateMerge revisionState = statePerKey.get(key);

        // Delta event and no full
        if ((!isBaseEventType) && (revisionState == null)) {
            return; // Ignore the event, its a delta and we don't currently have a full event for it
        }

        // New full event
        if (revisionState == null) {
            revisionState = new RevisionStateMerge(underlyingEvent, null, null);
            statePerKey.put(key, revisionState);

            // prepare revison event
            revisionEvent.setLastBaseEvent(underlyingEvent);
            revisionEvent.setKey(key);
            revisionEvent.setOverlay(null);
            revisionEvent.setLatest(true);

            // Insert into indexes for fast deletion, if there are any
            for (EventTable table : indexRepository.getTables()) {
                table.add(newData, namedWindowRootView.getAgentInstanceContext());
            }

            // post to data window
            revisionState.setLastEvent(revisionEvent);
            namedWindowRootView.updateChildren(new EventBean[]{revisionEvent}, null);
            return;
        }

        // handle update, changing revision state and event as required
        updateStrategy.handleUpdate(isBaseEventType, revisionState, revisionEvent, typesDesc);

        // prepare revision event
        revisionEvent.setLastBaseEvent(revisionState.getBaseEventUnderlying());
        revisionEvent.setOverlay(revisionState.getOverlays());
        revisionEvent.setKey(key);
        revisionEvent.setLatest(true);

        // get prior event
        RevisionEventBeanMerge lastEvent = revisionState.getLastEvent();
        lastEvent.setLatest(false);

        // data to post
        EventBean[] newDataPost = new EventBean[]{revisionEvent};
        EventBean[] oldDataPost = new EventBean[]{lastEvent};

        // update indexes
        for (EventTable table : indexRepository.getTables()) {
            table.remove(oldDataPost, namedWindowRootView.getAgentInstanceContext());
            table.add(newDataPost, namedWindowRootView.getAgentInstanceContext());
        }

        // keep reference to last event
        revisionState.setLastEvent(revisionEvent);

        namedWindowRootView.updateChildren(newDataPost, oldDataPost);
    }

    public Collection<EventBean> getSnapshot(EPStatementAgentInstanceHandle createWindowStmtHandle, Viewable parent) {
        createWindowStmtHandle.getStatementAgentInstanceLock().acquireReadLock();
        try {
            Iterator<EventBean> it = parent.iterator();
            if (!it.hasNext()) {
                return Collections.EMPTY_LIST;
            }
            ArrayDeque<EventBean> list = new ArrayDeque<EventBean>();
            while (it.hasNext()) {
                RevisionEventBeanMerge fullRevision = (RevisionEventBeanMerge) it.next();
                Object key = fullRevision.getKey();
                RevisionStateMerge state = statePerKey.get(key);
                list.add(state.getLastEvent());
            }
            return list;
        } finally {
            createWindowStmtHandle.getStatementAgentInstanceLock().releaseReadLock();
        }
    }

    public void removeOldData(EventBean[] oldData, EventTableIndexRepository indexRepository, AgentInstanceContext agentInstanceContext) {
        for (EventBean anOldData : oldData) {
            RevisionEventBeanMerge theEvent = (RevisionEventBeanMerge) anOldData;

            // If the remove event is the latest event, remove from all caches
            if (theEvent.isLatest()) {
                Object key = theEvent.getKey();
                statePerKey.remove(key);

                for (EventTable table : indexRepository.getTables()) {
                    table.remove(oldData, agentInstanceContext);
                }
            }
        }
    }

    private RevisionTypeDesc makeTypeDesc(EventType eventType, ConfigurationRevisionEventType.PropertyRevision propertyRevision) {
        EventPropertyGetter[] keyPropertyGetters = PropertyUtility.getGetters(eventType, revisionSpec.getKeyPropertyNames());

        int len = revisionSpec.getChangesetPropertyNames().length;
        List<EventPropertyGetter> listOfGetters = new ArrayList<EventPropertyGetter>();
        List<Integer> listOfIndexes = new ArrayList<Integer>();

        for (int i = 0; i < len; i++) {
            String propertyName = revisionSpec.getChangesetPropertyNames()[i];
            EventPropertyGetter getter = null;

            if (propertyRevision != ConfigurationRevisionEventType.PropertyRevision.MERGE_EXISTS) {
                getter = eventType.getGetter(revisionSpec.getChangesetPropertyNames()[i]);
            } else {
                // only declared properties may be used a dynamic properties to avoid confusion of properties suddenly appearing
                for (String propertyNamesDeclared : eventType.getPropertyNames()) {
                    if (propertyNamesDeclared.equals(propertyName)) {
                        // use dynamic properties
                        getter = eventType.getGetter(revisionSpec.getChangesetPropertyNames()[i] + "?");
                        break;
                    }
                }
            }

            if (getter != null) {
                listOfGetters.add(getter);
                listOfIndexes.add(i);
            }
        }

        EventPropertyGetter[] changesetPropertyGetters = listOfGetters.toArray(new EventPropertyGetter[listOfGetters.size()]);
        int[] changesetPropertyIndex = new int[listOfIndexes.size()];
        for (int i = 0; i < listOfIndexes.size(); i++) {
            changesetPropertyIndex[i] = listOfIndexes.get(i);
        }

        return new RevisionTypeDesc(keyPropertyGetters, changesetPropertyGetters, changesetPropertyIndex);
    }
}
