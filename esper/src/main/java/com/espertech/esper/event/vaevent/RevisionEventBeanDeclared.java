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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;

/**
 * Revision event bean for the overlayed scheme.
 */
public class RevisionEventBeanDeclared implements EventBean {
    private final RevisionEventType revisionEventType;
    private final EventBean underlyingFullOrDelta;

    private Object key;
    private EventBean lastBaseEvent;
    private RevisionBeanHolder[] holders;
    private boolean isLatest;

    /**
     * Ctor.
     *
     * @param eventType  revision event type
     * @param underlying event wrapped
     */
    public RevisionEventBeanDeclared(RevisionEventType eventType, EventBean underlying) {
        this.revisionEventType = eventType;
        this.underlyingFullOrDelta = underlying;
    }

    /**
     * Returns true if latest event, or false if not.
     *
     * @return indicator if latest
     */
    public boolean isLatest() {
        return isLatest;
    }

    /**
     * Set flag to indicate there is a later event.
     *
     * @param latest flag to set
     */
    public void setLatest(boolean latest) {
        isLatest = latest;
    }

    /**
     * Sets the key value.
     *
     * @param key value
     */
    public void setKey(Object key) {
        this.key = key;
    }

    /**
     * Sets the last base event.
     *
     * @param lastBaseEvent base event
     */
    public void setLastBaseEvent(EventBean lastBaseEvent) {
        this.lastBaseEvent = lastBaseEvent;
    }

    /**
     * Sets versions.
     *
     * @param holders versions
     */
    public void setHolders(RevisionBeanHolder[] holders) {
        this.holders = holders;
    }

    /**
     * Returns last base event.
     *
     * @return base event
     */
    public EventBean getLastBaseEvent() {
        return lastBaseEvent;
    }

    /**
     * Returns wrapped event.
     *
     * @return wrapped event
     */
    public EventBean getUnderlyingFullOrDelta() {
        return underlyingFullOrDelta;
    }

    /**
     * Returns the key.
     *
     * @return key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Returns the revision event type.
     *
     * @return type
     */
    public RevisionEventType getRevisionEventType() {
        return revisionEventType;
    }

    public EventType getEventType() {
        return revisionEventType;
    }

    public Object get(String property) throws PropertyAccessException {
        EventPropertyGetter getter = revisionEventType.getGetter(property);
        if (getter == null) {
            return null;
        }
        return getter.get(this);
    }

    public Object getUnderlying() {
        return RevisionEventBeanDeclared.class;
    }

    public Object getFragment(String propertyExpression) {
        EventPropertyGetter getter = revisionEventType.getGetter(propertyExpression);
        if (getter == null) {
            throw PropertyAccessException.notAValidProperty(propertyExpression);
        }
        return getter.getFragment(this);
    }

    /**
     * Returns a versioned value.
     *
     * @param parameters getter parameters
     * @return value
     */
    public Object getVersionedValue(RevisionGetterParameters parameters) {
        RevisionBeanHolder holderMostRecent = null;

        if (holders != null) {
            for (int numSet : parameters.getPropertyGroups()) {
                RevisionBeanHolder holder = holders[numSet];
                if (holder != null) {
                    if (holderMostRecent == null) {
                        holderMostRecent = holder;
                    } else {
                        if (holder.getVersion() > holderMostRecent.getVersion()) {
                            holderMostRecent = holder;
                        }
                    }
                }
            }
        }

        // none found, use last full event
        if (holderMostRecent == null) {
            if (lastBaseEvent == null) {
                return null;
            }
            return parameters.getBaseGetter().get(lastBaseEvent);
        }

        return holderMostRecent.getValueForProperty(parameters.getPropertyNumber());
    }
}
