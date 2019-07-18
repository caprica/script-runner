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
 * Specification for a component interested in script events.
 */
public interface ScriptListener {

    void beginScript();

    void comment(String comment);

    void sql(String sql);

    void success();

    void error(String error);

    void updateCount(int updateCount);

    void resultSet(ResultSetMetaData resultSetMetaData);

    void row(ResultSet resultSet);

    void noMoreRows();

    void endScript(int updateCount, int successCount, int warningCount, int errorCount, long duration);

}
