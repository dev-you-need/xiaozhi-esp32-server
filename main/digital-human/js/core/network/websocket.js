// Модуль обработки сообщений WebSocket
import { getConfig, saveConnectionUrls } from '../../config/manager.js?v=0205';
import { uiController } from '../../ui/controller.js?v=0205';
import { log } from '../../utils/logger.js?v=0205';
import { getAudioPlayer } from '../audio/player.js?v=0205';
import { getAudioRecorder } from '../audio/recorder.js?v=0205';
import { executeMcpTool, getMcpTools, setWebSocket as setMcpWebSocket } from '../mcp/tools.js?v=0205';
import { webSocketConnect } from './ota-connector.js?v=0205';

// Класс обработчика WebSocket
export class WebSocketHandler {
    constructor() {
        this.websocket = null;
        this.onConnectionStateChange = null;
        this.onRecordButtonStateChange = null;
        this.onSessionStateChange = null;
        this.onSessionEmotionChange = null;
        this.onChatMessage = null; // Новое: обратный вызов сообщения чата
        this.currentSessionId = null;
        this.isRemoteSpeaking = false;
    }

    // Отправка hello-рукопожатия
    async sendHelloMessage() {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) return false;

        try {
            const config = getConfig();

            const helloMessage = {
                type: 'hello',
                device_id: config.deviceId,
                device_name: config.deviceName,
                device_mac: config.deviceMac,
                token: config.token,
                features: {
                    mcp: true,
                    emoji: config.emojiEnabled
                }
            };

            log('Отправка hello-рукопожатия', 'info');
            this.websocket.send(JSON.stringify(helloMessage));

            return new Promise(resolve => {
                const timeout = setTimeout(() => {
                    log('Тайм-аут ожидания hello-ответа', 'error');
                    log('Подсказка: попробуйте нажать кнопку "Тест аутентификации" для проверки подключения', 'info');
                    resolve(false);
                }, 5000);

                const onMessageHandler = (event) => {
                    try {
                        const response = JSON.parse(event.data);
                        if (response.type === 'hello' && response.session_id) {
                            log(`Успешное рукопожатие с сервером, ID сессии: ${response.session_id}`, 'success');
                            clearTimeout(timeout);
                            this.websocket.removeEventListener('message', onMessageHandler);
                            resolve(true);
                        }
                    } catch (e) {
                        // Игнорирование не-JSON сообщений
                    }
                };

                this.websocket.addEventListener('message', onMessageHandler);
            });
        } catch (error) {
            log(`Ошибка отправки hello-сообщения: ${error.message}`, 'error');
            return false;
        }
    }

    _sendWakeupMessages(sessionId) {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) return;

        // listen detect
        this.websocket.send(JSON.stringify({
            session_id: sessionId,
            type: 'listen',
            state: 'detect',
            text: 'Привет'
        }));
        log('Отправка listen detect, слово активации: Привет', 'info');

        // listen start: начало прослушивания
        this.websocket.send(JSON.stringify({
            session_id: sessionId,
            type: 'listen',
            state: 'start',
            mode: 'auto'
        }));
        log('Отправка listen start', 'info');
    }

    // Обработка текстовых сообщений
    handleTextMessage(message) {
        if (message.type === 'hello') {
            log(`Ответ сервера: ${JSON.stringify(message, null, 2)}`, 'success');
            window.cameraAvailable = true;
            log('Подключение успешно, камера доступна', 'success');
            uiController.updateDialButton(true);

            this._sendWakeupMessages(message.session_id);

            uiController.startAIChatSession();
        } else if (message.type === 'tts') {
            this.handleTTSMessage(message);
        } else if (message.type === 'audio') {
            log(`Получено сообщение управления аудио: ${JSON.stringify(message)}`, 'info');
        } else if (message.type === 'stt') {
            log(`Результат распознавания: ${message.text}`, 'info');
            // Проверка необходимости привязки устройства
            if (message.text && (message.text.includes('绑定') || message.text.includes('bind'))) {
                log('Получен запрос на привязку устройства, обновление статуса камеры', 'warning');
                window.cameraAvailable = false;
                // Отключение камеры
                if (typeof window.stopCamera === 'function') {
                    window.stopCamera();
                }
                // Обновление состояния кнопки камеры
                const cameraBtn = document.getElementById('cameraBtn');
                if (cameraBtn) {
                    cameraBtn.classList.remove('camera-active');
                    cameraBtn.querySelector('.btn-text').textContent = 'Камера';
                    cameraBtn.disabled = true;
                    cameraBtn.title = 'Сначала введите код привязки';
                }
            }
            // Использование нового обратного вызова сообщения чата для отображения STT-сообщений
            if (this.onChatMessage && message.text) {
                this.onChatMessage(message.text, true);
            }
        } else if (message.type === 'llm') {
            log(`Ответ большой модели: ${message.text}`, 'info');
            // Использование нового обратного вызова сообщения чата для отображения ответа LLM
            if (this.onChatMessage && message.text) {
                this.onChatMessage(message.text, false);
            }

            // Если содержится эмодзи, обновление sessionStatus эмодзи и запуск действия Live2D
            if (message.text && /[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(message.text)) {
                // Извлечение эмодзи
                const emojiMatch = message.text.match(/[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u);
                if (emojiMatch && this.onSessionEmotionChange) {
                    this.onSessionEmotionChange(emojiMatch[0]);
                }

                // Запуск действия эмоции Live2D
                if (message.emotion) {
                    console.log(`Получено сообщение об эмоции: emotion=${message.emotion}, text=${message.text}`);
                    this.triggerLive2DEmotionAction(message.emotion);
                }
            }

            // Добавление в диалог только тогда, когда текст не является только эмодзи
            // Удаление эмодзи из текста и проверка наличия оставшегося содержимого
            const textWithoutEmoji = message.text ? message.text.replace(/[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/gu, '').trim() : '';
            if (textWithoutEmoji && this.onChatMessage) {
                this.onChatMessage(message.text, false);
            }
        } else if (message.type === 'mcp') {
            this.handleMCPMessage(message);
        } else {
            log(`Неизвестный тип сообщения: ${message.type}`, 'info');
            if (this.onChatMessage) {
                this.onChatMessage(`Неизвестный тип сообщения: ${message.type}\n${JSON.stringify(message, null, 2)}`, false);
            }
        }
    }

    // Обработка TTS-сообщений
    handleTTSMessage(message) {
        if (message.state === 'start') {
            log('Сервер начал отправлять речь', 'info');
            this.currentSessionId = message.session_id;
            this.isRemoteSpeaking = true;
            if (this.onSessionStateChange) {
                this.onSessionStateChange(true);
            }

            // Запуск анимации разговора Live2D
            this.startLive2DTalking();
        } else if (message.state === 'sentence_start') {
            log(`Сервер отправляет сегмент речи: ${message.text}`, 'info');
            this.ttsSentenceCount = (this.ttsSentenceCount || 0) + 1;

            if (message.text && this.onChatMessage) {
                this.onChatMessage(message.text, false);
            }

            // Убедиться, что анимация запущена в начале предложения
            const live2dManager = window.chatApp?.live2dManager;
            if (live2dManager && !live2dManager.isTalking) {
                this.startLive2DTalking();
            }
        } else if (message.state === 'sentence_end') {
            log(`Сегмент речи завершен: ${message.text}`, 'info');

            // Не останавливать анимацию при завершении предложения, ожидание следующего предложения или окончательной остановки
        } else if (message.state === 'stop') {
            log('Сервер завершил передачу речи, очистка всех аудиобуферов', 'info');

            // Очистка всех аудиобуферов и остановка воспроизведения
            const audioPlayer = getAudioPlayer();
            audioPlayer.clearAllAudio();

            this.isRemoteSpeaking = false;
            if (this.onRecordButtonStateChange) {
                this.onRecordButtonStateChange(false);
            }
            if (this.onSessionStateChange) {
                this.onSessionStateChange(false);
            }

            // Отложенная остановка анимации разговора Live2D для обеспечения воспроизведения всех предложений
            setTimeout(() => {
                this.stopLive2DTalking();
                this.ttsSentenceCount = 0; // Сброс счетчика
            }, 1000); // Задержка 1 секунда для обеспечения завершения всех предложений
        }
    }

    // Запуск анимации разговора Live2D
    startLive2DTalking() {
        try {
            // Получение экземпляра менеджера Live2D
            const live2dManager = window.chatApp?.live2dManager;
            if (live2dManager && live2dManager.live2dModel) {
                // Использование узла анализатора аудиоплеера
                live2dManager.startTalking();
                log('Анимация разговора Live2D запущена', 'info');
            }
        } catch (error) {
            log(`Ошибка запуска анимации разговора Live2D: ${error.message}`, 'error');
        }
    }

    // Остановка анимации разговора Live2D
    stopLive2DTalking() {
        try {
            const live2dManager = window.chatApp?.live2dManager;
            if (live2dManager) {
                live2dManager.stopTalking();
                log('Анимация разговора Live2D остановлена', 'info');
            }
        } catch (error) {
            log(`Ошибка остановки анимации разговора Live2D: ${error.message}`, 'error');
        }
    }

    // Инициализация аудиоанализатора Live2D
    initializeLive2DAudioAnalyzer() {
        try {
            const live2dManager = window.chatApp?.live2dManager;
            if (live2dManager) {
                // Инициализация аудиоанализатора (с использованием контекста аудиоплеера)
                if (live2dManager.initializeAudioAnalyzer()) {
                    log('Аудиоанализатор Live2D успешно инициализирован, подключен к аудиоплееру', 'success');
                } else {
                    log('Ошибка инициализации аудиоанализатора Live2D, будет использоваться имитированная анимация', 'warning');
                }
            }
        } catch (error) {
            log(`Ошибка инициализации аудиоанализатора Live2D: ${error.message}`, 'error');
        }
    }

    // Обработка MCP-сообщений
    handleMCPMessage(message) {
        const payload = message.payload || {};
        log(`Отправка сервера: ${JSON.stringify(message)}`, 'info');

        if (payload.method === 'tools/list') {
            const tools = getMcpTools();

            const replyMessage = JSON.stringify({
                "session_id": message.session_id || "",
                "type": "mcp",
                "payload": {
                    "jsonrpc": "2.0",
                    "id": payload.id,
                    "result": {
                        "tools": tools
                    }
                }
            });
            log(`Отправка клиента: ${replyMessage}`, 'info');
            this.websocket.send(replyMessage);
            log(`Ответ списком инструментов MCP: ${tools.length} инструментов`, 'info');

        } else if (payload.method === 'tools/call') {
            const toolName = payload.params?.name;
            const toolArgs = payload.params?.arguments;

            log(`Вызов инструмента: ${toolName} Аргументы: ${JSON.stringify(toolArgs)}`, 'info');

            executeMcpTool(toolName, toolArgs).then(result => {
                const replyMessage = JSON.stringify({
                    "session_id": message.session_id || "",
                    "type": "mcp",
                    "payload": {
                        "jsonrpc": "2.0",
                        "id": payload.id,
                        "result": {
                            "content": [
                                {
                                    "type": "text",
                                    "text": JSON.stringify(result)
                                }
                            ],
                            "isError": false
                        }
                    }
                });

                log(`Отправка клиента: ${replyMessage}`, 'info');
                this.websocket.send(replyMessage);
            }).catch(error => {
                log(`Ошибка выполнения инструмента: ${error.message}`, 'error');
                const errorReply = JSON.stringify({
                    "session_id": message.session_id || "",
                    "type": "mcp",
                    "payload": {
                        "jsonrpc": "2.0",
                        "id": payload.id,
                        "error": {
                            "code": -32603,
                            "message": error.message
                        }
                    }
                });
                this.websocket.send(errorReply);
            });
        } else if (payload.method === 'initialize') {
            log(`Получен запрос на инициализацию инструментов: ${JSON.stringify(payload.params)}`, 'info');
            // Сохранение адреса интерфейса визуального анализа
            const visionUrl = document.getElementById('visionUrl');
            const visionConfig = payload?.params?.capabilities?.vision;
            if (visionConfig && typeof visionConfig === 'object' && visionConfig.url && visionConfig.token) {
                const visionConfigStr = JSON.stringify(visionConfig);
                localStorage.setItem('xz_tester_vision', visionConfigStr);
                if (visionUrl) visionUrl.value = visionConfig.url;
            } else {
                localStorage.removeItem('xz_tester_vision');
                if (visionUrl) visionUrl.value = '';
            }

            const replyMessage = JSON.stringify({
                "session_id": message.session_id || "",
                "type": "mcp",
                "payload": {
                    "jsonrpc": "2.0",
                    "id": payload.id,
                    "result": {
                        "protocolVersion": "2024-11-05",
                        "capabilities": {
                            "tools": {}
                        },
                        "serverInfo": {
                            "name": "xiaozhi-web-test",
                            "version": "2.1.0"
                        }
                    }
                }
            });
            log(`Ответ на инициализацию`, 'info');
            this.websocket.send(replyMessage);
        } else {
            log(`Неизвестный метод MCP: ${payload.method}`, 'warning');
        }
    }

    // Обработка двоичных сообщений
    async handleBinaryMessage(data) {
        try {
            let arrayBuffer;
            if (data instanceof ArrayBuffer) {
                arrayBuffer = data;
            } else if (data instanceof Blob) {
                arrayBuffer = await data.arrayBuffer();
                log(`Получены аудиоданные Blob, размер: ${arrayBuffer.byteLength} байт`, 'debug');
            } else {
                log(`Получены двоичные данные неизвестного типа: ${typeof data}`, 'warning');
                return;
            }

            const opusData = new Uint8Array(arrayBuffer);
            const audioPlayer = getAudioPlayer();
            audioPlayer.enqueueAudioData(opusData);
        } catch (error) {
            log(`Ошибка обработки двоичного сообщения: ${error.message}`, 'error');
        }
    }

    // Подключение к серверу WebSocket
    async connect() {
        const config = getConfig();
        log('Проверка статуса OTA...', 'info');
        saveConnectionUrls();

        try {
            const otaUrl = document.getElementById('otaUrl').value.trim();
            const ws = await webSocketConnect(otaUrl, config);
            if (ws === undefined) {
                return false;
            }
            this.websocket = ws;

            // Установка типа получаемых двоичных данных как ArrayBuffer
            this.websocket.binaryType = 'arraybuffer';

            // Установка экземпляра WebSocket для модуля MCP
            setMcpWebSocket(this.websocket);

            // Установка WebSocket для рекордера
            const audioRecorder = getAudioRecorder();
            audioRecorder.setWebSocket(this.websocket);

            this.setupEventHandlers();

            return true;
        } catch (error) {
            log(`Ошибка подключения: ${error.message}`, 'error');
            if (this.onConnectionStateChange) {
                this.onConnectionStateChange(false);
            }
            return false;
        }
    }

    // Настройка обработчиков событий
    setupEventHandlers() {
        this.websocket.onopen = async () => {
            const url = document.getElementById('serverUrl').value;
            log(`Подключено к серверу: ${url}`, 'success');

            if (this.onConnectionStateChange) {
                this.onConnectionStateChange(true);
            }

            // После успешного подключения состояние по умолчанию - прослушивание
            this.isRemoteSpeaking = false;
            if (this.onSessionStateChange) {
                this.onSessionStateChange(false);
            }

            // Инициализация аудиоанализатора Live2D при успешном подключении WebSocket
            this.initializeLive2DAudioAnalyzer();

            await this.sendHelloMessage();
        };

        this.websocket.onclose = () => {
            log('Соединение разорвано', 'info');

            if (this.onConnectionStateChange) {
                this.onConnectionStateChange(false);
            }

            const audioRecorder = getAudioRecorder();
            audioRecorder.stop();

            // Отключение камеры
            if (typeof window.stopCamera === 'function') {
                window.stopCamera();
            }

            // Скрытие области отображения камеры
            const cameraContainer = document.getElementById('cameraContainer');
            if (cameraContainer) {
                cameraContainer.classList.remove('active');
            }
        };

        this.websocket.onerror = (error) => {
            log(`Ошибка WebSocket: ${error.message || 'Неизвестная ошибка'}`, 'error');
            uiController.addChatMessage(`⚠️ Ошибка WebSocket: ${error.message || 'Неизвестная ошибка'}`, false);
            if (this.onConnectionStateChange) {
                this.onConnectionStateChange(false);
            }
        };

        this.websocket.onmessage = (event) => {
            try {
                if (typeof event.data === 'string') {
                    const message = JSON.parse(event.data);
                    this.handleTextMessage(message);
                } else {
                    this.handleBinaryMessage(event.data);
                }
            } catch (error) {
                log(`Ошибка обработки сообщения WebSocket: ${error.message}`, 'error');
                // Старая функция addMessage больше не используется, так как элемент conversationDiv не существует
                // Сообщения об ошибках будут отображаться другим способом
            }
        };
    }

    // Разрыв соединения
    disconnect() {
        if (!this.websocket) return;

        this.websocket.close();
        const audioRecorder = getAudioRecorder();
        audioRecorder.stop();

        // Отключение камеры
        if (typeof window.stopCamera === 'function') {
            window.stopCamera();
        }

        // Скрытие области отображения камеры
        const cameraContainer = document.getElementById('cameraContainer');
        if (cameraContainer) {
            cameraContainer.classList.remove('active');
        }
    }

    // Отправка текстового сообщения
    sendTextMessage(text) {
        if (text === '' || !this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            return false;
        }

        try {
            // Если собеседник говорит, сначала отправка сообщения о прерывании
            if (this.isRemoteSpeaking && this.currentSessionId) {
                const abortMessage = {
                    session_id: this.currentSessionId,
                    type: 'abort',
                    reason: 'wake_word_detected'
                };
                this.websocket.send(JSON.stringify(abortMessage));
                log('Отправка сообщения о прерывании', 'info');
            }

            const listenMessage = {
                type: 'listen',
                state: 'detect',
                text: text
            };

            this.websocket.send(JSON.stringify(listenMessage));
            log(`Отправка текстового сообщения: ${text}`, 'info');

            return true;
        } catch (error) {
            log(`Ошибка отправки сообщения: ${error.message}`, 'error');
            return false;
        }
    }

    /**
     * Запуск действия эмоции Live2D
     * @param {string} emotion - Название эмоции
     */
    triggerLive2DEmotionAction(emotion) {
        try {
            const live2dManager = window.chatApp?.live2dManager;
            if (live2dManager && typeof live2dManager.triggerEmotionAction === 'function') {
                live2dManager.triggerEmotionAction(emotion);
                log(`Запуск действия эмоции Live2D: ${emotion}`, 'info');
            } else {
                log(`Не удалось запустить действие эмоции Live2D: менеджер Live2D не найден или метод недоступен`, 'warning');
            }
        } catch (error) {
            log(`Ошибка запуска действия эмоции Live2D: ${error.message}`, 'error');
        }
    }

    // Получение экземпляра WebSocket
    getWebSocket() {
        return this.websocket;
    }

    // Проверка состояния подключения
    isConnected() {
        return this.websocket && this.websocket.readyState === WebSocket.OPEN;
    }
}

// Создание синглтона
let wsHandlerInstance = null;

export function getWebSocketHandler() {
    if (!wsHandlerInstance) {
        wsHandlerInstance = new WebSocketHandler();
    }
    return wsHandlerInstance;
}
