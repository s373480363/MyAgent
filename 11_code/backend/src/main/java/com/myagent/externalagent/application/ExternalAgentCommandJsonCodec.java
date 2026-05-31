package com.myagent.externalagent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myagent.common.error.BizException;
import com.myagent.common.error.ErrorCode;
import com.myagent.externalagent.application.command.CreateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentCommand;
import com.myagent.externalagent.application.command.UpdateExternalAgentSecretsCommand;
import com.myagent.externalagent.application.result.ExternalAgentSecretHeaderResult;
import com.myagent.externalagent.domain.ExternalAgentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 外部 Agent commandJson 规范化与脱敏工具。
 */
@Component
public class ExternalAgentCommandJsonCodec {

    /**
     * 敏感 header 名称列表字段。
     */
    private static final String SECRET_HEADER_NAMES_FIELD = "secretHeaderNames";

    /**
     * 敏感 header 值映射字段。
     */
    private static final String SECRET_HEADER_VALUES_FIELD = "secretHeaderValues";

    /**
     * JSON 对象映射器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造工具类。
     *
     * @param objectMapper JSON 对象映射器
     */
    public ExternalAgentCommandJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 规范化创建请求中的 commandJson。
     *
     * @param adapterType 外部 Agent 类型
     * @param commandJson 原始 commandJson
     * @param secretHeaders 敏感 header 请求项
     * @return 规范化后的 commandJson
     */
    public JsonNode normalizeForCreate(
            ExternalAgentType adapterType,
            JsonNode commandJson,
            List<CreateExternalAgentCommand.SecretHeaderItem> secretHeaders
    ) {
        return switch (adapterType) {
            case CUSTOM_HTTP -> normalizeHttpCreate(commandJson, secretHeaders);
            case CODEX_CLI, OPENCODE_CLI, CUSTOM_CLI -> normalizeCliCommand(commandJson, false);
        };
    }

    /**
     * 规范化普通更新请求中的 commandJson。
     *
     * @param adapterType 外部 Agent 类型
     * @param commandJson 原始 commandJson
     * @param secretHeaders 敏感 header 定义
     * @param existingCommandJson 现有 commandJson
     * @return 规范化后的 commandJson
     */
    public JsonNode normalizeForUpdate(
            ExternalAgentType adapterType,
            JsonNode commandJson,
            List<UpdateExternalAgentCommand.SecretHeaderItem> secretHeaders,
            JsonNode existingCommandJson
    ) {
        return switch (adapterType) {
            case CUSTOM_HTTP -> normalizeHttpUpdate(commandJson, secretHeaders, existingCommandJson);
            case CODEX_CLI, OPENCODE_CLI, CUSTOM_CLI -> {
                validateNoSecretHeaders(secretHeaders);
                yield normalizeCliCommand(commandJson, false);
            }
        };
    }

    /**
     * 更新敏感 secret。
     *
     * @param existingCommandJson 现有 commandJson
     * @param items 覆盖写入项
     * @param clearHeaderNames 显式清空 header 名称列表
     * @return 更新后的 commandJson
     */
    public JsonNode updateSecrets(
            JsonNode existingCommandJson,
            List<UpdateExternalAgentSecretsCommand.Item> items,
            List<String> clearHeaderNames
    ) {
        ObjectNode root = requireObject(existingCommandJson, "commandJson");
        LinkedHashMap<String, String> plainHeaders = readStringMap(root.path("headers"), "headers");
        List<String> secretHeaderNames = readSecretHeaderNames(root);
        LinkedHashMap<String, String> secretValues = readSecretHeaderValues(root);

        for (String clearHeaderName : clearHeaderNames == null ? List.<String>of() : clearHeaderNames) {
            String normalized = normalizeHeaderName(clearHeaderName);
            secretHeaderNames.removeIf(name -> normalizeHeaderName(name).equals(normalized));
            secretValues.entrySet().removeIf(entry -> normalizeHeaderName(entry.getKey()).equals(normalized));
        }

        for (UpdateExternalAgentSecretsCommand.Item item : items == null ? List.<UpdateExternalAgentSecretsCommand.Item>of() : items) {
            String headerName = normalizeRequiredHeaderName(item.headerName());
            ensureHeaderNotDuplicatedWithPlainHeaders(plainHeaders, headerName);
            if (item.secretValue() == null || item.secretValue().isBlank()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "敏感 header 的 secretValue 不能为空。");
            }
            if (secretHeaderNames.stream().noneMatch(name -> normalizeHeaderName(name).equals(normalizeHeaderName(headerName)))) {
                secretHeaderNames.add(headerName);
            }
            removeByNormalizedKey(secretValues, headerName);
            secretValues.put(headerName, item.secretValue());
        }

        ObjectNode normalized = normalizeHttpStructure(root);
        writeSecretHeaders(normalized, secretHeaderNames, secretValues);
        return normalized;
    }

    /**
     * 生成详情接口使用的脱敏 commandJson。
     *
     * @param adapterType 外部 Agent 类型
     * @param storedCommandJson 持久化 commandJson
     * @return 脱敏后的 commandJson
     */
    public JsonNode sanitizeForResponse(ExternalAgentType adapterType, JsonNode storedCommandJson) {
        if (storedCommandJson == null || storedCommandJson.isNull()) {
            return objectMapper.createObjectNode();
        }
        ObjectNode copy = requireObject(storedCommandJson.deepCopy(), "commandJson");
        if (adapterType == ExternalAgentType.CUSTOM_HTTP) {
            copy.remove(SECRET_HEADER_NAMES_FIELD);
            copy.remove(SECRET_HEADER_VALUES_FIELD);
        }
        return copy;
    }

    /**
     * 读取敏感 header 元信息列表。
     *
     * @param adapterType 外部 Agent 类型
     * @param storedCommandJson 持久化 commandJson
     * @return 敏感 header 元信息
     */
    public List<ExternalAgentSecretHeaderResult> listSecretHeaderMetas(ExternalAgentType adapterType, JsonNode storedCommandJson) {
        if (adapterType != ExternalAgentType.CUSTOM_HTTP || storedCommandJson == null || storedCommandJson.isNull()) {
            return List.of();
        }
        ObjectNode root = requireObject(storedCommandJson, "commandJson");
        List<String> secretHeaderNames = readSecretHeaderNames(root);
        Map<String, String> secretValues = readSecretHeaderValues(root);
        return secretHeaderNames.stream()
                .map(headerName -> new ExternalAgentSecretHeaderResult(
                        headerName,
                        findByNormalizedKey(secretValues, headerName) != null
                ))
                .toList();
    }

    /**
     * 断言敏感 header 已全部配置。
     *
     * @param adapterType 外部 Agent 类型
     * @param storedCommandJson 持久化 commandJson
     */
    public void assertSecretsConfigured(ExternalAgentType adapterType, JsonNode storedCommandJson) {
        if (adapterType != ExternalAgentType.CUSTOM_HTTP) {
            return;
        }
        ObjectNode root = requireObject(storedCommandJson, "commandJson");
        List<String> secretHeaderNames = readSecretHeaderNames(root);
        Map<String, String> secretValues = readSecretHeaderValues(root);
        for (String headerName : secretHeaderNames) {
            if (findByNormalizedKey(secretValues, headerName) == null) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "敏感 header " + headerName + " 尚未配置密钥，无法发起外部 Agent 调用。");
            }
        }
    }

    /**
     * 合并 HTTP 普通 header 和敏感 header。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return 合并后的 header 映射
     */
    public LinkedHashMap<String, String> mergeHttpHeaders(JsonNode storedCommandJson) {
        ObjectNode root = requireObject(storedCommandJson, "commandJson");
        LinkedHashMap<String, String> merged = readStringMap(root.path("headers"), "headers");
        readSecretHeaderValues(root).forEach(merged::put);
        return merged;
    }

    /**
     * 读取 resultSource 类型。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return resultSource 类型
     */
    public String getResultSourceType(JsonNode storedCommandJson) {
        JsonNode typeNode = requireObject(storedCommandJson, "commandJson").path("resultSource").path("type");
        if (typeNode.isTextual()) {
            return typeNode.asText();
        }
        return "";
    }

    /**
     * 读取 CLI command。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return CLI command
     */
    public String getCliCommand(JsonNode storedCommandJson) {
        return requireObject(storedCommandJson, "commandJson").path("command").asText("");
    }

    /**
     * 读取 CLI arguments。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return CLI arguments
     */
    public List<String> getCliArguments(JsonNode storedCommandJson) {
        ArrayNode argumentsNode = requireArray(requireObject(storedCommandJson, "commandJson").path("arguments"), "arguments");
        List<String> arguments = new ArrayList<>();
        argumentsNode.forEach(item -> {
            if (!item.isTextual()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "CLI arguments 必须全部为字符串。");
            }
            arguments.add(item.asText());
        });
        return arguments;
    }

    /**
     * 读取 CLI environment。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return environment 映射
     */
    public Map<String, String> getCliEnvironment(JsonNode storedCommandJson) {
        return readStringMap(requireObject(storedCommandJson, "commandJson").path("environment"), "environment");
    }

    /**
     * 读取 HTTP 方法。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return HTTP 方法
     */
    public String getHttpMethod(JsonNode storedCommandJson) {
        return requireObject(storedCommandJson, "commandJson").path("method").asText("POST");
    }

    /**
     * 读取 HTTP URL。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return HTTP URL
     */
    public String getHttpUrl(JsonNode storedCommandJson) {
        return requireObject(storedCommandJson, "commandJson").path("url").asText("");
    }

    /**
     * 读取 HTTP bodyTemplate。
     *
     * @param storedCommandJson 持久化 commandJson
     * @return bodyTemplate
     */
    public JsonNode getHttpBodyTemplate(JsonNode storedCommandJson) {
        JsonNode bodyTemplate = requireObject(storedCommandJson, "commandJson").path("bodyTemplate");
        if (bodyTemplate.isMissingNode() || bodyTemplate.isNull()) {
            return objectMapper.createObjectNode();
        }
        return bodyTemplate.deepCopy();
    }

    /**
     * 规范化 CLI commandJson。
     *
     * @param commandJson 原始 commandJson
     * @param allowMissingArguments 是否允许缺省 arguments
     * @return 规范化后的 commandJson
     */
    private JsonNode normalizeCliCommand(JsonNode commandJson, boolean allowMissingArguments) {
        ObjectNode root = requireObject(commandJson, "commandJson");
        String command = requireNonBlankText(root.path("command"), "command");
        ArrayNode argumentsNode = requireArray(root.path("arguments"), "arguments");
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("command", command);
        ArrayNode normalizedArguments = normalized.putArray("arguments");
        argumentsNode.forEach(item -> {
            if (!item.isTextual()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "CLI arguments 必须全部为字符串。");
            }
            normalizedArguments.add(item.asText());
        });
        if (!allowMissingArguments && normalizedArguments.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "CLI arguments 不能为空数组。");
        }
        normalized.set("resultSource", normalizeResultSource(root.path("resultSource")));
        normalized.set("environment", objectMapper.valueToTree(readStringMap(root.path("environment"), "environment")));
        return normalized;
    }

    /**
     * 规范化创建时的 HTTP commandJson。
     *
     * @param commandJson 原始 commandJson
     * @param secretHeaders 敏感 header 请求项
     * @return 规范化后的 commandJson
     */
    private JsonNode normalizeHttpCreate(
            JsonNode commandJson,
            List<CreateExternalAgentCommand.SecretHeaderItem> secretHeaders
    ) {
        ObjectNode normalized = normalizeHttpStructure(requireObject(commandJson, "commandJson"));
        List<String> secretHeaderNames = new ArrayList<>();
        LinkedHashMap<String, String> secretValues = new LinkedHashMap<>();
        for (CreateExternalAgentCommand.SecretHeaderItem item : secretHeaders == null
                ? List.<CreateExternalAgentCommand.SecretHeaderItem>of()
                : secretHeaders) {
            String headerName = normalizeRequiredHeaderName(item.headerName());
            ensureHeaderNotDuplicatedWithPlainHeaders(readStringMap(normalized.path("headers"), "headers"), headerName);
            if (secretHeaderNames.stream().noneMatch(name -> normalizeHeaderName(name).equals(normalizeHeaderName(headerName)))) {
                secretHeaderNames.add(headerName);
            }
            if (item.secretValue() != null && !item.secretValue().isBlank()) {
                removeByNormalizedKey(secretValues, headerName);
                secretValues.put(headerName, item.secretValue());
            }
        }
        writeSecretHeaders(normalized, secretHeaderNames, secretValues);
        return normalized;
    }

    /**
     * 规范化更新时的 HTTP commandJson。
     *
     * @param commandJson 原始 commandJson
     * @param secretHeaders 敏感 header 定义
     * @param existingCommandJson 现有 commandJson
     * @return 规范化后的 commandJson
     */
    private JsonNode normalizeHttpUpdate(
            JsonNode commandJson,
            List<UpdateExternalAgentCommand.SecretHeaderItem> secretHeaders,
            JsonNode existingCommandJson
    ) {
        ObjectNode normalized = normalizeHttpStructure(requireObject(commandJson, "commandJson"));
        ObjectNode existing = requireObject(existingCommandJson, "existingCommandJson");
        List<String> effectiveHeaderNames;
        if (secretHeaders == null) {
            effectiveHeaderNames = readSecretHeaderNames(existing);
        } else {
            effectiveHeaderNames = new ArrayList<>();
            for (UpdateExternalAgentCommand.SecretHeaderItem item : secretHeaders) {
                String headerName = normalizeRequiredHeaderName(item.headerName());
                if (item.secretValue() != null && !item.secretValue().isBlank()) {
                    throw new BizException(ErrorCode.INVALID_ARGUMENT, "普通更新接口不允许提交敏感 header secret，请改用 /api/external-agents/{adapterId}/secrets。");
                }
                ensureHeaderNotDuplicatedWithPlainHeaders(readStringMap(normalized.path("headers"), "headers"), headerName);
                if (effectiveHeaderNames.stream().noneMatch(name -> normalizeHeaderName(name).equals(normalizeHeaderName(headerName)))) {
                    effectiveHeaderNames.add(headerName);
                }
            }
        }
        LinkedHashMap<String, String> existingSecretValues = readSecretHeaderValues(existing);
        LinkedHashMap<String, String> effectiveSecretValues = new LinkedHashMap<>();
        for (String headerName : effectiveHeaderNames) {
            String existingValue = findByNormalizedKey(existingSecretValues, headerName);
            if (existingValue != null) {
                effectiveSecretValues.put(headerName, existingValue);
            }
        }
        writeSecretHeaders(normalized, effectiveHeaderNames, effectiveSecretValues);
        return normalized;
    }

    /**
     * 校验 secretHeaders 未被错误传入。
     *
     * @param secretHeaders 敏感 header 定义
     */
    private void validateNoSecretHeaders(List<?> secretHeaders) {
        if (secretHeaders != null && !secretHeaders.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "只有 CUSTOM_HTTP 支持敏感 header 配置。");
        }
    }

    /**
     * 规范化 HTTP 基础结构。
     *
     * @param root 原始 JSON 对象
     * @return 规范化后的 JSON 对象
     */
    private ObjectNode normalizeHttpStructure(ObjectNode root) {
        ObjectNode normalized = objectMapper.createObjectNode();
        normalized.put("method", requireNonBlankText(root.path("method"), "method"));
        normalized.put("url", requireNonBlankText(root.path("url"), "url"));
        normalized.set("headers", objectMapper.valueToTree(readStringMap(root.path("headers"), "headers")));
        JsonNode bodyTemplate = root.path("bodyTemplate");
        normalized.set("bodyTemplate", bodyTemplate.isMissingNode() || bodyTemplate.isNull()
                ? objectMapper.createObjectNode()
                : bodyTemplate.deepCopy());
        normalized.set("resultSource", normalizeResultSource(root.path("resultSource")));
        return normalized;
    }

    /**
     * 规范化 resultSource。
     *
     * @param resultSourceNode 原始 resultSource
     * @return 规范化后的 resultSource
     */
    private JsonNode normalizeResultSource(JsonNode resultSourceNode) {
        if (resultSourceNode == null || resultSourceNode.isMissingNode() || resultSourceNode.isNull()) {
            return objectMapper.createObjectNode();
        }
        if (!resultSourceNode.isObject()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "resultSource 必须是对象。");
        }
        return resultSourceNode.deepCopy();
    }

    /**
     * 写入敏感 header 内部字段。
     *
     * @param root 目标 JSON 对象
     * @param headerNames header 名称列表
     * @param secretValues secret 值映射
     */
    private void writeSecretHeaders(ObjectNode root, List<String> headerNames, LinkedHashMap<String, String> secretValues) {
        ArrayNode namesNode = objectMapper.createArrayNode();
        Set<String> normalizedNames = new LinkedHashSet<>();
        for (String headerName : headerNames) {
            String normalized = normalizeHeaderName(headerName);
            if (normalizedNames.add(normalized)) {
                namesNode.add(headerName);
            }
        }
        root.set(SECRET_HEADER_NAMES_FIELD, namesNode);
        ObjectNode valuesNode = objectMapper.createObjectNode();
        secretValues.forEach(valuesNode::put);
        root.set(SECRET_HEADER_VALUES_FIELD, valuesNode);
    }

    /**
     * 读取敏感 header 名称列表。
     *
     * @param root JSON 对象
     * @return 名称列表
     */
    private List<String> readSecretHeaderNames(ObjectNode root) {
        ArrayNode arrayNode = requireArray(root.path(SECRET_HEADER_NAMES_FIELD), SECRET_HEADER_NAMES_FIELD, true);
        List<String> result = new ArrayList<>();
        if (arrayNode == null) {
            return result;
        }
        arrayNode.forEach(item -> {
            if (!item.isTextual()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "敏感 header 名称必须为字符串。");
            }
            String headerName = normalizeRequiredHeaderName(item.asText());
            if (result.stream().noneMatch(name -> normalizeHeaderName(name).equals(normalizeHeaderName(headerName)))) {
                result.add(headerName);
            }
        });
        return result;
    }

    /**
     * 读取敏感 header 值映射。
     *
     * @param root JSON 对象
     * @return secret 值映射
     */
    private LinkedHashMap<String, String> readSecretHeaderValues(ObjectNode root) {
        return readStringMap(root.path(SECRET_HEADER_VALUES_FIELD), SECRET_HEADER_VALUES_FIELD);
    }

    /**
     * 读取字符串对象映射。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 字符串映射
     */
    private LinkedHashMap<String, String> readStringMap(JsonNode node, String fieldName) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return result;
        }
        if (!node.isObject()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 必须是对象。");
        }
        node.fields().forEachRemaining(entry -> {
            String headerName = normalizeRequiredHeaderName(entry.getKey());
            if (!entry.getValue().isTextual()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 中的值必须为字符串。");
            }
            ensureNoDuplicateKey(result, headerName, fieldName);
            result.put(headerName, entry.getValue().asText());
        });
        return result;
    }

    /**
     * 读取对象节点。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 对象节点
     */
    private ObjectNode requireObject(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 必须是对象。");
        }
        return (ObjectNode) node;
    }

    /**
     * 读取数组节点。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 数组节点
     */
    private ArrayNode requireArray(JsonNode node, String fieldName) {
        return requireArray(node, fieldName, false);
    }

    /**
     * 读取数组节点。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @param allowMissing 是否允许缺省
     * @return 数组节点
     */
    private ArrayNode requireArray(JsonNode node, String fieldName, boolean allowMissing) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            if (allowMissing) {
                return null;
            }
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 必须是数组。");
        }
        if (!node.isArray()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 必须是数组。");
        }
        return (ArrayNode) node;
    }

    /**
     * 读取必填文本值。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 文本值
     */
    private String requireNonBlankText(JsonNode node, String fieldName) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 不能为空。");
        }
        return node.asText();
    }

    /**
     * 规范化必填 header 名称。
     *
     * @param headerName header 名称
     * @return 规范化后的名称
     */
    private String normalizeRequiredHeaderName(String headerName) {
        if (headerName == null || headerName.isBlank()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "headerName 不能为空。");
        }
        return headerName.trim();
    }

    /**
     * 规范化 header 名称用于去重比较。
     *
     * @param headerName header 名称
     * @return 规范化名称
     */
    private String normalizeHeaderName(String headerName) {
        return normalizeRequiredHeaderName(headerName).toLowerCase(Locale.ROOT);
    }

    /**
     * 校验普通 header 和敏感 header 不冲突。
     *
     * @param plainHeaders 普通 header
     * @param headerName 敏感 header 名称
     */
    private void ensureHeaderNotDuplicatedWithPlainHeaders(Map<String, String> plainHeaders, String headerName) {
        if (findByNormalizedKey(plainHeaders, headerName) != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "同一个 header 不能同时作为普通 header 和敏感 header。");
        }
    }

    /**
     * 校验对象内部键名不重复。
     *
     * @param map 目标映射
     * @param key 待写入键
     * @param fieldName 字段名
     */
    private void ensureNoDuplicateKey(Map<String, String> map, String key, String fieldName) {
        if (findByNormalizedKey(map, key) != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " 中存在重复 header 名称。");
        }
    }

    /**
     * 按大小写不敏感方式查找映射值。
     *
     * @param map 目标映射
     * @param key 键
     * @return 命中的值
     */
    private String findByNormalizedKey(Map<String, String> map, String key) {
        String normalized = normalizeHeaderName(key);
        return map.entrySet().stream()
                .filter(entry -> normalizeHeaderName(entry.getKey()).equals(normalized))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 按大小写不敏感方式删除映射中的键。
     *
     * @param map 目标映射
     * @param key 键
     */
    private void removeByNormalizedKey(Map<String, String> map, String key) {
        String normalized = normalizeHeaderName(key);
        map.entrySet().removeIf(entry -> normalizeHeaderName(entry.getKey()).equals(normalized));
    }
}
