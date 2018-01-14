package net.globulus.simi.api.processor.codegen;

import net.globulus.simi.api.*;
import net.globulus.simi.api.processor.ExposedClass;
import net.globulus.simi.api.processor.ExposedMethod;
import net.globulus.simi.api.processor.javawriter.SimiApiJavaWriter;

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
			jw.emitImports(SimiValue.class.getCanonicalName());
			jw.emitImports(SimiObject.class.getCanonicalName());
			jw.emitImports(SimiEnvironment.class.getCanonicalName());
			jw.emitEmptyLine();

			jw.emitJavadoc("Generated class by @%s . Do not modify this code!", className);
			jw.beginType(className, "class", EnumSet.of(Modifier.PUBLIC), null, SimiApiClass.class.getName());
			jw.emitEmptyLine();

			jw.beginConstructor(EnumSet.of(Modifier.PUBLIC));
			jw.endConstructor();
			jw.emitEmptyLine();

			String simiValue = SimiValue.class.getSimpleName();
			jw.beginMethod(simiValue, "call", EnumSet.of(Modifier.PUBLIC),
					"String", "className", "String", "methodName", "SimiObject", "self",
					"SimiEnvironment", "environment",  "java.util.List<SimiValue>", "args");

			jw.beginControlFlow("switch (className)");
			for (ExposedClass exposedClass : classes) {
				jw.emitRaw("case \"%s\":", exposedClass.name);
				jw.beginControlFlow("switch (methodName)");
				for (ExposedMethod exposedMethod : exposedClass.methods) {
					StringBuilder params = new StringBuilder("self, environment");
					int length = exposedMethod.params.size() / 2 - 2;
					for (int i = 0; i < length; i++) {
						params.append(", ").append("args.get(").append(i).append(')');
					}
					jw.emitRaw("case \"%s\": return %s.%s(%s);",
							exposedMethod.name, exposedClass.name, exposedMethod.name, params.toString());
				}
				jw.emitStatement("default: return null");
				jw.endControlFlow();
			}
			jw.emitStatement("default: return null");
			jw.endControlFlow();
			jw.endMethod();
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
