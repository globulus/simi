package net.globulus.simi.api.processor.codegen;

import net.globulus.simi.api.processor.javawriter.SimiApiJavaWriter;

import java.io.IOException;

public interface CodeGen<T> {

  void generateCode(T type, SimiApiJavaWriter jw) throws IOException;
}
