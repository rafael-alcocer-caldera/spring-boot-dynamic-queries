/**
 * Copyright [2023] [RAFAEL ALCOCER CALDERA]
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rafael.alcocer.caldera.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * This class has the necessary methods to execute dynamic queries.
 * 
 * @author Rafael Alcocer Caldera
 */
@RequiredArgsConstructor
@Component
public class DynamicQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicQuery.class);

    private final Connection conn;

    /**
     * 
     * Gets the ResultSet, using the generic query and passing the list of
     * parameters.
     * 
     * @param query      SELECT * FROM %s WHERE %s = ?
     * @param parameters the column values
     * @return ResultSet
     * @throws SQLException
     */
    public ResultSet getResultSet(String query, List<?> parameters) throws SQLException {
        LOGGER.info("##### conn: " + conn);
        LOGGER.info("##### query: " + query);
        LOGGER.info("##### parameters: " + parameters);

        ResultSet rs = null;

        PreparedStatement ps = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        ParameterMetaData parameterMetaData = ps.getParameterMetaData();

        int parametersCount = parameterMetaData.getParameterCount();
        int lisCount = parameters.size();

        LOGGER.info("##### parametersCount: " + parametersCount);
        LOGGER.info("##### lisCount: " + lisCount);

        if (lisCount == parametersCount) {
            for (int i = 0; i < parametersCount; i++) {
                // Starts with 1 not 0 due SQL
                int parameterType = parameterMetaData.getParameterType(i + 1);

                LOGGER.info("##### parameterType: " + parameterType);

                switch (parameterType) {
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.LONGVARCHAR:
                    String stringParam = (String) parameters.get(i);
                    ps.setString(i + 1, stringParam);
                    break;

                case Types.BIT:
                case Types.BOOLEAN:
                    boolean booleanParam = ((Boolean) parameters.get(i)).booleanValue();
                    ps.setBoolean(i + 1, booleanParam);
                    break;

                case Types.TINYINT:
                    byte byteParam = ((Byte) parameters.get(i)).byteValue();
                    ps.setByte(i + 1, byteParam);
                    break;

                case Types.SMALLINT:
                    short shortParam = ((Short) parameters.get(i)).shortValue();
                    ps.setShort(i + 1, shortParam);
                    break;

                case Types.INTEGER:
                    int intParam = ((Integer) parameters.get(i)).intValue();
                    ps.setInt(i + 1, intParam);
                    break;

                case Types.BIGINT:
                    long longParam = ((Long) parameters.get(i)).longValue();
                    ps.setLong(i + 1, longParam);
                    break;

                case Types.REAL:
                    float floatParam = ((Float) parameters.get(i)).floatValue();
                    ps.setFloat(i + 1, floatParam);
                    break;

                case Types.FLOAT:
                case Types.DOUBLE:
                    double doubleParam = ((Double) parameters.get(i)).doubleValue();
                    ps.setDouble(i + 1, doubleParam);
                    break;

                case Types.NUMERIC:
                case Types.DECIMAL:
                    BigDecimal bigdecimalParam = (BigDecimal) parameters.get(i);
                    ps.setBigDecimal(i + 1, bigdecimalParam);
                    break;

                case Types.DATE:
                    java.sql.Date dateParam = (java.sql.Date) parameters.get(i);
                    ps.setDate(i + 1, dateParam);
                    break;

                case Types.TIME:
                    java.sql.Time timeParam = (java.sql.Time) parameters.get(i);
                    ps.setTime(i + 1, timeParam);
                    break;

                case Types.TIMESTAMP:
                    java.sql.Timestamp timestampParam = (java.sql.Timestamp) parameters.get(i);
                    ps.setTimestamp(i + 1, timestampParam);
                    break;
                }
            }

            rs = ps.executeQuery();

            LOGGER.info("##### ResultSet: " + rs);
        }

        return rs;
    }

    /**
     * Inserts one row.
     * 
     * @param tableName  the name of the table
     * @param parameters the list of parameters
     * @return 1 if the row was inserted successfully 0 if not
     */
    public int insertOneRow(String tableName, List<?> parameters) {
        String insertQuery = createDynamicInsertQuery(tableName);
        LOGGER.info("##### insertQuery: " + insertQuery);

        try (PreparedStatement ps = conn.prepareStatement(insertQuery, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE);) {

            for (int i = 1; i <= parameters.size(); i++) {
                ps.setObject(i, parameters.get(i - 1));
            }

            return ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    /**
     * Inserts multiple rows using batch.
     * 
     * @param tableName        the name of the table
     * @param listOfParameters list of parameters
     * @return array of updated rows
     */
    public int[] insertMultipleRows(String tableName, List<List<?>> listOfParameters) {
        String insertQuery = createDynamicInsertQuery(tableName);
        LOGGER.info("##### insertQuery: " + insertQuery);

        try (PreparedStatement ps = conn.prepareStatement(insertQuery, ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE);) {

            listOfParameters.forEach(parameters -> {
                for (int i = 1; i <= parameters.size(); i++) {
                    try {
                        ps.setObject(i, parameters.get(i - 1));
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                try {
                    ps.addBatch();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            return ps.executeBatch();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return new int[0];
    }

    /**
     * Inserts a row from a ResutSet.
     * 
     * @param rs
     * @param parameters
     * @throws SQLException
     */
    public void insertRowFromResultSet(ResultSet rs, List<?> parameters) throws SQLException {
        rs.moveToInsertRow();

        ResultSetMetaData rsMetaData = rs.getMetaData();
        int columnCount = rsMetaData.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            int columnType = rsMetaData.getColumnType(i + 1);

            switch (columnType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                rs.updateString(i + 1, (String) parameters.get(i));
                break;

            case Types.BIT:
            case Types.BOOLEAN:
                rs.updateBoolean(i + 1, ((Boolean) parameters.get(i)).booleanValue());
                break;

            case Types.TINYINT:
                rs.updateByte(i, ((Byte) parameters.get(i)).byteValue());
                break;

            case Types.SMALLINT:
                rs.updateShort(i, ((Short) parameters.get(i)).shortValue());
                break;

            case Types.INTEGER:
                rs.updateInt(i, ((Integer) parameters.get(i)).intValue());
                break;

            case Types.BIGINT:
                rs.updateLong(i, ((Long) parameters.get(i)).longValue());
                break;

            case Types.REAL:
                rs.updateFloat(i, ((Float) parameters.get(i)).floatValue());
                break;

            case Types.FLOAT:
            case Types.DOUBLE:
                rs.updateDouble(i, ((Double) parameters.get(i)).doubleValue());
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                rs.updateBigDecimal(i, (BigDecimal) parameters.get(i));
                break;

            case Types.DATE:
                rs.updateDate(i, (java.sql.Date) parameters.get(i));
                break;

            case Types.TIME:
                rs.updateTime(i, (java.sql.Time) parameters.get(i));
                break;

            case Types.TIMESTAMP:
                rs.updateTimestamp(i, (java.sql.Timestamp) parameters.get(i));
                break;
            }
        }

        rs.insertRow();

        // return rs.rowInserted(); // Regresa un boolean
        // MySQL no soporta "rs.rowInserted()" => com.mysql.jdbc.NotImplemented:
        // Feature not implemented
    }

    /**
     * Updates data from a ResultSet.
     * 
     * @param rs
     * @param parameters
     * @throws SQLException
     */
    public void update(ResultSet rs, List<?> parameters) throws SQLException {
        ResultSetMetaData rsMetaData = rs.getMetaData();
        int columnCount = rsMetaData.getColumnCount();

        for (int i = 0; i < columnCount; i++) {
            int columnType = rsMetaData.getColumnType(i + 1);

            switch (columnType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                rs.updateString(i + 1, (String) parameters.get(i));
                break;

            case Types.BIT:
            case Types.BOOLEAN:
                rs.updateBoolean(i + 1, ((Boolean) parameters.get(i)).booleanValue());
                break;

            case Types.TINYINT:
                rs.updateByte(i, ((Byte) parameters.get(i)).byteValue());
                break;

            case Types.SMALLINT:
                rs.updateShort(i, ((Short) parameters.get(i)).shortValue());
                break;

            case Types.INTEGER:
                rs.updateInt(i, ((Integer) parameters.get(i)).intValue());
                break;

            case Types.BIGINT:
                rs.updateLong(i, ((Long) parameters.get(i)).longValue());
                break;

            case Types.REAL:
                rs.updateFloat(i, ((Float) parameters.get(i)).floatValue());
                break;

            case Types.FLOAT:
            case Types.DOUBLE:
                rs.updateDouble(i, ((Double) parameters.get(i)).doubleValue());
                break;

            case Types.NUMERIC:
            case Types.DECIMAL:
                rs.updateBigDecimal(i, (BigDecimal) parameters.get(i));
                break;

            case Types.DATE:
                rs.updateDate(i, (java.sql.Date) parameters.get(i));
                break;

            case Types.TIME:
                rs.updateTime(i, (java.sql.Time) parameters.get(i));
                break;

            case Types.TIMESTAMP:
                rs.updateTimestamp(i, (java.sql.Timestamp) parameters.get(i));
                break;
            }
        }

        rs.updateRow();

        // return rs.rowUpdated(); // Regresa un boolean
        // MySQL no soporta "rs.rowUpdated()" => com.mysql.jdbc.NotImplemented:
        // Feature not implemented
    }

    /**
     * Elimina el renglon obtenido del ResultSet.
     * 
     * @param rs
     * @throws SQLException
     */
    public void delete(ResultSet rs) throws SQLException {
        rs.deleteRow();

        // return rs.rowDeleted(); // Regresa un boolean
        // MySQL no soporta "rs.rowDeleted()" => com.mysql.jdbc.NotImplemented:
        // Feature not implemented
    }

    /**
     * Deletes all rows.
     * 
     * @param tableName the name of the table
     * @return the number of rows deleted
     */
    public int deleteAllRows(String tableName) {
        String deletQuery = """
                DELETE FROM %s
                         """.formatted(tableName);

        try (PreparedStatement ps = conn.prepareStatement(deletQuery);) {
            return ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    /**
     * Gets the list of column names dynamically from a table.
     * 
     * @param tableName the name of the table
     * @return list of column names
     */
    public List<String> getColumns(String tableName) {
        List<String> columnNames = new ArrayList<String>();
        DatabaseMetaData databaseMetaData = null;

        try {
            databaseMetaData = conn.getMetaData();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if (databaseMetaData != null) {
            try (ResultSet columns = databaseMetaData.getColumns(null, null, tableName, null)) {
                LOGGER.info("##### columns: " + columns);

                while (columns.next()) {
                    String COLUMN_NAME = columns.getString("COLUMN_NAME");
                    String COLUMN_SIZE = columns.getString("COLUMN_SIZE");
                    String DATA_TYPE = columns.getString("DATA_TYPE");
                    String IS_NULLABLE = columns.getString("IS_NULLABLE");
                    String IS_AUTOINCREMENT = columns.getString("IS_AUTOINCREMENT");
                    String COLUMN_DEF = columns.getString("COLUMN_DEF");
                    String TYPE_NAME = columns.getString("TYPE_NAME");
                    String IS_GENERATEDCOLUMN = columns.getString("IS_GENERATEDCOLUMN");

                    LOGGER.info("##### COLUMN_NAME: " + COLUMN_NAME);
                    LOGGER.info("##### COLUMN_SIZE: " + COLUMN_SIZE);
                    LOGGER.info("##### DATA_TYPE: " + DATA_TYPE);
                    LOGGER.info("##### IS_NULLABLE: " + IS_NULLABLE);
                    LOGGER.info("##### IS_AUTOINCREMENT: " + IS_AUTOINCREMENT);
                    LOGGER.info("##### COLUMN_DEF: " + COLUMN_DEF);
                    LOGGER.info("##### TYPE_NAME: " + TYPE_NAME);
                    LOGGER.info("##### IS_GENERATEDCOLUMN: " + IS_GENERATEDCOLUMN);
                    LOGGER.info("---------------------------------------");

                    if ("serial".equalsIgnoreCase(TYPE_NAME) || "YES".equalsIgnoreCase(IS_AUTOINCREMENT)
                            || "CURRENT_TIMESTAMP".equalsIgnoreCase(COLUMN_DEF)) {
                        continue;
                    }

                    columnNames.add(COLUMN_NAME);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        return columnNames;
    }

    /*
     * public List<String> getColumns(String tableName) { List<String> columnNames =
     * new ArrayList<String>();
     * 
     * try (PreparedStatement ps = conn
     * .prepareStatement(queryConfig.getSelectQuery() + " " + tableName +
     * " WHERE 0 = 1"); ResultSet rs = ps.executeQuery();) { ResultSetMetaData rsmd
     * = rs.getMetaData();
     * 
     * for (int i = 1; i <= rsmd.getColumnCount(); i++) {
     * LOGGER.info("##### getColumnTypeName: " + rsmd.getColumnTypeName(i));
     * LOGGER.info("##### getColumnType: " + rsmd.getColumnType(i));
     * LOGGER.info("##### isAutoIncrement: " + rsmd.isAutoIncrement(i));
     * LOGGER.info("##### isWritable: " + rsmd.isWritable(i));
     * 
     * if ("serial".equalsIgnoreCase(rsmd.getColumnTypeName(i)) && true ==
     * rsmd.isAutoIncrement(i)) { continue; }
     * 
     * columnNames.add(rsmd.getColumnLabel(i));
     * LOGGER.info("---------------------------------------"); } } catch
     * (SQLException ex) { ex.printStackTrace(); }
     * 
     * return columnNames; }
     */

    public String createDynamicInsertQuery(String tableName) {
        List<String> columnNames = getColumns(tableName);
        LOGGER.info("##### columnNames: " + columnNames);
        LOGGER.info("##### columnNames.size(): " + columnNames.size());

        StringBuilder insertColumns = new StringBuilder();
        StringBuilder insertValues = new StringBuilder();

        for (int i = 0; i < columnNames.size(); i++) {
            insertColumns.append(columnNames.get(i));
            insertColumns.append(", ");
            insertValues.append("?");
            insertValues.append(", ");
        }

        insertColumns.delete(insertColumns.length() - 2, insertColumns.length());
        insertValues.delete(insertValues.length() - 2, insertValues.length());

        return """
                INSERT INTO %s (%s)
                VALUES (%s)
                         """.formatted(tableName, insertColumns, insertValues);
    }
}
