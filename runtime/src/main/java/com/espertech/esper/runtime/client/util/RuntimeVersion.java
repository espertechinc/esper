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
package com.espertech.esper.runtime.client.util;

/**
 * Runtime version.
 */
public class RuntimeVersion {
    /**
     * Current runtime version.
     */
    public static final String RUNTIME_VERSION = "8.1.0";

    /**
     * Current runtime major version.
     */
    public static final int MAJOR;

    /**
     * Current runtime minor version.
     */
    public static final int MINOR;

    /**
     * Current runtime patch version.
     */
    public static final int PATCH;

    static {
        MajorMinorPatch level = parseVersion(RUNTIME_VERSION);
        MAJOR = level.major;
        MINOR = level.minor;
        PATCH = level.patch;
    }

    /**
     * Compare major and minor version
     * @param compilerVersion version to compare
     * @throws VersionException when a difference is found
     */
    public static void checkVersion(String compilerVersion) throws VersionException {
        if (RuntimeVersion.RUNTIME_VERSION.equals(compilerVersion)) {
            return;
        }

        RuntimeVersion.MajorMinorPatch compiler;
        try {
            compiler = parseVersion(compilerVersion);
        } catch (NumberFormatException ex) {
            throw new VersionException(ex.getMessage(), ex);
        }

        if (compiler.getMajor() != RuntimeVersion.MAJOR || compiler.getMinor() != RuntimeVersion.MINOR) {
            throw new VersionException("Major or minor version of compiler and runtime mismatch; The runtime version is " + RuntimeVersion.RUNTIME_VERSION + " and the compiler version of the compiled unit is " + compilerVersion);
        }
    }

    private static MajorMinorPatch parseVersion(String version) throws NumberFormatException {
        if (version == null || version.trim().length() == 0) {
            throw new NumberFormatException("Null or empty semantic version");
        }
        String[] split = version.split("\\.");
        if (split.length != 3) {
            throw new NumberFormatException("Invalid semantic version '" + version + "'");
        }
        try {
            return new MajorMinorPatch(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        } catch (Throwable t) {
            throw new NumberFormatException("Invalid semantic version '" + version + "'");
        }
    }

    private static class MajorMinorPatch {
        private final int major;
        private final int minor;
        private final int patch;

        MajorMinorPatch(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        int getMajor() {
            return major;
        }

        int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }
    }

    public static class VersionException extends Exception {
        VersionException(String message) {
            super(message);
        }

        VersionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
