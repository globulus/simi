package net.globulus.simi.api;

import java.util.Collections;

public interface Codifiable {
    String toCode(int indentationLevel, boolean ignoreFirst);
    int getLineNumber();
    boolean hasBreakPoint();

    static String getIndentation(int level) {
        if (level == 0) {
            return "";
        }
        final String indentation = "    ";
        return String.join("", Collections.nCopies(level, indentation));
    }
}
