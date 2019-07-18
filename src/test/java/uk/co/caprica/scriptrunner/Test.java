package uk.co.caprica.scriptrunner;

import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;

public class Test {

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost/mydb", "username", "password")) {
            ScriptRunner scriptRunner = new ScriptRunner(connection);
            scriptRunner.setAutoCommit(true);
            scriptRunner.setSendFullScript(false);
            scriptRunner.addScriptListener(new DefaultScriptListener(new PrintWriter(new OutputStreamWriter(System.out), true)));
            scriptRunner.runScript(new FileReader("test.sql"));
        }
    }

}
