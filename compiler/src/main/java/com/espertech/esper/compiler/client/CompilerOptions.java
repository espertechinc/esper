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
package com.espertech.esper.compiler.client;

import com.espertech.esper.compiler.client.option.*;

/**
 * Callbacks and optional values for the compiler to determine modifiers, statement name,
 * statement user object, module name and module-uses.
 * All values are optional and can be null.
 */
public class CompilerOptions {
    private BusModifierEventTypeOption busModifierEventType;
    private AccessModifierContextOption accessModifierContext;
    private AccessModifierEventTypeOption accessModifierEventType;
    private AccessModifierExpressionOption accessModifierExpression;
    private AccessModifierNamedWindowOption accessModifierNamedWindow;
    private AccessModifierScriptOption accessModifierScript;
    private AccessModifierTableOption accessModifierTable;
    private AccessModifierVariableOption accessModifierVariable;
    private StatementUserObjectOption statementUserObject;
    private StatementNameOption statementName;
    private ModuleNameOption moduleName;
    private ModuleUsesOption moduleUses;

    /**
     * Returns the callback that determines the access modifier of a given event type
     *
     * @return callback returning an access modifier for an event type
     */
    public AccessModifierEventTypeOption getAccessModifierEventType() {
        return accessModifierEventType;
    }

    /**
     * Sets the callback that determines the access modifier of a given event type.
     *
     * @param accessModifierEventType callback returning an access modifier for an event type
     * @return itself
     */
    public CompilerOptions setAccessModifierEventType(AccessModifierEventTypeOption accessModifierEventType) {
        this.accessModifierEventType = accessModifierEventType;
        return this;
    }

    /**
     * Returns the callback that determines a compiler-time statement user object for a
     * statement. The user object is available from EPStatement by method {@code getUserObjectCompileTime}.
     *
     * @return callback to set a compile-time statement user object
     */
    public StatementUserObjectOption getStatementUserObject() {
        return statementUserObject;
    }

    /**
     * Sets the callback that determines a compiler-time statement user object for a
     * statement. The user object is available from EPStatement by method {@code getUserObjectCompileTime}.
     *
     * @param statementUserObject callback to set a compile-time statement user object
     * @return itself
     */
    public CompilerOptions setStatementUserObject(StatementUserObjectOption statementUserObject) {
        this.statementUserObject = statementUserObject;
        return this;
    }

    /**
     * Sets the callback that determines whether the event type is visible in the event bus i.e.
     * available for use with send-event.
     *
     * @param busModifierEventType callback to set the event type bus modifier value
     * @return itself
     */
    public CompilerOptions setBusModifierEventType(BusModifierEventTypeOption busModifierEventType) {
        this.busModifierEventType = busModifierEventType;
        return this;
    }

    /**
     * Returns the callback that determines whether the event type is visible in the event bus i.e.
     * available for use with send-event.
     *
     * @return callback to set the event type bus modifier value
     */
    public BusModifierEventTypeOption getBusModifierEventType() {
        return busModifierEventType;
    }

    /**
     * Returns the callback that determines the access modifier of a given context.
     *
     * @return callback returning an access modifier for a context
     */
    public AccessModifierContextOption getAccessModifierContext() {
        return accessModifierContext;
    }

    /**
     * Sets the callback that determines the access modifier of a given context.
     *
     * @param accessModifierContext callback returning an access modifier for a context
     * @return itself
     */
    public CompilerOptions setAccessModifierContext(AccessModifierContextOption accessModifierContext) {
        this.accessModifierContext = accessModifierContext;
        return this;
    }

    /**
     * Returns the callback that determines the access modifier of a given variable.
     *
     * @return callback returning an access modifier for a variable
     */
    public AccessModifierVariableOption getAccessModifierVariable() {
        return accessModifierVariable;
    }

    /**
     * Sets the callback that determines the access modifier of a given variable.
     *
     * @param accessModifierVariable callback returning an access modifier for a variable
     * @return itself
     */
    public CompilerOptions setAccessModifierVariable(AccessModifierVariableOption accessModifierVariable) {
        this.accessModifierVariable = accessModifierVariable;
        return this;
    }

    /**
     * Returns the callback that determines the access modifier of a given declared expression.
     *
     * @return callback returning an access modifier for a declared expression
     */
    public AccessModifierExpressionOption getAccessModifierExpression() {
        return accessModifierExpression;
    }

    /**
     * Sets the callback that determines the access modifier of a given declared expression.
     *
     * @param accessModifierExpression callback returning an access modifier for a declared expression
     * @return itself
     */
    public CompilerOptions setAccessModifierExpression(AccessModifierExpressionOption accessModifierExpression) {
        this.accessModifierExpression = accessModifierExpression;
        return this;
    }

    /**
     * Returns the callback that determines the access modifier of a given declared expression.
     *
     * @return callback returning an access modifier for a declared expression
     */
    public AccessModifierTableOption getAccessModifierTable() {
        return accessModifierTable;
    }

    /**
     * Returns the callback that determines the access modifier of a given table.
     *
     * @param accessModifierTable callback returning an access modifier for a table
     * @return itself
     */
    public CompilerOptions setAccessModifierTable(AccessModifierTableOption accessModifierTable) {
        this.accessModifierTable = accessModifierTable;
        return this;
    }

    /**
     * Returns the callback that determines the statement name
     *
     * @return callback returning the statement name
     */
    public StatementNameOption getStatementName() {
        return statementName;
    }

    /**
     * Sets the callback that determines the statement name
     *
     * @param statementName callback returning the statement name
     * @return itself
     */
    public CompilerOptions setStatementName(StatementNameOption statementName) {
        this.statementName = statementName;
        return this;
    }

    /**
     * Returns the callback that determines the access modifier of a given named window.
     *
     * @return callback returning an access modifier for an named window
     */
    public AccessModifierNamedWindowOption getAccessModifierNamedWindow() {
        return accessModifierNamedWindow;
    }

    /**
     * Sets the callback that determines the access modifier of a given named window.
     *
     * @param accessModifierNamedWindow callback returning an access modifier for an named window
     * @return itself
     */
    public CompilerOptions setAccessModifierNamedWindow(AccessModifierNamedWindowOption accessModifierNamedWindow) {
        this.accessModifierNamedWindow = accessModifierNamedWindow;
        return this;
    }

    /**
     * Returns the callback that determines the access modifier of a given script.
     *
     * @return callback returning an access modifier for a script
     */
    public AccessModifierScriptOption getAccessModifierScript() {
        return accessModifierScript;
    }

    /**
     * Sets the callback that determines the access modifier of a given script.
     *
     * @param accessModifierScript callback returning an access modifier for a script
     * @return itself
     */
    public CompilerOptions setAccessModifierScript(AccessModifierScriptOption accessModifierScript) {
        this.accessModifierScript = accessModifierScript;
        return this;
    }

    /**
     * Returns the callback that determines the module name.
     *
     * @return callback returning the module name to use
     */
    public ModuleNameOption getModuleName() {
        return moduleName;
    }

    /**
     * Sets the callback that determines the module name.
     *
     * @param moduleName callback returning the module name to use
     * @return itself
     */
    public CompilerOptions setModuleName(ModuleNameOption moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    /**
     * Returns the callback that determines the module uses.
     *
     * @return callback returning the module uses
     */
    public ModuleUsesOption getModuleUses() {
        return moduleUses;
    }

    /**
     * Sets the callback that determines the module uses.
     *
     * @param moduleUses callback returning the module uses
     * @return itself
     */
    public CompilerOptions setModuleUses(ModuleUsesOption moduleUses) {
        this.moduleUses = moduleUses;
        return this;
    }
}
