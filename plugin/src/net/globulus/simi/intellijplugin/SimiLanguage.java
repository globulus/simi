package net.globulus.simi.intellijplugin;

import com.intellij.lang.Language;

public class SimiLanguage extends Language {

    public static final SimiLanguage INSTANCE = new SimiLanguage();

    private SimiLanguage() {
        super("Simi");
    }
}
