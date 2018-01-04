// This is a generated file. Not intended for manual editing.
package net.globulus.simi.intellijplugin.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static net.globulus.simi.intellijplugin.psi.SimiTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import net.globulus.simi.intellijplugin.psi.*;

public class SimiPropertyImpl extends ASTWrapperPsiElement implements SimiProperty {

  public SimiPropertyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull SimiVisitor visitor) {
    visitor.visitProperty(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof SimiVisitor) accept((SimiVisitor)visitor);
    else super.accept(visitor);
  }

}
