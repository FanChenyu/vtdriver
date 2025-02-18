/*
Copyright 2021 JD Project Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package testsuite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.util.internal.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import testsuite.internal.TestSuiteShardSpec;
import testsuite.internal.environment.DriverEnv;
import testsuite.internal.environment.TestSuiteEnv;
import testsuite.internal.testcase.TestSuiteCase;
import testsuite.util.printer.TestSuitePrinter;

public abstract class TestSuite extends TestSuitePrinter {

    /**
     * <p>
     * get Connection by <code>TestSuiteEnv</code>
     * </p>
     *
     * @param env {@link TestSuiteEnv}
     * @return {@link Connection}
     * @throws SQLException when errors occurred
     */
    protected static Connection getConnection(TestSuiteEnv env) throws SQLException {
        return env.getDevConnection();
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException throwables) {
                printFail(throwables.getMessage());
            }
        }
    }

    protected static <T extends TestSuiteCase> List<T> iterateExecFile(String filename, Class<T> caseClazz) throws IOException {
        BufferedReader br = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8);
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            content.append(line.trim());
        }
        if (StringUtil.isNullOrEmpty(content.toString())) {
            return Collections.emptyList();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        List<T> testCaseList = objectMapper.readValue(content.toString(),
            typeFactory.constructCollectionType(List.class, caseClazz));
        testCaseList.forEach(testCase -> testCase.setFile(filename));
        return testCaseList;
    }

    protected static String getConnectionUrl(TestSuiteEnv env) {
        return env.getDevConnectionUrl();
    }

    protected <T extends TestSuiteCase> List<T> initCase(String filename, Class<T> caseClazz, String keyspace) throws IOException {
        List<T> testCaseList = iterateExecFile(filename, caseClazz);
        replaceKeyspace(testCaseList, keyspace);
        return testCaseList;
    }

    protected void replaceKeyspace(List<? extends TestSuiteCase> caseList, String keyspace) {
        for (TestSuiteCase testCase : caseList) {
            String query = testCase.getQuery().replaceAll(":KS|:ks", keyspace);
            testCase.setQuery(query);
        }
    }

    public void sleep(long millsec) {
        try {
            TimeUnit.SECONDS.sleep(millsec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class Driver {
        public static DriverEnv of(TestSuiteShardSpec shardSpec) {
            return new DriverEnv(shardSpec);
        }

        public static DriverEnv of(TestSuiteShardSpec shardSpec, String charEncoding) {
            return new DriverEnv(shardSpec, charEncoding);
        }
    }
}
