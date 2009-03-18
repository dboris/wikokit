
package wikipedia.sql;

import wikipedia.sql.Connect;
import java.sql.*;

/** Misc SQL routines.
 */
public class UtilSQL {

    /** Deletes all records from the table 'table_name', resets auto increment.
     *
     * DELETE FROM table_name;
     * ALTER TABLE table_name AUTO_INCREMENT = 0;
     */
    public static void deleteAllRecordsResetAutoIncrement (Connect connect, String table_name) {

        Statement   s = null;
        ResultSet   rs= null;

        try {
            s = connect.conn.createStatement ();
            s.addBatch("DELETE FROM "+ table_name +";");
            s.addBatch("ALTER TABLE "+ table_name +" AUTO_INCREMENT = 0;");
            s.executeBatch();

        } catch(SQLException ex) {
            System.err.println("SQLException (wikipedia.sql UtilSQL.java deleteAllRecordsResetAutoIncrement()):: table = "+ table_name +"; msg = " + ex.getMessage());
        } finally {
            if (rs != null) {   try { rs.close(); } catch (SQLException sqlEx) { }  rs = null; }
            if (s != null)  {   try { s.close();  } catch (SQLException sqlEx) { }  s = null;  }
        }
    }

}
