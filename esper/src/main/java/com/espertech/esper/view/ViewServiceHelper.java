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

import com.espertech.esper.client.annotation.Audit;
import com.espertech.esper.client.annotation.AuditEnum;
import com.espertech.esper.client.annotation.HintEnum;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.context.util.AgentInstanceViewFactoryChainContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.ViewSpec;
import com.espertech.esper.epl.virtualdw.VirtualDWViewFactory;
import com.espertech.esper.view.std.GroupByViewFactoryMarker;
import com.espertech.esper.view.std.MergeViewFactoryMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Utility methods to deal with chains of views, and for merge/group-by views.
 */
public class ViewServiceHelper {
    public static Set<String> getUniqueCandidateProperties(List<ViewFactory> viewFactory, Annotation[] annotations) {
        boolean disableUniqueImplicit = HintEnum.DISABLE_UNIQUE_IMPLICIT_IDX.getHint(annotations) != null;
        if (viewFactory == null || viewFactory.isEmpty()) {
            return null;
        }
        if (viewFactory.get(0) instanceof GroupByViewFactoryMarker) {
            ExprNode[] criteria = ((GroupByViewFactoryMarker) viewFactory.get(0)).getCriteriaExpressions();
            Set<String> groupedCriteria = ExprNodeUtilityCore.getPropertyNamesIfAllProps(criteria);
            if (groupedCriteria == null) {
                return null;
            }
            if (viewFactory.get(1) instanceof DataWindowViewFactoryUniqueCandidate && !disableUniqueImplicit) {
                DataWindowViewFactoryUniqueCandidate uniqueFactory = (DataWindowViewFactoryUniqueCandidate) viewFactory.get(1);
                Set<String> uniqueCandidates = uniqueFactory.getUniquenessCandidatePropertyNames();
                if (uniqueCandidates != null) {
                    uniqueCandidates.addAll(groupedCriteria);
                }
                return uniqueCandidates;
            }
            return null;
        } else if (viewFactory.get(0) instanceof DataWindowViewFactoryUniqueCandidate && !disableUniqueImplicit) {
            DataWindowViewFactoryUniqueCandidate uniqueFactory = (DataWindowViewFactoryUniqueCandidate) viewFactory.get(0);
            return uniqueFactory.getUniquenessCandidatePropertyNames();
        } else if (viewFactory.get(0) instanceof VirtualDWViewFactory) {
            VirtualDWViewFactory vdw = (VirtualDWViewFactory) viewFactory.get(0);
            return vdw.getUniqueKeys();
        }
        return null;
    }

    /**
     * Add merge views for any views in the chain requiring a merge (group view).
     * Appends to the list of view specifications passed in one ore more
     * new view specifications that represent merge views.
     * Merge views have the same parameter list as the (group) view they merge data for.
     *
     * @param specifications is a list of view definitions defining the chain of views.
     * @throws ViewProcessingException indicating that the view chain configuration is invalid
     */
    protected static void addMergeViews(List<ViewSpec> specifications)
            throws ViewProcessingException {
        if (log.isDebugEnabled()) {
            log.debug(".addMergeViews Incoming specifications=" + Arrays.toString(specifications.toArray()));
        }

        // A grouping view requires a merge view and cannot be last since it would not group sub-views
        if (specifications.size() > 0) {
            ViewSpec lastView = specifications.get(specifications.size() - 1);
            ViewEnum viewEnum = ViewEnum.forName(lastView.getObjectNamespace(), lastView.getObjectName());
            if ((viewEnum != null) && (viewEnum.getMergeView() != null)) {
                throw new ViewProcessingException("Invalid use of the '" +
                        lastView.getObjectName() + "' view, the view requires one or more child views to group, or consider using the group-by clause");
            }
        }

        LinkedList<ViewSpec> mergeViewSpecs = new LinkedList<ViewSpec>();

        for (ViewSpec spec : specifications) {
            ViewEnum viewEnum = ViewEnum.forName(spec.getObjectNamespace(), spec.getObjectName());
            if (viewEnum == null) {
                continue;
            }

            if (viewEnum.getMergeView() == null) {
                continue;
            }

            // The merge view gets the same parameters as the view that requires the merge
            ViewSpec mergeViewSpec = new ViewSpec(viewEnum.getMergeView().getNamespace(), viewEnum.getMergeView().getName(),
                    spec.getObjectParameters());

            // The merge views are added to the beginning of the list.
            // This enables group views to stagger ie. marketdata.group("symbol").group("feed").xxx.merge(...).merge(...)
            mergeViewSpecs.addFirst(mergeViewSpec);
        }

        specifications.addAll(mergeViewSpecs);

        if (log.isDebugEnabled()) {
            log.debug(".addMergeViews Outgoing specifications=" + Arrays.toString(specifications.toArray()));
        }
    }

    /**
     * Instantiate a chain of views.
     *
     * @param parentViewable          - parent view to add the chain to
     * @param viewFactories           - is the view factories to use to make each view, or reuse and existing view
     * @param viewFactoryChainContext context
     * @return chain of views instantiated
     */
    public static List<View> instantiateChain(Viewable parentViewable,
                                              List<ViewFactory> viewFactories,
                                              AgentInstanceViewFactoryChainContext viewFactoryChainContext) {
        List<View> newViews = new LinkedList<View>();
        Viewable parent = parentViewable;

        boolean grouped = false;
        for (int i = 0; i < viewFactories.size(); i++) {
            ViewFactory viewFactory = viewFactories.get(i);
            if (viewFactory instanceof MergeViewFactoryMarker) {
                grouped = false;
            }

            // Create the new view object
            View currentView;
            if (grouped) {
                currentView = viewFactory.makeViewGroupDelegate();
            } else {
                currentView = viewFactory.makeView(viewFactoryChainContext);
            }

            if (viewFactory instanceof GroupByViewFactoryMarker) {
                grouped = true;
            }

            newViews.add(currentView);
            parent.addView(currentView);

            // Next parent is the new view
            parent = currentView;
        }

        return newViews;
    }

    public static void removeFirstUnsharedView(List<View> childViews) {
        for (int i = childViews.size() - 1; i >= 0; i--) {
            View child = childViews.get(i);
            Viewable parent = child.getParent();
            if (parent == null) {
                return;
            }
            parent.removeView(child);
            if (parent.hasViews()) {
                return;
            }
        }
    }

    /**
     * Removes a view from a parent view returning the orphaned parent views in a list.
     *
     * @param parentViewable - parent to remove view from
     * @param viewToRemove   - view to remove
     * @return chain of orphaned views
     */
    protected static List<View> removeChainLeafView(Viewable parentViewable,
                                                    Viewable viewToRemove) {
        List<View> removedViews = new LinkedList<View>();

        // The view to remove must be a leaf node - non-leaf views are just not removed
        if (viewToRemove.hasViews()) {
            return removedViews;
        }

        // Find child viewToRemove among descendent views
        List<View> viewPath = ViewSupport.findDescendent(parentViewable, viewToRemove);

        if (viewPath == null) {
            String message = "Viewable not found when removing view " + viewToRemove;
            throw new IllegalArgumentException(message);
        }

        // The viewToRemove is a direct child view of the stream
        if (viewPath.isEmpty()) {
            boolean isViewRemoved = parentViewable.removeView((View) viewToRemove);

            if (!isViewRemoved) {
                String message = "Failed to remove immediate child view " + viewToRemove;
                log.error(".remove " + message);
                throw new IllegalStateException(message);
            }

            removedViews.add((View) viewToRemove);
            return removedViews;
        }

        View[] viewPathArray = viewPath.toArray(new View[viewPath.size()]);
        View currentView = (View) viewToRemove;

        // Remove child from parent views until a parent view has more children,
        // or there are no more parents (index=0).
        for (int index = viewPathArray.length - 1; index >= 0; index--) {
            boolean isViewRemoved = viewPathArray[index].removeView(currentView);
            removedViews.add(currentView);

            if (!isViewRemoved) {
                String message = "Failed to remove view " + currentView;
                log.error(".remove " + message);
                throw new IllegalStateException(message);
            }

            // If the parent views has more child views, we are done
            if (viewPathArray[index].hasViews()) {
                break;
            }

            // The parent of the top parent is the stream, remove from stream
            if (index == 0) {
                parentViewable.removeView(viewPathArray[0]);
                removedViews.add(viewPathArray[0]);
            } else {
                currentView = viewPathArray[index];
            }
        }

        return removedViews;
    }

    /**
     * Match the views under the stream to the list of view specications passed in.
     * The method changes the view specifications list passed in and removes those
     * specifications for which matcing views have been found.
     * If none of the views under the stream matches the first view specification passed in,
     * the method returns the stream itself and leaves the view specification list unchanged.
     * If one view under the stream matches, the view's specification is removed from the list.
     * The method will then attempt to determine if any child views of that view also match
     * specifications.
     *
     * @param rootViewable         is the top rootViewable event stream to which all views are attached as child views
     *                             This parameter is changed by this method, ie. specifications are removed if they match existing views.
     * @param viewFactories        is the view specifications for making views
     * @param agentInstanceContext agent instance context
     * @return a pair of (A) the stream if no views matched, or the last child view that matched (B) the full list
     * of parent views
     */
    protected static Pair<Viewable, List<View>> matchExistingViews(Viewable rootViewable,
                                                                   List<ViewFactory> viewFactories,
                                                                   AgentInstanceContext agentInstanceContext) {
        Viewable currentParent = rootViewable;
        List<View> matchedViewList = new LinkedList<View>();

        boolean foundMatch;

        if (viewFactories.isEmpty()) {
            return new Pair<Viewable, List<View>>(rootViewable, Collections.<View>emptyList());
        }

        do {
            foundMatch = false;

            for (View childView : currentParent.getViews()) {
                ViewFactory currentFactory = viewFactories.get(0);

                if (!(currentFactory.canReuse(childView, agentInstanceContext))) {
                    continue;
                }

                // The specifications match, check current data window size
                viewFactories.remove(0);
                currentParent = childView;
                foundMatch = true;
                matchedViewList.add(childView);
                break;
            }
        }
        while (foundMatch && (!viewFactories.isEmpty()));

        return new Pair<Viewable, List<View>>(currentParent, matchedViewList);
    }

    /**
     * Given a list of view specifications obtained from by parsing this method instantiates a list of view factories.
     * The view factories are not yet aware of each other after leaving this method (so not yet chained logically).
     * They are simply instantiated and assigned view parameters.
     *
     * @param streamNum        is the stream number
     * @param viewSpecList     is the view definition
     * @param statementContext is statement service context and statement info
     * @param isSubquery       subquery indicator
     * @param subqueryNumber   for subqueries
     * @return list of view factories
     * @throws ViewProcessingException if the factory cannot be creates such as for invalid view spec
     */
    public static List<ViewFactory> instantiateFactories(int streamNum,
                                                         List<ViewSpec> viewSpecList,
                                                         StatementContext statementContext,
                                                         boolean isSubquery,
                                                         int subqueryNumber)
            throws ViewProcessingException {
        List<ViewFactory> factoryChain = new ArrayList<ViewFactory>();

        boolean grouped = false;
        int groupCount = 0;
        for (ViewSpec spec : viewSpecList) {
            // Create the new view factory
            ViewFactory viewFactory = statementContext.getViewResolutionService().create(spec.getObjectNamespace(), spec.getObjectName());

            Audit audit = AuditEnum.VIEW.getAudit(statementContext.getAnnotations());
            if (audit != null) {
                viewFactory = (ViewFactory) ViewFactoryProxy.newInstance(statementContext.getEngineURI(), statementContext.getStatementName(), viewFactory, spec.getObjectName());
            }
            factoryChain.add(viewFactory);

            // Set view factory parameters
            try {
                ViewFactoryContext context = new ViewFactoryContext(statementContext, streamNum, spec.getObjectNamespace(), spec.getObjectName(), isSubquery, subqueryNumber, grouped);
                viewFactory.setViewParameters(context, spec.getObjectParameters());
            } catch (ViewParameterException e) {
                throw new ViewProcessingException("Error in view '" + spec.getObjectName() +
                        "', " + e.getMessage(), e);
            }

            if (viewFactory instanceof GroupByViewFactoryMarker) {
                grouped = true;
                groupCount++;
            }
            if (viewFactory instanceof MergeViewFactoryMarker) {
                grouped = false;
            }
        }

        if (groupCount > 1) {
            throw new ViewProcessingException("Multiple groupwin-declarations are not supported");
        }

        return factoryChain;
    }

    private static final Logger log = LoggerFactory.getLogger(ViewServiceHelper.class);
}
