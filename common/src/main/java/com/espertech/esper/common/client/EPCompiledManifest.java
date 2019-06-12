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
package com.espertech.esper.common.client;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Manifest is part of the {@link EPCompiled} and provides information for the runtime that
 * allows it to use the byte code.
 */
public class EPCompiledManifest implements Serializable {
    private static final long serialVersionUID = -5866999692666004050L;
    private final String compilerVersion;
    private final String moduleProviderClassName;
    private final String queryProviderClassName;
    private final boolean targetHA;

    /**
     * Ctor.
     *
     * @param compilerVersion         compiler version
     * @param moduleProviderClassName class name of the class providing the module, or null for fire-and-forget query
     * @param queryProviderClassName  class name of the class providing the fire-and-forget query, or null when this is a module
     * @param targetHA flag indicating whether the compiler targets high-availability
     */
    public EPCompiledManifest(String compilerVersion, String moduleProviderClassName, String queryProviderClassName, boolean targetHA) {
        this.compilerVersion = compilerVersion;
        this.moduleProviderClassName = moduleProviderClassName;
        this.queryProviderClassName = queryProviderClassName;
        this.targetHA = targetHA;
    }

    /**
     * Returns the compiler version.
     *
     * @return compiler version
     */
    public String getCompilerVersion() {
        return compilerVersion;
    }

    /**
     * Returns the class name of the class providing the module, or null for fire-and-forget query
     *
     * @return class name
     */
    public String getModuleProviderClassName() {
        return moduleProviderClassName;
    }

    /**
     * Returns the class name of the class providing the fire-and-forget query, or null when this is a module
     *
     * @return class name
     */
    public String getQueryProviderClassName() {
        return queryProviderClassName;
    }

    /**
     * Returns flag indicating whether the compiler targets high-availability
     * @return indicator
     */
    public boolean isTargetHA() {
        return targetHA;
    }

    /**
     * Write the manifest to output.
     *
     * @param output output
     * @throws IOException when an IO exception occurs
     */
    public void write(DataOutput output) throws IOException {
        output.writeUTF(compilerVersion);
        writeNullableString(moduleProviderClassName, output);
        writeNullableString(queryProviderClassName, output);
        output.writeBoolean(targetHA);
    }

    /**
     * Read the manifest from input.
     *
     * @param input input
     * @return manifest
     * @throws IOException when an IO exception occurs
     */
    public static EPCompiledManifest read(DataInput input) throws IOException {
        String compilerVersion = input.readUTF();
        String moduleClassName = readNullableString(input);
        String queryClassName = readNullableString(input);
        boolean targetHA = input.readBoolean();
        return new EPCompiledManifest(compilerVersion, moduleClassName, queryClassName, targetHA);
    }

    private void writeNullableString(String value, DataOutput output) throws IOException {
        if (value == null) {
            output.writeBoolean(false);
            return;
        }
        output.writeBoolean(true);
        output.writeUTF(value);
    }

    private static String readNullableString(DataInput input) throws IOException {
        boolean hasValue = input.readBoolean();
        if (!hasValue) {
            return null;
        }
        return input.readUTF();
    }
}
