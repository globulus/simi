package net.globulus.simi;

interface ErrorWatcher {
    void report(String file, int line, String where, String message);
    void runtimeError(RuntimeError error);
}
