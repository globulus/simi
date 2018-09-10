package net.globulus.simi;

import net.globulus.simi.api.Codifiable;

import java.util.*;
import java.util.Scanner;

final class Debugger {

    static final String BREAKPOINT_LEXEME = "BP";

    private FrameStack stack;
    private Scanner scanner;
    private Evaluator evaluator;
    private Environment inspectingEnvironment;
    private Set<Stmt> ignoredBreakpoints;
    private Stmt currentBreakpoint;

    Debugger() {
        stack = new FrameStack();
        scanner = new Scanner(System.in);
        ignoredBreakpoints = new HashSet<>();
    }

    void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    void push(Frame frame) {
        stack.push(frame);
    }

    void triggerBreakpoint(Stmt stmt) {
        if (ignoredBreakpoints.contains(stmt)) {
            return;
        }
        currentBreakpoint = stmt;
        System.out.println("***** BREAKPOINT *****\n");
        print(0);
        scanInput();
    }

    private void print(int frameIndex) {
        List<Frame> frames = stack.toList();
        System.out.println("============================");
        Frame focusFrame = frames.get(frameIndex);
        inspectingEnvironment = focusFrame.environment;
        if (focusFrame.before != null) {
            for (Codifiable codifiable : focusFrame.before) {
                System.out.println(codifiable.toCode(0, true));
            }
        }
        System.out.println(focusFrame.line.toCode(0, true));
        if (focusFrame.after != null) {
            for (Codifiable codifiable : focusFrame.after) {
                System.out.println(codifiable.toCode(0, true));
            }
        }
        System.out.println("============================");
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            System.out.print("[" + i + "] Line " + frame.line.getLineNumber() + ": ");
            System.out.println(frame.line.toCode(0, true));
        }
        System.out.println("\n#### ENVIRONMENT ####\n");
        System.out.println(focusFrame.environment.toStringWithoutGlobal());
        printHelp();
    }

    private void printHelp() {
        System.out.println("\nCommands:\n" +
                "i [index]: Inspect environment at stack index\n" +
                "e [expr]: Evaluates expression within current environment\n" +
                "r: Remove current breakpoint for this debugger session\n" +
                "g: Prints global environment\n" +
                "anything else: continue with program execution\n");
    }

    private void scanInput() {
        String input = scanner.nextLine();
        if (input.isEmpty()) {
            return;
        }
        switch (input.charAt(0)) {
            case 'i': { // Inspect environment at stack index
                int index = Integer.parseInt(input.substring(2));
                print(index);
            } break;
            case 'e': { // Evaluate expression
                if (evaluator == null) {
                    throw new IllegalStateException("Evaluator not set!");
                }
                System.out.println(evaluator.eval(input.substring(2), inspectingEnvironment));
                printHelp();
            } break;
            case 'r': {
                if (currentBreakpoint != null) {
                    ignoredBreakpoints.add(currentBreakpoint);
                    currentBreakpoint = null;
                }
                System.out.println("Breakpoint removed.");
                printHelp();
            } break;
            case 'g': { // Print global environment
                if (evaluator == null) {
                    throw new IllegalStateException("Evaluator not set!");
                }
                System.out.println(evaluator.getGlobalEnvironment().toString());
                printHelp();
            } break;
            default:
                return;
        }
        scanInput();
    }

    static class Frame {

        final Environment environment;
        final Codifiable line;
        final Codifiable[] before;
        final Codifiable[] after;

        Frame(Environment environment,
              Codifiable line,
              Codifiable[] before,
              Codifiable[] after) {
            this.environment = environment;
            this.line = line;
            this.before = before;
            this.after = after;
        }
    }

    private static class FrameStack {

        private static final int MAX_SIZE = 20;

        private Frame[] stack;
        private int index;

        FrameStack() {
            stack = new Frame[MAX_SIZE];
            index = 0;
        }

        void push(Frame frame) {
            stack[index] = frame;
            index++;
            if (index == MAX_SIZE) {
                index = 0;
            }
        }

        List<Frame> toList() {
            List<Frame> list = new ArrayList<>(MAX_SIZE);
            for (int i = index; i < MAX_SIZE; i++) {
                if (stack[i] != null) {
                    list.add(stack[i]);
                }
            }
            for (int i = 0; i < index; i++) {
                if (stack[i] != null) {
                    list.add(stack[i]);
                }
            }
            Collections.reverse(list);
            return list;
        }
    }

    interface Evaluator {
        String eval(String input, Environment environment);
        Environment getGlobalEnvironment();
    }
}
