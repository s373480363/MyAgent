package com.myagent.externalagent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myagent.common.domain.EnableStatus;
import com.myagent.externalagent.application.result.ExternalAgentTestResult;
import com.myagent.externalagent.domain.ExternalAgentType;
import com.myagent.externalagent.repository.ExternalAgentRecord;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 外部 Agent 测试执行器测试。
 */
class ExternalAgentTestExecutorTests {

    /**
     * JSON 对象映射器。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * HTTP 目标不可达时，错误摘要必须可定位且不能拼出 null。
     *
     * @throws Exception 构造请求失败时抛出
     */
    @Test
    void httpFailureSummaryIncludesReadableCauseAndTarget() throws Exception {
        ExternalAgentTestExecutor executor = new ExternalAgentTestExecutor(
                OBJECT_MAPPER,
                new ExternalAgentCommandJsonCodec(OBJECT_MAPPER)
        );
        int port = unusedPort();

        ExternalAgentTestResult result = executor.execute(
                httpRecord("http://127.0.0.1:" + port + "/run", "HTTP_BODY_JSON"),
                "smoke",
                OBJECT_MAPPER.createObjectNode()
        );

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage())
                .isNotBlank()
                .contains("调用外部 HTTP Agent 失败")
                .contains("127.0.0.1:" + port + "/run")
                .doesNotContain("null");
    }

    /**
     * 目标返回非法 JSON 时，错误摘要必须明确指出解析失败。
     *
     * @throws Exception 启停本地 HTTP 服务失败时抛出
     */
    @Test
    void httpJsonParseFailureIsReportedClearly() throws Exception {
        ExternalAgentTestExecutor executor = new ExternalAgentTestExecutor(
                OBJECT_MAPPER,
                new ExternalAgentCommandJsonCodec(OBJECT_MAPPER)
        );
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/run", new PlainTextHandler("not-json"));
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/run";

            ExternalAgentTestResult result = executor.execute(
                    httpRecord(url, "HTTP_BODY_JSON"),
                    "smoke",
                    OBJECT_MAPPER.createObjectNode()
            );

            assertThat(result.success()).isFalse();
            assertThat(result.httpStatus()).isEqualTo(200);
            assertThat(result.summary()).isEqualTo("not-json");
            assertThat(result.errorMessage())
                    .isNotBlank()
                    .contains("输出解析失败")
                    .contains(url);
        } finally {
            server.stop(0);
        }
    }

    /**
     * 构造 HTTP 外部 Agent 记录。
     *
     * @param url HTTP 地址
     * @param resultSourceType 结果来源类型
     * @return 外部 Agent 记录
     * @throws Exception JSON 构造失败时抛出
     */
    private ExternalAgentRecord httpRecord(String url, String resultSourceType) throws Exception {
        return new ExternalAgentRecord(
                1L,
                "custom-http",
                ExternalAgentType.CUSTOM_HTTP,
                "HTTP Agent",
                "",
                OBJECT_MAPPER.readTree("""
                        {
                          "method": "POST",
                          "url": "%s",
                          "headers": {
                            "Content-Type": "application/json"
                          },
                          "bodyTemplate": {
                            "prompt": "{prompt}"
                          },
                          "resultSource": {
                            "type": "%s"
                          },
                          "secretHeaderNames": [],
                          "secretHeaderValues": {}
                        }
                        """.formatted(url, resultSourceType)),
                "",
                5,
                false,
                false,
                false,
                null,
                EnableStatus.ENABLED,
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * 申请一个未占用的本地端口。
     *
     * @return 未占用端口
     * @throws IOException 端口申请失败时抛出
     */
    private int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * 纯文本响应处理器。
     */
    private static final class PlainTextHandler implements HttpHandler {

        /**
         * 响应体。
         */
        private final String body;

        /**
         * 构造纯文本响应处理器。
         *
         * @param body 响应体
         */
        private PlainTextHandler(String body) {
            this.body = body;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
