package net.globulus.simi.sql;

import net.globulus.simi.SimiMapper;
import net.globulus.simi.api.*;

import java.sql.*;
import java.util.List;
import java.util.Map;

@SimiJavaClass
public class MariaDb {

    private static final String NATIVE_CONN = "native_conn";

    @SimiJavaMethod
    public static SimiProperty open(SimiObject self, BlockInterpreter interpreter, SimiProperty props) {
        String ip = "localhost";
        long port = 3306;
        String db = "test";
        String user = "root";
        String password = "";
        if (props != null) {
            Map<String, Object> map = SimiMapper.fromObject(props.getValue().getObject());
            String mapIp = (String) map.get("ip");
            if (mapIp != null) {
                ip = mapIp;
            }
            Long mapPort = (Long) map.get("port");
            if (mapPort != null) {
                port = mapPort;
            }
            String mapDb = (String) map.get("db");
            if (mapDb != null) {
                db = mapDb;
            }
            String mapUser = (String) map.get("user");
            if (mapUser != null) {
                user = mapUser;
            }
            String mapPassword = (String) map.get("password");
            if (mapPassword != null) {
                password = mapPassword;
            }
        }
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            Connection conn = DriverManager.getConnection(String.format("jdbc:mariadb://%s:%d/%s?user=%s&password=%s", ip, port, db, user, password));
            ConnectionWrapper wrapper = new ConnectionWrapper(conn);
            self.set(NATIVE_CONN, new SimiValue.Object(wrapper), interpreter.getEnvironment());
        } catch (SQLException e) {
            e.printStackTrace() ;
            Util.raiseSqlException(e, interpreter);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty close(SimiObject self, BlockInterpreter interpreter) {
        Connection conn = getConn(self, interpreter);
        try {
            conn.close();
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty execute(SimiObject self, BlockInterpreter interpreter, SimiProperty sql) {
        Connection conn = getConn(self, interpreter);
        try {
            Statement stmt = conn.createStatement();
            boolean success = stmt.execute(sql.getValue().getString());
            ResultSet resultSet = stmt.getResultSet();
            stmt.close();
            return SimiResultSet.init(resultSet, interpreter);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    private static Connection getConn(SimiObject self, BlockInterpreter interpreter) {
        return ((ConnectionWrapper) self.get(NATIVE_CONN, interpreter.getEnvironment()).getValue().getObject()).conn;
    }

    private static class ConnectionWrapper implements SimiObject {

        Connection conn;

        ConnectionWrapper(Connection conn) {
            this.conn = conn;
        }

        @Override
        public SimiClass getSimiClass() {
            return null;
        }

        @Override
        public SimiProperty get(String s, SimiEnvironment simiEnvironment) {
            return null;
        }

        @Override
        public void set(String s, SimiProperty simiProperty, SimiEnvironment simiEnvironment) {

        }

        @Override
        public SimiObject clone(boolean b) {
            return null;
        }

        @Override
        public List<SimiValue> keys() {
            return null;
        }

        @Override
        public List<SimiValue> values() {
            return null;
        }

        @Override
        public String toCode(int i, boolean b) {
            return null;
        }

        @Override
        public int getLineNumber() {
            return 0;
        }

        @Override
        public String getFileName() {
            return null;
        }

        @Override
        public boolean hasBreakPoint() {
            return false;
        }

        @Override
        public int compareTo(SimiObject o) {
            return 0;
        }
    }
}
