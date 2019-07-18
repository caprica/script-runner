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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Empty implementation for a {@link ScriptListener}.
 */
public class ScriptAdapter implements ScriptListener {

    @Override
    public void beginScript() {
    }

    @Override
    public void comment(String comment) {
    }

    @Override
    public void sql(String sql) {
    }

    @Override
    public void success() {
    }

    @Override
    public void error(String error) {
    }

    @Override
    public void updateCount(int updateCount) {
    }

    @Override
    public void resultSet(ResultSetMetaData resultSetMetaData) {
    }

    @Override
    public void row(ResultSet resultSet) {
    }

    @Override
    public void noMoreRows() {
    }

    @Override
    public void endScript(int successCount, int warningCount, int errorCount, long duration) {
    }

}
