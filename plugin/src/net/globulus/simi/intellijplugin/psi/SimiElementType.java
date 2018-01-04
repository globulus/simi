package net.globulus.simi.intellijplugin.psi;


import com.intellij.psi.tree.IElementType;
import net.globulus.simi.intellijplugin.SimiLanguage;
import org.jetbrains.annotations.*;

public class SimiElementType extends IElementType {
    public SimiElementType(@NotNull @NonNls String debugName) {
        super(debugName, SimiLanguage.INSTANCE);
    }
}