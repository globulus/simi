package net.globulus.simi.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import net.globulus.simi.intellijplugin.SimiLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SimiTokenType extends IElementType {
    public SimiTokenType(@NotNull @NonNls String debugName) {
        super(debugName, SimiLanguage.INSTANCE);
    }

    @Override
    public String toString() {
        return "SimiTokenType." + super.toString();
    }
}
