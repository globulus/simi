package net.globulus.simi.api;

import java.util.List;

public interface SimiBlock {
    List<? extends SimiStatement> getStatements();
}
