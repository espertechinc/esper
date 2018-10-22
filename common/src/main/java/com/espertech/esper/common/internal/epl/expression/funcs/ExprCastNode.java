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
package com.espertech.esper.common.internal.epl.expression.funcs;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.pattern.observer.TimerScheduleISO8601Parser;
import com.espertech.esper.common.internal.schedule.ScheduleParameterException;
import com.espertech.esper.common.internal.settings.RuntimeSettingsTimeZoneField;
import com.espertech.esper.common.internal.type.ClassIdentifierWArray;
import com.espertech.esper.common.internal.util.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the CAST(expression, type) function is an expression tree.
 */
public class ExprCastNode extends ExprNodeBase {
    private final ClassIdentifierWArray classIdentifierWArray;

    private ExprCastNodeForge forge;

    /**
     * Ctor.
     *
     * @param classIdentifierWArray the the name of the type to cast to
     */
    public ExprCastNode(ClassIdentifierWArray classIdentifierWArray) {
        this.classIdentifierWArray = classIdentifierWArray;
    }

    public ExprEvaluator getExprEvaluator() {
        checkValidated(forge);
        return forge.getExprEvaluator();
    }

    public ExprForge getForge() {
        checkValidated(forge);
        return forge;
    }

    public ClassIdentifierWArray getClassIdentifierWArray() {
        return classIdentifierWArray;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length == 0 || this.getChildNodes().length > 2) {
            throw new ExprValidationException("Cast function node must have one or two child expressions");
        }

        Class fromType = this.getChildNodes()[0].getForge().getEvaluationType();
        String classIdentifier = classIdentifierWArray.getClassIdentifier();
        int arrayDimensions = classIdentifierWArray.getArrayDimensions();

        // determine date format parameter
        Map<String, ExprNamedParameterNode> namedParams = ExprNodeUtilityValidate.getNamedExpressionsHandleDups(Arrays.asList(this.getChildNodes()));
        ExprNodeUtilityValidate.validateNamed(namedParams, new String[]{"dateformat"});
        ExprNamedParameterNode dateFormatParameter = namedParams.get("dateformat");
        if (dateFormatParameter != null) {
            ExprNodeUtilityValidate.validateNamedExpectType(dateFormatParameter, new Class[]{String.class, DateFormat.class, DateTimeFormatter.class});
        }

        // identify target type
        // try the primitive names including "string"
        SimpleTypeCaster caster;
        Class targetType = JavaClassHelper.getPrimitiveClassForName(classIdentifier.trim());
        if (!classIdentifierWArray.isArrayOfPrimitive()) {
            targetType = JavaClassHelper.getBoxedType(targetType);
        }
        targetType = applyDimensions(targetType);

        boolean numeric;
        CasterParserComputerForge casterParserComputerForge = null;
        if (dateFormatParameter != null) {
            if (fromType != String.class) {
                throw new ExprValidationException("Use of the '" + dateFormatParameter.getParameterName() + "' named parameter requires a string-type input");
            }
            if (targetType == null) {
                try {
                    targetType = JavaClassHelper.getClassForName(classIdentifier.trim(), validationContext.getClasspathImportService().getClassForNameProvider());
                    targetType = applyDimensions(targetType);
                } catch (ClassNotFoundException e) {
                    // expected
                }
            }

            // dynamic or static date format
            numeric = false;
            caster = null;
            if (targetType == Date.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("date")) {
                targetType = Date.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToDateWStaticISOFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToDateWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToDateWExprFormatComputer(desc.getDynamicDateFormat(), desc.isDeployTimeConstant());
                }
            } else if (targetType == Calendar.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("calendar")) {
                targetType = Calendar.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToCalendarWStaticISOFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToCalendarWStaticFormatComputer(desc.getStaticDateFormatString(), TimeZone.getDefault()); // Note how code-generation does not use the default time zone
                } else {
                    casterParserComputerForge = new StringToCalendarWExprFormatComputer(desc.getDynamicDateFormat(), TimeZone.getDefault());
                }
            } else if (targetType == Long.class) {
                targetType = Long.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, false);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLongWStaticISOFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToLongWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToLongWExprFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdatetime")) {
                targetType = LocalDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalDateTimeIsoFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToLocalDateTimeWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToLocalDateTimeWExprFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalDate.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localdate")) {
                targetType = LocalDate.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalDateIsoFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToLocalDateWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToLocalDateWExprFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == LocalTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("localtime")) {
                targetType = LocalTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToLocalTimeIsoFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToLocalTimeWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToLocalTimeWExprFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else if (targetType == ZonedDateTime.class || classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("zoneddatetime")) {
                targetType = ZonedDateTime.class;
                ExprCastNodeDateDesc desc = validateDateFormat(dateFormatParameter, validationContext, true);
                if (desc.isIso8601Format()) {
                    casterParserComputerForge = new StringToZonedDateTimeIsoFormatComputer();
                } else if (desc.getStaticDateFormatString() != null) {
                    casterParserComputerForge = new StringToZonedDateTimeWStaticFormatComputer(desc.getStaticDateFormatString());
                } else {
                    casterParserComputerForge = new StringToZonedDateTimeWExprFormatComputerForge(desc.getDynamicDateFormat());
                }
            } else {
                throw new ExprValidationException("Use of the '" + dateFormatParameter.getParameterName() + "' named parameter requires a target type of calendar, date, long, localdatetime, localdate, localtime or zoneddatetime");
            }
        } else if (targetType != null) {
            targetType = JavaClassHelper.getBoxedType(targetType);
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = caster.isNumericCast();
        } else if (classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("BigInteger".toLowerCase(Locale.ENGLISH))) {
            targetType = BigInteger.class;
            targetType = applyDimensions(targetType);
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = true;
        } else if (classIdentifier.trim().toLowerCase(Locale.ENGLISH).equals("BigDecimal".toLowerCase(Locale.ENGLISH))) {
            targetType = BigDecimal.class;
            targetType = applyDimensions(targetType);
            caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            numeric = true;
        } else {
            try {
                targetType = JavaClassHelper.getClassForName(classIdentifier.trim(), validationContext.getClasspathImportService().getClassForNameProvider());
            } catch (ClassNotFoundException e) {
                throw new ExprValidationException("Class as listed in cast function by name '" + classIdentifier + "' cannot be loaded", e);
            }
            targetType = applyDimensions(targetType);
            numeric = JavaClassHelper.isNumeric(targetType);
            if (numeric) {
                caster = SimpleTypeCasterFactory.getCaster(fromType, targetType);
            } else {
                caster = new SimpleTypeCasterAnyType(targetType);
            }
        }

        // assign a computer unless already assigned
        if (casterParserComputerForge == null) {
            // to-string
            if (targetType == String.class) {
                casterParserComputerForge = new StringXFormComputer();
            } else if (fromType == String.class && targetType != Character.class) {
                // parse
                SimpleTypeParserSPI parser = SimpleTypeParserFactory.getParser(JavaClassHelper.getBoxedType(targetType));
                casterParserComputerForge = new StringParserComputer(parser);
            } else if (numeric) {
                // numeric cast with check
                casterParserComputerForge = new NumberCasterComputer(caster);
            } else {
                // non-numeric cast
                casterParserComputerForge = new NonnumericCasterComputer(caster);
            }
        }

        // determine constant or not
        Object theConstant = null;
        boolean isConstant = false;
        if (this.getChildNodes()[0].getForge().getForgeConstantType().isCompileTimeConstant()) {
            isConstant = casterParserComputerForge.isConstantForConstInput();
            if (isConstant) {
                Object in = this.getChildNodes()[0].getForge().getExprEvaluator().evaluate(null, true, null);
                theConstant = in == null ? null : casterParserComputerForge.getEvaluatorComputer().compute(in, null, true, null);
            }
        }

        forge = new ExprCastNodeForge(this, casterParserComputerForge, targetType, isConstant, theConstant);
        return null;
    }

    public boolean isConstantResult() {
        checkValidated(forge);
        return forge.isConstant();
    }

    public Class getTargetType() {
        checkValidated(forge);
        return forge.getEvaluationType();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("cast(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(",");
        classIdentifierWArray.toEPL(writer);
        for (int i = 1; i < this.getChildNodes().length; i++) {
            writer.write(",");
            this.getChildNodes()[i].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        }
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix) {
        if (!(node instanceof ExprCastNode)) {
            return false;
        }
        ExprCastNode other = (ExprCastNode) node;
        return other.classIdentifierWArray.equals(this.classIdentifierWArray);
    }

    /**
     * Casting and parsing computer.
     */
    public interface CasterParserComputerForge {
        public boolean isConstantForConstInput();

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope);

        public CasterParserComputer getEvaluatorComputer();
    }

    /**
     * Casting and parsing computer.
     */
    public interface CasterParserComputer {
        /**
         * Compute an result performing casting and parsing.
         *
         * @param input                to process
         * @param eventsPerStream      events per stream
         * @param newData              new data indicator
         * @param exprEvaluatorContext evaluation context
         * @return cast or parse result
         */
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext);
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringXFormComputer implements CasterParserComputerForge, CasterParserComputer {
        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return input.toString();
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return exprDotMethod(input, "toString");
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NumberCasterComputer implements CasterParserComputerForge, CasterParserComputer {
        private final SimpleTypeCaster numericTypeCaster;

        public NumberCasterComputer(SimpleTypeCaster numericTypeCaster) {
            this.numericTypeCaster = numericTypeCaster;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            if (input instanceof Number) {
                return numericTypeCaster.cast(input);
            }
            return null;
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            if (inputType.isPrimitive() || JavaClassHelper.isSubclassOrImplementsInterface(inputType, Number.class)) {
                return numericTypeCaster.codegen(input, inputType, codegenMethodScope, codegenClassScope);
            }
            CodegenMethod methodNode = codegenMethodScope.makeChild(evaluationType, NumberCasterComputer.class, codegenClassScope).addParam(inputType, "input");

            methodNode.getBlock()
                    .ifInstanceOf("input", Number.class)
                    .blockReturn(numericTypeCaster.codegen(ref("input"), inputType, methodNode, codegenClassScope))
                    .methodReturn(constantNull());
            return localMethod(methodNode, input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class StringParserComputer implements CasterParserComputer, CasterParserComputerForge {
        private final SimpleTypeParserSPI parser;

        public StringParserComputer(SimpleTypeParserSPI parser) {
            this.parser = parser;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parser.parse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return parser.codegen(input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    /**
     * Casting and parsing computer.
     */
    public static class NonnumericCasterComputer implements CasterParserComputerForge, CasterParserComputer {
        private final SimpleTypeCaster caster;

        public NonnumericCasterComputer(SimpleTypeCaster numericTypeCaster) {
            this.caster = numericTypeCaster;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return caster.cast(input);
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return caster.codegen(input, inputType, codegenMethodScope, codegenClassScope);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static EPException handleParseException(DateFormat format, String date, Exception ex) {
        String pattern;
        if (format instanceof SimpleDateFormat) {
            pattern = ((SimpleDateFormat) format).toPattern();
        } else {
            pattern = format.toString();
        }
        return handleParseException(pattern, date, ex);
    }

    public static EPException handleParseException(String formatString, String date, Exception ex) {
        return new EPException("Exception parsing date '" + date + "' format '" + formatString + "': " + ex.getMessage(), ex);
    }

    public static EPException handleParseISOException(String date, ScheduleParameterException ex) {
        return new EPException("Exception parsing iso8601 date '" + date + "': " + ex.getMessage(), ex);
    }

    public static abstract class StringToDateLongWStaticFormat implements CasterParserComputerForge, CasterParserComputer {
        protected final String dateFormatString;

        public StringToDateLongWStaticFormat(String dateFormatString) {
            this.dateFormatString = dateFormatString;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public boolean isConstantForConstInput() {
            return true;
        }
    }

    public static abstract class StringToDateLongWExprFormatForge implements CasterParserComputerForge {
        protected final ExprForge dateFormatForge;

        protected StringToDateLongWExprFormatForge(ExprForge dateFormatForge) {
            this.dateFormatForge = dateFormatForge;
        }

        public boolean isConstantForConstInput() {
            return false;
        }
    }

    public static abstract class StringToDateLongWExprormatEval implements CasterParserComputer {
        protected final ExprEvaluator dateFormatEval;

        public StringToDateLongWExprormatEval(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }
    }

    public static class StringToDateWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToDateWStaticFormatComputer(String dateFormatString) {
            super(dateFormatString);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToDateWStaticFormatParseSafe(new SimpleDateFormat(dateFormatString), input);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param format format
         * @param input  input
         * @return date
         */
        public static Date stringToDateWStaticFormatParseSafe(DateFormat format, Object input) {
            try {
                return format.parse(input.toString());
            } catch (ParseException e) {
                throw handleParseException(format, input.toString(), e);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToDateWStaticFormatComputer.class, "stringToDateWStaticFormatParseSafe", formatFieldJava7(dateFormatString, codegenClassScope), input);
        }
    }

    public abstract static class StringToJava8WStaticFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        protected final String format;

        public StringToJava8WStaticFormatComputer(String format) {
            this.format = format;
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public abstract Object parse(String input);

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return parse(input.toString());
        }

        protected CodegenExpression codegenFormatter(CodegenClassScope codegenClassScope) {
            return codegenClassScope.addFieldUnshared(true, DateTimeFormatter.class, staticMethod(DateTimeFormatter.class, "ofPattern", constant(format)));
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToLocalDateTimeIsoFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        public boolean isConstantForConstInput() {
            return true;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateTimeWStaticFormatComputer.class, "stringToLocalDateTimeWStaticFormatParse", input, publicConstValue(DateTimeFormatter.class, "ISO_DATE_TIME"));
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return StringToLocalDateTimeWStaticFormatComputer.stringToLocalDateTimeWStaticFormatParse(input.toString(), DateTimeFormatter.ISO_DATE_TIME);
        }
    }

    public static class StringToLocalDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateTimeWStaticFormatComputer(String format) {
            super(format);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return ldt
         */
        public static LocalDateTime stringToLocalDateTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalDateTimeWStaticFormatParse(input, DateTimeFormatter.ISO_DATE_TIME);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateTimeWStaticFormatComputer.class, "stringToLocalDateTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToLocalDateIsoFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        public boolean isConstantForConstInput() {
            return true;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateWStaticFormatComputer.class, "stringToLocalDateWStaticFormatParse", input, publicConstValue(DateTimeFormatter.class, "ISO_DATE"));
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return StringToLocalDateWStaticFormatComputer.stringToLocalDateWStaticFormatParse(input.toString(), DateTimeFormatter.ISO_DATE);
        }
    }

    public static class StringToLocalDateWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalDateWStaticFormatComputer(String format) {
            super(format);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return ld
         */
        public static LocalDate stringToLocalDateWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalDateWStaticFormatParse(input, DateTimeFormatter.ofPattern(format));
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalDateWStaticFormatComputer.class, "stringToLocalDateWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToLocalTimeIsoFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        public boolean isConstantForConstInput() {
            return true;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalTimeWStaticFormatComputer.class, "stringToLocalTimeWStaticFormatParse", input, publicConstValue(DateTimeFormatter.class, "ISO_TIME"));
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return StringToLocalTimeWStaticFormatComputer.stringToLocalTimeWStaticFormatParse(input.toString(), DateTimeFormatter.ISO_TIME);
        }
    }

    public static class StringToLocalTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToLocalTimeWStaticFormatComputer(String format) {
            super(format);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return lt
         */
        public static LocalTime stringToLocalTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return LocalTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringToLocalTimeWStaticFormatParse(input, DateTimeFormatter.ofPattern(format));
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLocalTimeWStaticFormatComputer.class, "stringToLocalTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public static class StringToZonedDateTimeIsoFormatComputer implements CasterParserComputerForge, CasterParserComputer {
        public boolean isConstantForConstInput() {
            return true;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToZonedDateTimeWStaticFormatComputer.class, "stringZonedDateTimeWStaticFormatParse", input, publicConstValue(DateTimeFormatter.class, "ISO_ZONED_DATE_TIME"));
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return StringToZonedDateTimeWStaticFormatComputer.stringZonedDateTimeWStaticFormatParse(input.toString(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
        }
    }

    public static class StringToZonedDateTimeWStaticFormatComputer extends StringToJava8WStaticFormatComputer {
        public StringToZonedDateTimeWStaticFormatComputer(String format) {
            super(format);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input     string
         * @param formatter formatter
         * @return lt
         */
        public static ZonedDateTime stringZonedDateTimeWStaticFormatParse(String input, DateTimeFormatter formatter) {
            try {
                return ZonedDateTime.parse(input, formatter);
            } catch (DateTimeParseException e) {
                throw handleParseException(formatter.toString(), input, e);
            }
        }

        public Object parse(String input) {
            return stringZonedDateTimeWStaticFormatParse(input, DateTimeFormatter.ofPattern(format));
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToZonedDateTimeWStaticFormatComputer.class, "stringZonedDateTimeWStaticFormatParse", input, codegenFormatter(codegenClassScope));
        }
    }

    public abstract static class StringToJava8WExprFormatComputerForge implements CasterParserComputerForge {
        protected final ExprForge dateFormatForge;

        public StringToJava8WExprFormatComputerForge(ExprForge dateFormatForge) {
            this.dateFormatForge = dateFormatForge;
        }

        public boolean isConstantForConstInput() {
            return false;
        }
    }

    public abstract static class StringToJava8WExprFormatComputerEval implements CasterParserComputer {
        protected final ExprEvaluator dateFormatEval;

        public StringToJava8WExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            this.dateFormatEval = dateFormatEval;
        }
    }

    public static class StringToLocalDateTimeWExprFormatComputerForge extends StringToJava8WExprFormatComputerForge {
        public StringToLocalDateTimeWExprFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalDateTimeWExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalDateTimeWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalDateTimeWExprFormatComputerEval extends StringToJava8WExprFormatComputerEval {
        public StringToLocalDateTimeWExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalDateTimeWStaticFormatComputer.stringToLocalDateTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(LocalDateTime.class, StringToLocalDateTimeWExprFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            CodegenExpression formatter;
            if (dateFormatForge.getForgeConstantType().isConstant()) {
                formatter = formatFieldExpr(DateTimeFormatter.class, dateFormatForge, codegenClassScope);
            } else {
                method.getBlock().declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope)));
                formatter = ref("formatter");
            }
            method.getBlock().methodReturn(staticMethod(StringToLocalDateTimeWStaticFormatComputer.class, "stringToLocalDateTimeWStaticFormatParse", ref("input"), formatter));
            return localMethod(method, input);
        }
    }

    public static class StringToLocalDateWExprFormatComputerForge extends StringToJava8WExprFormatComputerForge {
        public StringToLocalDateWExprFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalDateWExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalDateWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalDateWExprFormatComputerEval extends StringToJava8WExprFormatComputerEval {
        public StringToLocalDateWExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public LocalDate compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalDateWStaticFormatComputer.stringToLocalDateWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(LocalDate.class, StringToLocalDateWExprFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            CodegenExpression format;
            if (dateFormatForge.getForgeConstantType().isConstant()) {
                format = formatFieldExpr(DateTimeFormatter.class, dateFormatForge, codegenClassScope);
            } else {
                method.getBlock()
                        .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope)));
                format = ref("formatter");
            }
            method.getBlock().methodReturn(staticMethod(StringToLocalDateWStaticFormatComputer.class, "stringToLocalDateWStaticFormatParse", ref("input"), format));
            return localMethod(method, input);
        }
    }

    public static class StringToLocalTimeWExprFormatComputerForge extends StringToJava8WExprFormatComputerForge {
        public StringToLocalTimeWExprFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLocalTimeWExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLocalTimeWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLocalTimeWExprFormatComputerEval extends StringToJava8WExprFormatComputerEval {
        public StringToLocalTimeWExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public LocalTime compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToLocalTimeWStaticFormatComputer.stringToLocalTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(LocalTime.class, StringToLocalTimeWExprFormatComputerForge.class, codegenClassScope).addParam(String.class, "input");

            CodegenExpression format;
            if (dateFormatForge.getForgeConstantType().isConstant()) {
                format = formatFieldExpr(DateTimeFormatter.class, dateFormatForge, codegenClassScope);
            } else {
                methodNode.getBlock()
                        .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope)));
                format = ref("formatter");
            }
            methodNode.getBlock()
                    .methodReturn(staticMethod(StringToLocalTimeWStaticFormatComputer.class, "stringToLocalTimeWStaticFormatParse", ref("input"), format));
            return localMethod(methodNode, input);
        }
    }

    public static class StringToZonedDateTimeWExprFormatComputerForge extends StringToJava8WExprFormatComputerForge {
        public StringToZonedDateTimeWExprFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToZonedDateTimeWExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToZonedDateTimeWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToZonedDateTimeWExprFormatComputerEval extends StringToJava8WExprFormatComputerEval {
        public StringToZonedDateTimeWExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public ZonedDateTime compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            DateTimeFormatter formatter = stringToDateTimeFormatterSafe(format);
            return StringToZonedDateTimeWStaticFormatComputer.stringZonedDateTimeWStaticFormatParse(input.toString(), formatter);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod methodNode = codegenMethodScope.makeChild(ZonedDateTime.class, StringToZonedDateTimeWExprFormatComputerEval.class, codegenClassScope).addParam(String.class, "input");
            CodegenExpression format;
            if (dateFormatForge.getForgeConstantType().isConstant()) {
                format = formatFieldExpr(DateTimeFormatter.class, dateFormatForge, codegenClassScope);
            } else {
                methodNode.getBlock()
                        .declareVar(DateTimeFormatter.class, "formatter", staticMethod(ExprCastNode.class, "stringToDateTimeFormatterSafe", dateFormatForge.evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope)));
                format = ref("formatter");
            }
            methodNode.getBlock().methodReturn(staticMethod(StringToZonedDateTimeWStaticFormatComputer.class, "stringZonedDateTimeWStaticFormatParse", ref("input"), format));
            return localMethod(methodNode, input);
        }
    }

    public static class StringToDateWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return date
         */
        public static Date stringToDateWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input).getTime();
            } catch (ScheduleParameterException e) {
                throw handleParseISOException(input, e);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToDateWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToDateWStaticISOFormatComputer.class, "stringToDateWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToLongWStaticFormatComputer extends StringToDateLongWStaticFormat {
        public StringToLongWStaticFormatComputer(String dateFormatString) {
            super(dateFormatString);
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToLongWStaticFormatParseSafe(new SimpleDateFormat(dateFormatString), input);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param format format
         * @param input  input
         * @return msec
         */
        public static long stringToLongWStaticFormatParseSafe(DateFormat format, Object input) {
            try {
                return format.parse(input.toString()).getTime();
            } catch (ParseException e) {
                throw handleParseException(format, input.toString(), e);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLongWStaticFormatComputer.class, "stringToLongWStaticFormatParseSafe", formatFieldJava7(dateFormatString, codegenClassScope), input);
        }
    }

    public static class StringToLongWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return msec
         */
        public static long stringToLongWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input).getTimeInMillis();
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input, ex);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToLongWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToLongWStaticISOFormatComputer.class, "stringToLongWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToCalendarWStaticFormatComputer extends StringToDateLongWStaticFormat {

        private final TimeZone timeZone;

        public StringToCalendarWStaticFormatComputer(String dateFormatString, TimeZone timeZone) {
            super(dateFormatString);
            this.timeZone = timeZone;
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToCalendarWStaticFormatParse(new SimpleDateFormat(dateFormatString), input, timeZone);
        }

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param format   format
         * @param input    input
         * @param timeZone time zone
         * @return cal
         */
        public static Calendar stringToCalendarWStaticFormatParse(DateFormat format, Object input, TimeZone timeZone) {
            try {
                Calendar cal = Calendar.getInstance(timeZone);
                Date date = format.parse(input.toString());
                cal.setTime(date);
                return cal;
            } catch (ParseException ex) {
                throw handleParseException(format, input.toString(), ex);
            }
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
            return staticMethod(StringToCalendarWStaticFormatComputer.class, "stringToCalendarWStaticFormatParse", formatFieldJava7(dateFormatString, codegenClassScope), input, timeZoneField);
        }
    }

    public static class StringToCalendarWStaticISOFormatComputer implements CasterParserComputerForge, CasterParserComputer {

        /**
         * NOTE: Code-generation-invoked method, method name and parameter order matters
         *
         * @param input input
         * @return cal
         */
        public static Calendar stringToCalendarWStaticISOParse(String input) {
            try {
                return TimerScheduleISO8601Parser.parseDate(input);
            } catch (ScheduleParameterException ex) {
                throw handleParseISOException(input, ex);
            }
        }

        public Object compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            return stringToCalendarWStaticISOParse(input.toString());
        }

        public boolean isConstantForConstInput() {
            return true;
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return staticMethod(StringToCalendarWStaticISOFormatComputer.class, "stringToCalendarWStaticISOParse", input);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return this;
        }
    }

    public static class StringToDateWExprFormatComputer extends StringToDateLongWExprFormatForge {
        public StringToDateWExprFormatComputer(ExprForge dateFormatForge, boolean deployTimeConstant) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToDateExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToDateExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToDateExprFormatComputerEval extends StringToDateLongWExprormatEval {
        public StringToDateExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Date compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToDateWStaticFormatComputer.stringToDateWStaticFormatParseSafe(dateFormat, input);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge formatExpr, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(Date.class, StringToDateExprFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            CodegenExpression dateFormat;
            if (formatExpr.getForgeConstantType().isConstant()) {
                dateFormat = formatFieldExpr(DateFormat.class, formatExpr, codegenClassScope);
            } else {
                method.getBlock()
                        .declareVar(Object.class, "format", formatExpr.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                        .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")));
                dateFormat = ref("dateFormat");
            }
            method.getBlock().methodReturn(staticMethod(StringToDateWStaticFormatComputer.class, "stringToDateWStaticFormatParseSafe", dateFormat, ref("input")));
            return localMethod(method, input);
        }
    }

    public static class StringToLongWExprFormatComputerForge extends StringToDateLongWExprFormatForge {

        public StringToLongWExprFormatComputerForge(ExprForge dateFormatForge) {
            super(dateFormatForge);
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToLongWExprFormatComputerEval(dateFormatForge.getExprEvaluator());
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToLongWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope);
        }
    }

    public static class StringToLongWExprFormatComputerEval extends StringToDateLongWExprormatEval {

        public StringToLongWExprFormatComputerEval(ExprEvaluator dateFormatEval) {
            super(dateFormatEval);
        }

        public Long compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToLongWStaticFormatComputer.stringToLongWStaticFormatParseSafe(dateFormat, input);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge formatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            CodegenMethod method = codegenMethodScope.makeChild(Long.class, StringToLongWExprFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            CodegenExpression format;
            if (formatForge.getForgeConstantType().isConstant()) {
                format = formatFieldExpr(DateFormat.class, formatForge, codegenClassScope);
            } else {
                method.getBlock()
                        .declareVar(Object.class, "format", formatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                        .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")));
                format = ref("dateFormat");
            }
            method.getBlock().methodReturn(staticMethod(StringToLongWStaticFormatComputer.class, "stringToLongWStaticFormatParseSafe", format, ref("input")));
            return localMethod(method, input);
        }
    }

    public static class StringToCalendarWExprFormatComputer extends StringToDateLongWExprFormatForge {

        private final TimeZone timeZone;

        public StringToCalendarWExprFormatComputer(ExprForge dateFormatForge, TimeZone timeZone) {
            super(dateFormatForge);
            this.timeZone = timeZone;
        }

        public CasterParserComputer getEvaluatorComputer() {
            return new StringToCalendarWExprFormatComputerEval(dateFormatForge.getExprEvaluator(), timeZone);
        }

        public CodegenExpression codegenPremade(Class evaluationType, CodegenExpression input, Class inputType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
            return StringToCalendarWExprFormatComputerEval.codegen(input, dateFormatForge, codegenMethodScope, exprSymbol, codegenClassScope, timeZone);
        }
    }

    public static class StringToCalendarWExprFormatComputerEval extends StringToDateLongWExprormatEval {

        private final TimeZone timeZone;

        public StringToCalendarWExprFormatComputerEval(ExprEvaluator dateFormatEval, TimeZone timeZone) {
            super(dateFormatEval);
            this.timeZone = timeZone;
        }

        public Calendar compute(Object input, EventBean[] eventsPerStream, boolean newData, ExprEvaluatorContext exprEvaluatorContext) {
            Object format = dateFormatEval.evaluate(eventsPerStream, newData, exprEvaluatorContext);
            SimpleDateFormat dateFormat = stringToSimpleDateFormatSafe(format);
            return StringToCalendarWStaticFormatComputer.stringToCalendarWStaticFormatParse(dateFormat, input, timeZone);
        }

        public static CodegenExpression codegen(CodegenExpression input, ExprForge dateFormatForge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope, TimeZone timeZone) {
            CodegenExpression timeZoneField = codegenClassScope.addOrGetFieldSharable(RuntimeSettingsTimeZoneField.INSTANCE);
            CodegenMethod method = codegenMethodScope.makeChild(Calendar.class, StringToCalendarWExprFormatComputerEval.class, codegenClassScope).addParam(Object.class, "input");
            CodegenExpression format;
            if (dateFormatForge.getForgeConstantType().isConstant()) {
                format = formatFieldExpr(DateFormat.class, dateFormatForge, codegenClassScope);
            } else {
                method.getBlock()
                        .declareVar(Object.class, "format", dateFormatForge.evaluateCodegen(Object.class, method, exprSymbol, codegenClassScope))
                        .declareVar(SimpleDateFormat.class, "dateFormat", staticMethod(ExprCastNode.class, "stringToSimpleDateFormatSafe", ref("format")));
                format = ref("dateFormat");
            }
            method.getBlock().methodReturn(staticMethod(StringToCalendarWStaticFormatComputer.class, "stringToCalendarWStaticFormatParse", format, ref("input"), timeZoneField));
            return localMethod(method, input);
        }
    }

    private ExprCastNodeDateDesc validateDateFormat(ExprNamedParameterNode dateFormatParameter, ExprValidationContext validationContext, boolean java8Formatter) throws ExprValidationException {
        boolean iso8601Format = false;
        ExprNode formatExpr = dateFormatParameter.getChildNodes()[0];
        ExprForge formatForge = formatExpr.getForge();
        Class formatReturnType = formatExpr.getForge().getEvaluationType();
        String staticFormatString = null;

        if (formatReturnType == String.class) {
            if (formatExpr.getForge().getForgeConstantType().isCompileTimeConstant()) {
                staticFormatString = (String) formatForge.getExprEvaluator().evaluate(null, true, null);
                if (staticFormatString.toLowerCase(Locale.ENGLISH).trim().equals("iso")) {
                    iso8601Format = true;
                } else {
                    if (!java8Formatter) {
                        try {
                            new SimpleDateFormat(staticFormatString);
                        } catch (RuntimeException ex) {
                            throw new ExprValidationException("Invalid date format '" + staticFormatString + "' (as obtained from new SimpleDateFormat): " + ex.getMessage(), ex);
                        }
                    } else {
                        try {
                            DateTimeFormatter.ofPattern(staticFormatString);
                        } catch (RuntimeException ex) {
                            throw new ExprValidationException("Invalid date format '" + staticFormatString + "' (as obtained from DateTimeFormatter.ofPattern): " + ex.getMessage(), ex);
                        }
                    }
                }
            }
        } else {
            if (!java8Formatter) {
                if (!JavaClassHelper.isSubclassOrImplementsInterface(formatReturnType, DateFormat.class)) {
                    throw getFailedExpected(DateFormat.class, formatReturnType);
                }
            } else {
                if (!JavaClassHelper.isSubclassOrImplementsInterface(formatReturnType, DateTimeFormatter.class)) {
                    throw getFailedExpected(DateTimeFormatter.class, formatReturnType);
                }
            }
        }

        return new ExprCastNodeDateDesc(iso8601Format, formatForge, staticFormatString, formatForge.getForgeConstantType().isConstant());
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param format format
     * @return date format
     */
    public static SimpleDateFormat stringToSimpleDateFormatSafe(Object format) {
        if (format == null) {
            throw new EPException("Null date format returned by 'dateformat' expression");
        }
        try {
            return new SimpleDateFormat(format.toString());
        } catch (RuntimeException ex) {
            throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
        }
    }

    public static DateTimeFormatter stringToDateTimeFormatterSafe(Object format) {
        if (format == null) {
            throw new EPException("Null date format returned by 'dateformat' expression");
        }
        try {
            return DateTimeFormatter.ofPattern(format.toString());
        } catch (RuntimeException ex) {
            throw new EPException("Invalid date format '" + format.toString() + "': " + ex.getMessage(), ex);
        }
    }

    private static CodegenExpression formatFieldJava7(String dateFormatString, CodegenClassScope codegenClassScope) {
        return codegenClassScope.addFieldUnshared(true, DateFormat.class, newInstance(SimpleDateFormat.class, constant(dateFormatString)));
    }

    private static CodegenExpression formatFieldJava8(String dateFormatString, CodegenClassScope codegenClassScope) {
        return codegenClassScope.addFieldUnshared(true, String.class, staticMethod(DateTimeFormatter.class, "ofPattern", constant(dateFormatString)));
    }

    private static CodegenExpression formatFieldExpr(Class type, ExprForge formatExpr, CodegenClassScope codegenClassScope) {
        CodegenMethod formatEval = CodegenLegoMethodExpression.codegenExpression(formatExpr, codegenClassScope.getPackageScope().getInitMethod(), codegenClassScope);
        CodegenExpression formatInit = localMethod(formatEval, constantNull(), constantTrue(), constantNull());
        return codegenClassScope.addFieldUnshared(true, type, formatInit);
    }

    private ExprValidationException getFailedExpected(Class expected, Class received) {
        return new ExprValidationException("Invalid format, expected string-format or " + expected.getSimpleName() + " but received " + JavaClassHelper.getClassNameFullyQualPretty(received));
    }

    private Class applyDimensions(Class targetType) {
        if (targetType == null) {
            return null;
        }
        if (classIdentifierWArray.getArrayDimensions() == 0) {
            return targetType;
        }
        return JavaClassHelper.getArrayType(targetType, classIdentifierWArray.getArrayDimensions());
    }
}
