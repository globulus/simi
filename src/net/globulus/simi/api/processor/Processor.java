package net.globulus.simi.api.processor;

import net.globulus.simi.api.*;
import net.globulus.simi.api.processor.codegen.SimiJavaCodeGen;
import net.globulus.simi.api.processor.util.ProcessorLog;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.*;

public class Processor extends AbstractProcessor {

	private static final List<Class<? extends Annotation>> ANNOTATIONS = Arrays.asList(
			SimiJavaGlobal.class,
			SimiJavaClass.class,
			SimiJavaMethod.class
	);

	private Elements mElementUtils;
	private Types mTypeUtils;
	private Filer mFiler;

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);

		ProcessorLog.init(env);
		mElementUtils = env.getElementUtils();
		mTypeUtils = env.getTypeUtils();
		mFiler = env.getFiler();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> types = new LinkedHashSet<>();
		for (Class<? extends Annotation> annotation : ANNOTATIONS) {
			types.add(annotation.getCanonicalName());
		}
		return types;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		List<ExposedClass> exposedClasses = new ArrayList<>();

		List<ExposedMethod> globals = new ArrayList<>();
		for (Element element : roundEnv.getElementsAnnotatedWith(SimiJavaGlobal.class)) {
			if (!isValidMethod(element, true)) {
				continue;
			}
			globals.add(new ExposedMethod(element));
		}
		if (!globals.isEmpty()) {
			exposedClasses.add(new ExposedClass(Constants.GLOBALS_CLASS_NAME, globals));
		}

		for (Element element : roundEnv.getElementsAnnotatedWith(SimiJavaClass.class)) {
			if (!isValidClass(element)) {
				continue;
			}
			TypeElement typeElement = (TypeElement) element;

			List<ExposedMethod> methods = new ArrayList<>();

			SimiJavaClass annotation = element.getAnnotation(SimiJavaClass.class);
			String name = annotation.name().isEmpty() ? typeElement.getSimpleName().toString() : annotation.name();

			List<? extends Element> memberFields = mElementUtils.getAllMembers(typeElement);
			if (memberFields != null) {

				for (Element member : memberFields) {
					if (member.getKind() != ElementKind.METHOD) {
						continue;
					}
					SimiJavaMethod methodAnnotation = member.getAnnotation(SimiJavaMethod.class);
					if (methodAnnotation != null && isValidMethod(member, false)) {
						methods.add(new ExposedMethod(member));
					}
				}
				exposedClasses.add(new ExposedClass(name, methods));
			}
		}

		new SimiJavaCodeGen().generate(mFiler, exposedClasses);

		return true;
	}

	private boolean isValidClass(Element element) {
		if (element.getKind() != ElementKind.CLASS) {
			ProcessorLog.error(element,
			"Element %s is annotated with @%s but is not a class. Only Classes are supported",
					element.getSimpleName(), SimiJavaClass.class.getSimpleName());
			return false;
		}
		return true;
	}

	private boolean isValidMethod(Element element, boolean global) {
		String masterError = global
				? "A Simi API method must of of format: public static SimiValue NAME(SimiObject sender, BlockInterpreter interpreter...)!"
				: "A Simi API global function must of of format: public static SimiValue NAME(...)!";
		if (!element.getModifiers().contains(Modifier.PUBLIC)
				|| !element.getModifiers().contains(Modifier.STATIC)) {
			ProcessorLog.error(element, masterError);
			return false;
		}
		ExecutableType method = (ExecutableType) element.asType();
		TypeMirror simiValue = mElementUtils.getTypeElement(SimiValue.class.getCanonicalName()).asType();
		if (!mTypeUtils.isSameType(method.getReturnType(), simiValue)) {
			ProcessorLog.error(element, masterError);
			return false;
		}
		if (!global) {
			List<? extends TypeMirror> params = method.getParameterTypes();
			if (params.size() < 2) {
				ProcessorLog.error(element, masterError);
				return false;
			}
			TypeMirror param0 = method.getParameterTypes().get(0);
			TypeMirror simiObject = mElementUtils.getTypeElement(SimiObject.class.getCanonicalName()).asType();
			if (!mTypeUtils.isSameType(param0, simiObject)) {
				ProcessorLog.error(element, masterError);
				return false;
			}
			TypeMirror param1 = method.getParameterTypes().get(1);
			TypeMirror blockInt = mElementUtils.getTypeElement(BlockInterpreter.class.getCanonicalName()).asType();
			if (!mTypeUtils.isSameType(param1, blockInt)) {
				ProcessorLog.error(element, masterError);
				return false;
			}
		}
		return true;
	}
}
