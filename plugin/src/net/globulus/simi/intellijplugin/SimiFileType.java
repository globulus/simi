package net.globulus.simi.intellijplugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SimiFileType extends LanguageFileType {

    public static final SimiFileType INSTANCE = new SimiFileType();

    private SimiFileType() {
        super(SimiLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Simi file";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Simi language file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "simi";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return SimiIcons.FILE;
    }
}
