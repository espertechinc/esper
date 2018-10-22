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

import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.virtualdw.VirtualDWViewFactoryForge;
import com.espertech.esper.common.internal.view.core.DataWindowViewForgeUniqueCandidate;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactoryForge;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Analysis result for joins.
 */
public class StreamJoinAnalysisResultCompileTime {
    private final int numStreams;
    private boolean[] unidirectional;
    private boolean[] unidirectionalNonDriving;
    private boolean isPureSelfJoin;
    private boolean[] hasChildViews;
    private NamedWindowMetaData[] namedWindowsPerStream;
    private String[][][] uniqueKeys;
    private TableMetaData[] tablesPerStream;

    /**
     * Ctor.
     *
     * @param numStreams number of streams
     */
    public StreamJoinAnalysisResultCompileTime(int numStreams) {
        this.numStreams = numStreams;
        isPureSelfJoin = false;
        unidirectional = new boolean[numStreams];
        unidirectionalNonDriving = new boolean[numStreams];
        hasChildViews = new boolean[numStreams];
        namedWindowsPerStream = new NamedWindowMetaData[numStreams];
        uniqueKeys = new String[numStreams][][];
        tablesPerStream = new TableMetaData[numStreams];
    }

    /**
     * Sets flag.
     *
     * @param index index
     */
    public void setUnidirectionalInd(int index) {
        unidirectional[index] = true;
    }

    /**
     * Sets flag.
     *
     * @param index index
     */
    public void setUnidirectionalNonDriving(int index) {
        unidirectionalNonDriving[index] = true;
    }

    /**
     * Sets self-join.
     *
     * @param pureSelfJoin if a self join
     */
    public void setPureSelfJoin(boolean pureSelfJoin) {
        isPureSelfJoin = pureSelfJoin;
    }

    /**
     * Sets child view flags.
     *
     * @param index to set
     */
    public void setHasChildViews(int index) {
        this.hasChildViews[index] = true;
    }

    /**
     * Returns unidirection ind.
     *
     * @return unidirectional flags
     */
    public boolean[] getUnidirectionalInd() {
        return unidirectional;
    }

    /**
     * Returns child view flags.
     *
     * @return flags
     */
    public boolean[] getHasChildViews() {
        return hasChildViews;
    }

    public NamedWindowMetaData[] getNamedWindowsPerStream() {
        return namedWindowsPerStream;
    }

    public void setNamedWindowsPerStream(int streamNum, NamedWindowMetaData metadata) {
        namedWindowsPerStream[streamNum] = metadata;
    }

    /**
     * Returns streams num.
     *
     * @return num
     */
    public int getNumStreams() {
        return numStreams;
    }

    public String[][][] getUniqueKeys() {
        return uniqueKeys;
    }

    public void setTablesForStream(int streamNum, TableMetaData metadata) {
        this.tablesPerStream[streamNum] = metadata;
    }

    public TableMetaData[] getTablesPerStream() {
        return tablesPerStream;
    }

    public void addUniquenessInfo(List<ViewFactoryForge>[] unmaterializedViewChain, Annotation[] annotations) {
        for (int i = 0; i < unmaterializedViewChain.length; i++) {
            Set<String> uniquenessProps = getUniqueCandidateProperties(unmaterializedViewChain[i], annotations);
            if (uniquenessProps != null) {
                uniqueKeys[i] = new String[1][];
                uniqueKeys[i][0] = uniquenessProps.toArray(new String[uniquenessProps.size()]);
            }
        }
    }

    public boolean isUnidirectional() {
        for (boolean ind : unidirectional) {
            if (ind) {
                return true;
            }
        }
        return false;
    }

    public boolean isUnidirectionalAll() {
        for (boolean ind : unidirectional) {
            if (!ind) {
                return false;
            }
        }
        return true;
    }

    public boolean isPureSelfJoin() {
        return isPureSelfJoin;
    }

    public int getUnidirectionalCount() {
        int count = 0;
        for (boolean ind : unidirectional) {
            count += ind ? 1 : 0;
        }
        return count;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(StreamJoinAnalysisResultRuntime.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(StreamJoinAnalysisResultRuntime.class, "ar", newInstance(StreamJoinAnalysisResultRuntime.class))
                .exprDotMethod(ref("ar"), "setPureSelfJoin", constant(isPureSelfJoin))
                .exprDotMethod(ref("ar"), "setUnidirectional", constant(unidirectional))
                .exprDotMethod(ref("ar"), "setUnidirectionalNonDriving", constant(unidirectionalNonDriving))
                .exprDotMethod(ref("ar"), "setNamedWindows", makeNamedWindows(method, symbols))
                .exprDotMethod(ref("ar"), "setTables", makeTables(method, symbols))
                .methodReturn(ref("ar"));
        return localMethod(method);
    }

    public boolean isVirtualDW(int stream) {
        return namedWindowsPerStream[stream] != null && namedWindowsPerStream[stream].isVirtualDataWindow();
    }

    private CodegenExpression makeTables(CodegenMethod method, SAIFFInitializeSymbol symbols) {
        CodegenExpression[] init = new CodegenExpression[tablesPerStream.length];
        for (int i = 0; i < init.length; i++) {
            init[i] = tablesPerStream[i] == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(tablesPerStream[i], symbols.getAddInitSvc(method));
        }
        return newArrayWithInit(Table.class, init);
    }

    private CodegenExpression makeNamedWindows(CodegenMethod method, SAIFFInitializeSymbol symbols) {
        CodegenExpression[] init = new CodegenExpression[namedWindowsPerStream.length];
        for (int i = 0; i < init.length; i++) {
            init[i] = namedWindowsPerStream[i] == null ? constantNull() : NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindowsPerStream[i], symbols.getAddInitSvc(method));
        }
        return newArrayWithInit(NamedWindow.class, init);
    }

    public static Set<String> getUniqueCandidateProperties(List<ViewFactoryForge> forges, Annotation[] annotations) {
        boolean disableUniqueImplicit = HintEnum.DISABLE_UNIQUE_IMPLICIT_IDX.getHint(annotations) != null;
        if (forges == null || forges.isEmpty()) {
            return null;
        }
        if (forges.get(0) instanceof GroupByViewFactoryForge) {
            GroupByViewFactoryForge grouped = (GroupByViewFactoryForge) forges.get(0);
            ExprNode[] criteria = grouped.getCriteriaExpressions();
            Set<String> groupedCriteria = ExprNodeUtilityQuery.getPropertyNamesIfAllProps(criteria);
            if (groupedCriteria == null) {
                return null;
            }
            ViewFactoryForge inner = grouped.getGroupeds().get(0);
            if (inner instanceof DataWindowViewForgeUniqueCandidate && !disableUniqueImplicit) {
                DataWindowViewForgeUniqueCandidate uniqueFactory = (DataWindowViewForgeUniqueCandidate) inner;
                Set<String> uniqueCandidates = uniqueFactory.getUniquenessCandidatePropertyNames();
                if (uniqueCandidates != null) {
                    uniqueCandidates.addAll(groupedCriteria);
                }
                return uniqueCandidates;
            }
            return null;
        } else if (forges.get(0) instanceof DataWindowViewForgeUniqueCandidate && !disableUniqueImplicit) {
            DataWindowViewForgeUniqueCandidate uniqueFactory = (DataWindowViewForgeUniqueCandidate) forges.get(0);
            return uniqueFactory.getUniquenessCandidatePropertyNames();
        } else if (forges.get(0) instanceof VirtualDWViewFactoryForge) {
            VirtualDWViewFactoryForge vdw = (VirtualDWViewFactoryForge) forges.get(0);
            return vdw.getUniqueKeys();
        }
        return null;
    }
}
