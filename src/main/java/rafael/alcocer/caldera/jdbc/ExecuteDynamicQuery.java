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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Executes the tests of the DynamicQuery class.
 * 
 * @author Rafael Alcocer Caldera
 */
@RequiredArgsConstructor
@Component
public class ExecuteDynamicQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteDynamicQuery.class);

    private final DynamicQuery dynamicQuery;

    public void executeInsert(String tableName) {
        List<Object> parameters = new ArrayList<>();
        parameters.add("rac");
        parameters.add("Rafael Alcocer");
        parameters.add("ra@test.com");
        parameters.add("515.123.4567");
        parameters.add(true);

        LOGGER.info("##### inserted: " + dynamicQuery.insertOneRow(tableName, parameters));
    }

    public void executeMultipleInserts(String tableName, int numberOfRecordsToInsert) {
        List<List<?>> listOfParameters = new ArrayList<>();

        IntStream.rangeClosed(1, numberOfRecordsToInsert).forEach(i -> {
            List<?> parameters = generateParameters(i);
            listOfParameters.add(parameters);
        });

        int[] rowsInserted = dynamicQuery.insertMultipleRows(tableName, listOfParameters);

        LOGGER.info("##### inserted: " + rowsInserted);
        LOGGER.info("##### total inserted: " + rowsInserted.length);
    }

    public void executeSelectBy(String tableName, String columnName, String columnValue) {
        String querySelect = """
                SELECT *
                FROM %s
                WHERE %s = ?
                         """.formatted(tableName, columnName);

        List<String> parameters = new ArrayList<>();
        parameters.add(columnValue);

        try (ResultSet rs = dynamicQuery.getResultSet(querySelect, parameters);) {
            if (rs != null) {
                List<String> columnNames = new ArrayList<>();
                ResultSetMetaData rsmd = rs.getMetaData();

                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    columnNames.add(rsmd.getColumnLabel(i));
                }

                LOGGER.info("##### columnNames.size(): " + columnNames.size());
                LOGGER.info("##### columnNames: " + columnNames);

                while (rs.next()) {
                    for (int i = 0; i < columnNames.size(); i++) {
                        if (rs.getObject(columnNames.get(i)) != null) {
                            LOGGER.info("##### " + columnNames.get(i) + ": " + rs.getObject(columnNames.get(i)));
                        }
                    }

                    LOGGER.info("---------------------------------");
                }
            } else {
                LOGGER.info("#####ResultSet in null");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void executeDeleteAll(String tableName) {
        int rowsDeleted = dynamicQuery.deleteAllRows(tableName);
        LOGGER.info("##### rowsDeleted: " + rowsDeleted);
    }

    public List<?> generateParameters(int i) {
        Random random = new Random();
        int randomNumber = random.nextInt(1000);
        boolean active = random.nextBoolean();

        List<Object> parameters = new ArrayList<>();
        parameters.add("username" + i);
        parameters.add("Name" + i);
        parameters.add("email" + i + "@test.com");
        parameters.add("" + randomNumber + "-" + randomNumber + "-" + randomNumber);
        parameters.add(active);

        return parameters;
    }
}
