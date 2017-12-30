package net.globulus.simi.api.processor.codegen;

import net.globulus.simi.api.processor.ExposedClass;
import net.globulus.simi.api.processor.ExposedMethod;
import net.globulus.simi.api.processor.javawriter.SimiApiJavaWriter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.EnumSet;

public class ExposedClassCodeGen implements CodeGen<ExposedClass> {

    public void generateCode(ExposedClass type, SimiApiJavaWriter jw) throws IOException {
		jw.emitEmptyLine();
		jw.beginType(type.name, "class", EnumSet.of(Modifier.PUBLIC, Modifier.STATIC), null);
		jw.emitEmptyLine();

		ExposedMethodCodeGen methodCodeGen = new ExposedMethodCodeGen();
		for (ExposedMethod exposedMethod : type.methods) {
			methodCodeGen.generateCode(exposedMethod, jw);
		}

		jw.endType();
		jw.emitEmptyLine();
    }
}
