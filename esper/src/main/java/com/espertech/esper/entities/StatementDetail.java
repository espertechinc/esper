/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.entities;

import com.espertech.esper.client.soda.EPStatementObjectModel;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Statement detail information.
 */
@XmlRootElement
public class StatementDetail {
    private String engineURI;
    private String statementName;
    private String epl;
    private String eplNoAnnotation;
    private String state;
    private String lastStateChange;
    private boolean pattern;
    private String description;
    private String type;
    private String annotations;
    private PropertyDetail[] properties;
    private EPStatementObjectModel soda;

    public StatementDetail(String engineURI, String statementName, String epl, String eplNoAnnotation, String state, String lastStateChange, boolean pattern, String description, String type, String annotations, PropertyDetail[] properties, EPStatementObjectModel soda) {
        this.engineURI = engineURI;
        this.statementName = statementName;
        this.epl = epl;
        this.eplNoAnnotation = eplNoAnnotation;
        this.state = state;
        this.lastStateChange = lastStateChange;
        this.pattern = pattern;
        this.description = description;
        this.type = type;
        this.annotations = annotations;
        this.properties = properties;
        this.soda = soda;
    }

    public StatementDetail() {
    }

    /**
     * Returns the engine URI.
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }

    /**
     * Returns the statement name
     * @return statement name
     */
    public String getStatementName() {
        return statementName;
    }

    /**
     * Returns the statement EPL.
     * @return statement EPL text
     */
    public String getEpl() {
        return epl;
    }

    /**
     * Returns the statement state.
     * @return statement state
     */
    public String getState() {
        return state;
    }

    /**
     * Returns the date of the last state change.
     * @return date of last state change
     */
    public String getLastStateChange() {
        return lastStateChange;
    }

    /**
     * Returns true for a pattern-only statement without select-clause
     * @return pattern indicator is true for pattern-only statement without select-clause
     */
    public boolean isPattern() {
        return pattern;
    }

    /**
     * Returns the statement description, if provided.
     * @return statement description, if provided.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the statement type, an enumeration value indicating the type of statement.
     * @return statement type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns statement output event properties
     * @return property descriptors
     */
    public PropertyDetail[] getProperties() {
        return properties;
    }

    /**
     * Returns the EPL without the annotations, if any where specified.
     * @return EPL without annotations
     */
    public String getEplNoAnnotation() {
        return eplNoAnnotation;
    }

    /**
     * Returns the annotations only.
     * @return annotation text
     */
    public String getAnnotations() {
        return annotations;
    }

    /**
     * Returns the Statement Object Model (SODA) of the EPL, if asked for.
     * @return statement object model
     */
    @XmlTransient
    public EPStatementObjectModel getSoda() {
        return soda;
    }

    /**
     * Sets the engine URI
     * @param engineURI engine URI
     */
    public void setEngineURI(String engineURI) {
        this.engineURI = engineURI;
    }

    /**
     * Sets the statement name
     * @param statementName statement name
     */
    public void setStatementName(String statementName) {
        this.statementName = statementName;
    }

    public void setEpl(String epl) {
        this.epl = epl;
    }

    public void setEplNoAnnotation(String eplNoAnnotation) {
        this.eplNoAnnotation = eplNoAnnotation;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setLastStateChange(String lastStateChange) {
        this.lastStateChange = lastStateChange;
    }

    public void setPattern(boolean pattern) {
        this.pattern = pattern;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public void setProperties(PropertyDetail[] properties) {
        this.properties = properties;
    }

    public void setSoda(EPStatementObjectModel soda) {
        this.soda = soda;
    }
}
