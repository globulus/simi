package net.globulus.simi;

import net.globulus.simi.api.Codifiable;

import java.util.*;
import java.util.Scanner;

final class Debugger {

    static final String BREAKPOINT_LEXEME = "BP";

    private FrameStack stack;
    private Scanner scanner;
    private Evaluator evaluator;

    Debugger() {
        stack = new FrameStack();
        scanner = new Scanner(System.in);
    }

    void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    void push(Frame frame) {
        stack.push(frame);
    }

    void print(int frameIndex) {
        List<Frame> frames = stack.toList();
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            System.out.print("[" + i + "] Line " + frame.line.getLineNumber() + ": ");
            System.out.println(frame.line.toCode(0, true));
        }
        System.out.println(frames.get(frameIndex).environment.toStringWithoutGlobal());
        printHelp();
    }

    void triggerBreakpoint() {
        print(0);
        scanInput();
    }

    private void printHelp() {
        System.out.println("\nCommands:\n" +
                "i [index]: Inspect environment at stack index\n" +
                "e [expr]: Evaluates expression within current environment\n" +
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
                System.out.println(evaluator.eval(input.substring(2)));
            } break;
            case 'g': { // Print global environment
                if (evaluator == null) {
                    throw new IllegalStateException("Evaluator not set!");
                }
                System.out.println(evaluator.getGlobalEnvironment().toString());
            } break;
            default: return;
        }
        scanInput();
    }

    static class Frame {

        final Environment environment;
        final Codifiable line;

        Frame(Environment environment, Codifiable line) {
            this.environment = environment;
            this.line = line;
        }
    }

    private static class FrameStack {

        private static final int MAX_SIZE = 10;

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
        String eval(String input);
        Environment getGlobalEnvironment();
    }
}
