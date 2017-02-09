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
package com.espertech.esper.view;

import com.espertech.esper.client.EventBean;

/**
 * The View interface provides a way for a stream, data provider, or another view,
 * to notify an object of additions and deletions to its data set.
 * Views are themselves Viewable by other Views, and can implement their own set of cached data internally.
 * The contract is that a View is wholly derived from the object (its parent) to which it is attached.
 * That is, it must be able to reconstitute its cached state from a playback of source data passed to it
 * through the update() method.
 * <p>
 * A view's job is to derive some data from the data in the Viewable object to which it is attached.
 * This can happen by a 'push' mechanism whereby new data in the underlying collection is pushed to the view
 * through the update method. A view that operates in this mode incrementally updates its derived data and
 * then provides this data to any queries or requesters through its Data interface and potentially through
 * other customized methods it exposes. When these methods are called, the view in push mode does not contact
 * its parent: it just supplies the requester with the data it already derived. The push mode is efficient
 * when data in a view is slow-changing with respect to how much its data is requested. For example, a view
 * calculating the mean of an intermittent signal over time may be queried very frequently. It incrementally
 * updates its statistic and then provides that quantity to callers whenever they want it, which may be much
 * more frequently than the incoming signal occurs.
 * <p>
 * The 'pull' mechanism is driven by requests to the view's Data interface or other customized data access methods.
 * A view operating in 'pull' mode may know whether it is "clean" or "dirty" by listening to its update method, or
 * it may not get any calls to its update method, and have to consult its parent to re-derive data when it is called.
 * This mode is efficient when requests to a view for its data are infrequent compared to the update frequency of its
 * parent's data. For example, a temperature sensor may be changing on a near-continuous basis, and a view which
 * derives some quantity from that sensor may be queried irregularly. It is most efficient for that view to operate
 * in pull mode, and only update itself when it is asked by some consumer for its derived quantity. It then asks the
 * temperature sensor for the current temperature, does its derivation, and returns to the requester.
 * <p>
 * To feed views that are registered with it, a view should only call the update method on its child views when its own
 * data has changed. If it receives an update which results in no change to its data, it should not update any children
 * views.
 */
public interface View extends EventCollection, Viewable {
    /**
     * Returns the View's parent Viewable.
     *
     * @return viewable
     */
    public Viewable getParent();

    /**
     * Called when the View is added to a Viewable object.
     *
     * @param parent is the parent that this view is a child of
     */
    public void setParent(Viewable parent);

    /**
     * Notify that data has been added or removed from the Viewable parent.
     * The last object in the newData array of objects would be the newest object added to the parent view.
     * The first object of the oldData array of objects would be the oldest object removed from the parent view.
     * <p>
     * If the call to update contains new (inserted) data, then the first argument will be a non-empty list and the
     * second will be empty. Similarly, if the call is a notification of deleted data, then the first argument will be
     * empty and the second will be non-empty. Either the newData or oldData will be non-null.
     * This method won't be called with both arguments being null, but either one could be null.
     * The same is true for zero-length arrays. Either newData or oldData will be non-empty.
     * If both are non-empty, then the update is a modification notification.
     * <p>
     * When update() is called on a view by the parent object, the data in newData will be in the collection of the
     * parent, and its data structures will be arranged to reflect that.
     * The data in oldData will not be in the parent's data structures, and any access to the parent will indicate that
     * that data is no longer there.
     *
     * @param newData is the new data that has been added to the parent view
     * @param oldData is the old data that has been removed from the parent view
     */
    public void update(EventBean[] newData, EventBean[] oldData);
}
