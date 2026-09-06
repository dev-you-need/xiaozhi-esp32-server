package xiaozhi.modules.agent.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.DateUtils;
import xiaozhi.common.utils.MessageUtils;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatHistoryReportDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.service.biz.AgentChatHistoryBizService;
import xiaozhi.modules.security.user.SecurityUser;

@Tag(name = "智能体聊天历史管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/chat-history")
public class AgentChatHistoryController {
    private final AgentChatHistoryBizService agentChatHistoryBizService;
    private final AgentChatHistoryService agentChatHistoryService;
    private final AgentService agentService;
    private final RedisUtils redisUtils;

    
/**
     * Запрос отчёта чата сервиса XiaoZhi
     * <p>
     * Запрос отчёта чата сервиса XiaoZhi с аудиоданными Base64 и связанной информацией.
     *
     * @param request Объект запроса с загружаемым файлом и связанной информацией
     */

    @Operation(summary = "小智服务聊天上报请求")
    @PostMapping("/report")
    public Result<Boolean> uploadFile(@Valid @RequestBody AgentChatHistoryReportDTO request) {
        Boolean result = agentChatHistoryBizService.report(request);
        return new Result<Boolean>().ok(result);
    }

    
/**
     * Получить ссылку для скачивания истории чата
     * 
     * @param agentId   ID агента
     * @param sessionId ID сессии
     * @return UUID в качестве идентификатора загрузки
     */

    @Operation(summary = "获取聊天记录下载链接")
    @RequiresPermissions("sys:role:normal")
    @PostMapping("/getDownloadUrl/{agentId}/{sessionId}")
    public Result<String> getDownloadUrl(@PathVariable("agentId") String agentId,
            @PathVariable("sessionId") String sessionId) {
        // Получить текущего пользователя
        UserDetail user = SecurityUser.getUser();
        // Проверить права доступа
        if (!agentService.checkAgentPermission(agentId, user.getId())) {
            throw new RenException(ErrorCode.CHAT_HISTORY_NO_PERMISSION);
        }

        // Сгенерировать UUID
        String uuid = UUID.randomUUID().toString();
        // Сохранить agentId и sessionId в Redis в формате agentId:sessionId
        redisUtils.set(RedisKeys.getChatHistoryKey(uuid), agentId + ":" + sessionId);

        return new Result<String>().ok(uuid);
    }

    
/**
     * Скачать историю чата текущей сессии
     * 
     * @param uuid     Идентификатор загрузки
     * @param response HTTP-ответ
     */

    @Operation(summary = "下载本会话聊天记录")
    @GetMapping("/download/{uuid}/current")
    public void downloadCurrentSession(@PathVariable("uuid") String uuid,
            HttpServletResponse response) {
        // Получить agentId и sessionId из Redis
        String agentSessionInfo = (String) redisUtils.get(RedisKeys.getChatHistoryKey(uuid));
        if (StringUtils.isBlank(agentSessionInfo)) {
            throw new RenException(ErrorCode.DOWNLOAD_LINK_EXPIRED);
        }

        try {
            // Разобрать agentId и sessionId
            String[] parts = agentSessionInfo.split(":");
            if (parts.length != 2) {
                throw new RenException(ErrorCode.DOWNLOAD_LINK_INVALID);
            }
            String agentId = parts[0];
            String sessionId = parts[1];

            // Выполнить загрузку
            downloadChatHistory(agentId, List.of(sessionId), response);
        } finally {
            // Удалить UUID после загрузки для предотвращения несанкционированного доступа
            redisUtils.delete(RedisKeys.getChatHistoryKey(uuid));
        }
    }

    
/**
     * Скачать историю чата текущей и 20 предыдущих сессий
     * 
     * @param uuid     Идентификатор загрузки
     * @param response HTTP-ответ
     */

    @Operation(summary = "下载本会话及前20条会话聊天记录")
    @GetMapping("/download/{uuid}/previous")
    public void downloadCurrentSessionWithPrevious(@PathVariable("uuid") String uuid,
            HttpServletResponse response) {
        // Получить agentId и sessionId из Redis
        String agentSessionInfo = (String) redisUtils.get(RedisKeys.getChatHistoryKey(uuid));
        if (StringUtils.isBlank(agentSessionInfo)) {
            throw new RenException(ErrorCode.DOWNLOAD_LINK_EXPIRED);
        }

        try {
            // Разобрать agentId и sessionId
            String[] parts = agentSessionInfo.split(":");
            if (parts.length != 2) {
                throw new RenException(ErrorCode.DOWNLOAD_LINK_INVALID);
            }
            String agentId = parts[0];
            String sessionId = parts[1];

            // Получить список всех сессий
            Map<String, Object> params = Map.of(
                    "agentId", agentId,
                    Constant.PAGE, 1,
                    Constant.LIMIT, 1000 // Получить достаточно сессий
            );
            PageData<AgentChatSessionDTO> sessionPage = agentChatHistoryService.getSessionListByAgentId(params);
            List<AgentChatSessionDTO> allSessions = sessionPage.getList();

            // Найти позицию текущей сессии в списке
            int currentIndex = -1;
            for (int i = 0; i < allSessions.size(); i++) {
                if (allSessions.get(i).getSessionId().equals(sessionId)) {
                    currentIndex = i;
                    break;
                }
            }

            // Если текущая сессия найдена — собрать ID текущей и 20 предыдущих сессий
            List<String> sessionIdsToDownload = new ArrayList<>();
            if (currentIndex != -1) {
                // Начиная с текущей сессии, взять до 20 сессий (включая текущую)
                int endIndex = Math.min(allSessions.size() - 1, currentIndex + 20); // Обеспечить отсутствие выхода за границы массива
                for (int i = currentIndex; i <= endIndex; i++) {
                    sessionIdsToDownload.add(allSessions.get(i).getSessionId());
                }
            }

            // Если текущая сессия не найдена — загрузить хотя бы текущую
            if (sessionIdsToDownload.isEmpty()) {
                sessionIdsToDownload.add(sessionId);
            }
            downloadChatHistory(agentId, sessionIdsToDownload, response);
        } finally {
            // Удалить UUID после загрузки для предотвращения несанкционированного доступа
            redisUtils.delete(RedisKeys.getChatHistoryKey(uuid));
        }
    }

    
/**
     * Скачать историю чата указанных сессий
     * 
     * @param agentId    ID агента
     * @param sessionIds ID списка сессий
     * @param response   HTTP-ответ
     */

    private void downloadChatHistory(String agentId, List<String> sessionIds, HttpServletResponse response) {
        try {
            // Установить заголовки ответа
            response.setContentType("text/plain;charset=UTF-8");
            String fileName = URLEncoder.encode("history.txt", StandardCharsets.UTF_8.toString());
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            // Получить историю чата и записать в поток ответа
            try (OutputStream out = response.getOutputStream()) {
                // Сформировать записи чата для каждой сессии
                for (String sessionId : sessionIds) {
                    // Получить все записи чата для сессии
                    List<AgentChatHistoryDTO> chatHistoryList = agentChatHistoryService
                            .getChatHistoryBySessionId(agentId, sessionId);

                    // Извлечь время создания первого сообщения как время сессии
                    if (!chatHistoryList.isEmpty()) {
                        Date firstMessageTime = chatHistoryList.get(0).getCreatedAt();
                        String sessionTimeStr = DateUtils.format(firstMessageTime, DateUtils.DATE_TIME_PATTERN);
                        out.write((sessionTimeStr + "\n").getBytes(StandardCharsets.UTF_8));
                    }

                    for (AgentChatHistoryDTO message : chatHistoryList) {
                        String role = message.getChatType() == 1 ? MessageUtils.getMessage(ErrorCode.CHAT_ROLE_USER)
                                : MessageUtils.getMessage(ErrorCode.CHAT_ROLE_AGENT);
                        String direction = message.getChatType() == 1 ? ">>" : "<<";
                        Date messageTime = message.getCreatedAt();
                        String messageTimeStr = DateUtils.format(messageTime, DateUtils.DATE_TIME_PATTERN);
                        String content = message.getContent();

                        String line = "[" + role + "]-[" + messageTimeStr + "]" + direction + ":" + content + "\n";
                        out.write(line.getBytes(StandardCharsets.UTF_8));
                    }

                    // Добавить пустую строку-разделитель между сессиями
                    if (sessionIds.indexOf(sessionId) < sessionIds.size() - 1) {
                        out.write("\n".getBytes(StandardCharsets.UTF_8));
                    }
                }

                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
