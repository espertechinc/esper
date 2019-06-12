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

import java.io.Serializable;
import java.util.Map;

/**
 * The byte code and manifest of a compile EPL module or EPL fire-and-forget query.
 */
public class EPCompiled implements Serializable {
    private static final long serialVersionUID = -8931728660516931214L;
    private final Map<String, byte[]> classes;
    private final EPCompiledManifest manifest;

    /**
     * Ctor.
     *
     * @param classes  map of class name and byte code for a classloader
     * @param manifest the manifest
     */
    public EPCompiled(Map<String, byte[]> classes, EPCompiledManifest manifest) {
        this.classes = classes;
        this.manifest = manifest;
    }

    /**
     * Returns a map of class name and byte code for a classloader
     *
     * @return classes
     */
    public Map<String, byte[]> getClasses() {
        return classes;
    }

    /**
     * Returns a manifest object
     *
     * @return manifest
     */
    public EPCompiledManifest getManifest() {
        return manifest;
    }
}
