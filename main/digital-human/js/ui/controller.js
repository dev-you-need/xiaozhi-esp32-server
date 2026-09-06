// Модуль контроллера интерфейса
import { loadConfig, saveConfig } from '../config/manager.js?v=0205';
import { getAudioPlayer } from '../core/audio/player.js?v=0205';
import { getAudioRecorder } from '../core/audio/recorder.js?v=0205';
import { requestWakewordBridge, stopWakewordBridgeListener, startWakewordBridgeListener, getWakewordBridgeUrl, onNextBridgeConnected } from '../core/network/wakeword-bridge.js?v=0205';
import { getWebSocketHandler } from '../core/network/websocket.js?v=0205';
import { log } from '../utils/logger.js?v=0205';

// Класс контроллера интерфейса
class UIController {
    constructor() {
        this.isEditing = false;
        this.visualizerCanvas = null;
        this.visualizerContext = null;
        this.audioStatsTimer = null;
        this.currentBackgroundIndex = localStorage.getItem('backgroundIndex') ? parseInt(localStorage.getItem('backgroundIndex')) : 0;
        this.backgroundImages = ['1.png', '2.png', '3.png'];
        this.dialBtnDisabled = false;
        this.isConnecting = false;
        this.lastWakewordDialTime = 0;

        // Привязка методов
        this.init = this.init.bind(this);
        this.initEventListeners = this.initEventListeners.bind(this);
        this.updateDialButton = this.updateDialButton.bind(this);
        this.addChatMessage = this.addChatMessage.bind(this);
        this.switchBackground = this.switchBackground.bind(this);
        this.switchLive2DModel = this.switchLive2DModel.bind(this);
        this.showModal = this.showModal.bind(this);
        this.hideModal = this.hideModal.bind(this);
        this.switchTab = this.switchTab.bind(this);
        this.applyWakewordConfig = this.applyWakewordConfig.bind(this);
        this.handleApplyWakeword = this.handleApplyWakeword.bind(this);
        this.triggerWakewordDial = this.triggerWakewordDial.bind(this);
    }

    // Инициализация
    init() {
        console.log('Инициализация UIController начата');

        this.visualizerCanvas = document.getElementById('audioVisualizer');
        if (this.visualizerCanvas) {
            this.visualizerContext = this.visualizerCanvas.getContext('2d');
            this.initVisualizer();
        }

        // Проверка наличия кнопки подключения при инициализации
        const connectBtn = document.getElementById('connectBtn');
        console.log('Кнопка подключения при инициализации:', connectBtn);

        this.initEventListeners();
        this.startAudioStatsMonitor();
        loadConfig();

        // Регистрация обратного вызова записи
        const audioRecorder = getAudioRecorder();
        audioRecorder.onRecordingStart = (seconds) => {
            this.updateRecordButtonState(true, seconds);
        };

        // Инициализация отображения статуса
        this.updateConnectionUI(false);
        // Применение сохраненного фона
        const backgroundContainer = document.querySelector('.background-container');
        if (backgroundContainer) {
            backgroundContainer.style.backgroundImage = `url('./images/${this.backgroundImages[this.currentBackgroundIndex]}')`;
        }

        this.updateDialButton(false);

        console.log('Инициализация UIController завершена');
    }

    // Инициализация визуализатора
    initVisualizer() {
        if (this.visualizerCanvas) {
            this.visualizerCanvas.width = this.visualizerCanvas.clientWidth;
            this.visualizerCanvas.height = this.visualizerCanvas.clientHeight;
            this.visualizerContext.fillStyle = '#fafafa';
            this.visualizerContext.fillRect(0, 0, this.visualizerCanvas.width, this.visualizerCanvas.height);
        }
    }

    // Инициализация слушателей событий
    initEventListeners() {
        // Кнопка настроек
        const settingsBtn = document.getElementById('settingsBtn');
        if (settingsBtn) {
            settingsBtn.addEventListener('click', () => {
                this.showModal('settingsModal');
            });
        }

        // Кнопка переключения фона
        const backgroundBtn = document.getElementById('backgroundBtn');
        if (backgroundBtn) {
            backgroundBtn.addEventListener('click', this.switchBackground);
        }

        // Событие изменения выбора модели
        const modelSelect = document.getElementById('live2dModelSelect');
        if (modelSelect) {
            modelSelect.addEventListener('change', () => {
                this.switchLive2DModel();
            });
        }

        // Кнопка переключения камеры
        const cameraSwitch = document.getElementById('cameraSwitch');
        const cameraSwitchMask = document.getElementById('cameraSwitchMask');
        if (cameraSwitchMask) {
            cameraSwitchMask.addEventListener('click', () => {
                const isCameraActive = cameraSwitch.classList.contains('active');
                if (isCameraActive) {
                    window.switchCamera();
                }
            })
        }

        // Кнопка набора номера
        const dialBtn = document.getElementById('dialBtn');
        if (dialBtn) {
            dialBtn.addEventListener('click', () => {
                dialBtn.disabled = true;
                this.dialBtnDisabled = true;
                setTimeout(() => {
                    dialBtn.disabled = false;
                    this.dialBtnDisabled = false;
                }, 3000);

                const wsHandler = getWebSocketHandler();
                const isConnected = wsHandler.isConnected();

                if (isConnected) {
                    wsHandler.disconnect();
                    this.updateDialButton(false);
                    if (cameraSwitch) cameraSwitch.classList.remove('active');
                    this.addChatMessage('Отключено, до встречи~😊', false);
                } else {
                    // Проверка заполненности URL OTA
                    const otaUrlInput = document.getElementById('otaUrl');
                    if (!otaUrlInput || !otaUrlInput.value.trim()) {
                        // Если URL OTA не заполнен, отображение модального окна настроек и переключение на вкладку устройства
                        this.showModal('settingsModal');
                        this.switchTab('device');
                        this.addChatMessage('Пожалуйста, заполните URL OTA-сервера', false);
                        return;
                    }

                    // Запуск процесса подключения
                    this.handleConnect();
                }
            });
        }

        // Кнопка камеры
        const cameraBtn = document.getElementById('cameraBtn');
        let cameraTimer = null;
        if (cameraBtn) {
            cameraBtn.addEventListener('click', () => {
                if (cameraTimer) {
                    clearTimeout(cameraTimer);
                    cameraTimer = null;
                }
                cameraTimer = setTimeout(() => {
                    const cameraContainer = document.getElementById('cameraContainer');
                    if (!cameraContainer) {
                        log('Контейнер камеры не существует', 'warning');
                        return;
                    }

                    const isActive = cameraContainer.classList.contains('active');
                    if (isActive) {
                        // Отключение камеры
                        if (typeof window.stopCamera === 'function') {
                            if (cameraSwitch) cameraSwitch.classList.remove('active');
                            window.stopCamera();
                        }
                        cameraContainer.classList.remove('active');
                        cameraBtn.classList.remove('camera-active');
                        cameraBtn.querySelector('.btn-text').textContent = 'Камера';
                        log('Камера отключена', 'info');
                    } else {
                        // Включение камеры
                        if (typeof window.startCamera === 'function') {
                            window.startCamera().then(success => {
                                if (success) {
                                    cameraBtn.classList.add('camera-active');
                                    cameraBtn.querySelector('.btn-text').textContent = 'Выкл.';
                                } else {
                                    this.addChatMessage('⚠️ Ошибка запуска камеры, проверьте разрешения браузера', false);
                                }
                            }).catch(error => {
                                log(`Исключение при запуске камеры: ${error.message}`, 'error');
                            });
                        } else {
                            log('Функция startCamera не определена', 'warning');
                        }
                    }
                }, 300);
            });
        }

        // Кнопка записи
        const recordBtn = document.getElementById('recordBtn');
        if (recordBtn) {
            let recordTimer = null;
            recordBtn.addEventListener('click', () => {
                if (recordTimer) {
                    clearTimeout(recordTimer);
                    recordTimer = null;
                }
                recordTimer = setTimeout(() => {
                    const audioRecorder = getAudioRecorder();
                    if (audioRecorder.isRecording) {
                        audioRecorder.stop();
                        // Восстановление кнопки записи в нормальное состояние
                        recordBtn.classList.remove('recording');
                        recordBtn.querySelector('.btn-text').textContent = 'Запись';
                    } else {
                        // Обновление состояния кнопки на запись
                        recordBtn.classList.add('recording');
                        recordBtn.querySelector('.btn-text').textContent = 'Запись...';

                        // Начало записи, обновление состояния кнопки с задержкой
                        setTimeout(() => {
                            audioRecorder.start();
                        }, 100);
                    }
                }, 300);
            });
        }

        // Слушатель событий ввода сообщения чата
        const chatIpt = document.getElementById('chatIpt');
        if (chatIpt) {
            const wsHandler = getWebSocketHandler();
            chatIpt.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    if (e.target.value) {
                        wsHandler.sendTextMessage(e.target.value);
                        e.target.value = '';
                        return;
                    }
                }
            });
        }

        // Кнопка закрытия
        const closeButtons = document.querySelectorAll('.close-btn');
        closeButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const modal = e.target.closest('.modal');
                if (modal) {
                    if (modal.id === 'settingsModal') {
                        saveConfig();
                    }
                    this.hideModal(modal.id);
                }
            });
        });

        // Переключение вкладок настроек
        const tabBtns = document.querySelectorAll('.tab-btn');
        tabBtns.forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.switchTab(e.target.dataset.tab);
            });
        });

        const applyWakewordBtn = document.getElementById('applyWakewordBtn');
        if (applyWakewordBtn) {
            applyWakewordBtn.addEventListener('click', this.handleApplyWakeword);
        }

        // Закрытие модального окна по клику на фон (только для определенных модальных окон отключено)
        const modals = document.querySelectorAll('.modal');
        modals.forEach(modal => {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    // settingsModal, mcpToolModal, mcpPropertyModal могут быть закрыты только по клику на X
                    const nonClosableModals = ['settingsModal', 'mcpToolModal', 'mcpPropertyModal'];
                    if (nonClosableModals.includes(modal.id)) {
                        return; // Запрет закрытия по клику на фон
                    }
                    this.hideModal(modal.id);
                }
            });
        });

        // Кнопка добавления инструмента MCP
        const addMCPToolBtn = document.getElementById('addMCPToolBtn');
        if (addMCPToolBtn) {
            addMCPToolBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.addMCPTool();
            });
        }

        // Кнопки подключения и отправки не удалены, могут быть добавлены к кнопке набора номера позже
    }

    // Обновление интерфейса состояния подключения
    updateConnectionUI(isConnected) {
        const connectionStatus = document.getElementById('connectionStatus');
        const statusDot = document.querySelector('.status-dot');

        if (connectionStatus) {
            if (isConnected) {
                connectionStatus.textContent = 'Подключено';
                if (statusDot) {
                    statusDot.className = 'status-dot status-connected';
                }
            } else {
                connectionStatus.textContent = 'Не в сети';
                if (statusDot) {
                    statusDot.className = 'status-dot status-disconnected';
                }
            }
        }
    }

    // Обновление состояния кнопки набора номера
    updateDialButton(isConnected) {
        const dialBtn = document.getElementById('dialBtn');
        const recordBtn = document.getElementById('recordBtn');
        const cameraBtn = document.getElementById('cameraBtn');

        if (dialBtn) {
            if (isConnected) {
                dialBtn.classList.add('dial-active');
                dialBtn.querySelector('.btn-text').textContent = 'Завершить';
                // Обновление иконки кнопки набора номера на иконку завершения вызова
                dialBtn.querySelector('svg').innerHTML = `
                    <path d="M12,9C10.4,9 9,10.4 9,12C9,13.6 10.4,15 12,15C13.6,15 15,13.6 15,12C15,10.4 13.6,9 12,9M12,17C9.2,17 7,14.8 7,12C7,9.2 9.2,7 12,7C14.8,7 17,9.2 17,12C17,14.8 14.8,17 12,17M12,4.5C7,4.5 2.7,7.6 1,12C2.7,16.4 7,19.5 12,19.5C17,19.5 21.3,16.4 23,12C21.3,7.6 17,4.5 12,4.5Z"/>
                `;
            } else {
                dialBtn.classList.remove('dial-active');
                dialBtn.querySelector('.btn-text').textContent = 'Вызов';
                // Восстановление иконки кнопки набора номера
                dialBtn.querySelector('svg').innerHTML = `
                    <path d="M6.62,10.79C8.06,13.62 10.38,15.94 13.21,17.38L15.41,15.18C15.69,14.9 16.08,14.82 16.43,14.93C17.55,15.3 18.75,15.5 20,15.5A1,1 0 0,1 21,16.5V20A1,1 0 0,1 20,21A17,17 0 0,1 3,4A1,1 0 0,1 4,3H7.5A1,1 0 0,1 8.5,4C8.5,5.25 8.7,6.45 9.07,7.57C9.18,7.92 9.1,8.31 8.82,8.59L6.62,10.79Z"/>
                `;
            }
        }

        // Обновление состояния кнопки камеры - сброс значения по умолчанию при отключении
        if (cameraBtn && !isConnected) {
            const cameraContainer = document.getElementById('cameraContainer');
            if (cameraContainer && cameraContainer.classList.contains('active')) {
                cameraContainer.classList.remove('active');
            }
            cameraBtn.classList.remove('camera-active');
            cameraBtn.querySelector('.btn-text').textContent = 'Камера';
            cameraBtn.disabled = true;
            cameraBtn.title = 'Сначала подключитесь к серверу';
            // Отключение камеры
            if (typeof window.stopCamera === 'function') {
                window.stopCamera();
            }
        }

        // Обновление состояния кнопки камеры - включение при подключении и доступности камеры
        if (cameraBtn && isConnected) {
            if (window.cameraAvailable) {
                cameraBtn.disabled = false;
                cameraBtn.title = 'Включить/выключить камеру';
            } else {
                cameraBtn.disabled = true;
                cameraBtn.title = 'Сначала введите код привязки';
            }
        }

        // Обновление состояния кнопки записи
        if (recordBtn) {
            const microphoneAvailable = window.microphoneAvailable !== false;
            if (isConnected && microphoneAvailable) {
                recordBtn.disabled = false;
                recordBtn.title = 'Начать запись';
                // Восстановление кнопки записи в нормальное состояние
                recordBtn.querySelector('.btn-text').textContent = 'Запись';
                recordBtn.classList.remove('recording');
            } else {
                recordBtn.disabled = true;
                if (!microphoneAvailable) {
                    recordBtn.title = window.isHttpNonLocalhost ? 'В настоящее время из-за HTTP-доступа запись невозможна, доступно только текстовое взаимодействие' : 'Микрофон недоступен';
                } else {
                    recordBtn.title = 'Сначала подключитесь к серверу';
                }
                // Восстановление кнопки записи в нормальное состояние
                recordBtn.querySelector('.btn-text').textContent = 'Запись';
                recordBtn.classList.remove('recording');
            }
        }
    }

    // Обновление состояния кнопки записи
    updateRecordButtonState(isRecording, seconds = 0) {
        const recordBtn = document.getElementById('recordBtn');
        if (recordBtn) {
            if (isRecording) {
                recordBtn.querySelector('.btn-text').textContent = `Запись...`;
                recordBtn.classList.add('recording');
            } else {
                recordBtn.querySelector('.btn-text').textContent = 'Запись';
                recordBtn.classList.remove('recording');
            }
            // Включение кнопки только при доступности микрофона
            recordBtn.disabled = window.microphoneAvailable === false;
        }
    }

    /**
     * Обновление состояния доступности микрофона
     * @param {boolean} isAvailable - Доступен ли микрофон
     * @param {boolean} isHttpNonLocalhost - Является ли это HTTP-доступом не через localhost
     */
    updateMicrophoneAvailability(isAvailable, isHttpNonLocalhost) {
        const recordBtn = document.getElementById('recordBtn');
        if (!recordBtn) return;
        if (!isAvailable) {
            // Отключение кнопки записи
            recordBtn.disabled = true;
            // Обновление текста и заголовка кнопки
            recordBtn.querySelector('.btn-text').textContent = 'Запись';
            recordBtn.title = isHttpNonLocalhost ? 'В настоящее время из-за HTTP-доступа запись невозможна, доступно только текстовое взаимодействие' : 'Микрофон недоступен';

        } else {
            // Если подключено, включение кнопки записи
            const wsHandler = getWebSocketHandler();
            if (wsHandler && wsHandler.isConnected()) {
                recordBtn.disabled = false;
                recordBtn.title = 'Начать запись';
            }
        }
    }

    // Добавление сообщения чата
    addChatMessage(content, isUser = false) {
        const chatStream = document.getElementById('chatStream');
        if (!chatStream) return;

        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${isUser ? 'user' : 'ai'}`;
        messageDiv.innerHTML = `<div class="message-bubble">${content}</div>`;
        chatStream.appendChild(messageDiv);

        // Прокрутка вниз
        chatStream.scrollTop = chatStream.scrollHeight;
    }

    // Переключение фона
    switchBackground() {
        this.currentBackgroundIndex = (this.currentBackgroundIndex + 1) % this.backgroundImages.length;
        const backgroundContainer = document.querySelector('.background-container');
        if (backgroundContainer) {
            backgroundContainer.style.backgroundImage = `url('./images/${this.backgroundImages[this.currentBackgroundIndex]}')`;
        }
        localStorage.setItem('backgroundIndex', this.currentBackgroundIndex);
    }

    // Переключение модели Live2D
    switchLive2DModel() {
        const modelSelect = document.getElementById('live2dModelSelect');
        if (!modelSelect) {
            console.error('Выпадающий список выбора модели не существует');
            return;
        }

        const selectedModel = modelSelect.value;
        const app = window.chatApp;

        if (app && app.live2dManager) {
            app.live2dManager.switchModel(selectedModel)
                .then(success => {
                    if (success) {
                        this.addChatMessage(`Переключено на модель: ${selectedModel}`, false);
                    } else {
                        this.addChatMessage('Ошибка переключения модели', false);
                    }
                })
                .catch(error => {
                    console.error('Ошибка переключения модели:', error);
                    this.addChatMessage('Ошибка переключения модели', false);
                });
        } else {
            this.addChatMessage('Менеджер Live2D не инициализирован', false);
        }
    }

    // Отображение модального окна
    showModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
        }
    }

    // Скрытие модального окна
    hideModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'none';
        }
    }

    // Переключение вкладки
    switchTab(tabName) {
        // Удаление активного класса со всех вкладок
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        tabBtns.forEach(btn => btn.classList.remove('active'));
        tabContents.forEach(content => content.classList.remove('active'));

        // Активация выбранной вкладки
        const activeTabBtn = document.querySelector(`[data-tab="${tabName}"]`);
        const activeTabContent = document.getElementById(`${tabName}Tab`);

        if (activeTabBtn && activeTabContent) {
            activeTabBtn.classList.add('active');
            activeTabContent.classList.add('active');
        }
    }

    applyWakewordConfig(config = {}) {
        const wakewordEnabledInput = document.getElementById('wakewordEnabled');
        const wakewordListInput = document.getElementById('wakewordList');

        if (!wakewordEnabledInput || !wakewordListInput) {
            return;
        }

        const wakeWords = Array.isArray(config.wakeWords)
            ? config.wakeWords.filter(item => typeof item === 'string' && item.trim())
            : [];

        wakewordEnabledInput.value = config.enabled === false ? 'false' : 'true';
        wakewordListInput.value = wakeWords.join('\n');
        saveConfig();
    }

    async handleApplyWakeword() {
        const wakewordEnabledInput = document.getElementById('wakewordEnabled');
        const wakewordListInput = document.getElementById('wakewordList');
        const wakewordWsUrlInput = document.getElementById('wakewordWsUrl');
        if (!wakewordEnabledInput || !wakewordListInput) {
            return;
        }

        const wakeWords = wakewordListInput.value
            .split(/\r?\n/u)
            .map(item => item.trim())
            .filter(Boolean)
            .filter((item, index, items) => items.indexOf(item) === index);

        const payload = {
            enabled: wakewordEnabledInput.value !== 'false',
            wakeWords,
        };

        if (payload.enabled && payload.wakeWords.length === 0) {
            this.addChatMessage('При включении слов активации необходимо ввести хотя бы одно слово активации.', false);
            return;
        }

        const applyWakewordBtn = document.getElementById('applyWakewordBtn');
        if (applyWakewordBtn) {
            applyWakewordBtn.disabled = true;
            applyWakewordBtn.textContent = 'Применение...';
        }

        try {
            // Сохранение адреса в localStorage
            if (wakewordWsUrlInput && wakewordWsUrlInput.value.trim()) {
                localStorage.setItem('xz_tester_wakewordWsUrl', wakewordWsUrlInput.value.trim());
            }

            // Сравнение нового адреса и текущего адреса подключения
            const newWsUrl = localStorage.getItem('xz_tester_wakewordWsUrl');
            const currentWsUrl = getWakewordBridgeUrl();
            const urlChanged = newWsUrl !== currentWsUrl;

            if (urlChanged) {
                // Адрес изменился, сначала подтверждение
                const shouldRestart = window.confirm('Адрес изменился, продолжить? (Старое соединение будет разорвано и установлено новое)');
                if (!shouldRestart) {
                    // Восстановление старого адреса из localStorage
                    localStorage.setItem('xz_tester_wakewordWsUrl', currentWsUrl);
                    this.addChatMessage('Изменение адреса отменено.', false);
                    return;
                }

                // Разрыв старого соединения
                stopWakewordBridgeListener();

                // Запуск нового соединения (автоматическое использование нового адреса)
                startWakewordBridgeListener();

                // Ожидание bridge_connected
                await new Promise((resolve, reject) => {
                    const timeout = setTimeout(() => {
                        reject(new Error('Тайм-аут подключения к новому серверу'));
                    }, 5000);

                    onNextBridgeConnected(() => {
                        clearTimeout(timeout);
                        resolve();
                    });
                });

                // Отправка конфигурации и перезапуск
                await requestWakewordBridge('set_wakeword_config', payload);
                this.applyWakewordConfig(payload);
                await requestWakewordBridge('restart_wakeword_service');
                this.addChatMessage('Конфигурация слов активации сохранена, служба слов активации перезапускается.', false);
            } else {
                // Адрес не изменился:操作 непосредственно в текущем соединении
                const response = await requestWakewordBridge('set_wakeword_config', payload);
                this.applyWakewordConfig(response.payload || payload);

                const shouldRestart = window.confirm('Слова активации сохранены. Перезапустить службу слов активации сейчас для немедленного применения?');
                if (!shouldRestart) {
                    this.addChatMessage('Конфигурация слов активации сохранена, для применения можно перезапустить службу позже.', false);
                    return;
                }

                await requestWakewordBridge('restart_wakeword_service');
                this.addChatMessage('Конфигурация слов активации сохранена, служба слов активации перезапускается.', false);
            }
        } catch (error) {
            this.addChatMessage(`Ошибка применения слов активации: ${error.message}`, false);
        } finally {
            if (applyWakewordBtn) {
                applyWakewordBtn.disabled = false;
                applyWakewordBtn.textContent = 'Применить слова активации';
            }
        }
    }

    // Начало сеанса AI-чата после подключения
    startAIChatSession() {
        this.addChatMessage('Подключение успешно, начните общение~😊', false);
        // Проверка доступности микрофона и отображение сообщений об ошибках при необходимости
        if (!window.microphoneAvailable) {
            if (window.isHttpNonLocalhost) {
                this.addChatMessage('⚠️ В настоящее время из-за HTTP-доступа запись невозможна, доступно только текстовое взаимодействие', false);
            } else {
                this.addChatMessage('⚠️ Микрофон недоступен, проверьте настройки разрешений, доступно только текстовое взаимодействие', false);
            }
        }
        // Начало записи только при доступности микрофона
        if (window.microphoneAvailable) {
            const recordBtn = document.getElementById('recordBtn');
            if (recordBtn) {
                recordBtn.click();
            }
        }
        // Запуск камеры только при доступности камеры (привязанной кодом подтверждения)
        if (window.cameraAvailable && typeof window.startCamera === 'function') {
            window.startCamera().then(success => {
                if (success) {
                    const cameraBtn = document.getElementById('cameraBtn');
                    if (cameraBtn) {
                        cameraBtn.classList.add('camera-active');
                        cameraBtn.querySelector('.btn-text').textContent = 'Выкл.';
                    }
                } else {
                    this.addChatMessage('⚠️ Ошибка запуска камеры, возможно, отклонено браузером', false);
                }
            }).catch(error => {
                log(`Исключение при запуске камеры: ${error.message}`, 'error');
            });
        }
    }

    // Обработка нажатия кнопки подключения
    async handleConnect() {
        const wsHandler = getWebSocketHandler();
        if (this.isConnecting || (wsHandler && wsHandler.isConnected())) {
            log('Соединение уже существует или выполняется, пропуск данного запроса на набор номера', 'info');
            return;
        }

        this.isConnecting = true;
        console.log('Вызов handleConnect');

        try {
            // Переключение на вкладку настроек устройства
            this.switchTab('device');

            // Ожидание обновления DOM
            await new Promise(resolve => setTimeout(resolve, 50));

            const otaUrlInput = document.getElementById('otaUrl');

            console.log('Элемент otaUrl:', otaUrlInput);

            if (!otaUrlInput || !otaUrlInput.value) {
                this.addChatMessage('Пожалуйста, введите адрес OTA-сервера', false);
                return;
            }

            const otaUrl = otaUrlInput.value;
            console.log('Значение otaUrl:', otaUrl);

            // Обновление состояния кнопки набора номера на "Подключение"
            const dialBtn = document.getElementById('dialBtn');
            if (dialBtn) {
                dialBtn.classList.add('dial-active');
                dialBtn.querySelector('.btn-text').textContent = 'Подключение...';
                dialBtn.disabled = true;
            }

            // Отображение сообщения о подключении
            this.addChatMessage('Подключение к серверу...', false);

            const chatIpt = document.getElementById('chatIpt');
            if (chatIpt) {
                chatIpt.style.display = 'flex';
            }

            // Получение экземпляра обработчика WebSocket
            // Регистрация обратного вызова состояния подключения ДО подключения
            wsHandler.onConnectionStateChange = (isConnected) => {
                this.updateConnectionUI(isConnected);
                this.updateDialButton(isConnected);
            };

            // Регистрация обратного вызова сообщения чата ДО подключения
            wsHandler.onChatMessage = (text, isUser) => {
                this.addChatMessage(text, isUser);
            };

            // Регистрация обратного вызова состояния кнопки записи ДО подключения
            wsHandler.onRecordButtonStateChange = (isRecording) => {
                const recordBtn = document.getElementById('recordBtn');
                if (recordBtn) {
                    if (isRecording) {
                        recordBtn.classList.add('recording');
                        recordBtn.querySelector('.btn-text').textContent = 'Запись...';
                    } else {
                        recordBtn.classList.remove('recording');
                        recordBtn.querySelector('.btn-text').textContent = 'Запись';
                    }
                }
            };

            const isConnected = await wsHandler.connect();

            if (isConnected) {
                // Проверка доступности микрофона (повторная проверка после подключения)
                const { checkMicrophoneAvailability } = await import('../core/audio/recorder.js?v=0205');
                const micAvailable = await checkMicrophoneAvailability();

                if (!micAvailable) {
                    const isHttp = window.isHttpNonLocalhost;
                    if (isHttp) {
                        this.addChatMessage('⚠️ В настоящее время из-за HTTP-доступа запись невозможна, доступно только текстовое взаимодействие', false);
                    }
                    // Обновление глобального состояния
                    window.microphoneAvailable = false;
                }

                // Обновление состояния кнопки набора номера
                const dialBtn = document.getElementById('dialBtn');
                if (dialBtn) {
                    if (!this.dialBtnDisabled) {
                        dialBtn.disabled = false;
                    }
                    dialBtn.querySelector('.btn-text').textContent = 'Завершить';
                    dialBtn.classList.add('dial-active');
                }

                this.hideModal('settingsModal');
            } else {
                throw new Error('Ошибка подключения OTA');
            }
        } catch (error) {
            console.error('Подробности ошибки подключения:', {
                message: error.message,
                stack: error.stack,
                name: error.name
            });

            // Отображение сообщения об ошибке
            const errorMessage = error.message.includes('Cannot set properties of null')
                ? 'Ошибка подключения: проверьте подключение устройства'
                : `Ошибка подключения: ${error.message}`;

            this.addChatMessage(errorMessage, false);

            // Восстановление состояния кнопки набора номера
            const dialBtn = document.getElementById('dialBtn');
            if (dialBtn) {
                if (!this.dialBtnDisabled) {
                    dialBtn.disabled = false;
                }
                dialBtn.querySelector('.btn-text').textContent = 'Вызов';
                dialBtn.classList.remove('dial-active');
                console.log('Состояние кнопки набора номера успешно восстановлено');
            }
        } finally {
            this.isConnecting = false;
        }
    }

    async triggerWakewordDial(wakeWord = 'Слово активации') {
        const wsHandler = getWebSocketHandler();
        const now = Date.now();

        if (wsHandler && wsHandler.isConnected()) {
            log('Страница подключена, пропуск автоматического набора номера', 'info');
            return false;
        }

        if (this.isConnecting || this.dialBtnDisabled) {
            log('Страница подключается, пропуск повторной активации', 'info');
            return false;
        }

        if (now - this.lastWakewordDialTime < 3000) {
            log('Слишком частая активация, пропуск данного автоматического набора номера', 'warning');
            return false;
        }

        this.lastWakewordDialTime = now;
        this.addChatMessage(`Обнаружено слово активации "${wakeWord}", подготовка к подключению к серверу...`, false);
        await this.handleConnect();
        return true;
    }

    // Добавление инструмента MCP
    addMCPTool() {
        const mcpToolsList = document.getElementById('mcpToolsList');
        if (!mcpToolsList) return;

        const toolId = `mcp-tool-${Date.now()}`;
        const toolDiv = document.createElement('div');
        toolDiv.className = 'properties-container';
        toolDiv.innerHTML = `
            <div class="property-item">
                <input type="text" placeholder="Название инструмента" value="Новый инструмент">
                <input type="text" placeholder="Описание инструмента" value="Описание инструмента">
                <button class="remove-property" onclick="uiController.removeMCPTool('${toolId}')">Удалить</button>
            </div>
        `;

        mcpToolsList.appendChild(toolDiv);
    }

    // Удаление инструмента MCP
    removeMCPTool(toolId) {
        const toolElement = document.getElementById(toolId);
        if (toolElement) {
            toolElement.remove();
        }
    }

    // Обновление отображения статистики аудио
    updateAudioStats() {
        const audioPlayer = getAudioPlayer();
        if (!audioPlayer) return;

        const stats = audioPlayer.getAudioStats();
        // Здесь можно добавить логику обновления интерфейса статистики аудио
    }

    // Запуск мониторинга статистики аудио
    startAudioStatsMonitor() {
        // Обновление статистики аудио каждые 100 мс
        this.audioStatsTimer = setInterval(() => {
            this.updateAudioStats();
        }, 100);
    }

    // Остановка мониторинга статистики аудио
    stopAudioStatsMonitor() {
        if (this.audioStatsTimer) {
            clearInterval(this.audioStatsTimer);
            this.audioStatsTimer = null;
        }
    }

    // Отрисовка формы волны визуализатора аудио
    drawVisualizer(dataArray) {
        if (!this.visualizerContext || !this.visualizerCanvas) return;

        this.visualizerContext.fillStyle = '#fafafa';
        this.visualizerContext.fillRect(0, 0, this.visualizerCanvas.width, this.visualizerCanvas.height);

        const barWidth = (this.visualizerCanvas.width / dataArray.length) * 2.5;
        let barHeight;
        let x = 0;

        for (let i = 0; i < dataArray.length; i++) {
            barHeight = dataArray[i] / 2;

            // Создание градиентного цвета: от фиолетового к синему к зеленому
            const gradient = this.visualizerContext.createLinearGradient(0, 0, 0, this.visualizerCanvas.height);
            gradient.addColorStop(0, '#8e44ad');
            gradient.addColorStop(0.5, '#3498db');
            gradient.addColorStop(1, '#1abc9c');

            this.visualizerContext.fillStyle = gradient;
            this.visualizerContext.fillRect(x, this.visualizerCanvas.height - barHeight, barWidth, barHeight);
            x += barWidth + 1;
        }
    }

    // Обновление интерфейса статуса сеанса
    updateSessionStatus(isSpeaking) {
        // Здесь можно добавить логику обновления интерфейса статуса сеанса
        // Например: обновление статуса движения рта модели Live2D
    }

    // Обновление эмоции сеанса
    updateSessionEmotion(emoji) {
        // Здесь можно добавить логику обновления эмоции
        // Например: отображение эмодзи в индикаторе статуса
    }
}

// Создание синглтона
export const uiController = new UIController();

// Экспорт класса для использования в модуле
export { UIController };
