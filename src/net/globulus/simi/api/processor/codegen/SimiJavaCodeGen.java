package net.globulus.simi.api.processor.codegen;

import net.globulus.simi.api.processor.ExposedClass;
import net.globulus.simi.api.processor.javawriter.SimiApiJavaWriter;
import net.globulus.simi.api.Constants;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by gordanglavas on 01/10/16.
 */
public class SimiJavaCodeGen {

	public void generate(Filer filer,
						 List<ExposedClass> classes) {
		try {
			String packageName = Constants.PACKAGE_SIMI_API;
			String className = Constants.API_CLASS_NAME;

			JavaFileObject jfo = filer.createSourceFile(packageName + "." + className);
			Writer writer = jfo.openWriter();
			SimiApiJavaWriter jw = new SimiApiJavaWriter(writer);
			jw.emitPackage(packageName);
			jw.emitImports(Constants.IMPORT_SIMI_OBJECT);
			jw.emitImports(Constants.IMPORT_SIMI_VALUE);
			jw.emitEmptyLine();

			jw.emitJavadoc("Generated class by @%s . Do not modify this code!", className);
			jw.beginType(className, "class", EnumSet.of(Modifier.PUBLIC), null);
			jw.emitEmptyLine();

			jw.beginConstructor(EnumSet.of(Modifier.PRIVATE));
			jw.endConstructor();
			jw.emitEmptyLine();

			ExposedClassCodeGen classCodeGen = new ExposedClassCodeGen();
			for (ExposedClass exposedClass : classes) {
				classCodeGen.generateCode(exposedClass, jw);
			}

			jw.endType();
			jw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
