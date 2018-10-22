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
package com.espertech.esper.common.client.util;

/**
 * Visibility modifiers for EPL objects.
 */
public enum NameAccessModifier {
    /**
     * Transient is used for non-visible objects that are only visible for the purpose of statement-internal processing.
     */
    TRANSIENT(false, true),

    /**
     * Private is used for objects that may be used with the same module.
     */
    PRIVATE(true, true),

    /**
     * Protected is used for objects that may be used with the modules of the same module name.
     */
    PROTECTED(true, false),

    /**
     * Public is used for objects that may be used by other modules irrespective of module names.
     */
    PUBLIC(true, false),

    /**
     * Preconfigured is used for objects that are preconfigured by configuration.
     */
    PRECONFIGURED(false, false);

    private final boolean isAccessModifier;
    private final boolean privateOrTransient;

    NameAccessModifier(boolean isAccessModifier, boolean privateOrTransient) {
        this.isAccessModifier = isAccessModifier;
        this.privateOrTransient = privateOrTransient;
    }

    /**
     * Returns indicator whether the object is visible.
     * <p>
     * Always false if the object is private or transient.
     * </p>
     * <p>
     * Always true if the object is public or preconfigured.
     * </p>
     * <p>
     * For protected the module name must match
     * </p>
     *
     * @param objectVisibility object visibility
     * @param objectModuleName object module name
     * @param importModuleName my module name
     * @return indicator
     */
    public static boolean visible(NameAccessModifier objectVisibility, String objectModuleName, String importModuleName) {
        if (objectVisibility.isPrivateOrTransient()) {
            return false;
        }
        if (objectVisibility == NameAccessModifier.PROTECTED) {
            return compareModuleName(objectModuleName, importModuleName);
        }
        return true;
    }

    /**
     * Returns true if the modifier can be used by modules i.e. returns true for private, protected and public.
     * Returns false for preconfigured since preconfigured is reserved for configured objects.
     * Returns false for transient as transient is reserved for internal use
     *
     * @return indicator
     */
    public boolean isModuleProvidedAccessModifier() {
        return isAccessModifier;
    }

    /**
     * Returns true for a public and protected and false for all others
     *
     * @return indicator
     */
    public boolean isNonPrivateNonTransient() {
        return !privateOrTransient && this != PRECONFIGURED;
    }

    /**
     * Returns true for a private and transient and false for all others
     *
     * @return indicator
     */
    public boolean isPrivateOrTransient() {
        return privateOrTransient;
    }

    private static boolean compareModuleName(String objectModuleName, String importModuleName) {
        if (objectModuleName == null && importModuleName == null) {
            return true;
        }
        if (objectModuleName != null && importModuleName != null) {
            return objectModuleName.equals(importModuleName);
        }
        return false;
    }
}
