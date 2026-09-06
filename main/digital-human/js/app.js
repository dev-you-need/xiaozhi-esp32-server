// Основная точка входа приложения
import { checkOpusLoaded, initOpusEncoder } from './core/audio/opus-codec.js?v=0205';
import { getAudioPlayer } from './core/audio/player.js?v=0205';
import { checkMicrophoneAvailability, isHttpNonLocalhost } from './core/audio/recorder.js?v=0205';
import { initMcpTools } from './core/mcp/tools.js?v=0205';
import { startWakewordBridgeListener } from './core/network/wakeword-bridge.js?v=0205';
import { uiController } from './ui/controller.js?v=0205';
import { log } from './utils/logger.js?v=0205';

// Вспомогательная функция: преобразование Base64 данных в Blob
function dataURItoBlob(dataURI) {
    const byteString = atob(dataURI.split(',')[1]);
    const mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0];
    const ab = new ArrayBuffer(byteString.length);
    const ia = new Uint8Array(ab);
    for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }
    return new Blob([ab], { type: mimeString });
}

// Класс приложения
class App {
    constructor() {
        this.uiController = null;
        this.audioPlayer = null;
        this.live2dManager = null;
        this.cameraStream = null;
        this.currentFacingMode = 'user';
    }

    // Инициализация приложения
    async init() {
        log('Инициализация приложения...', 'info');
        // Инициализация контроллера интерфейса
        this.uiController = uiController;
        this.uiController.init();
        // Проверка библиотеки Opus
        checkOpusLoaded();
        // Инициализация кодера Opus
        initOpusEncoder();
        // Инициализация аудиоплеера
        this.audioPlayer = getAudioPlayer();
        await this.audioPlayer.start();
        // Инициализация инструментов MCP
        initMcpTools();
        // Инициализация локального прослушивания событий активации
        startWakewordBridgeListener();
        // Проверка доступности микрофона
        await this.checkMicrophoneAvailability();
        // Проверка доступности камеры
        this.checkCameraAvailability();
        // Инициализация Live2D
        await this.initLive2D();
        // Инициализация камеры
        this.initCamera();
        // Закрытие индикатора загрузки
        this.setModelLoadingStatus(false);
        log('Инициализация приложения завершена', 'success');
    }

    // Инициализация Live2D
    async initLive2D() {
        try {
            // Проверка загрузки Live2DManager
            if (typeof window.Live2DManager === 'undefined') {
                throw new Error('Live2DManager не загружен, проверьте порядок подключения скриптов');
            }
            this.live2dManager = new window.Live2DManager();
            await this.live2dManager.initializeLive2D();
            // Обновление состояния интерфейса
            const live2dStatus = document.getElementById('live2dStatus');
            if (live2dStatus) {
                live2dStatus.textContent = '● Загружено';
                live2dStatus.className = 'status loaded';
            }
            log('Инициализация Live2D завершена', 'success');
        } catch (error) {
            log(`Ошибка инициализации Live2D: ${error.message}`, 'error');
            // Обновление состояния интерфейса
            const live2dStatus = document.getElementById('live2dStatus');
            if (live2dStatus) {
                live2dStatus.textContent = '● Ошибка загрузки';
                live2dStatus.className = 'status error';
            }
        }
    }

    // Установка статуса загрузки модели
    setModelLoadingStatus(isLoading) {
        const modelLoading = document.getElementById('modelLoading');
        if (modelLoading) {
            modelLoading.style.display = isLoading ? 'flex' : 'none';
        }
    }

    /**
     * Проверка доступности микрофона
     * Вызывается при инициализации приложения для проверки доступности микрофона и обновления состояния интерфейса
     */
    async checkMicrophoneAvailability() {
        try {
            const isAvailable = await checkMicrophoneAvailability();
            const isHttp = isHttpNonLocalhost();
            // Сохранение состояния доступности в глобальной переменной
            window.microphoneAvailable = isAvailable;
            window.isHttpNonLocalhost = isHttp;
            // Обновление интерфейса
            if (this.uiController) {
                this.uiController.updateMicrophoneAvailability(isAvailable, isHttp);
            }
            log(`Проверка доступности микрофона завершена: ${isAvailable ? 'Доступен' : 'Недоступен'}`, isAvailable ? 'success' : 'warning');
        } catch (error) {
            log(`Ошибка проверки доступности микрофона: ${error.message}`, 'error');
            // Установка значения по умолчанию как недоступен
            window.microphoneAvailable = false;
            window.isHttpNonLocalhost = isHttpNonLocalhost();
            if (this.uiController) {
                this.uiController.updateMicrophoneAvailability(false, window.isHttpNonLocalhost);
            }
        }
    }

    // Проверка доступности камеры
    checkCameraAvailability() {
        window.cameraAvailable = true;
        log('Проверка доступности камеры завершена: по умолчанию привязан код подтверждения', 'success');
    }

    // Инициализация камеры
    async initCamera() {
        const cameraContainer = document.getElementById('cameraContainer');
        const cameraVideo = document.getElementById('cameraVideo');
        const cameraSwitch = document.getElementById('cameraSwitch');
        const cameraSwitchMask = document.getElementById('cameraSwitchMask');
        const dialBtn = document.getElementById('dialBtn');

        if (!cameraContainer || !cameraVideo) {
            log('Элементы камеры не найдены, пропуск инициализации', 'warning');
            return Promise.resolve(false);
        }

        let isDragging = false;
        let currentX, currentY, initialX, initialY;
        let xOffset = 0, yOffset = 0;

        cameraContainer.addEventListener('mousedown', dragStart);
        document.addEventListener('mousemove', drag);
        document.addEventListener('mouseup', dragEnd);
        cameraContainer.addEventListener('touchstart', dragStart, { passive: false });
        document.addEventListener('touchmove', drag, { passive: false });
        document.addEventListener('touchend', dragEnd);

        function dragStart(e) {
            if (e.type === 'touchstart') {
                initialX = e.touches[0].clientX - xOffset;
                initialY = e.touches[0].clientY - yOffset;
            } else {
                initialX = e.clientX - xOffset;
                initialY = e.clientY - yOffset;
            }
            isDragging = true;
            cameraContainer.classList.add('dragging');
        }

        function drag(e) {
            if (isDragging) {
                e.preventDefault();
                if (e.type === 'touchmove') {
                    currentX = e.touches[0].clientX - initialX;
                    currentY = e.touches[0].clientY - initialY;
                } else {
                    currentX = e.clientX - initialX;
                    currentY = e.clientY - initialY;
                }
                xOffset = currentX;
                yOffset = currentY;
                cameraContainer.style.transform = `translate3d(${currentX}px, ${currentY}px, 0)`;
            }
        }

        function dragEnd() {
            initialX = currentX;
            initialY = currentY;
            isDragging = false;
            cameraContainer.classList.remove('dragging');
        }

        return new Promise((resolve) => {
            window.startCamera = async () => {
                try {
                    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                        log('Браузер не поддерживает API камеры', 'warning');
                        return false;
                    }
                    log('Запрос разрешения на использование камеры...', 'info');
                    this.cameraStream = await navigator.mediaDevices.getUserMedia({
                        video: { width: 180, height: 240, facingMode: this.currentFacingMode },
                        audio: false
                    });
                    cameraVideo.srcObject = this.cameraStream;
                    const devices = await navigator.mediaDevices.enumerateDevices();
                    const videoDevices = devices.filter(device => device.kind === 'videoinput');
                    if (videoDevices.length > 1) {
                        if (cameraSwitch) cameraSwitch.classList.add('active'); 
                    }
                    cameraContainer.classList.add('active');

                    // Случай завершения при переключении
                    const hasActive = dialBtn.classList.contains('dial-active');
                    if (!hasActive) {
                        cameraContainer.classList.remove('active');
                        cameraSwitch.classList.remove('active');
                        window.stopCamera();
                    }
                    log('Камера запущена', 'success');
                    return true;
                } catch (error) {
                    log(`Ошибка запуска камеры: ${error.name} - ${error.message}`, 'error');
                    if (error.name === 'NotAllowedError') {
                        log('Доступ к камере отклонен, проверьте настройки браузера', 'warning');
                    } else if (error.name === 'NotFoundError') {
                        log('Камера не найдена', 'warning');
                    } else if (error.name === 'NotReadableError') {
                        log('Камера занята другой программой', 'warning');
                    }
                    return false;
                }
            };

            window.stopCamera = () => {
                if (this.cameraStream) {
                    this.cameraStream.getTracks().forEach(track => track.stop());
                    this.cameraStream = null;
                    cameraVideo.srcObject = null;
                    log('Камера отключена', 'info');
                }
            };

            window.switchCamera = async() => {
                if (window.switchCameraTimer) return;
                if (this.cameraStream) {
                    const currentTransform = window.getComputedStyle(cameraContainer).transform;
                    const originalTransform = currentTransform === 'none' ? 'translate(0px, 0px)' : currentTransform;
                    cameraContainer.style.setProperty('--original-transform', originalTransform);
                    cameraContainer.classList.add('flip');
                    if (cameraSwitchMask) cameraSwitchMask.style.opacity = 0; 
                    this.currentFacingMode = this.currentFacingMode === 'user' ? 'environment' : 'user';
                    window.stopCamera();
                    window.startCamera();
                    
                    window.switchCameraTimer = setTimeout(() => {
                        if (this.currentFacingMode === 'user') {
                            cameraVideo.style.transform = 'scaleX(-1)';
                        } else {
                            cameraVideo.style.transform = 'scaleX(1)';
                        }
                        window.switchCameraTimer = null;
                        cameraContainer.classList.remove('flip');
                        cameraContainer.style.removeProperty('--original-transform');
                        if (cameraSwitchMask) cameraSwitchMask.style.opacity = 1; 
                    }, 500);
                }
            };

            window.takePhoto = (question = 'Опишите увиденные предметы') => {
                return new Promise(async (resolve) => {
                    const canvas = document.createElement('canvas');
                    const video = cameraVideo;

                    if (!video || video.readyState !== video.HAVE_ENOUGH_DATA) {
                        log('Не удалось сделать снимок: камера не готова', 'warning');
                        resolve({
                            success: false,
                            error: 'Камера не готова, убедитесь, что она подключена и запущена'
                        });
                        return;
                    }

                    canvas.width = video.videoWidth || 180;
                    canvas.height = video.videoHeight || 240;
                    const ctx = canvas.getContext('2d');
                    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

                    const photoData = canvas.toDataURL('image/jpeg', 0.8);
                    log(`Снимок сделан успешно, длина данных изображения: ${photoData.length}`, 'success');

                    try {
                        const xz_tester_vision = localStorage.getItem('xz_tester_vision');
                        if (xz_tester_vision) {
                            let visionInfo = null;

                            try {
                                visionInfo = JSON.parse(xz_tester_vision);
                            } catch (err) {
                                throw new Error(`Ошибка разбора конфигурации визуального анализа`);
                            }

                            const { url, token } = visionInfo || {};
                            if (!url || !token) {
                                throw new Error('Ошибка визуального анализа: в конфигурации отсутствует адрес интерфейса (url) или токен (token)');
                            }

                            log(`Отправка изображения в интерфейс визуального анализа: ${url}`, 'info');

                            const deviceId = document.getElementById('deviceMac')?.value || '';
                            const clientId = document.getElementById('clientId')?.value || 'web_test_client';

                            const formData = new FormData();
                            formData.append('question', question);
                            formData.append('image', dataURItoBlob(photoData), 'photo.jpg');

                            const response = await fetch(url, {
                                method: 'POST',
                                body: formData,
                                headers: {
                                    'Device-Id': deviceId,
                                    'Client-Id': clientId,
                                    'Authorization': `Bearer ${token}`
                                }
                            });

                            if (!response.ok) {
                                throw new Error(`HTTP ошибка! Статус: ${response.status}`);
                            }

                            const analysisResult = await response.json();
                            log(`Визуальный анализ завершен: ${JSON.stringify(analysisResult).substring(0, 200)}...`, 'success');

                            resolve({
                                success: true,
                                message: question,
                                photo_data: photoData,
                                photo_width: canvas.width,
                                photo_height: canvas.height,
                                vision_analysis: analysisResult
                            });
                        } else {
                            log('Служба визуального анализа не настроена', 'warning');
                        }
                    } catch (error) {
                        log(`Ошибка визуального анализа: ${error.message}`, 'error');
                        resolve({
                            success: true,
                            message: question,
                            photo_data: photoData,
                            photo_width: canvas.width,
                            photo_height: canvas.height,
                            vision_analysis: {
                                success: false,
                                error: error.message,
                                fallback: 'Не удалось подключиться к службе визуального анализа'
                            }
                        });
                    }
                });
            };

            log('Инициализация камеры завершена', 'success');
            resolve(true);
        });
    }
}

// Создание и запуск приложения
const app = new App();
// Экспорт экземпляра приложения в глобальную область для доступа из других модулей
window.chatApp = app;
document.addEventListener('DOMContentLoaded', () => {
    // Инициализация приложения
    app.init();
});
export default app;
