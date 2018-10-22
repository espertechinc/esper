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
package com.espertech.esper.common.internal.epl.historical.database.core;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;

import java.util.Map;

/**
 * Column-level configuration settings are held in this immutable descriptor.
 */
public class ColumnSettings {
    private ConfigurationCommonDBRef.MetadataOriginEnum metadataOriginEnum;
    private ConfigurationCommonDBRef.ColumnChangeCaseEnum columnCaseConversionEnum;
    private Map<Integer, String> javaSqlTypeBinding;

    /**
     * Ctor.
     *
     * @param metadataOriginEnum       defines how to obtain output columnn metadata
     * @param columnCaseConversionEnum defines if to change case on output columns
     * @param javaSqlTypeBinding       is the Java sql types mapping to Java class
     */
    public ColumnSettings(ConfigurationCommonDBRef.MetadataOriginEnum metadataOriginEnum,
                          ConfigurationCommonDBRef.ColumnChangeCaseEnum columnCaseConversionEnum,
                          Map<Integer, String> javaSqlTypeBinding) {
        this.metadataOriginEnum = metadataOriginEnum;
        this.columnCaseConversionEnum = columnCaseConversionEnum;
        this.javaSqlTypeBinding = javaSqlTypeBinding;
    }

    /**
     * Returns the metadata orgin.
     *
     * @return indicator how the runtimeobtains output column metadata
     */
    public ConfigurationCommonDBRef.MetadataOriginEnum getMetadataRetrievalEnum() {
        return metadataOriginEnum;
    }

    /**
     * Returns the change case policy.
     *
     * @return indicator how the runtimeshould change case on output columns
     */
    public ConfigurationCommonDBRef.ColumnChangeCaseEnum getColumnCaseConversionEnum() {
        return columnCaseConversionEnum;
    }

    /**
     * Returns the mapping of java.sql.Types value to Java built-in name (class or simple name).
     *
     * @return map of sql type to Java built-in
     */
    public Map<Integer, String> getJavaSqlTypeBinding() {
        return javaSqlTypeBinding;
    }
}
