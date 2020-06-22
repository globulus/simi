package net.globulus.simi.warp.debug

import net.globulus.simi.tool.TokenPatcher
import net.globulus.simi.warp.CallFrame
import net.globulus.simi.warp.Function
import net.globulus.simi.warp.Vm
import java.util.*

class Debugger(private val vm: Vm) {
    private val scanner = Scanner(System.`in`)
    private var debuggingOff = false
    private val ignoredBreakpoints = mapOf<Function, List<Int>>()
    private var focusFrame: Int = 0
    private var status = StepStatus.BREAKPOINT
    private var triggerPoints = Stack<CodePointer>()
    private var triggerFrames = Stack<Int>()

    fun triggerBreakpoint(isCall: Boolean = false) {
        if (debuggingOff) {
            return
        }
        focusFrame = 0
        when (status) {
            StepStatus.BREAKPOINT -> {
                val posHasBreakpoint = vm.currentFunction.debugInfo.breakpoints.contains(vm.buffer.position())
                if (posHasBreakpoint && isBreakpointIgnored() || !posHasBreakpoint) {
                    return
                }
            }
            StepStatus.INTO -> {
                if (!isCall) {
                    return
                }
                status = StepStatus.BREAKPOINT
            }
            StepStatus.OVER -> {
                if (vm.fiber.fp > triggerFrames.peek()) {
                    return
                }
                if (currentCodePoint == triggerPoints.peek()) {
                    return
                }
                triggerPoints.pop()
                triggerFrames.pop()
                status = StepStatus.BREAKPOINT
            }
            StepStatus.OUT -> {
                if (currentCodePoint != triggerPoints.peek()) {
                    return
                }
                triggerPoints.pop()
                status = StepStatus.BREAKPOINT
            }
        }
        println("BREAKPOINT")
        focusFrame = 0
        printFocusFrame()
    }

    private fun printFocusFrame() {
        val focusFrameIndex = vm.fiber.fp - 1 - focusFrame
        val frame = vm.fiber.callFrames[focusFrameIndex]!!
        val debugInfo = frame.closure.function.debugInfo
        val codePointer = frame.getCurrentCodePoint()
        val tokens = debugInfo.tokens.filter { it.file == codePointer.file }
        val highlightTokenIndex = tokens.indexOfFirst { it.line == codePointer.line }
        println(TokenPatcher.patch(tokens, tokens[highlightTokenIndex]))
        println("===================")
        println("Call stack:")
        printCallStack(focusFrameIndex)
        printLocalsForFrame(frame, true)
        readInput(frame)
    }

    private fun printCallStack(focusFrameIndex: Int) {
        for (i in vm.fiber.fp - 1 downTo 0) {
            if (i == focusFrameIndex) {
                print("* ") // mark the focus frame
            }
            println(vm.fiber.callFrames[i])
        }
    }

    private fun printLocalsForFrame(frame: CallFrame, capped: Boolean) {
        println("===================")
        println("Locals:")
        val locals = frame.closure.function.debugInfo.locals
        var count = 0
        for ((sp, name) in locals) {
            vm.fiber.stack[frame.sp + sp]?.let {
                println("$name = ${vm.stringify(it)}")
                count++
            } ?: break
            if (capped && count > MAX_LOCALS) {
                println("...${locals.size - MAX_LOCALS} more locals available, use 'l' to print them all.")
                break
            }

        }
    }

    private fun readInput(frame: CallFrame) {
        print("šdb> ")
        val input = scanner.nextLine()
        if (input.isEmpty()) {
            return
        }
        when (input[0]) {
            'g' -> { // go to frame
                val loc = input.substring(2).toInt()
                focusFrame = loc
                printFocusFrame()
            }
            'e' -> { // evaluate
                val expr = input.substring(2)
                debuggingOff = true
                vm.push(expr)
                vm.gu()
                val res = vm.pop()
                debuggingOff = false
                println(res)
                readInput(frame)
            }
            'i' -> { // step into
                status = StepStatus.INTO
                triggerPoints.push(currentCodePoint)
            }
            'v' -> { // step over
                status = StepStatus.OVER
                triggerFrames.push(vm.fiber.fp)
                triggerPoints.push(currentCodePoint)
            }
            'o' -> { // step out
                status = StepStatus.OUT
            }
            'l' -> { // print all locals
                printLocalsForFrame(frame, false)
                readInput(frame)
            }
            else -> { // reset the step status
                status = StepStatus.BREAKPOINT
            }
        }
    }

    private val currentCodePoint: CodePointer get() = vm.fiber.frame.getCurrentCodePoint()

    private fun isBreakpointIgnored(): Boolean {
        return ignoredBreakpoints[vm.currentFunction]?.contains(vm.buffer.position()) == true
    }

    private enum class StepStatus {
        BREAKPOINT, INTO, OVER, OUT
    }

    companion object {
        private const val MAX_LOCALS = 5 // maximum number of locals to be printed out in regular mode
    }
}