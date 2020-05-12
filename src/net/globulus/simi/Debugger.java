package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

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
            "anything else: continue with program execution\n" +
            "\n";

    private enum DebuggerState {
        OUTPUT, INPUT
    }
    private DebuggerState state;

    private Capture capture;
    private DebuggerInterface debuggerInterface;
    private StringBuilder output;
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

    private DebuggerWatcher watcher;

    Debugger(DebuggerInterface debuggerInterface) {
        state = DebuggerState.OUTPUT;
        capture = new Capture(new FrameStack(), new FrameStack());
        this.debuggerInterface = debuggerInterface;
        output = new StringBuilder();
        ignoredBreakpoints = new HashSet<>();
        addedBreakpoints = new HashSet<>();
        watch = new HashMap<>();
        allExceptions = false;
        debuggingOff = false;
        firstHelp = true;
        stepState = StepState.NONE;
        watcher = new Watcher();
    }

    void setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    void pushLine(Frame frame) {
        capture.lineStack.push(frame);
    }

    void pushCall(Frame frame) {
        capture.callStack.push(frame);
        if (stepState == StepState.STEP_OVER) {
            stepState = StepState.STEP_OVER_SUSPENDED;
            stepOverDepth++;
        } else if (stepState == StepState.STEP_OVER_SUSPENDED) {
            stepOverDepth++;
        }
    }

    void popCall() {
        capture.callStack.pop();
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
        currentStack = capture.callStack;
        output.append("***** BREAKPOINT *****\n\n");
        print(0, true);
        scanInput();
    }

    void triggerException(Stmt stmt, SimiExceptionWithDebugInfo e, boolean fatal) {
        if (debuggingOff || !(allExceptions || fatal)) {
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
        currentStack = e.capture.callStack;
        output.append("***** ").append(fatal ? "FATAL " : "").append("EXCEPTION *****\n\n")
                .append(e.exception.getMessage()).append("\n\n");
        print(0, true);
        scanInput();
    }

    DebuggerInterface getInterface() {
        return debuggerInterface;
    }

    DebuggerWatcher getWatcher() {
        return watcher;
    }

    private void print(int frameIndex, boolean printLine) {
        List<Frame> frames = currentStack.toList();
        output.append("============================\n");
        if (printLine && currentStack != capture.lineStack) {
            focusFrame = capture.lineStack.toList().get(frameIndex);
        } else {
            focusFrame = frames.get(frameIndex);
        }
        if (focusFrame.before != null) {
            for (Codifiable codifiable : focusFrame.before) {
                output.append(codifiable.toCode(0, true)).append("\n");
            }
        }
        focusFrame.print(null, output);
        if (focusFrame.after != null) {
            for (Codifiable codifiable : focusFrame.after) {
                output.append(codifiable.toCode(0, true)).append("\n");
            }
        }
        output.append("============================\n");
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            frame.print(i, output);
        }
        output.append("\n#### ENVIRONMENT ####\n\n");
        output.append(focusFrame.environment.toStringWithoutValuesOrGlobal()).append("\n");
        if (!watch.isEmpty()) {
            output.append("\n#### WATCH ####\n\n");
            for (Map.Entry<String, String> watched : watcher.getWatch().entrySet()) {
                output.append(watched.getKey()).append(" = ").append(watched.getValue()).append("\n");
            }
        }
        if (firstHelp) {
            firstHelp = false;
            printHelp();
        }
    }

    private void printHelp() {
        output.append(HELP);
    }

    private void scanInput() {
        flush();
        state = DebuggerState.INPUT;
        String syncInput = debuggerInterface.read(); // Try sync input
        if (syncInput != null) {
            parseInput(syncInput);
        } else {
            try {
                String asyncInput = debuggerInterface.getInputQueue().take();
                parseInput(asyncInput);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
                currentStack = capture.callStack;
                print(0, true);
            }
            break;
            case 'l': { // Print line stack
                currentStack = capture.lineStack;
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
                output.append(evaluator.eval(input.substring(2), focusFrame.environment)).append("\n");
            }
            break;
            case 'w': {
                String name = input.substring(2);
                watch.put(name, focusFrame.sourceEnvironment);
                output.append("Added to watch: ").append(name).append("\n");
            }
            break;
            case 'n':
                stepState = StepState.STEP_INTO;
                flush();
                return;
            case 'v':
                stepState = StepState.STEP_OVER;
                stepOverDepth = 0;
                flush();
                return;
            case 'a': {
                if (currentBreakpoint != null) {
                    if (ignoredBreakpoints.contains(currentBreakpoint)) {
                        ignoredBreakpoints.remove(currentBreakpoint);
                    } else {
                        addedBreakpoints.add(currentBreakpoint);
                    }
                }
                output.append("Breakpoint added.").append("\n");
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
                output.append("Breakpoint removed.").append("\n");
            }
            break;
            case 'x': { // Toggle catching all exceptions
                allExceptions = !allExceptions;
                output.append("All exceptions will be caught: ").append(allExceptions).append("\n");
            }
            break;
            case 'o': { // Toggle debugging on/off
                debuggingOff = !debuggingOff;
                output.append("Debugging turned off: ").append(debuggingOff).append("\n");
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
                output.append(evaluator.getGlobalEnvironment().toString()).append("\n");
            }
            break;
            default:
                debuggerInterface.resume();
                return;
        }
        scanInput();
    }

    private void flush() {
        debuggerInterface.flush(output.toString());
        output = new StringBuilder();
    }

    Capture copyCapture() {
        return new Capture(capture.lineStack.copy(), capture.callStack.copy());
    }

    public interface FrameDump {
        String getLine();
        String getZippedEnvironment();
        Map<String, String> getFullEnvironment();
        SimiObject toSimiObject(SimiClassImpl objectClass);
    }

    static class Capture {
        final FrameStack lineStack;
        final FrameStack callStack;

        public Capture(FrameStack lineStack, FrameStack callStack) {
            this.lineStack = lineStack;
            this.callStack = callStack;
        }
    }

    static class Frame implements FrameDump {

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

        void print(Integer index, StringBuilder output) {
            if (index != null) {
                output.append("[").append(index).append("] ");
            }
            output.append("\"").append(line.getFileName()).append("\" line ").append(line.getLineNumber() - 1).append(": ");
            output.append(line.toCode(0, true)).append("\n");
        }

        @Override
        public String getLine() {
            return line.toCode(0, true);
        }

        @Override
        public String getZippedEnvironment() {
            return environment.toStringWithoutValuesOrGlobal();
        }

        @Override
        public Map<String, String> getFullEnvironment() {
            return environment.dumpValues();
        }

        @Override
        public SimiObject toSimiObject(SimiClassImpl objectClass) {
            LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
            props.put("line", new SimiValue.String(getLine()));
            props.put("zippedEnvironment", new SimiValue.String(getZippedEnvironment()));
            props.put("fullEnvironment", new SimiValue.Object(SimiMapper.toObject(getFullEnvironment(), true)));
            return new SimiObjectImpl(objectClass, true, props, null);
        }
    }

    private static class FrameStack {

        private static final int MAX_SIZE = 50;

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

        FrameStack copy() {
            FrameStack copy = new FrameStack();
            copy.index = index;
            copy.stack = Arrays.copyOf(stack, stack.length);
            return copy;
        }
    }

    interface Evaluator {
        String eval(String input, Environment environment);

        Environment getGlobalEnvironment();
    }

    public interface DebuggerInterface {
        void flush(String s);

        String read();

        BlockingQueue<String> getInputQueue();

        void resume();
    }

    public static class ConsoleInterface implements DebuggerInterface {

        private Scanner scanner;

        public ConsoleInterface() {
            scanner = new Scanner(System.in);
        }

        @Override
        public void flush(String s) {
            System.out.println(s);
        }

        @Override
        public String read() {
            return scanner.nextLine();
        }

        @Override
        public BlockingQueue<String> getInputQueue() {
            return null;
        }

        @Override
        public void resume() {
            // Nothing to do in a sync interface
        }
    }

    public interface DebuggerWatcher {
        List<? extends FrameDump> getLineStack();

        List<? extends FrameDump> getCallStack();

        FrameDump getFocusFrame();

        Map<String, String> getWatch();

        Map<String, String> getGlobalEnvironment();
    }

    private class Watcher implements DebuggerWatcher {

        @Override
        public List<? extends FrameDump> getLineStack() {
            return capture.lineStack.toList();
        }

        @Override
        public List<? extends Frame> getCallStack() {
            return capture.callStack.toList();
        }

        @Override
        public FrameDump getFocusFrame() {
            return focusFrame;
        }

        @Override
        public Map<String, String> getWatch() {
            return watch.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                SimiProperty value = e.getValue().tryGet(e.getKey());
                                return (value != null) ? value.toString() : "nil";
                            }
                    ));
        }

        @Override
        public Map<String, String> getGlobalEnvironment() {
            return evaluator.getGlobalEnvironment().dumpValues();
        }
    }
}
