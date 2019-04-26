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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.stage1.spec.ViewSpec;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.serde.compiletime.eventtype.SerdeEventTypeUtility;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactoryForge;
import com.espertech.esper.common.internal.view.groupwin.MergeViewFactoryForge;
import com.espertech.esper.common.internal.view.intersect.IntersectViewFactoryForge;
import com.espertech.esper.common.internal.view.union.UnionViewFactoryForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ViewFactoryForgeUtil {

    public static void determineViewSchedules(List<ViewFactoryForge>[] forgesPerStream, List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        ViewForgeVisitorSchedulesCollector collector = new ViewForgeVisitorSchedulesCollector(scheduleHandleCallbackProviders);
        for (int stream = 0; stream < forgesPerStream.length; stream++) {
            for (ViewFactoryForge forge : forgesPerStream[stream]) {
                forge.accept(collector);
            }
        }
    }

    public static void determineViewSchedules(List<ViewFactoryForge> forges, List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders) {
        ViewForgeVisitorSchedulesCollector collector = new ViewForgeVisitorSchedulesCollector(scheduleHandleCallbackProviders);
        for (ViewFactoryForge forge : forges) {
            forge.accept(collector);
        }
    }

    public static ViewFactoryForgeDesc createForges(ViewSpec[] viewSpecDefinitions, ViewFactoryForgeArgs args, EventType parentEventType)
            throws ExprValidationException {
        try {
            // Clone the view spec list to prevent parameter modification
            List<ViewSpec> viewSpecList = new ArrayList<ViewSpec>(Arrays.asList(viewSpecDefinitions));
            ViewForgeEnv viewForgeEnv = new ViewForgeEnv(args);
            List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

            // Inspect views and add merge views if required
            // As users can specify merge views, if they are not provided they get added
            addMergeViews(viewSpecList);

            // Instantiate factories, not making them aware of each other yet, we now have a chain
            List<ViewFactoryForge> forgesChain = instantiateFactories(viewSpecList, args, viewForgeEnv);

            // Determine event type serdes that may be required
            for (ViewFactoryForge forge : forgesChain) {
                if (forge instanceof DataWindowViewForge) {
                    List<StmtClassForgeableFactory> serdeForgeables = SerdeEventTypeUtility.plan(parentEventType, viewForgeEnv.getStatementRawInfo(), viewForgeEnv.getSerdeEventTypeRegistry(), viewForgeEnv.getSerdeResolver());
                    additionalForgeables.addAll(serdeForgeables);
                }
            }

            // Build data window views that occur next to each other ("d d", "d d d") into a single intersection or union
            // Calls attach on the contained-views.
            List<ViewFactoryForge> forgesChainWIntersections = buildIntersectionsUnions(forgesChain, args, viewForgeEnv, parentEventType);

            // Verify group window use
            verifyGroups(forgesChainWIntersections);

            // Build group window views that may contain data windows and also intersection and union
            // Calls attach on the contained-views.
            List<ViewFactoryForge> forgesGrouped = buildGrouped(forgesChainWIntersections, args, viewForgeEnv, parentEventType);

            EventType eventType = parentEventType;

            for (int i = 0; i < forgesGrouped.size(); i++) {
                ViewFactoryForge factoryToAttach = forgesGrouped.get(i);
                try {
                    factoryToAttach.attach(eventType, args.getStreamNum(), viewForgeEnv);
                    eventType = factoryToAttach.getEventType();
                } catch (ViewParameterException ex) {
                    throw new ViewProcessingException(ex.getMessage(), ex);
                }
            }

            // get multikey forges
            List<StmtClassForgeableFactory> multikeyForges = getMultikeyForges(forgesGrouped, viewForgeEnv);
            additionalForgeables.addAll(multikeyForges);

            return new ViewFactoryForgeDesc(forgesGrouped, additionalForgeables);
        } catch (ViewProcessingException ex) {
            throw new ExprValidationException("Failed to validate data window declaration: " + ex.getMessage(), ex);
        }
    }

    private static List<StmtClassForgeableFactory> getMultikeyForges(List<ViewFactoryForge> forges, ViewForgeEnv viewForgeEnv) {
        List<StmtClassForgeableFactory> factories = new ArrayList<>(1);
        getMultikeyForgesRecursive(forges, factories, viewForgeEnv);
        return factories;
    }

    private static void getMultikeyForgesRecursive(List<ViewFactoryForge> forges, List<StmtClassForgeableFactory> multikeyForges, ViewForgeEnv viewForgeEnv) {
        for (ViewFactoryForge forge : forges) {
            multikeyForges.addAll(forge.initAdditionalForgeables(viewForgeEnv));
            getMultikeyForgesRecursive(forge.getInnerForges(), multikeyForges, viewForgeEnv);
        }
    }

    private static List<ViewFactoryForge> buildGrouped(List<ViewFactoryForge> forgesChain, ViewFactoryForgeArgs args, ViewForgeEnv viewForgeEnv, EventType parentEventType) {
        if (forgesChain.isEmpty()) {
            return forgesChain;
        }
        if (!(forgesChain.get(0) instanceof GroupByViewFactoryForge)) { // group is always first
            return forgesChain;
        }
        GroupByViewFactoryForge group = (GroupByViewFactoryForge) forgesChain.get(0);

        // find merge
        int indexMerge = -1;
        for (int i = 0; i < forgesChain.size(); i++) {
            if (forgesChain.get(i) instanceof MergeViewFactoryForge) {
                indexMerge = i;
                break;
            }
        }

        if (indexMerge == -1 || indexMerge == 1) {
            throw new IllegalArgumentException();
        }

        // obtain list of grouped forges
        List<ViewFactoryForge> groupeds = new ArrayList<>(indexMerge - 1);
        EventType eventType = parentEventType;

        for (int i = 1; i < indexMerge; i++) {
            ViewFactoryForge forge = forgesChain.get(i);
            groupeds.add(forge);

            try {
                forge.attach(eventType, args.getStreamNum(), viewForgeEnv);
            } catch (ViewParameterException ex) {
                throw new ViewProcessingException(ex.getMessage(), ex);
            }
        }
        group.setGroupeds(groupeds);

        // obtain list of remaining
        List<ViewFactoryForge> remainder = new ArrayList<>(1);
        remainder.add(group);
        for (int i = indexMerge + 1; i < forgesChain.size(); i++) {
            remainder.add(forgesChain.get(i));
        }

        // the result is the remainder
        return remainder;
    }

    // Identify a sequence of data windows and replace with an intersection or union
    private static List<ViewFactoryForge> buildIntersectionsUnions(List<ViewFactoryForge> forges, ViewFactoryForgeArgs args, ViewForgeEnv viewForgeEnv, EventType parentEventType) {
        List<ViewFactoryForge> result = new ArrayList<>(forges.size());
        List<ViewFactoryForge> dataWindows = new ArrayList<>(2);

        for (ViewFactoryForge forge : forges) {
            if (forge instanceof DataWindowViewForge) {
                dataWindows.add(forge);
            } else {
                if (!dataWindows.isEmpty()) {
                    if (dataWindows.size() == 1) {
                        result.addAll(dataWindows);
                    } else {
                        ViewFactoryForge intersectUnion = makeIntersectOrUnion(dataWindows, args, viewForgeEnv, parentEventType);
                        result.add(intersectUnion);
                    }
                    dataWindows.clear();
                }
                result.add(forge);
            }
        }

        if (!dataWindows.isEmpty()) {
            if (dataWindows.size() == 1) {
                result.addAll(dataWindows);
            } else {
                ViewFactoryForge intersectUnion = makeIntersectOrUnion(dataWindows, args, viewForgeEnv, parentEventType);
                result.add(intersectUnion);
            }
        }
        return result;
    }

    private static ViewFactoryForge makeIntersectOrUnion(List<ViewFactoryForge> dataWindows, ViewFactoryForgeArgs args, ViewForgeEnv viewForgeEnv,
                                                         EventType parentEventType) {
        for (ViewFactoryForge forge : dataWindows) {
            try {
                forge.attach(parentEventType, args.getStreamNum(), viewForgeEnv);
            } catch (ViewParameterException ex) {
                throw new ViewProcessingException(ex.getMessage(), ex);
            }
        }

        if (args.getOptions().isRetainUnion()) {
            return new UnionViewFactoryForge(new ArrayList<>(dataWindows));
        }
        return new IntersectViewFactoryForge(new ArrayList<>(dataWindows));
    }

    private static void verifyGroups(List<ViewFactoryForge> forges) {
        GroupByViewFactoryForge group = null;
        MergeViewFactoryForge merge = null;
        int numDataWindows = 0;
        for (ViewFactoryForge forge : forges) {
            if (forge instanceof GroupByViewFactoryForge) {
                if (group == null) {
                    group = (GroupByViewFactoryForge) forge;
                } else {
                    throw new ViewProcessingException("Multiple groupwin-declarations are not supported");
                }
            }
            if (forge instanceof MergeViewFactoryForge) {
                if (merge == null) {
                    merge = (MergeViewFactoryForge) forge;
                } else {
                    throw new ViewProcessingException("Multiple merge-declarations are not supported");
                }
            }
            numDataWindows += forge instanceof DataWindowViewForge ? 1 : 0;
        }

        if (group != null && group != forges.get(0)) {
            throw new ViewProcessingException("The 'groupwin' declaration must occur in the first position");
        }
        if (merge != null) {
            if (numDataWindows > 1) {
                throw new ViewProcessingException("The 'merge' declaration cannot be used in conjunction with multiple data windows");
            }
            if (group == null) {
                throw new ViewProcessingException("The 'merge' declaration cannot be used in without a 'group' declaration");
            }
            if (!ExprNodeUtilityCompare.deepEquals(group.getViewParameters(), merge.getViewParameters())) {
                throw new ViewProcessingException("Mismatching parameters between 'group' and 'merge'");
            }

        }
    }

    private static List<ViewFactoryForge> instantiateFactories(List<ViewSpec> viewSpecList, ViewFactoryForgeArgs args, ViewForgeEnv viewForgeEnv)
            throws ViewProcessingException {
        List<ViewFactoryForge> forges = new ArrayList<>();

        for (ViewSpec spec : viewSpecList) {

            // Create the new view factory
            ViewFactoryForge viewFactoryForge = args.getViewResolutionService().create(spec.getObjectNamespace(), spec.getObjectName(), args.getOptionalCreateNamedWindowName());
            forges.add(viewFactoryForge);

            // Set view factory parameters
            try {
                viewFactoryForge.setViewParameters(spec.getObjectParameters(), viewForgeEnv, args.getStreamNum());
            } catch (ViewParameterException e) {
                throw new ViewProcessingException("Error in view '" + spec.getObjectName() +
                        "', " + e.getMessage(), e);
            }
        }
        return forges;
    }

    private static void addMergeViews(List<ViewSpec> specifications)
            throws ViewProcessingException {

        // A grouping view requires a merge view and cannot be last since it would not group sub-views
        if (specifications.size() > 0) {
            ViewSpec lastView = specifications.get(specifications.size() - 1);
            ViewEnum viewEnum = ViewEnum.forName(lastView.getObjectNamespace(), lastView.getObjectName());
            if ((viewEnum != null) && (viewEnum.getMergeView() != null)) {
                throw new ViewProcessingException("Invalid use of the '" +
                        lastView.getObjectName() + "' view, the view requires one or more child views to group, or consider using the group-by clause");
            }
        }

        LinkedList<ViewSpec> mergeViewSpecs = new LinkedList<>();
        boolean foundMerge = false;
        for (ViewSpec spec : specifications) {
            ViewEnum viewEnum = ViewEnum.forName(spec.getObjectNamespace(), spec.getObjectName());
            if (viewEnum == ViewEnum.GROUP_MERGE) {
                foundMerge = true;
                break;
            }
        }
        if (foundMerge) {
            return;
        }

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
    }

    public static CodegenMethod makeViewFactories(List<ViewFactoryForge> forges, Class generator, CodegenMethodScope parent, CodegenClassScope classScope, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ViewFactory[].class, generator, classScope);
        method.getBlock().declareVar(ViewFactory[].class, "groupeds", newArrayByLength(ViewFactory.class, constant(forges.size())));
        for (int i = 0; i < forges.size(); i++) {
            method.getBlock().assignArrayElement("groupeds", constant(i), forges.get(i).make(method, symbols, classScope));
        }
        method.getBlock().methodReturn(ref("groupeds"));
        return method;
    }

    public static CodegenExpression codegenForgesWInit(List<ViewFactoryForge> forges, int streamNum, Integer subqueryNum,
                                                       CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ViewFactory[].class, ViewFactoryForgeUtil.class, classScope);
        method.getBlock()
                .declareVar(ViewFactory[].class, "factories", newArrayByLength(ViewFactory.class, constant(forges.size())));

        boolean grouped = !forges.isEmpty() && forges.get(0) instanceof GroupByViewFactoryForge;
        method.getBlock().declareVar(ViewFactoryContext.class, "ctx", newInstance(ViewFactoryContext.class))
                .exprDotMethod(ref("ctx"), "setStreamNum", constant(streamNum))
                .exprDotMethod(ref("ctx"), "setSubqueryNumber", constant(subqueryNum))
                .exprDotMethod(ref("ctx"), "setGrouped", constant(grouped));
        for (int i = 0; i < forges.size(); i++) {
            String ref = "factory_" + i;
            method.getBlock().declareVar(ViewFactory.class, ref, forges.get(i).make(method, symbols, classScope))
                    .exprDotMethod(ref(ref), "init", ref("ctx"), symbols.getAddInitSvc(method))
                    .assignArrayElement(ref("factories"), constant(i), ref(ref));
        }

        method.getBlock().methodReturn(ref("factories"));
        return localMethod(method);
    }

    public static boolean hasDataWindows(List<ViewFactoryForge> views) {
        for (ViewFactoryForge view : views) {
            if (view instanceof DataWindowViewForge) {
                return true;
            }
            if (view instanceof GroupByViewFactoryForge) {
                GroupByViewFactoryForge grouped = (GroupByViewFactoryForge) view;
                return hasDataWindows(grouped.getGroupeds());
            }
        }
        return false;
    }
}
