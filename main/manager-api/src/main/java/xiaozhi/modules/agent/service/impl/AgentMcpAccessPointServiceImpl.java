package xiaozhi.modules.agent.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.utils.AESUtils;
import xiaozhi.common.utils.JsonUtils;
import xiaozhi.modules.agent.Enums.XiaoZhiMcpJsonRpcJson;
import xiaozhi.modules.agent.service.AgentMcpAccessPointService;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.utils.WebSocketClientManager;

@AllArgsConstructor
@Service
@Slf4j
public class AgentMcpAccessPointServiceImpl implements AgentMcpAccessPointService {
    private SysParamsService sysParamsService;

    @Override
    public String getAgentMcpAccessAddress(String id) {
        // Получить адрес MCP
        String url = sysParamsService.getValue(Constant.SERVER_MCP_ENDPOINT, true);
        if (StringUtils.isBlank(url) || "null".equals(url)) {
            return null;
        }
        URI uri = getURI(url);
        // Получить префикс URL MCP агента
        String agentMcpUrl = getAgentMcpUrl(uri);
        // Получить ключ
        String key = getSecretKey(uri);
        // Получить зашифрованный токен
        String encryptToken = encryptToken(id, key);
        // Выполнить URL-кодирование токена
        String encodedToken = URLEncoder.encode(encryptToken, StandardCharsets.UTF_8);
        // Вернуть формат пути MCP агента
        agentMcpUrl = "%s/mcp/?token=%s".formatted(agentMcpUrl, encodedToken);
        return agentMcpUrl;
    }

    @Override
    public List<String> getAgentMcpToolsList(String id) {
        String wsUrl = getAgentMcpAccessAddress(id);
        if (StringUtils.isBlank(wsUrl)) {
            return List.of();
        }

        // Заменить /mcp на /call
        wsUrl = wsUrl.replace("/mcp/", "/call/");

        try {
            // Создать WebSocket соединение, увеличить тайм-аут до 15 секунд
            try (WebSocketClientManager client = WebSocketClientManager.build(
                    new WebSocketClientManager.Builder()
                            .uri(wsUrl)
                            .bufferSize(1024 * 1024)
                            .connectTimeout(8, TimeUnit.SECONDS)
                            .maxSessionDuration(10, TimeUnit.SECONDS))) {

                // Шаг 1: Отправить сообщение инициализации и ожидать ответа
                log.info("发送MCP初始化消息，智能体ID: {}", id);
                client.sendText(XiaoZhiMcpJsonRpcJson.getInitializeJson());

                // Ожидание ответа инициализации (id=1) - убрана фиксированная задержка, переход на ответное управление
                List<String> initResponses = client.listenerWithoutClose(response -> {
                    try {
                        Map<String, Object> jsonMap = JsonUtils.parseMap(response);
                        if (jsonMap != null && Integer.valueOf(1).equals(jsonMap.get("id"))) {
                            // Проверить наличие поля result, указывающего на успешную инициализацию
                            return jsonMap.containsKey("result") && !jsonMap.containsKey("error");
                        }
                        return false;
                    } catch (Exception e) {
                        log.warn("解析初始化响应失败: {}", response, e);
                        return false;
                    }
                });

                // Проверка ответа инициализации
                boolean initSucceeded = false;
                for (String response : initResponses) {
                    try {
                        Map<String, Object> jsonMap = JsonUtils.parseMap(response);
                        if (jsonMap != null && Integer.valueOf(1).equals(jsonMap.get("id"))) {
                            if (jsonMap.containsKey("result")) {
                                log.info("MCP初始化成功，智能体ID: {}", id);
                                initSucceeded = true;
                                break;
                            } else if (jsonMap.containsKey("error")) {
                                log.error("MCP初始化失败，智能体ID: {}, 错误: {}", id, jsonMap.get("error"));
                                return List.of();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("处理初始化响应失败: {}", response, e);
                    }
                }

                if (!initSucceeded) {
                    log.error("未收到有效的MCP初始化响应，智能体ID: {}", id);
                    return List.of();
                }

                // Шаг 2: Отправить уведомление о завершении инициализации - только после получения ответа initialize
                log.info("发送MCP初始化完成通知，智能体ID: {}", id);
                client.sendText(XiaoZhiMcpJsonRpcJson.getNotificationsInitializedJson());
                // Шаг 3: Запросить список инструментов - отправить сразу, без дополнительной задержки
                log.info("发送MCP工具列表请求，智能体ID: {}", id);
                client.sendText(XiaoZhiMcpJsonRpcJson.getToolsListJson());

                // Ожидание ответа списка инструментов (id=2)
                List<String> toolsResponses = client.listener(response -> {
                    try {
                        Map<String, Object> jsonMap = JsonUtils.parseMap(response);
                        return jsonMap != null && Integer.valueOf(2).equals(jsonMap.get("id"));
                    } catch (Exception e) {
                        log.warn("解析工具列表响应失败: {}", response, e);
                        return false;
                    }
                });

                // Обработка ответа списка инструментов
                for (String response : toolsResponses) {
                    try {
                        Map<String, Object> jsonMap = JsonUtils.parseMap(response);
                        if (jsonMap != null && Integer.valueOf(2).equals(jsonMap.get("id"))) {
                            // Проверить наличие поля result
                            Object resultObj = jsonMap.get("result");
                            if (resultObj instanceof Map<?, ?>) {
                                Map<String, Object> resultMap = JsonUtils.toStringObjectMap(resultObj);
                                Object toolsObj = resultMap.get("tools");
                                if (toolsObj instanceof List<?>) {
                                    List<Map<String, Object>> toolsList = JsonUtils.toStringObjectMapList(toolsObj);
                                    // Извлечь список имен инструментов
                                    List<String> result = toolsList.stream()
                                            .map(tool -> String.class.cast(tool.get("name")))
                                            .filter(name -> name != null)
                                            .sorted()
                                            .collect(Collectors.toList());
                                    log.info("成功获取MCP工具列表，智能体ID: {}, 工具数量: {}", id, result.size());
                                    return result;
                                }
                            } else if (jsonMap.containsKey("error")) {
                                log.error("获取工具列表失败，智能体ID: {}, 错误: {}", id, jsonMap.get("error"));
                                return List.of();
                            }
                        }
                    } catch (Exception e) {
                        log.warn("处理工具列表响应失败: {}", response, e);
                    }
                }

                log.warn("未找到有效的工具列表响应，智能体ID: {}", id);
                return List.of();

            }
        } catch (Exception e) {
            log.error("获取智能体 MCP 工具列表失败，智能体ID: {},错误原因：{}", id, e.getMessage());
            return List.of();
        }
    }

    
/**
     * Получить объект URI
     * 
     * @param url Путь
     * @return Объект URI
     */

    private static URI getURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            log.error("路径格式不正确路径：{}，\n错误信息:{}", url, e.getMessage());
            throw new RuntimeException("mcp的地址存在错误，请进入参数管理修改mcp接入点地址");
        }
    }

    
/**
     * Получить ключ
     *
     * @param uri Адрес MCP
     * @return Ключ
     */

    private static String getSecretKey(URI uri) {
        // Получить параметры
        String query = uri.getQuery();
        // Получить ключ шифрования aes
        String str = "key=";
        return query.substring(query.indexOf(str) + str.length());
    }

    
/**
     * Получить URL точки доступа MCP агента
     *
     * @param uri Адрес MCP
     * @return АгентURL точки доступа MCP
     */

    private String getAgentMcpUrl(URI uri) {
        // Получить протокол
        String wsScheme = (uri.getScheme().equals("https")) ? "wss" : "ws";
        // Получить хост, порт, путь
        String path = uri.getSchemeSpecificPart();
        // Получить путь до последнего /
        path = path.substring(0, path.lastIndexOf("/"));
        return wsScheme + ":" + path;
    }

    
/**
     * Получить токен, зашифрованный по ID агента
     *
     * @param agentId ID агента
     * @param key     Ключ шифрования
     * @return Зашифрованный токен
     */

    private static String encryptToken(String agentId, String key) {
        // Выполнить шифрование MD5 по ID агента
        String md5 = DigestUtil.md5Hex(agentId);
        // Текст для шифрования AES
        String json = "{\"agentId\": \"%s\"}".formatted(md5);
        // Зашифровать в значение токена
        return AESUtils.encrypt(key, json);
    }
}
