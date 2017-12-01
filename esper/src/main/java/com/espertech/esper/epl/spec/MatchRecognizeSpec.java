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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.rowregex.RowRegexExprNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification for match_recognize.
 */
public class MatchRecognizeSpec implements Serializable {
    private List<ExprNode> partitionByExpressions;
    private List<MatchRecognizeMeasureItem> measures;
    private RowRegexExprNode pattern;
    private List<MatchRecognizeDefineItem> defines;
    private boolean isAllMatches;
    private MatchRecognizeSkip skip;
    private MatchRecognizeInterval interval;
    private static final long serialVersionUID = -2402650987323748877L;

    /**
     * Ctor.
     */
    public MatchRecognizeSpec() {
        partitionByExpressions = new ArrayList<ExprNode>();
        measures = new ArrayList<MatchRecognizeMeasureItem>();
        defines = new ArrayList<MatchRecognizeDefineItem>();
        skip = new MatchRecognizeSkip(MatchRecognizeSkipEnum.PAST_LAST_ROW);
    }

    /**
     * Interval part of null.
     *
     * @return interval
     */
    public MatchRecognizeInterval getInterval() {
        return interval;
    }

    /**
     * Sets the interval.
     *
     * @param interval to set
     */
    public void setInterval(MatchRecognizeInterval interval) {
        this.interval = interval;
    }

    /**
     * True for all-matches.
     *
     * @return indicator all-matches
     */
    public boolean isAllMatches() {
        return isAllMatches;
    }

    /**
     * Set to true for all-matches.
     *
     * @param allMatches indicator all-matches
     */
    public void setAllMatches(boolean allMatches) {
        isAllMatches = allMatches;
    }

    /**
     * Returns partition expressions.
     *
     * @return partition expressions
     */
    public List<ExprNode> getPartitionByExpressions() {
        return partitionByExpressions;
    }

    /**
     * Returns partition expressions.
     *
     * @param partitionByExpressions partition expressions
     */
    public void setPartitionByExpressions(List<ExprNode> partitionByExpressions) {
        this.partitionByExpressions = partitionByExpressions;
    }

    /**
     * Add a measure item.
     *
     * @param item to add
     */
    public void addMeasureItem(MatchRecognizeMeasureItem item) {
        measures.add(item);
    }

    /**
     * Returns the define items.
     *
     * @return define items
     */
    public List<MatchRecognizeDefineItem> getDefines() {
        return defines;
    }

    /**
     * Sets the pattern.
     *
     * @param pattern to set
     */
    public void setPattern(RowRegexExprNode pattern) {
        this.pattern = pattern;
    }

    /**
     * Adds a define item.
     *
     * @param define to add
     */
    public void addDefine(MatchRecognizeDefineItem define) {
        this.defines.add(define);
    }

    /**
     * Returns measures.
     *
     * @return measures
     */
    public List<MatchRecognizeMeasureItem> getMeasures() {
        return measures;
    }

    /**
     * Returns the pattern.
     *
     * @return pattern
     */
    public RowRegexExprNode getPattern() {
        return pattern;
    }

    /**
     * Returns the skip.
     *
     * @return skip
     */
    public MatchRecognizeSkip getSkip() {
        return skip;
    }

    /**
     * Sets the skip.
     *
     * @param skip to set
     */
    public void setSkip(MatchRecognizeSkip skip) {
        this.skip = skip;
    }

    /**
     * Set measures.
     *
     * @param measures to set
     */
    public void setMeasures(List<MatchRecognizeMeasureItem> measures) {
        this.measures = measures;
    }

    /**
     * Set defines.
     *
     * @param defines to set
     */
    public void setDefines(List<MatchRecognizeDefineItem> defines) {
        this.defines = defines;
    }
}
