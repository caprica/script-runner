/**
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.caprica.scriptrunner;

import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;

public class DefaultScriptListener implements ScriptListener {

    private final PrintWriter out;

    private String separatorLine;
    private String templateLine;
    private String headerLine;

    private int MAXIMUM_DISPLAY_SIZE = 40;

    public DefaultScriptListener(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void beginScript() {
        reset();
        out.printf("Begin script execution at %s%n%n", DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
    }

    @Override
    public void comment(String comment) {
        out.println(comment);
    }

    @Override
    public void sql(String sql) {
        out.println(sql);
    }

    @Override
    public void success() {
    }

    @Override
    public void error(String error) {
    }

    @Override
    public void updateCount(int updateCount) {
        out.printf("%d row(s) updated%n%n", updateCount);
    }

    @Override
    public void resultSet(ResultSetMetaData resultSetMetaData) {
        prepareResultSet(resultSetMetaData);

        out.printf("%s%n", separatorLine);
        out.printf("%s%n", headerLine);
        out.printf("%s%n", separatorLine);
    }

    @Override
    public void row(ResultSet resultSet) {
        try {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Object[] values = new Object[resultSetMetaData.getColumnCount()];
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                String value = resultSet.getString(i);
                if (value != null) {
                    value = value.replaceAll("\\r\\n|\\r|\\n", "\u240d");
                } else {
                    value = "NULL";
                }
                values[i-1] = value;
            }
            out.printf("%s%n", String.format(templateLine, values));
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void noMoreRows() {
        out.printf("%s%n%n", separatorLine);
    }

    @Override
    public void endScript(int updateCount, int successCount, int warningCount, int errorCount, long duration) {
        out.printf("End script execution at %s%n%n", DateFormat.getTimeInstance(DateFormat.LONG).format(new Date()));
        out.printf("  Updates: %d rows%n%n", updateCount);
        out.printf("Successes: %d%n", successCount);
        out.printf(" Warnings: %d%n", warningCount);
        out.printf("   Errors: %d%n", errorCount);
        out.printf("    Total: %d%n", successCount + warningCount + errorCount);
        out.println();
        out.printf(" Duration: %d seconds%n", duration / 1000);
        out.println();
    }

    private void reset() {
        this.separatorLine = null;
        this.templateLine = null;
        this.headerLine = null;
    }

    private void prepareResultSet(ResultSetMetaData resultSetMetaData) {
        try {
            StringBuilder separatorLine = new StringBuilder(100);
            StringBuilder templateLine = new StringBuilder(100);
            separatorLine.append('+');
            templateLine.append('|');
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                if (i > 1) {
                    separatorLine.append('+');
                    templateLine.append('|');
                }

                int displaySize = Math.min(MAXIMUM_DISPLAY_SIZE, resultSetMetaData.getColumnDisplaySize(i));
                separatorLine.append(Strings.repeat("=", displaySize));
                templateLine.append(String.format("%%-%d.%ds", displaySize, displaySize));
            }
            separatorLine.append('+');
            templateLine.append('|');

            this.templateLine = templateLine.toString();
            this.separatorLine = separatorLine.toString();

            Object[] labels = new Object[resultSetMetaData.getColumnCount()];
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                labels[i-1] = resultSetMetaData.getColumnLabel(i);
            }
            this.headerLine = String.format(this.templateLine, labels);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
