package net.globulus.simi.api.processor.util;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;

public class TypeUtils {

  public static boolean isTypeOf(Element element, Class<?> clazz) {
    return isTypeOf(element.asType(), clazz);
  }

  public static boolean isTypeOf(TypeMirror type, Class<?> clazz) {
    return type.toString().equals(clazz.getCanonicalName());
  }

  public static String getPackageName(Elements elementUtils, TypeElement type) throws IOException {
    PackageElement pkg = elementUtils.getPackageOf(type);
    if (!pkg.isUnnamed()) {
      return pkg.getQualifiedName().toString();
    } else {
      return ""; // Default package
    }
  }

  public static String getBinaryName(Elements elementUtils, TypeElement type) throws IOException {
    String packageName = getPackageName(elementUtils, type);
    String qualifiedName = type.getQualifiedName().toString();
    if (packageName.length() > 0) {
      return packageName + '.' + qualifiedName.substring(packageName.length() + 1).replace('.', '$');
    } else {
      return qualifiedName.replace('.', '$');
    }
  }
}
