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

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An SQL script runner.
 * <p>
 * Original implementation from the MyBatis project licensed under the Apache License 2.0.
 */
public class ScriptRunner {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private static final String DEFAULT_DELIMITER = ";";

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("^\\s*((--)|(//))?\\s*(//)?\\s*@DELIMITER\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

    private final Connection connection;

    private final List<ScriptListener> listenerList = new ArrayList<>();

    private boolean stopOnError;
    private boolean throwWarning;
    private boolean autoCommit;
    private boolean sendFullScript;
    private boolean removeCRs;
    private boolean escapeProcessing = true;

    private int successCount;
    private int warningCount;
    private int errorCount;
    private long startTime;
    private long endTime;

    private PrintWriter logWriter = new PrintWriter(System.out);
    private PrintWriter errorLogWriter = new PrintWriter(System.err);

    private String delimiter = DEFAULT_DELIMITER;
    private boolean fullLineDelimiter;

    public ScriptRunner(Connection connection) {
        this.connection = connection;
    }

    public void addScriptListener(ScriptListener listener) {
        listenerList.add(listener);
    }

    public void removeScriptListener(ScriptListener listener) {
        listenerList.remove(listener);
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public void setThrowWarning(boolean throwWarning) {
        this.throwWarning = throwWarning;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void setSendFullScript(boolean sendFullScript) {
        this.sendFullScript = sendFullScript;
    }

    public void setRemoveCRs(boolean removeCRs) {
        this.removeCRs = removeCRs;
    }

    public void setEscapeProcessing(boolean escapeProcessing) {
        this.escapeProcessing = escapeProcessing;
    }

    public void setLogWriter(PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    public void setErrorLogWriter(PrintWriter errorLogWriter) {
        this.errorLogWriter = errorLogWriter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setFullLineDelimiter(boolean fullLineDelimiter) {
        this.fullLineDelimiter = fullLineDelimiter;
    }

    public void runScript(Reader reader) {
        reset();

        startTime = System.currentTimeMillis();

        notifyBeginScript();
        setAutoCommit();
        try {
            if (sendFullScript) {
                executeFullScript(reader);
            } else {
                executeLineByLine(reader);
            }
        } finally {
            rollbackConnection();
            endTime = System.currentTimeMillis();
            notifyEndScript(successCount, warningCount, errorCount, endTime - startTime);
        }
    }

    private void executeFullScript(Reader reader) {
        StringBuilder script = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                script.append(line);
                script.append(LINE_SEPARATOR);
            }
            String command = script.toString().trim();
            notifySql(command);
            executeStatement(command);
            notifySuccess();
            commitConnection();
        } catch (Exception e) {
            notifyError(e.getMessage());
            String message = "Error executing: " + script + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeException(message, e);
        }
    }

    private void executeLineByLine(Reader reader) {
        StringBuilder command = new StringBuilder();
        try {
            BufferedReader lineReader = new BufferedReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                handleLine(command, line);
            }
            commitConnection();
            checkForMissingLineTerminator(command);
        } catch (Exception e) {
            String message = "Error executing: " + command + ".  Cause: " + e;
            printlnError(message);
            throw new RuntimeException(message, e);
        }
    }

    private void setAutoCommit() {
        try {
            if (autoCommit != connection.getAutoCommit()) {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Could not set AutoCommit to " + autoCommit + ". Cause: " + t, t);
        }
    }

    private void commitConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (Throwable t) {
            throw new RuntimeException("Could not commit transaction. Cause: " + t, t);
        }
    }

    private void rollbackConnection() {
        try {
            if (!connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (Throwable t) {
        }
    }

    private void checkForMissingLineTerminator(StringBuilder command) {
        if (command != null && command.toString().trim().length() > 0) {
            throw new RuntimeException("Line missing end-of-line terminator (" + delimiter + ") => " + command);
        }
    }

    private void handleLine(StringBuilder command, String line) throws SQLException {
        String trimmedLine = line.trim();
        if (lineIsComment(trimmedLine)) {
            Matcher matcher = DELIMITER_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                delimiter = matcher.group(5);
            }
            notifyComment(trimmedLine);
        } else if (commandReadyToExecute(trimmedLine)) {
            command.append(line.substring(0, line.lastIndexOf(delimiter)));
            command.append(LINE_SEPARATOR);
            executeStatement(command.toString().trim());
            command.setLength(0);
        } else if (trimmedLine.length() > 0) {
            command.append(line);
            command.append(LINE_SEPARATOR);
        }
    }

    private boolean lineIsComment(String trimmedLine) {
        return trimmedLine.startsWith("//") || trimmedLine.startsWith("--");
    }

    private boolean commandReadyToExecute(String trimmedLine) {
        return !fullLineDelimiter && trimmedLine.contains(delimiter) || fullLineDelimiter && trimmedLine.equals(delimiter);
    }

    private void executeStatement(String command) throws SQLException {
        notifySql(command);

        Statement statement = connection.createStatement();
        try {
            statement.setEscapeProcessing(escapeProcessing);
            String sql = command;
            if (removeCRs) {
                sql = sql.replaceAll("\r\n", "\n");
            }
            try {
                boolean hasResults = statement.execute(sql);

                successCount++;

                if (hasResults) {
//                    notifyResultSet();
                } else {
                    notifyUpdateCount(statement.getUpdateCount());
                }

                while (!(!hasResults && statement.getUpdateCount() == -1)) {
                    checkWarnings(statement);
                    printResults(statement, hasResults);
                    hasResults = statement.getMoreResults();
                }
            } catch (SQLWarning e) {
                warningCount++;
                throw e;
            } catch (SQLException e) {
                errorCount++;

                if (stopOnError) {
                    throw e;
                } else {
                    String message = "Error executing: " + command + ".  Cause: " + e;
                    printlnError(message);
                }
            }
        } finally {
            try {
                statement.close();
            } catch (Exception e) {
                // Ignore to workaround a bug in some connection pools
            }
        }
    }

    private void checkWarnings(Statement statement) throws SQLException {
        if (!throwWarning) {
            return;
        }
        // In Oracle, CREATE PROCEDURE, FUNCTION, etc. returns warning
        // instead of throwing exception if there is compilation error.
        SQLWarning warning = statement.getWarnings();
        if (warning != null) {
            throw warning;
        }
    }

    private void printResults(Statement statement, boolean hasResults) {
        if (!hasResults) {
            return;
        }
        try (ResultSet rs = statement.getResultSet()) {
            ResultSetMetaData md = rs.getMetaData();
            notifyResultSet(md);
            while (rs.next()) {
                notifyRow(rs);
            }
            notifyNoMoreRows();
        } catch (SQLException e) {
            printlnError("Error printing results: " + e.getMessage());
        }
    }

    private void print(Object o) {
        if (logWriter != null) {
            logWriter.print(o);
            logWriter.flush();
        }
    }

    private void println(Object o) {
        if (logWriter != null) {
            logWriter.println(o);
            logWriter.flush();
        }
    }

    private void printlnError(Object o) {
        if (errorLogWriter != null) {
            errorLogWriter.println(o);
            errorLogWriter.flush();
        }
    }

    private void reset() {
        this.successCount = 0;
        this.warningCount = 0;
        this.errorCount = 0;
        this.startTime = 0;
        this.endTime = 0;
    }

    private void notifyBeginScript() {
        notifyListeners(ScriptListener::beginScript);
    }

    private void notifyComment(String comment) {
        notifyListeners(scriptListener -> scriptListener.comment(comment));
    }

    private void notifySql(String sql) {
        notifyListeners(scriptListener -> scriptListener.sql(sql));
    }

    private void notifySuccess() {
        notifyListeners(ScriptListener::success);
    }

    private void notifyError(String error) {
        notifyListeners(scriptListener -> scriptListener.error(error));
    }

    private void notifyUpdateCount(int updateCount) {
        notifyListeners(scriptListener -> scriptListener.updateCount(updateCount));
    }

    private void notifyResultSet(ResultSetMetaData resultSetMetaData) {
        notifyListeners(scriptListener -> scriptListener.resultSet(resultSetMetaData));
    }

    private void notifyRow(ResultSet resultSet) {
        notifyListeners(scriptListener -> scriptListener.row(resultSet));
    }

    private void notifyNoMoreRows() {
        notifyListeners(ScriptListener::noMoreRows);
    }

    private void notifyEndScript(int successCount, int warningCount, int errorCount, long duration) {
        notifyListeners(scriptListener -> scriptListener.endScript(successCount, warningCount, errorCount, duration));
    }

    private void notifyListeners(Consumer<ScriptListener> consumer) {
        for (ScriptListener listener : listenerList) {
            consumer.accept(listener);
        }
    }

}
