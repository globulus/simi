package net.globulus.simi;

final class Constants {

    public static final String PRIVATE = "_";
    public static final String IMPLICIT = "#";

    public static final String INIT = "init";
    public static final String SELF = "self";
    public static final String SELF_DEF = "self(def)";
    public static final String SUPER = "super";
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String SET = "set";

    public static final String ITERATE = "iterate";
    public static final String NEXT = "next";
    public static final String HAS = "has";
    public static final String EQUALS = "equals";
    public static final String COMPARE_TO = "compareTo";
    public static final String TO_STRING = "toString";
    public static final String RAISE = "raise";

    public static final String CLASS_OBJECT = "Object";
    public static final String CLASS_FUNCTION = "Function";
    public static final String CLASS_STRING = "String";
    public static final String CLASS_NUMBER = "Number";
    public static final String CLASS_CLASS = "Class";
    public static final String CLASS_EXCEPTION = "Exception";
    public static final String CLASS_GLOBALS = net.globulus.simi.api.Constants.GLOBALS_CLASS_NAME;
    static final String CLASS_DEBUGGER = "Debugger";

    public static final String EXCEPTION_SCANNER = "ScannerException";
    public static final String EXCEPTION_PARSER = "ParserException";
    public static final String EXCEPTION_INTERPRETER = "InterpreterException";
    public static final String EXCEPTION_NUMBER_FORMAT = "NumberFormatException";
    public static final String EXCEPTION_NIL_REFERENCE = "NilReferenceException";
    public static final String EXCEPTION_TYPE_MISMATCH = "TypeMismatchException";

    private Constants() { }
}
