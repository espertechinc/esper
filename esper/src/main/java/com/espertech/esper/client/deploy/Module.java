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
package com.espertech.esper.client.deploy;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/**
 * Represent a deployment unit consisting of deployment declarative information (module name, uses and imports)
 * as well as EPL statements represented by {@link ModuleItem}. May have an additional user object and archive name
 * and uri pointing to the module source attached.
 * <p>
 * The module URI gets initialized with the filename, resource or URL being read, however may be overridden
 * and has not further meaning to the deployment.
 * <p>
 * The archive name and user object are opportunities to attach additional deployment information.
 */
public class Module implements Serializable {
    private static final long serialVersionUID = -6365726859286029218L;

    private String name;
    private String uri;
    private Set<String> uses;
    private Set<String> imports;
    private List<ModuleItem> items;
    private String archiveName;
    private Object userObject;
    private String moduleText;

    /**
     * Ctor.
     *
     * @param name       module name
     * @param uri        module uri
     * @param uses       names of modules that this module depends on
     * @param imports    the Java class imports
     * @param items      EPL statements
     * @param moduleText text of module
     */
    public Module(String name, String uri, Set<String> uses, Set<String> imports, List<ModuleItem> items, String moduleText) {
        this.name = name;
        this.uri = uri;
        this.uses = uses;
        this.imports = imports;
        this.items = items;
        this.moduleText = moduleText;
    }

    /**
     * Returns the name of the archive this module originated from, or null if not applicable.
     *
     * @return archive name
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * Set the name of the archive this module originated from, or null if not applicable.
     *
     * @param archiveName archive name
     */
    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    /**
     * Returns the optional user object that may be attached to the module.
     *
     * @return user object
     */
    public Object getUserObject() {
        return userObject;
    }

    /**
     * Sets an optional user object that may be attached to the module.
     *
     * @param userObject user object
     */
    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    /**
     * Returns the module name, if provided.
     *
     * @return module name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the module name or null if none provided.
     *
     * @param name module name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the module URI if provided.
     *
     * @return module URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the module URI or null if none provided.
     *
     * @param uri of module
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the dependencies the module may have on other modules.
     *
     * @return module dependencies
     */
    public Set<String> getUses() {
        return uses;
    }

    /**
     * Sets the dependencies the module may have on other modules.
     *
     * @param uses module dependencies
     */
    public void setUses(Set<String> uses) {
        this.uses = uses;
    }

    /**
     * Returns a list of statements (some may be comments only) that make up the module.
     *
     * @return statements
     */
    public List<ModuleItem> getItems() {
        return items;
    }

    /**
     * Sets a list of statements (some may be comments only) that make up the module.
     *
     * @param items statements
     */
    public void setItems(List<ModuleItem> items) {
        this.items = items;
    }

    /**
     * Returns the imports defined by the module.
     *
     * @return module imports
     */
    public Set<String> getImports() {
        return imports;
    }

    /**
     * Sets the imports defined by the module.
     *
     * @param imports module imports
     */
    public void setImports(Set<String> imports) {
        this.imports = imports;
    }

    /**
     * Returns module text.
     *
     * @return text
     */
    public String getModuleText() {
        return moduleText;
    }

    /**
     * Sets module text.
     *
     * @param moduleText text to set
     */
    public void setModuleText(String moduleText) {
        this.moduleText = moduleText;
    }

    public String toString() {
        StringWriter buf = new StringWriter();
        if (name == null) {
            buf.append("(unnamed)");
        } else {
            buf.append("'" + name + "'");
        }
        if (uri != null) {
            buf.append(" uri '" + uri + "'");
        }
        return buf.toString();
    }
}
