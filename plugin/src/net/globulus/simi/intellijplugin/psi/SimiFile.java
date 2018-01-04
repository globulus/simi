package net.globulus.simi.intellijplugin.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import net.globulus.simi.intellijplugin.SimiFileType;
import net.globulus.simi.intellijplugin.SimiLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SimiFile extends PsiFileBase {
    public SimiFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, SimiLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return SimiFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Simi File";
    }

    @Override
    public Icon getIcon(int flags) {
        return super.getIcon(flags);
    }
}
