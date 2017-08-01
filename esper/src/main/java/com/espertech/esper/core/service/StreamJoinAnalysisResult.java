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
package com.espertech.esper.core.service;

import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.epl.virtualdw.VirtualDWViewProviderForAgentInstance;
import com.espertech.esper.view.ViewFactoryChain;
import com.espertech.esper.view.ViewServiceHelper;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Analysis result for joins.
 */
public class StreamJoinAnalysisResult {
    private final int numStreams;
    private boolean[] isUnidirectionalInd;
    private boolean[] isUnidirectionalNonDriving;
    private boolean isPureSelfJoin;
    private boolean[] hasChildViews;
    private boolean[] isNamedWindow;
    private VirtualDWViewProviderForAgentInstance[] viewExternal;
    private String[][][] uniqueKeys;
    private TableMetadata[] tablesPerStream;

    /**
     * Ctor.
     *
     * @param numStreams number of streams
     */
    public StreamJoinAnalysisResult(int numStreams) {
        this.numStreams = numStreams;
        isPureSelfJoin = false;
        isUnidirectionalInd = new boolean[numStreams];
        isUnidirectionalNonDriving = new boolean[numStreams];
        hasChildViews = new boolean[numStreams];
        isNamedWindow = new boolean[numStreams];
        viewExternal = new VirtualDWViewProviderForAgentInstance[numStreams];
        uniqueKeys = new String[numStreams][][];
        tablesPerStream = new TableMetadata[numStreams];
    }

    /**
     * Sets flag.
     *
     * @param index index
     */
    public void setUnidirectionalInd(int index) {
        isUnidirectionalInd[index] = true;
    }

    /**
     * Sets flag.
     *
     * @param index index
     */
    public void setUnidirectionalNonDriving(int index) {
        isUnidirectionalNonDriving[index] = true;
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
        return isUnidirectionalInd;
    }

    /**
     * Returns non-driving unidirectional streams when partial self-joins.
     *
     * @return indicators
     */
    public boolean[] getUnidirectionalNonDriving() {
        return isUnidirectionalNonDriving;
    }

    /**
     * True for self-join.
     *
     * @return self-join
     */
    public boolean isPureSelfJoin() {
        return isPureSelfJoin;
    }

    /**
     * Returns child view flags.
     *
     * @return flags
     */
    public boolean[] getHasChildViews() {
        return hasChildViews;
    }

    /**
     * Return named window flags.
     *
     * @return flags
     */
    public boolean[] getNamedWindow() {
        return isNamedWindow;
    }

    /**
     * Sets named window flag
     *
     * @param index to set
     */
    public void setNamedWindow(int index) {
        isNamedWindow[index] = true;
    }

    /**
     * Returns streams num.
     *
     * @return num
     */
    public int getNumStreams() {
        return numStreams;
    }

    public VirtualDWViewProviderForAgentInstance[] getViewExternal() {
        return viewExternal;
    }

    public String[][][] getUniqueKeys() {
        return uniqueKeys;
    }

    public void setTablesForStream(int streamNum, TableMetadata metadata) {
        this.tablesPerStream[streamNum] = metadata;
    }

    public TableMetadata[] getTablesPerStream() {
        return tablesPerStream;
    }

    public void addUniquenessInfo(ViewFactoryChain[] unmaterializedViewChain, Annotation[] annotations) {
        for (int i = 0; i < unmaterializedViewChain.length; i++) {
            if (unmaterializedViewChain[i].getDataWindowViewFactoryCount() > 0) {
                Set<String> uniquenessProps = ViewServiceHelper.getUniqueCandidateProperties(unmaterializedViewChain[i].getViewFactoryChain(), annotations);
                if (uniquenessProps != null) {
                    uniqueKeys[i] = new String[1][];
                    uniqueKeys[i][0] = uniquenessProps.toArray(new String[uniquenessProps.size()]);
                }
            }
        }
    }

    public boolean isUnidirectional() {
        for (boolean ind : isUnidirectionalInd) {
            if (ind) {
                return true;
            }
        }
        return false;
    }

    public int getUnidirectionalStreamNumberFirst() {
        for (int i = 0; i < isUnidirectionalInd.length; i++) {
            if (isUnidirectionalInd[i]) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    public boolean isUnidirectionalAll() {
        for (boolean ind : isUnidirectionalInd) {
            if (!ind) {
                return false;
            }
        }
        return true;
    }

    public int getUnidirectionalCount() {
        int count = 0;
        for (boolean ind : isUnidirectionalInd) {
            count += ind ? 1 : 0;
        }
        return count;
    }
}
