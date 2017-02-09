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
package com.espertech.esper.view.std;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprNodeUtility;
import com.espertech.esper.event.EventBeanUtility;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The group view splits the data in a stream to multiple subviews, based on a key index.
 * The key is one or more fields in the stream. Any view that follows the GROUP view will be executed
 * separately on each subview, one per unique key.
 * <p>
 * The view takes a single parameter which is the field name returning the key value to group.
 * <p>
 * This view can, for example, be used to calculate the average price per symbol for a list of symbols.
 * <p>
 * The view treats its child views and their child views as prototypes. It dynamically instantiates copies
 * of each child view and their child views, and the child view's child views as so on. When there are
 * no more child views or the special merge view is encountered, it ends. The view installs a special merge
 * view unto each leaf child view that merges the value key that was grouped by back into the stream
 * using the group-by field name.
 */
public class GroupByViewImpl extends ViewSupport implements CloneableView, GroupByView {
    public final static String VIEWNAME = "Group-By";

    private final ExprNode[] criteriaExpressions;
    private final ExprEvaluator[] criteriaEvaluators;
    protected final AgentInstanceViewFactoryChainContext agentInstanceContext;
    private EventBean[] eventsPerStream = new EventBean[1];

    protected String[] propertyNames;
    protected final Map<Object, Object> subViewsPerKey = new HashMap<Object, Object>();

    private final HashMap<Object, Pair<Object, Object>> groupedEvents = new HashMap<Object, Pair<Object, Object>>();

    /**
     * Constructor.
     *
     * @param criteriaExpressions  is the fields from which to pull the values to group by
     * @param agentInstanceContext contains required view services
     * @param criteriaEvaluators   evaluators
     */
    public GroupByViewImpl(AgentInstanceViewFactoryChainContext agentInstanceContext, ExprNode[] criteriaExpressions, ExprEvaluator[] criteriaEvaluators) {
        this.agentInstanceContext = agentInstanceContext;
        this.criteriaExpressions = criteriaExpressions;
        this.criteriaEvaluators = criteriaEvaluators;

        propertyNames = new String[criteriaExpressions.length];
        for (int i = 0; i < criteriaExpressions.length; i++) {
            propertyNames[i] = ExprNodeUtility.toExpressionStringMinPrecedenceSafe(criteriaExpressions[i]);
        }
    }

    public View cloneView() {
        return new GroupByViewImpl(agentInstanceContext, criteriaExpressions, criteriaEvaluators);
    }

    /**
     * Returns the field name that provides the key valie by which to group by.
     *
     * @return field name providing group-by key.
     */
    public ExprNode[] getCriteriaExpressions() {
        return criteriaExpressions;
    }

    public final EventType getEventType() {
        // The schema is the parent view's schema
        return parent.getEventType();
    }

    public final void update(EventBean[] newData, EventBean[] oldData) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qViewProcessIRStream(this, "Grouped", newData, oldData);
        }

        // Algorithm for single new event
        if ((newData != null) && (oldData == null) && (newData.length == 1)) {
            EventBean theEvent = newData[0];
            EventBean[] newDataToPost = new EventBean[]{theEvent};

            Object groupByValuesKey = getGroupKey(theEvent);

            // Get child views that belong to this group-by value combination
            Object subViews = this.subViewsPerKey.get(groupByValuesKey);

            // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
            if (subViews == null) {
                subViews = makeSubViews(this, propertyNames, groupByValuesKey, agentInstanceContext);
                subViewsPerKey.put(groupByValuesKey, subViews);
            }

            updateChildViews(subViews, newDataToPost, null);
        } else {
            // Algorithm for dispatching multiple events
            if (newData != null) {
                for (EventBean newValue : newData) {
                    handleEvent(newValue, true);
                }
            }

            if (oldData != null) {
                for (EventBean oldValue : oldData) {
                    handleEvent(oldValue, false);
                }
            }

            // Update child views
            for (Map.Entry<Object, Pair<Object, Object>> entry : groupedEvents.entrySet()) {
                EventBean[] newEvents = convertToArray(entry.getValue().getFirst());
                EventBean[] oldEvents = convertToArray(entry.getValue().getSecond());
                updateChildViews(entry.getKey(), newEvents, oldEvents);
            }

            groupedEvents.clear();
        }

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aViewProcessIRStream();
        }
    }

    public final Iterator<EventBean> iterator() {
        throw new UnsupportedOperationException("Cannot iterate over group view, this operation is not supported");
    }

    public final String toString() {
        return this.getClass().getName() + " groupFieldNames=" + Arrays.toString(criteriaExpressions);
    }

    /**
     * Instantiate subviews for the given group view and the given key value to group-by.
     * Makes shallow copies of each child view and its subviews up to the merge point.
     * Sets up merge data views for merging the group-by key value back in.
     *
     * @param groupView            is the parent view for which to copy subviews for
     * @param groupByValues        is the key values to group-by
     * @param agentInstanceContext is the view services that sub-views may need
     * @param propertyNames        names of expressions or properties
     * @return a single view or a list of views that are copies of the original list, with copied children, with
     * data merge views added to the copied child leaf views.
     */
    public static Object makeSubViews(GroupByView groupView, String[] propertyNames, Object groupByValues,
                                      AgentInstanceViewFactoryChainContext agentInstanceContext) {
        if (!groupView.hasViews()) {
            String message = "Unexpected empty list of child nodes for group view";
            log.error(".copySubViews " + message);
            throw new EPException(message);
        }

        Object subviewHolder;
        if (groupView.getViews().length == 1) {
            subviewHolder = copyChildView(groupView, propertyNames, groupByValues, agentInstanceContext, groupView.getViews()[0]);
        } else {
            // For each child node
            ArrayList<View> subViewList = new ArrayList<View>(4);
            subviewHolder = subViewList;
            for (View originalChildView : groupView.getViews()) {
                View copyChildView = copyChildView(groupView, propertyNames, groupByValues, agentInstanceContext, originalChildView);
                subViewList.add(copyChildView);
            }
        }

        return subviewHolder;
    }

    public void visitViewContainer(ViewDataVisitorContained viewDataVisitor) {
        viewDataVisitor.visitPrimary(VIEWNAME, subViewsPerKey.size());
        for (Map.Entry<Object, Object> entry : subViewsPerKey.entrySet()) {
            GroupByViewImpl.visitView(viewDataVisitor, entry.getKey(), entry.getValue());
        }
    }

    public static void visitView(ViewDataVisitorContained viewDataVisitor, Object groupkey, Object subviewHolder) {
        if (subviewHolder == null) {
            return;
        }
        if (subviewHolder instanceof View) {
            viewDataVisitor.visitContained(groupkey, (View) subviewHolder);
            return;
        }
        if (subviewHolder instanceof Collection) {
            Collection<View> deque = (Collection<View>) subviewHolder;
            for (View view : deque) {
                viewDataVisitor.visitContained(groupkey, view);
                return;
            }
        }
    }

    @Override
    public boolean removeView(View view) {
        if (!(view instanceof GroupableView)) {
            super.removeView(view);
        }
        boolean removed = super.removeView(view);
        if (!removed) {
            return false;
        }
        if (!hasViews()) {
            subViewsPerKey.clear();
            return true;
        }
        GroupableView removedView = (GroupableView) view;
        Deque<Object> removedKeys = null;
        for (Map.Entry<Object, Object> entry : subViewsPerKey.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof View) {
                GroupableView subview = (GroupableView) value;
                if (compareViews(subview, removedView)) {
                    if (removedKeys == null) {
                        removedKeys = new ArrayDeque<Object>();
                    }
                    removedKeys.add(entry.getKey());
                }
            } else if (value instanceof List) {
                List<View> subviews = (List<View>) value;
                for (int i = 0; i < subviews.size(); i++) {
                    GroupableView subview = (GroupableView) subviews.get(i);
                    if (compareViews(subview, removedView)) {
                        subviews.remove(i);
                        if (subviews.isEmpty()) {
                            if (removedKeys == null) {
                                removedKeys = new ArrayDeque<Object>();
                            }
                            removedKeys.add(entry.getKey());
                        }
                        break;
                    }
                }
            }
        }
        if (removedKeys != null) {
            for (Object key : removedKeys) {
                subViewsPerKey.remove(key);
            }
        }
        return true;
    }

    private boolean compareViews(GroupableView subview, GroupableView removed) {
        return subview.getViewFactory() == removed.getViewFactory();
    }

    protected static void updateChildViews(Object subViews, EventBean[] newData, EventBean[] oldData) {
        if (subViews instanceof List) {
            List<View> viewList = (List<View>) subViews;
            ViewSupport.updateChildren(viewList, newData, oldData);
        } else {
            ((View) subViews).update(newData, oldData);
        }
    }

    private void handleEvent(EventBean theEvent, boolean isNew) {
        Object groupByValuesKey = getGroupKey(theEvent);

        // Get child views that belong to this group-by value combination
        Object subViews = this.subViewsPerKey.get(groupByValuesKey);

        // If this is a new group-by value, the list of subviews is null and we need to make clone sub-views
        if (subViews == null) {
            subViews = makeSubViews(this, propertyNames, groupByValuesKey, agentInstanceContext);
            subViewsPerKey.put(groupByValuesKey, subViews);
        }

        // Construct a pair of lists to hold the events for the grouped value if not already there
        Pair<Object, Object> pair = groupedEvents.get(subViews);
        if (pair == null) {
            pair = new Pair<Object, Object>(null, null);
            groupedEvents.put(subViews, pair);
        }

        // Add event to a child view event list for later child update that includes new and old events
        if (isNew) {
            pair.setFirst(addUpgradeToDequeIfPopulated(pair.getFirst(), theEvent));
        } else {
            pair.setSecond(addUpgradeToDequeIfPopulated(pair.getSecond(), theEvent));
        }
    }

    private static View copyChildView(GroupByView groupView, String[] propertyNames, Object groupByValues, AgentInstanceViewFactoryChainContext agentInstanceContext, View originalChildView) {
        if (originalChildView instanceof MergeView) {
            String message = "Unexpected merge view as child of group-by view";
            log.error(".copySubViews " + message);
            throw new EPException(message);
        }

        if (!(originalChildView instanceof CloneableView)) {
            throw new EPException("Unexpected error copying subview " + originalChildView.getClass().getName());
        }
        CloneableView cloneableView = (CloneableView) originalChildView;

        // Copy child node
        View copyChildView = cloneableView.cloneView();
        copyChildView.setParent(groupView);

        // Make the sub views for child copying from the original to the child
        copySubViews(groupView.getCriteriaExpressions(), propertyNames, groupByValues, originalChildView, copyChildView,
                agentInstanceContext);

        return copyChildView;
    }

    private static void copySubViews(ExprNode[] criteriaExpressions, String[] propertyNames, Object groupByValues, View originalView, View copyView,
                                     AgentInstanceViewFactoryChainContext agentInstanceContext) {
        for (View subView : originalView.getViews()) {
            // Determine if view is our merge view
            if (subView instanceof MergeViewMarker) {
                MergeViewMarker mergeView = (MergeViewMarker) subView;
                if (ExprNodeUtility.deepEquals(mergeView.getGroupFieldNames(), criteriaExpressions)) {
                    if (mergeView.getEventType() != copyView.getEventType()) {
                        // We found our merge view - install a new data merge view on top of it
                        AddPropertyValueOptionalView addPropertyView = new AddPropertyValueOptionalView(agentInstanceContext, propertyNames, groupByValues, mergeView.getEventType());

                        // Add to the copied parent subview the view merge data view
                        copyView.addView(addPropertyView);

                        // Add to the new merge data view the actual single merge view instance that clients may attached to
                        addPropertyView.addView(mergeView);

                        // Add a parent view to the single merge view instance
                        mergeView.addParentView(addPropertyView);
                    } else {
                        // Add to the copied parent subview the view merge data view
                        copyView.addView(mergeView);

                        // Add a parent view to the single merge view instance
                        mergeView.addParentView(copyView);
                    }

                    continue;
                }
            }

            if (!(subView instanceof CloneableView)) {
                throw new EPException("Unexpected error copying subview");
            }
            CloneableView cloneableView = (CloneableView) subView;
            View copiedChild = cloneableView.cloneView();
            copyView.addView(copiedChild);

            // Make the sub views for child
            copySubViews(criteriaExpressions, propertyNames, groupByValues, subView, copiedChild, agentInstanceContext);
        }
    }

    private Object getGroupKey(EventBean theEvent) {
        eventsPerStream[0] = theEvent;
        if (criteriaEvaluators.length == 1) {
            return criteriaEvaluators[0].evaluate(eventsPerStream, true, agentInstanceContext);
        }

        Object[] values = new Object[criteriaEvaluators.length];
        for (int i = 0; i < criteriaEvaluators.length; i++) {
            values[i] = criteriaEvaluators[i].evaluate(eventsPerStream, true, agentInstanceContext);
        }
        return new MultiKeyUntyped(values);
    }

    protected static Object addUpgradeToDequeIfPopulated(Object holder, EventBean theEvent) {
        if (holder == null) {
            return theEvent;
        } else if (holder instanceof Deque) {
            Deque<EventBean> deque = (Deque<EventBean>) holder;
            deque.add(theEvent);
            return deque;
        } else {
            ArrayDeque<EventBean> deque = new ArrayDeque<EventBean>(4);
            deque.add((EventBean) holder);
            deque.add(theEvent);
            return deque;
        }
    }

    protected static EventBean[] convertToArray(Object eventOrDeque) {
        if (eventOrDeque == null) {
            return null;
        }
        if (eventOrDeque instanceof EventBean) {
            return new EventBean[]{(EventBean) eventOrDeque};
        }
        return EventBeanUtility.toArray((ArrayDeque<EventBean>) eventOrDeque);
    }

    private static final Logger log = LoggerFactory.getLogger(GroupByViewImpl.class);
}
