package net.globulus.simi;

import net.globulus.simi.api.Codifiable;

import java.util.*;
import java.util.Scanner;

final class Debugger {

    static final String BREAKPOINT_LEXEME = "BP";

    private FrameStack lineStack;
    private FrameStack callStack;
    private Scanner scanner;
    private Evaluator evaluator;
    private Environment inspectingEnvironment;
    private Set<Stmt> ignoredBreakpoints;

    private Stmt currentBreakpoint;
    private FrameStack currentStack;

    Debugger() {
        lineStack = new FrameStack();
        callStack = new FrameStack();
        scanner = new Scanner(System.in);
        ignoredBreakpoints = new HashSet<>();
    }

    void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    void pushLine(Frame frame) {
        lineStack.push(frame);
    }

    void pushCall(Frame frame) {
        callStack.push(frame);
    }

    void popCall() {
        callStack.pop();
    }

    void triggerBreakpoint(Stmt stmt) {
        if (ignoredBreakpoints.contains(stmt)) {
            return;
        }
        currentBreakpoint = stmt;
        currentStack = callStack;
        System.out.println("***** BREAKPOINT *****\n");
        print(0, true);
        scanInput();
    }

    private void print(int frameIndex, boolean printLine) {
        List<Frame> frames = currentStack.toList();
        System.out.println("============================");
        Frame focusFrame;
        if (printLine && currentStack != lineStack) {
            focusFrame = lineStack.toList().get(frameIndex);
        } else {
            focusFrame = frames.get(frameIndex);
        }
        inspectingEnvironment = focusFrame.environment;
        if (focusFrame.before != null) {
            for (Codifiable codifiable : focusFrame.before) {
                System.out.println(codifiable.toCode(0, true));
            }
        }
        focusFrame.print(null);
        if (focusFrame.after != null) {
            for (Codifiable codifiable : focusFrame.after) {
                System.out.println(codifiable.toCode(0, true));
            }
        }
        System.out.println("============================");
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            frame.print(i);
        }
        System.out.println("\n#### ENVIRONMENT ####\n");
        System.out.println(focusFrame.environment.toStringWithoutValuesOrGlobal());
        printHelp();
    }

    private void printHelp() {
        System.out.println("\nCommands:\n" +
                "c: Print call stack\n" +
                "l: Print line stack\n" +
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
            case 'c': { // Print call stack
                currentStack = callStack;
                print(0, true);
            } break;
            case 'l': { // Print line stack
                currentStack = lineStack;
                print(0, true);
            } break;
            case 'i': { // Inspect environment at stack index
                int index = Integer.parseInt(input.substring(2));
                print(index, false);
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

        void print(Integer index) {
            if (index != null) {
                System.out.print("[" + index + "] ");
            }
            System.out.print("\"" + line.getFileName() + "\" line " + line.getLineNumber() + ": ");
            System.out.println(line.toCode(0, true));
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

        Frame pop() {
            Frame frame = stack[index];
            stack[index] = null;
            index--;
            if (index < 0) {
                if (stack[MAX_SIZE - 1] != null) {
                    index = MAX_SIZE - 1;
                } else {
                    index = 0;
                }
            }
            return frame;
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
