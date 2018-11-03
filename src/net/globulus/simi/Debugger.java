package net.globulus.simi;

import net.globulus.simi.api.Codifiable;
import net.globulus.simi.api.SimiException;
import net.globulus.simi.api.SimiProperty;

import java.util.*;
import java.util.Scanner;

public final class Debugger {

    static final String BREAKPOINT_LEXEME = "BP";

    private static final String HELP = "\nCommands:\n" +
            "c: Print call stack\n" +
            "l: Print line stack\n" +
            "i [index]: Inspect environment at stack index\n" +
            "e [expr]: Evaluates expression within current environment\n" +
            "w [name]: Adds variable in the current environment to Watch\n" +
            "n: Step into - trigger breakpoint at next line\n" +
            "v: Step over - trigger breakpoint at next line skipping calls\n" +
            "a: Add current line as breakpoint for this debugger session\n" +
            "r: Remove current breakpoint for this debugger session\n" +
            "x: Toggles catching all exceptions on/off (default off)\n" +
            "o: Toggles debugging on/off (default on)\n" +
            "h: Print help\n" +
            "g: Prints global environment\n" +
            "anything else: continue with program execution\n";

    private enum DebuggerState {
        OUTPUT, INPUT
    }
    private DebuggerState state;

    private FrameStack lineStack;
    private FrameStack callStack;
    private DebuggerInterface debuggerInterface;
    private Evaluator evaluator;
    private Frame focusFrame;
    private Set<Stmt> ignoredBreakpoints;
    private Set<Stmt> addedBreakpoints;
    private Map<String, Environment> watch;
    private boolean allExceptions;
    private boolean debuggingOff;
    private boolean firstHelp;

    private Stmt currentBreakpoint;
    private FrameStack currentStack;

    private enum StepState {
        NONE, STEP_INTO, STEP_OVER, STEP_OVER_SUSPENDED
    }
    private StepState stepState;
    private int stepOverDepth;

    Debugger(DebuggerInterface debuggerInterface) {
        state = DebuggerState.OUTPUT;
        lineStack = new FrameStack();
        callStack = new FrameStack();
        this.debuggerInterface = debuggerInterface;
        ignoredBreakpoints = new HashSet<>();
        addedBreakpoints = new HashSet<>();
        watch = new HashMap<>();
        allExceptions = false;
        debuggingOff = false;
        firstHelp = true;
        stepState = StepState.NONE;
    }

    void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    void pushLine(Frame frame) {
        lineStack.push(frame);
    }

    void pushCall(Frame frame) {
        callStack.push(frame);
        if (stepState == StepState.STEP_OVER) {
            stepState = StepState.STEP_OVER_SUSPENDED;
            stepOverDepth++;
        } else if (stepState == StepState.STEP_OVER_SUSPENDED) {
            stepOverDepth++;
        }
    }

    void popCall() {
        callStack.pop();
        if (stepState == StepState.STEP_OVER_SUSPENDED) {
            stepOverDepth--;
            if (stepOverDepth == 0) {
                stepState = StepState.STEP_OVER;
            }
        }
    }

    void triggerBreakpoint(Stmt stmt) {
        if (debuggingOff) {
            return;
        }
        if (stmt.hasBreakPoint()) {
            if (ignoredBreakpoints.contains(stmt)) {
                return;
            }
        } else if (!addedBreakpoints.contains(stmt)) {
            switch (stepState) {
                case NONE:
                case STEP_OVER_SUSPENDED:
                    return;
                case STEP_INTO:
                case STEP_OVER:
                    stepState = StepState.NONE;
                    break;
            }
        }
        currentBreakpoint = stmt;
        currentStack = callStack;
        debuggerInterface.print("***** BREAKPOINT *****\n");
        print(0, true);
        scanInput();
    }

    void triggerException(Stmt stmt, SimiException e, boolean fatal) {
        if (debuggingOff || !(allExceptions || fatal)) {
            return;
        }
        currentBreakpoint = stmt;
        currentStack = callStack;
        debuggerInterface.println("***** " + (fatal ? "FATAL " : "") + "EXCEPTION *****\n");
        debuggerInterface.println(e.getMessage() + "\n");
        print(0, true);
        scanInput();
    }

    private void print(int frameIndex, boolean printLine) {
        List<Frame> frames = currentStack.toList();
        debuggerInterface.println("============================");
        if (printLine && currentStack != lineStack) {
            focusFrame = lineStack.toList().get(frameIndex);
        } else {
            focusFrame = frames.get(frameIndex);
        }
        if (focusFrame.before != null) {
            for (Codifiable codifiable : focusFrame.before) {
                debuggerInterface.println(codifiable.toCode(0, true));
            }
        }
        focusFrame.print(null, debuggerInterface);
        if (focusFrame.after != null) {
            for (Codifiable codifiable : focusFrame.after) {
                debuggerInterface.println(codifiable.toCode(0, true));
            }
        }
        debuggerInterface.println("============================");
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            frame.print(i, debuggerInterface);
        }
        debuggerInterface.println("\n#### ENVIRONMENT ####\n");
        debuggerInterface.println(focusFrame.environment.toStringWithoutValuesOrGlobal());
        if (!watch.isEmpty()) {
            debuggerInterface.println("\n#### WATCH ####\n");
            for (Map.Entry<String, Environment> watched : watch.entrySet()) {
                String name = watched.getKey();
                SimiProperty value = watched.getValue().tryGet(name);
                debuggerInterface.println(name + " = " + ((value != null) ? value.toString() : "nil"));
            }
        }
        if (firstHelp) {
            firstHelp = false;
            printHelp();
        }
    }

    private void printHelp() {
        debuggerInterface.println(HELP);
    }

    private void scanInput() {
        state = DebuggerState.INPUT;
        String syncInput = debuggerInterface.read(); // Try sync input
        if (syncInput != null) {
            parseInput(syncInput);
        } else {
            synchronized (debuggerInterface.getLock()) {
                try {
                    debuggerInterface.getLock().wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    String asyncInput = debuggerInterface.read(); // Async input
                    parseInput(asyncInput);
                }
                String asyncInput = debuggerInterface.read(); // Async input
                parseInput(asyncInput);
            }
        }
    }

    private void parseInput(String input) {
        if (state == DebuggerState.OUTPUT) { // Don't parse async inputs debugger unless state is INPUT
            return;
        }
        state = DebuggerState.OUTPUT;
        if (input.isEmpty()) {
            return;
        }
        switch (input.charAt(0)) {
            case 'c': { // Print call stack
                currentStack = callStack;
                print(0, true);
            }
            break;
            case 'l': { // Print line stack
                currentStack = lineStack;
                print(0, true);
            }
            break;
            case 'i': { // Inspect environment at stack index
                int index = Integer.parseInt(input.substring(2));
                print(index, false);
            }
            break;
            case 'e': { // Evaluate expression
                if (evaluator == null) {
                    throw new IllegalStateException("Evaluator not set!");
                }
                debuggerInterface.println(evaluator.eval(input.substring(2), focusFrame.environment));
            }
            break;
            case 'w': {
                String name = input.substring(2);
                watch.put(name, focusFrame.sourceEnvironment);
                debuggerInterface.println("Added to watch: " + name);
            }
            break;
            case 'n':
                stepState = StepState.STEP_INTO;
                return;
            case 'v':
                stepState = StepState.STEP_OVER;
                stepOverDepth = 0;
                return;
            case 'a': {
                if (currentBreakpoint != null) {
                    if (ignoredBreakpoints.contains(currentBreakpoint)) {
                        ignoredBreakpoints.remove(currentBreakpoint);
                    } else {
                        addedBreakpoints.add(currentBreakpoint);
                    }
                }
                debuggerInterface.println("Breakpoint added.");
            }
            break;
            case 'r': {
                if (currentBreakpoint != null) {
                    if (addedBreakpoints.contains(currentBreakpoint)) {
                        addedBreakpoints.remove(currentBreakpoint);
                    } else {
                        ignoredBreakpoints.add(currentBreakpoint);
                    }
                    currentBreakpoint = null;
                }
                debuggerInterface.println("Breakpoint removed.");
            }
            break;
            case 'x': { // Toggle catching all exceptions
                allExceptions = !allExceptions;
                debuggerInterface.println("All exceptions will be caught: " + allExceptions);
            }
            break;
            case 'o': { // Toggle debugging on/off
                debuggingOff = !debuggingOff;
                debuggerInterface.println("Debugging turned off: " + debuggingOff);
            }
            break;
            case 'h': { // Print help
                printHelp();
            }
            break;
            case 'g': { // Print global environment
                if (evaluator == null) {
                    throw new IllegalStateException("Evaluator not set!");
                }
                debuggerInterface.println(evaluator.getGlobalEnvironment().toString());
            }
            break;
            default:
                debuggerInterface.resume();
                return;
        }
        scanInput();
    }

    static class Frame {

        final Environment environment;
        final Environment sourceEnvironment;
        final Codifiable line;
        final Codifiable[] before;
        final Codifiable[] after;

        Frame(Environment environment,
              Environment sourceEnvironment,
              Codifiable line,
              Codifiable[] before,
              Codifiable[] after) {
            this.environment = environment;
            this.sourceEnvironment = sourceEnvironment;
            this.line = line;
            this.before = before;
            this.after = after;
        }

        void print(Integer index, DebuggerInterface debuggerInterface) {
            if (index != null) {
                debuggerInterface.print("[" + index + "] ");
            }
            debuggerInterface.print("\"" + line.getFileName() + "\" line " + line.getLineNumber() + ": ");
            debuggerInterface.println(line.toCode(0, true));
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
    
    public interface DebuggerInterface {
        void print(String s);
        void println(String s);
        String read();
        Object getLock();
        void resume();
    }
    
    public static class ConsoleInterface implements DebuggerInterface {
        
        private Scanner scanner;
        
        public ConsoleInterface() {
            scanner = new Scanner(System.in);
        }

        @Override
        public void print(String s) {
            System.out.print(s);
        }

        @Override
        public void println(String s) {
            System.out.println(s);
        }

        @Override
        public String read() {
            return scanner.nextLine();
        }

        @Override
        public Object getLock() {
            return null; // Console interface is sync
        }

        @Override
        public void resume() {
            // Nothing to do in a sync interface
        }
    }
}
