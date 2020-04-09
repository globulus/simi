package net.globulus.simi;

import net.globulus.simi.api.SimiException;

public class SimiExceptionWithDebugInfo {

    public final SimiException exception;
    public final Debugger.Capture capture;

    public SimiExceptionWithDebugInfo(SimiException exception, Debugger.Capture capture) {
        this.exception = exception;
        this.capture = capture;
    }
}
