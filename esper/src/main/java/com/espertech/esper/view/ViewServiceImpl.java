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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.spec.StreamSpecOptions;
import com.espertech.esper.epl.spec.ViewSpec;
import com.espertech.esper.view.internal.IntersectViewFactory;
import com.espertech.esper.view.internal.UnionViewFactory;
import com.espertech.esper.view.std.GroupByViewFactoryMarker;
import com.espertech.esper.view.std.MergeViewFactory;
import com.espertech.esper.view.std.MergeViewFactoryMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of the view evaluation service business interface.
 */
public final class ViewServiceImpl implements ViewService {
    /**
     * Ctor.
     */
    public ViewServiceImpl() {
    }

    public ViewFactoryChain createFactories(int streamNum,
                                            EventType parentEventType,
                                            ViewSpec[] viewSpecDefinitions,
                                            StreamSpecOptions options,
                                            StatementContext context,
                                            boolean isSubquery,
                                            int subqueryNumber)
            throws ViewProcessingException {
        // Clone the view spec list to prevent parameter modification
        List<ViewSpec> viewSpecList = new ArrayList<ViewSpec>(Arrays.asList(viewSpecDefinitions));

        // Inspect views and add merge views if required
        ViewServiceHelper.addMergeViews(viewSpecList);

        // Instantiate factories, not making them aware of each other yet
        List<ViewFactory> viewFactories = ViewServiceHelper.instantiateFactories(streamNum, viewSpecList, context, isSubquery, subqueryNumber);

        ViewFactory parentViewFactory = null;
        List<ViewFactory> attachedViewFactories = new ArrayList<ViewFactory>();
        for (int i = 0; i < viewFactories.size(); i++) {
            ViewFactory factoryToAttach = viewFactories.get(i);
            try {
                factoryToAttach.attach(parentEventType, context, parentViewFactory, attachedViewFactories);
                attachedViewFactories.add(viewFactories.get(i));
                parentEventType = factoryToAttach.getEventType();
            } catch (ViewParameterException ex) {
                String text = "Error attaching view to parent view";
                if (i == 0) {
                    text = "Error attaching view to event stream";
                }
                throw new ViewProcessingException(text + ": " + ex.getMessage(), ex);
            }
        }

        // obtain count of data windows
        int dataWindowCount = 0;
        int firstNonDataWindowIndex = -1;
        for (int i = 0; i < viewFactories.size(); i++) {
            ViewFactory factory = viewFactories.get(i);
            if (factory instanceof DataWindowViewFactory) {
                dataWindowCount++;
                continue;
            }
            if ((factory instanceof GroupByViewFactoryMarker) || (factory instanceof MergeViewFactory)) {
                continue;
            }
            if (firstNonDataWindowIndex == -1) {
                firstNonDataWindowIndex = i;
            }
        }

        boolean isAllowMultipleExpiry = context.getConfigSnapshot().getEngineDefaults().getViewResources().isAllowMultipleExpiryPolicies();
        boolean isRetainIntersection = options.isRetainIntersection();
        boolean isRetainUnion = options.isRetainUnion();

        // Set the default to retain-intersection unless allow-multiple-expiry is turned on
        if ((!isAllowMultipleExpiry) && (!isRetainUnion)) {
            isRetainIntersection = true;
        }

        // handle multiple data windows with retain union.
        // wrap view factories into the union view factory and handle a group-by, if present
        if ((isRetainUnion || isRetainIntersection) && dataWindowCount > 1) {
            viewFactories = getRetainViewFactories(parentEventType, viewFactories, isRetainUnion, context);
        }

        return new ViewFactoryChain(parentEventType, viewFactories);
    }

    private List<ViewFactory> getRetainViewFactories(EventType parentEventType, List<ViewFactory> viewFactories, boolean isUnion, StatementContext context)
            throws ViewProcessingException {
        Set<Integer> groupByFactory = new HashSet<>();
        Set<Integer> mergeFactory = new HashSet<Integer>();
        List<ViewFactory> derivedValueViews = new ArrayList<ViewFactory>();
        List<ViewFactory> dataWindowViews = new ArrayList<ViewFactory>();
        for (int i = 0; i < viewFactories.size(); i++) {
            ViewFactory factory = viewFactories.get(i);
            if (factory instanceof GroupByViewFactoryMarker) {
                groupByFactory.add(i);
            } else if (factory instanceof MergeViewFactoryMarker) {
                mergeFactory.add(i);
            } else if (factory instanceof DataWindowViewFactory) {
                dataWindowViews.add(factory);
            } else {
                derivedValueViews.add(factory);
            }
        }

        if ((!groupByFactory.isEmpty()) && (groupByFactory.iterator().next() != 0)) {
            throw new ViewProcessingException("The groupwin view must occur in the first position in conjunction with multiple data windows");
        }
        if ((!groupByFactory.isEmpty()) && (mergeFactory.iterator().next() != (viewFactories.size() - 1))) {
            throw new ViewProcessingException("The merge view cannot be used in conjunction with multiple data windows");
        }

        GroupByViewFactoryMarker groupByViewFactory = null;
        MergeViewFactoryMarker mergeViewFactory = null;
        if (!groupByFactory.isEmpty()) {
            groupByViewFactory = (GroupByViewFactoryMarker) viewFactories.remove(0);
            mergeViewFactory = (MergeViewFactoryMarker) viewFactories.remove(viewFactories.size() - 1);
        }

        ViewFactory retainPolicy;
        if (isUnion) {
            UnionViewFactory viewFactory = (UnionViewFactory) context.getViewResolutionService().create("internal", "union");
            viewFactory.setParentEventType(parentEventType);
            viewFactory.setViewFactories(dataWindowViews);
            retainPolicy = viewFactory;
        } else {
            IntersectViewFactory viewFactory = (IntersectViewFactory) context.getViewResolutionService().create("internal", "intersect");
            viewFactory.setParentEventType(parentEventType);
            viewFactory.setViewFactories(dataWindowViews);
            retainPolicy = viewFactory;
        }

        List<ViewFactory> nonRetainViewFactories = new ArrayList<ViewFactory>();
        nonRetainViewFactories.add(retainPolicy);
        if (groupByViewFactory != null) {
            nonRetainViewFactories.add(0, (ViewFactory) groupByViewFactory);
            nonRetainViewFactories.addAll(derivedValueViews);
            nonRetainViewFactories.add((ViewFactory) mergeViewFactory);
        } else {
            nonRetainViewFactories.addAll(derivedValueViews);
        }

        return nonRetainViewFactories;
    }

    public ViewServiceCreateResult createViews(Viewable eventStreamViewable,
                                               List<ViewFactory> viewFactories,
                                               AgentInstanceViewFactoryChainContext viewFactoryChainContext,
                                               boolean hasPreviousNode) {
        // Attempt to find existing views under the stream that match specs.
        // The viewSpecList may have been changed by this method.
        Pair<Viewable, List<View>> resultPair;
        if (hasPreviousNode) {
            resultPair = new Pair<Viewable, List<View>>(eventStreamViewable, Collections.<View>emptyList());
        } else {
            resultPair = ViewServiceHelper.matchExistingViews(eventStreamViewable, viewFactories, viewFactoryChainContext.getAgentInstanceContext());
        }

        Viewable parentViewable = resultPair.getFirst();

        if (viewFactories.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(".createView No new views created, dumping stream ... " + eventStreamViewable);
                ViewSupport.dumpChildViews("EventStream ", eventStreamViewable);
            }

            return new ViewServiceCreateResult(parentViewable, parentViewable, Collections.<View>emptyList());   // we know its a view here since the factory list is empty
        }

        // Instantiate remaining chain of views from the remaining factories which didn't match to existing views.
        List<View> views = ViewServiceHelper.instantiateChain(parentViewable, viewFactories, viewFactoryChainContext);

        // Initialize any views that need initializing after the chain is complete
        for (View view : views) {
            if (view instanceof InitializableView) {
                InitializableView initView = (InitializableView) view;
                initView.initialize();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(".createView New views created for stream, all views ... " + eventStreamViewable);
            ViewSupport.dumpChildViews("EventStream ", eventStreamViewable);
        }

        return new ViewServiceCreateResult(views.get(views.size() - 1), views.get(0), views);
    }

    public void remove(EventStream eventStream, Viewable viewToRemove) {
        // If the viewToRemove to remove has child viewToRemove, don't disconnect - the child viewToRemove(s) need this
        if (viewToRemove.hasViews()) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(".remove Views before the remove of view " + viewToRemove + ", for event stream " + eventStream);
            ViewSupport.dumpChildViews("EventStream ", eventStream);
        }

        // Remove views in chain leaving only non-empty parent views to the child view to be removed
        ViewServiceHelper.removeChainLeafView(eventStream, viewToRemove);

        if (log.isDebugEnabled()) {
            log.debug(".remove Views after the remove, for event stream " + eventStream);
            ViewSupport.dumpChildViews("EventStream ", eventStream);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ViewServiceImpl.class);
}
