package net.globulus.simi.api.processor.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * Created by gordanglavas on 30/09/16.
 */
public class ProcessorLog {

	private static ProcessingEnvironment sEnv;

	private ProcessorLog() { }

	public static void init(ProcessingEnvironment processingEnv) {
		sEnv = processingEnv;
	}

	public static void error(Element element, String message, Object... args) {
		if (args.length > 0) {
			message = String.format(message, args);
		}
		sEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	public static void warn(Element element, String message, Object... args) {
		if (args.length > 0) {
			message = String.format(message, args);
		}
		sEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
	}

	public static void note(Element element, String message, Object... args) {
		if (args.length > 0) {
			message = String.format(message, args);
		}
		sEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, element);
	}
}
