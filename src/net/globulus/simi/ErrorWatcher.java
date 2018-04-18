package net.globulus.simi;

interface ErrorWatcher {
    void report(int line, String where, String message);
    void runtimeError(RuntimeError error);
}
