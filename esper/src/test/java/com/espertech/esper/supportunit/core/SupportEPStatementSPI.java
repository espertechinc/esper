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
package com.espertech.esper.supportunit.core;

import com.espertech.esper.client.*;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.core.service.EPStatementListenerSet;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementMetadata;
import com.espertech.esper.view.Viewable;

import java.lang.annotation.Annotation;
import java.util.Iterator;

public class SupportEPStatementSPI implements EPStatementSPI {

    public int getStatementId() {
        return 1;
    }

    public void setServiceIsolated(String serviceIsolated) {

    }

    public String getExpressionNoAnnotations() {
        return null;
    }

    public EPStatementListenerSet getListenerSet() {
        return null;
    }

    public void setListeners(EPStatementListenerSet listeners, boolean isRecovery) {

    }

    public void setCurrentState(EPStatementState currentState, long timeLastStateChange) {

    }

    public void setParentView(Viewable viewable) {

    }

    public StatementMetadata getStatementMetadata() {
        return null;
    }

    public StatementContext getStatementContext() {
        return null;
    }

    public boolean isNameProvided() {
        return false;
    }

    public void start() {

    }

    public void stop() {

    }

    public void destroy() {

    }

    public EPStatementState getState() {
        return null;
    }

    public boolean isStarted() {
        return false;
    }

    public boolean isStopped() {
        return false;
    }

    public boolean isDestroyed() {
        return false;
    }

    public String getText() {
        return null;
    }

    public String getName() {
        return null;
    }

    public long getTimeLastStateChange() {
        return 0;
    }

    public void setSubscriber(Object subscriber) throws EPSubscriberException {

    }

    public Object getSubscriber() {
        return null;
    }

    public boolean isPattern() {
        return false;
    }

    public Object getUserObject() {
        return null;
    }

    public void addListenerWithReplay(UpdateListener listener) {

    }

    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    public String getServiceIsolated() {
        return null;
    }

    public Iterator<EventBean> iterator(ContextPartitionSelector selector) {
        return null;
    }

    public SafeIterator<EventBean> safeIterator(ContextPartitionSelector selector) {
        return null;
    }

    public Iterator<EventBean> iterator() {
        return null;
    }

    public SafeIterator<EventBean> safeIterator() {
        return null;
    }

    public EventType getEventType() {
        return null;
    }

    public void addListener(UpdateListener listener) {

    }

    public void removeListener(UpdateListener listener) {

    }

    public void removeAllListeners() {

    }

    public void addListener(StatementAwareUpdateListener listener) {

    }

    public void removeListener(StatementAwareUpdateListener listener) {

    }

    public Iterator<StatementAwareUpdateListener> getStatementAwareListeners() {
        return null;
    }

    public Iterator<UpdateListener> getUpdateListeners() {
        return null;
    }

    public void setSubscriber(Object subscriber, String methodName) throws EPSubscriberException {
    }

    public Viewable getParentView() {
        return null;
    }
}
