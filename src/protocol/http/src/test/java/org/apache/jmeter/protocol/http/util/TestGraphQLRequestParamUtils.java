/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.protocol.http.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.config.GraphQLRequestParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.jknack.handlebars.internal.lang3.StringUtils;

public class TestGraphQLRequestParamUtils {

    private static final String OPERATION_NAME = "";

    private static final String QUERY =
            "query($id: ID!) {\n"
            + "  droid(id: $id) {\n"
            + "    id\n"
            + "    name\n"
            + "    friends {\n"
            + "      id\n"
            + "      name\n"
            + "      appearsIn\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String VARIABLES =
            "{\n"
            + "  \"id\": \"2001\"\n"
            + "}\n";

    private static final String EXPECTED_QUERY_GET_PARAM_VALUE =
            "query($id: ID!) { droid(id: $id) { id name friends { id name appearsIn } } }";

    private static final String EXPECTED_VARIABLES_GET_PARAM_VALUE = "{\"id\":\"2001\"}";

    private static final String EXPECTED_POST_BODY =
            "{"
            + "\"operationName\":null,"
            + "\"variables\":" + EXPECTED_VARIABLES_GET_PARAM_VALUE + ","
            + "\"query\":\"" + StringUtils.replace(QUERY.trim(), "\n", "\\n") + "\""
            + "}";

    private GraphQLRequestParams params;

    @BeforeEach
    public void setUp() {
        params = new GraphQLRequestParams(OPERATION_NAME, QUERY, VARIABLES);
    }

    @Test
    public void testIsGraphQLContentType() throws Exception {
        assertTrue(GraphQLRequestParamUtils.isGraphQLContentType("application/json"));
        assertTrue(GraphQLRequestParamUtils.isGraphQLContentType("application/json;charset=utf-8"));
        assertTrue(GraphQLRequestParamUtils.isGraphQLContentType("application/json; charset=utf-8"));

        assertFalse(GraphQLRequestParamUtils.isGraphQLContentType("application/vnd.api+json"));
        assertFalse(GraphQLRequestParamUtils.isGraphQLContentType("application/json-patch+json"));
        assertFalse(GraphQLRequestParamUtils.isGraphQLContentType(""));
        assertFalse(GraphQLRequestParamUtils.isGraphQLContentType(null));
    }

    @Test
    public void testToPostBodyString() throws Exception {
        assertEquals(EXPECTED_POST_BODY, GraphQLRequestParamUtils.toPostBodyString(params));
    }

    @Test
    public void testQueryToGetParamValue() throws Exception {
        assertEquals(EXPECTED_QUERY_GET_PARAM_VALUE, GraphQLRequestParamUtils.queryToGetParamValue(params.getQuery()));
    }

    @Test
    public void testVariablesToGetParamValue() throws Exception {
        assertEquals(EXPECTED_VARIABLES_GET_PARAM_VALUE,
                GraphQLRequestParamUtils.variablesToGetParamValue(params.getVariables()));
    }

    @Test
    public void testToGraphQLRequestParamsWithPostData() throws Exception {
        GraphQLRequestParams params = GraphQLRequestParamUtils
                .toGraphQLRequestParams(EXPECTED_POST_BODY.getBytes(StandardCharsets.UTF_8), null);
        assertNull(params.getOperationName());
        assertEquals(QUERY.trim(), params.getQuery());
        assertEquals(EXPECTED_VARIABLES_GET_PARAM_VALUE, params.getVariables());

        params = GraphQLRequestParamUtils.toGraphQLRequestParams(
                "{\"operationName\":\"op1\",\"variables\":{\"id\":123},\"query\":\"query { droid { id }}\"}"
                        .getBytes(StandardCharsets.UTF_8),
                null);
        assertEquals("op1", params.getOperationName());
        assertEquals("query { droid { id }}", params.getQuery());
        assertEquals("{\"id\":123}", params.getVariables());

        try {
            params = GraphQLRequestParamUtils.toGraphQLRequestParams("".getBytes(StandardCharsets.UTF_8), null);
            fail("Should have failed due to invalid json data.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            params = GraphQLRequestParamUtils.toGraphQLRequestParams("{}".getBytes(StandardCharsets.UTF_8), null);
            fail("Should have failed due to invalid json data.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            params = GraphQLRequestParamUtils
                    .toGraphQLRequestParams("{\"query\":\"select * from emp\"}".getBytes(StandardCharsets.UTF_8), null);
            fail("Should have failed due to invalid graph query param.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            params = GraphQLRequestParamUtils
                    .toGraphQLRequestParams("{\"operationName\":{\"id\":123},\"query\":\"query { droid { id }}\"}"
                            .getBytes(StandardCharsets.UTF_8), null);
            fail("Should have failed due to invalid graph operationName type.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            params = GraphQLRequestParamUtils.toGraphQLRequestParams(
                    "{\"variables\":\"r2d2\",\"query\":\"query { droid { id }}\"}".getBytes(StandardCharsets.UTF_8),
                    null);
            fail("Should have failed due to invalid graph variables type.");
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Test
    public void testToGraphQLRequestParamsWithHttpArguments() throws Exception {
        Arguments args = new Arguments();
        args.addArgument(new HTTPArgument("query", "query { droid { id }}", "=", false));
        GraphQLRequestParams params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
        assertNull(params.getOperationName());
        assertEquals("query { droid { id }}", params.getQuery());
        assertNull(params.getVariables());

        args = new Arguments();
        args.addArgument(new HTTPArgument("operationName", "op1", "=", false));
        args.addArgument(new HTTPArgument("query", "query { droid { id }}", "=", false));
        args.addArgument(new HTTPArgument("variables", "{\"id\":123}", "=", false));
        params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
        assertEquals("op1", params.getOperationName());
        assertEquals("query { droid { id }}", params.getQuery());
        assertEquals("{\"id\":123}", params.getVariables());

        args = new Arguments();
        args.addArgument(new HTTPArgument("query", "query+%7B+droid+%7B+id+%7D%7D", "=", true));
        params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
        assertNull(params.getOperationName());
        assertEquals("query { droid { id }}", params.getQuery());
        assertNull(params.getVariables());

        args = new Arguments();
        args.addArgument(new HTTPArgument("query", "query%20%7B%20droid%20%7B%20id%20%7D%7D", "=", true));
        params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
        assertNull(params.getOperationName());
        assertEquals("query { droid { id }}", params.getQuery());
        assertNull(params.getVariables());

        try {
            args = new Arguments();
            params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
            fail("Should have failed due to missing GraphQL parameters.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            args = new Arguments();
            args.addArgument(new HTTPArgument("query", "select * from emp", "=", false));
            params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
            fail("Should have failed due to invalid graph query param.");
        } catch (IllegalArgumentException ignore) {
        }

        try {
            args = new Arguments();
            args.addArgument(new HTTPArgument("query", "query { droid { id }}", "=", false));
            args.addArgument(new HTTPArgument("variables", "r2d2", "=", false));
            params = GraphQLRequestParamUtils.toGraphQLRequestParams(args, null);
            fail("Should have failed due to invalid graph query param.");
        } catch (IllegalArgumentException ignore) {
        }
    }
}
