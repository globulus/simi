package net.globulus.simi.mariadb;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiException;

import java.sql.SQLException;

class Util {

    private Util() { }

    static void raiseSqlException(SQLException e, BlockInterpreter interpreter) {
        interpreter.raiseException(new SimiException((SimiClass) interpreter.getEnvironment()
                .tryGet("SqlException").getValue().getObject(), e.getMessage()));
    }

}
