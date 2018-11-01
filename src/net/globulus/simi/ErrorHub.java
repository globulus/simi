package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiException;
import net.globulus.simi.api.SimiProperty;

import java.util.ArrayList;
import java.util.List;

class ErrorHub {

    private List<ErrorWatcher> watchers = new ArrayList<>();
    private BlockInterpreter interpreter;

    private static final ErrorHub instance = new ErrorHub();

    private ErrorHub() { }

    static ErrorHub sharedInstance() {
        return instance;
    }

    void setInterpreter(BlockInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    void error(String exceptionClass, String file, int line, String message) {
        report(exceptionClass, file, line, "", message);
    }

    void error(String exceptionClass, Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(exceptionClass, token.file, token.line, " at end", message);
        } else {
            report(exceptionClass, token.file, token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private void report(String exceptionClass, String file, int line, String where, String message) {
//        for (ErrorWatcher watcher : watchers) {
//            watcher.report(file, line, where, message);
//        }
        if (interpreter != null) {
            String exceptionMessage = "[\"" + file + "\" line " + line + "] Error" + where + ": " + message;
            SimiProperty exceptionClassProp = interpreter.getEnvironment().tryGet(exceptionClass);
            if (exceptionClassProp != null) {
                interpreter.raiseException(new SimiException((SimiClass) exceptionClassProp.getValue().getObject(), exceptionMessage));
                return;
            }
        }
        for (ErrorWatcher watcher : watchers) {
            watcher.report(file, line, where, message);
        }
    }

     void runtimeError(RuntimeError error) {
        for (ErrorWatcher watcher : watchers) {
             watcher.runtimeError(error);
         }
    }

    void addWatcher(ErrorWatcher watcher) {
        watchers.add(watcher);
    }

    void removeWatcher(ErrorWatcher watcher) {
        watchers.remove(watcher);
    }
}
