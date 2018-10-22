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
package com.espertech.esper.common.internal.context.aifactory.select;

import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadataUtil;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCompileTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewFactoryForgeUtil;

import java.util.List;

public class StatementForgeMethodSelectUtil {

    protected static boolean[] getHasIStreamOnly(boolean[] isNamedWindow, List<ViewFactoryForge>[] views) {
        boolean[] result = new boolean[views.length];
        for (int i = 0; i < views.length; i++) {
            if (isNamedWindow[i]) {
                continue;
            }
            result[i] = !ViewFactoryForgeUtil.hasDataWindows(views[i]);
        }
        return result;
    }

    static String[] determineStreamNames(StreamSpecCompiled[] streams) {
        String[] streamNames = new String[streams.length];
        for (int i = 0; i < streams.length; i++) {
            // Assign a stream name for joins, if not supplied
            streamNames[i] = streams[i].getOptionalStreamName();
            if (streamNames[i] == null) {
                streamNames[i] = "stream_" + i;
            }
        }
        return streamNames;
    }

    static StreamJoinAnalysisResultCompileTime verifyJoinViews(StatementSpecCompiled statementSpec, NamedWindowCompileTimeResolver namedWindowCompileTimeResolver)
            throws ExprValidationException {
        StreamSpecCompiled[] streamSpecs = statementSpec.getStreamSpecs();
        StreamJoinAnalysisResultCompileTime analysisResult = new StreamJoinAnalysisResultCompileTime(streamSpecs.length);
        if (streamSpecs.length < 2) {
            return analysisResult;
        }

        // Determine if any stream has a unidirectional keyword

        // inspect unidirectional indicator and named window flags
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (streamSpec.getOptions().isUnidirectional()) {
                analysisResult.setUnidirectionalInd(i);
            }
            if (streamSpec.getViewSpecs().length > 0) {
                analysisResult.setHasChildViews(i);
            }
            if (streamSpec instanceof NamedWindowConsumerStreamSpec) {
                NamedWindowConsumerStreamSpec nwSpec = (NamedWindowConsumerStreamSpec) streamSpec;
                if (nwSpec.getOptPropertyEvaluator() != null && !streamSpec.getOptions().isUnidirectional()) {
                    throw new ExprValidationException("Failed to validate named window use in join, contained-event is only allowed for named windows when marked as unidirectional");
                }
                NamedWindowMetaData nwinfo = nwSpec.getNamedWindow();
                analysisResult.setNamedWindowsPerStream(i, nwinfo);
                analysisResult.getUniqueKeys()[i] = EventTableIndexMetadataUtil.getUniqueness(nwinfo.getIndexMetadata(), nwinfo.getUniqueness());
            }
        }

        // non-outer-join: verify unidirectional can be on a single stream only
        if (statementSpec.getStreamSpecs().length > 1 && analysisResult.isUnidirectional()) {
            verifyJoinUnidirectional(analysisResult, statementSpec);
        }

        // count streams that provide data, excluding streams that poll data (DB and method)
        int countProviderNonpolling = 0;
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if ((streamSpec instanceof MethodStreamSpec) ||
                    (streamSpec instanceof DBStatementStreamSpec) ||
                    (streamSpec instanceof TableQueryStreamSpec)) {
                continue;
            }
            countProviderNonpolling++;
        }

        // if there is only one stream providing data, the analysis is done
        if (countProviderNonpolling == 1) {
            return analysisResult;
        }
        // there are multiple driving streams, verify the presence of a view for insert/remove stream

        // validation of join views works differently for unidirectional as there can be self-joins that don't require a view
        // see if this is a self-join in which all streams are filters and filter specification is the same.
        FilterSpecCompiled unidirectionalFilterSpec = null;
        FilterSpecCompiled lastFilterSpec = null;
        boolean pureSelfJoin = true;
        for (StreamSpecCompiled streamSpec : statementSpec.getStreamSpecs()) {
            if (!(streamSpec instanceof FilterStreamSpecCompiled)) {
                pureSelfJoin = false;
                continue;
            }

            FilterSpecCompiled filterSpec = ((FilterStreamSpecCompiled) streamSpec).getFilterSpecCompiled();
            if ((lastFilterSpec != null) && (!lastFilterSpec.equalsTypeAndFilter(filterSpec))) {
                pureSelfJoin = false;
            }
            if (streamSpec.getViewSpecs().length > 0) {
                pureSelfJoin = false;
            }
            lastFilterSpec = filterSpec;

            if (streamSpec.getOptions().isUnidirectional()) {
                unidirectionalFilterSpec = filterSpec;
            }
        }

        // self-join without views and not unidirectional
        if (pureSelfJoin && (unidirectionalFilterSpec == null)) {
            analysisResult.setPureSelfJoin(true);
            return analysisResult;
        }

        // weed out filter and pattern streams that don't have a view in a join
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            StreamSpecCompiled streamSpec = statementSpec.getStreamSpecs()[i];
            if (streamSpec.getViewSpecs().length > 0) {
                continue;
            }

            String name = streamSpec.getOptionalStreamName();
            if ((name == null) && (streamSpec instanceof FilterStreamSpecCompiled)) {
                name = ((FilterStreamSpecCompiled) streamSpec).getFilterSpecCompiled().getFilterForEventTypeName();
            }
            if ((name == null) && (streamSpec instanceof PatternStreamSpecCompiled)) {
                name = "pattern event stream";
            }

            if (streamSpec.getOptions().isUnidirectional()) {
                continue;
            }
            // allow a self-join without a child view, in that the filter spec is the same as the unidirection's stream filter
            if ((unidirectionalFilterSpec != null) &&
                    (streamSpec instanceof FilterStreamSpecCompiled) &&
                    (((FilterStreamSpecCompiled) streamSpec).getFilterSpecCompiled().equalsTypeAndFilter(unidirectionalFilterSpec))) {
                analysisResult.setUnidirectionalNonDriving(i);
                continue;
            }
            if ((streamSpec instanceof FilterStreamSpecCompiled) ||
                    (streamSpec instanceof PatternStreamSpecCompiled)) {
                throw new ExprValidationException("Joins require that at least one view is specified for each stream, no view was specified for " + name);
            }
        }

        return analysisResult;
    }

    private static void verifyJoinUnidirectional(StreamJoinAnalysisResultCompileTime analysisResult, StatementSpecCompiled statementSpec) throws ExprValidationException {
        int numUnidirectionalStreams = analysisResult.getUnidirectionalCount();
        int numStreams = statementSpec.getStreamSpecs().length;

        // only a single stream is unidirectional (applies to all but all-full-outer-join)
        if (!isFullOuterJoinAllStreams(statementSpec)) {
            if (numUnidirectionalStreams > 1) {
                throw new ExprValidationException("The unidirectional keyword can only apply to one stream in a join");
            }
        } else {
            // verify full-outer-join: requires unidirectional for all streams
            if (numUnidirectionalStreams > 1 && numUnidirectionalStreams < numStreams) {
                throw new ExprValidationException("The unidirectional keyword must either apply to a single stream or all streams in a full outer join");
            }
        }

        // verify no-child-view for unidirectional
        for (int i = 0; i < statementSpec.getStreamSpecs().length; i++) {
            if (analysisResult.getUnidirectionalInd()[i]) {
                if (analysisResult.getHasChildViews()[i]) {
                    throw new ExprValidationException("The unidirectional keyword requires that no views are declared onto the stream (applies to stream " + i + ")");
                }
            }
        }
    }

    private static boolean isFullOuterJoinAllStreams(StatementSpecCompiled statementSpec) {
        List<OuterJoinDesc> outers = statementSpec.getRaw().getOuterJoinDescList();
        if (outers == null || outers.size() == 0) {
            return false;
        }
        for (int stream = 0; stream < statementSpec.getStreamSpecs().length - 1; stream++) {
            if (outers.get(stream).getOuterJoinType() != OuterJoinType.FULL) {
                return false;
            }
        }
        return true;
    }
}
