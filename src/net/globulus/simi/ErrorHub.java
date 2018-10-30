package net.globulus.simi;

import java.util.ArrayList;
import java.util.List;

class ErrorHub {

    private List<ErrorWatcher> watchers = new ArrayList<>();

    private static final ErrorHub instance = new ErrorHub();

    private ErrorHub() { }

    static ErrorHub sharedInstance() {
        return instance;
    }

    void error(String file, int line, String message) {
        report(file, line, "", message);
    }

    void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.file, token.line, " at end", message);
        } else {
            report(token.file, token.line, " at '" + token.lexeme + "'", message);
        }
    }

    void report(String file, int line, String where, String message) {
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
