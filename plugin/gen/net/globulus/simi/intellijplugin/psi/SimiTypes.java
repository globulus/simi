// This is a generated file. Not intended for manual editing.
package net.globulus.simi.intellijplugin.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import net.globulus.simi.intellijplugin.psi.impl.*;

public interface SimiTypes {

  IElementType PROPERTY = new SimiElementType("PROPERTY");

  IElementType COMMENT = new SimiTokenType("COMMENT");
  IElementType CRLF = new SimiTokenType("CRLF");
  IElementType KEY = new SimiTokenType("KEY");
  IElementType SEPARATOR = new SimiTokenType("SEPARATOR");
  IElementType VALUE = new SimiTokenType("VALUE");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == PROPERTY) {
        return new SimiPropertyImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
