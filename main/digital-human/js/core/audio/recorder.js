// Модуль записи аудио
import { log } from '../../utils/logger.js?v=0205';
import { initOpusEncoder } from './opus-codec.js?v=0205';
import { getAudioPlayer } from './player.js?v=0205';

// Класс аудиорекордера
export class AudioRecorder {
    constructor() {
        this.isRecording = false;
        this.audioContext = null;
        this.analyser = null;
        this.audioProcessor = null;
        this.audioProcessorType = null;
        this.audioSource = null;
        this.opusEncoder = null;
        this.pcmDataBuffer = new Int16Array();
        this.audioBuffers = [];
        this.totalAudioSize = 0;
        this.visualizationRequest = null;
        this.recordingTimer = null;
        this.websocket = null;
        // Функции обратного вызова
        this.onRecordingStart = null;
        this.onRecordingStop = null;
        this.onVisualizerUpdate = null;
    }

    // Установка экземпляра WebSocket
    setWebSocket(ws) {
        this.websocket = ws;
    }

    // Получение экземпляра AudioContext
    getAudioContext() {
        return getAudioPlayer().getAudioContext();
    }

    // Инициализация кодера
    initEncoder() {
        if (!this.opusEncoder) {
            this.opusEncoder = initOpusEncoder();
        }
        return this.opusEncoder;
    }

    // Код обработчика PCM
    getAudioProcessorCode() {
        return `
            class AudioRecorderProcessor extends AudioWorkletProcessor {
                constructor() {
                    super();
                    this.buffers = [];
                    this.frameSize = 960;
                    this.buffer = new Int16Array(this.frameSize);
                    this.bufferIndex = 0;
                    this.isRecording = false;
                    this.port.onmessage = (event) => {
                        if (event.data.command === 'start') {
                            this.isRecording = true;
                            this.port.postMessage({ type: 'status', status: 'started' });
                        } else if (event.data.command === 'stop') {
                            this.isRecording = false;
                            if (this.bufferIndex > 0) {
                                const finalBuffer = this.buffer.slice(0, this.bufferIndex);
                                this.port.postMessage({ type: 'buffer', buffer: finalBuffer });
                                this.bufferIndex = 0;
                            }
                            this.port.postMessage({ type: 'status', status: 'stopped' });
                        }
                    };
                }
                process(inputs, outputs, parameters) {
                    if (!this.isRecording) return true;
                    const input = inputs[0][0];
                    if (!input) return true;
                    for (let i = 0; i < input.length; i++) {
                        if (this.bufferIndex >= this.frameSize) {
                            this.port.postMessage({ type: 'buffer', buffer: this.buffer.slice(0) });
                            this.bufferIndex = 0;
                        }
                        this.buffer[this.bufferIndex++] = Math.max(-32768, Math.min(32767, Math.floor(input[i] * 32767)));
                    }
                    return true;
                }
            }
            registerProcessor('audio-recorder-processor', AudioRecorderProcessor);
        `;
    }

    // Создание аудиообработчика
    async createAudioProcessor() {
        this.audioContext = this.getAudioContext();
        try {
            if (this.audioContext.audioWorklet) {
                const blob = new Blob([this.getAudioProcessorCode()], { type: 'application/javascript' });
                const url = URL.createObjectURL(blob);
                await this.audioContext.audioWorklet.addModule(url);
                URL.revokeObjectURL(url);
                const audioProcessor = new AudioWorkletNode(this.audioContext, 'audio-recorder-processor');
                audioProcessor.port.onmessage = (event) => {
                    if (event.data.type === 'buffer') {
                        this.processPCMBuffer(event.data.buffer);
                    }
                };
                log('Использование AudioWorklet для обработки аудио', 'success');
                const silent = this.audioContext.createGain();
                silent.gain.value = 0;
                audioProcessor.connect(silent);
                silent.connect(this.audioContext.destination);
                return { node: audioProcessor, type: 'worklet' };
            } else {
                log('AudioWorklet недоступен, использование ScriptProcessorNode в качестве резервного варианта', 'warning');
                return this.createScriptProcessor();
            }
        } catch (error) {
            log(`Ошибка создания аудиообработчика: ${error.message}, попытка резервного варианта`, 'error');
            return this.createScriptProcessor();
        }
    }

    // Создание ScriptProcessor в качестве резервного варианта
    createScriptProcessor() {
        try {
            const frameSize = 4096;
            const scriptProcessor = this.audioContext.createScriptProcessor(frameSize, 1, 1);
            scriptProcessor.onaudioprocess = (event) => {
                if (!this.isRecording) return;
                const input = event.inputBuffer.getChannelData(0);
                const buffer = new Int16Array(input.length);
                for (let i = 0; i < input.length; i++) {
                    buffer[i] = Math.max(-32768, Math.min(32767, Math.floor(input[i] * 32767)));
                }
                this.processPCMBuffer(buffer);
            };
            const silent = this.audioContext.createGain();
            silent.gain.value = 0;
            scriptProcessor.connect(silent);
            silent.connect(this.audioContext.destination);
            log('Успешное использование ScriptProcessorNode в качестве резервного варианта', 'warning');
            return { node: scriptProcessor, type: 'processor' };
        } catch (fallbackError) {
            log(`Резервный вариант также не удался: ${fallbackError.message}`, 'error');
            return null;
        }
    }

    // Обработка данных PCM-буфера
    processPCMBuffer(buffer) {
        if (!this.isRecording) return;
        const newBuffer = new Int16Array(this.pcmDataBuffer.length + buffer.length);
        newBuffer.set(this.pcmDataBuffer);
        newBuffer.set(buffer, this.pcmDataBuffer.length);
        this.pcmDataBuffer = newBuffer;
        const samplesPerFrame = 960;
        while (this.pcmDataBuffer.length >= samplesPerFrame) {
            const frameData = this.pcmDataBuffer.slice(0, samplesPerFrame);
            this.pcmDataBuffer = this.pcmDataBuffer.slice(samplesPerFrame);
            this.encodeAndSendOpus(frameData);
        }
    }

    // Кодирование и отправка данных Opus
    encodeAndSendOpus(pcmData = null) {
        if (!this.opusEncoder) {
            log('Кодер Opus не инициализирован', 'error');
            return;
        }
        try {
            if (pcmData) {
                const opusData = this.opusEncoder.encode(pcmData);
                if (opusData && opusData.length > 0) {
                    this.audioBuffers.push(opusData.buffer);
                    this.totalAudioSize += opusData.length;
                    if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
                        try {
                            this.websocket.send(opusData.buffer);
                        } catch (error) {
                            log(`Ошибка отправки WebSocket: ${error.message}`, 'error');
                        }
                    }
                } else {
                    log('Ошибка кодирования Opus, не возвращены допустимые данные', 'error');
                }
            } else {
                if (this.pcmDataBuffer.length > 0) {
                    const samplesPerFrame = 960;
                    if (this.pcmDataBuffer.length < samplesPerFrame) {
                        const paddedBuffer = new Int16Array(samplesPerFrame);
                        paddedBuffer.set(this.pcmDataBuffer);
                        this.encodeAndSendOpus(paddedBuffer);
                    } else {
                        this.encodeAndSendOpus(this.pcmDataBuffer.slice(0, samplesPerFrame));
                    }
                    this.pcmDataBuffer = new Int16Array(0);
                }
            }
        } catch (error) {
            log(`Ошибка кодирования Opus: ${error.message}`, 'error');
        }
    }

    // Начало записи
    async start() {
        if (this.isRecording) return false;
        try {
            if (!this.initEncoder()) {
                log('Невозможно начать запись: ошибка инициализации кодера Opus', 'error');
                return false;
            }
            log('Записывайте аудио не менее 1-2 секунд для сбора достаточного количества данных', 'info');
            const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, sampleRate: 16000, channelCount: 1 } });
            this.audioContext = this.getAudioContext();
            if (this.audioContext.state === 'suspended') {
                await this.audioContext.resume();
            }
            const processorResult = await this.createAudioProcessor();
            if (!processorResult) {
                log('Не удалось создать аудиообработчик', 'error');
                return false;
            }
            this.audioProcessor = processorResult.node;
            this.audioProcessorType = processorResult.type;
            this.audioSource = this.audioContext.createMediaStreamSource(stream);
            this.analyser = this.audioContext.createAnalyser();
            this.analyser.fftSize = 2048;
            this.audioSource.connect(this.analyser);
            this.audioSource.connect(this.audioProcessor);
            this.pcmDataBuffer = new Int16Array();
            this.audioBuffers = [];
            this.totalAudioSize = 0;
            this.isRecording = true;
            if (this.audioProcessorType === 'worklet' && this.audioProcessor.port) {
                this.audioProcessor.port.postMessage({ command: 'start' });
            }
            // Отправка сообщения о начале прослушивания
            if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
                log(`Отправлено сообщение о начале записи`, 'info');
            } else {
                log('WebSocket не подключен, невозможно отправить сообщение о начале', 'error');
                return false;
            }
            // Начало визуализации
            if (this.onVisualizerUpdate) {
                const dataArray = new Uint8Array(this.analyser.frequencyBinCount);
                this.startVisualization(dataArray);
            }
            // Немедленное уведомление о начале записи, обновление состояния кнопки
            if (this.onRecordingStart) {
                this.onRecordingStart(0);
            }
            // Запуск таймера записи
            let recordingSeconds = 0;
            this.recordingTimer = setInterval(() => {
                recordingSeconds += 0.1;
                if (this.onRecordingStart) {
                    this.onRecordingStart(recordingSeconds);
                }
            }, 100);
            log('Начата прямая PCM-запись', 'success');
            return true;
        } catch (error) {
            log(`Ошибка запуска прямой записи: ${error.message}`, 'error');
            this.isRecording = false;
            return false;
        }
    }

    // Начало визуализации
    startVisualization(dataArray) {
        const draw = () => {
            this.visualizationRequest = requestAnimationFrame(() => draw());
            if (!this.isRecording) return;
            this.analyser.getByteFrequencyData(dataArray);
            if (this.onVisualizerUpdate) {
                this.onVisualizerUpdate(dataArray);
            }
        };
        draw();
    }

    // Остановка записи
    stop() {
        if (!this.isRecording) return false;
        try {
            this.isRecording = false;
            if (this.audioProcessor) {
                if (this.audioProcessorType === 'worklet' && this.audioProcessor.port) {
                    this.audioProcessor.port.postMessage({ command: 'stop' });
                }
                this.audioProcessor.disconnect();
                this.audioProcessor = null;
            }
            if (this.audioSource) {
                this.audioSource.disconnect();
                this.audioSource = null;
            }
            if (this.visualizationRequest) {
                cancelAnimationFrame(this.visualizationRequest);
                this.visualizationRequest = null;
            }
            if (this.recordingTimer) {
                clearInterval(this.recordingTimer);
                this.recordingTimer = null;
            }
            // Кодирование и отправка оставшихся данных
            this.encodeAndSendOpus();
            // Отправка сигнала завершения
            if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
                const emptyOpusFrame = new Uint8Array(0);
                this.websocket.send(emptyOpusFrame);
                log('Отправлен сигнал остановки записи', 'info');
            }
            if (this.onRecordingStop) {
                this.onRecordingStop();
            }
            log('Прямая PCM-запись остановлена', 'success');
            return true;
        } catch (error) {
            log(`Ошибка остановки прямой записи: ${error.message}`, 'error');
            return false;
        }
    }

    // Получение анализатора
    getAnalyser() {
        return this.analyser;
    }
}

// Создание синглтона
let audioRecorderInstance = null;

export function getAudioRecorder() {
    if (!audioRecorderInstance) {
        audioRecorderInstance = new AudioRecorder();
    }
    return audioRecorderInstance;
}

/**
 * Проверка доступности микрофона
 * @returns {Promise<boolean>} Возвращает true, если доступен, false, если недоступен
 */
export async function checkMicrophoneAvailability() {
    // Проверка поддержки браузером API getUserMedia
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        log('Браузер не поддерживает API getUserMedia', 'warning');
        return false;
    }
    try {
        // Попытка доступа к микрофону
        const stream = await navigator.mediaDevices.getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, sampleRate: 16000, channelCount: 1 } });
        // Немедленная остановка всех дорожек для освобождения микрофона
        stream.getTracks().forEach(track => track.stop());
        log('Проверка доступности микрофона прошла успешно', 'success');
        return true;
    } catch (error) {
        log(`Микрофон недоступен: ${error.message}`, 'warning');
        return false;
    }
}

/**
 * Проверка, является ли это доступом HTTP не через localhost
 * @returns {boolean} Возвращает true, если это доступ HTTP не через localhost
 */
export function isHttpNonLocalhost() {
    const protocol = window.location.protocol;
    const hostname = window.location.hostname;
    // Проверка, является ли это протоколом HTTP
    if (protocol !== 'http:') {
        return false;
    }
    // localhost и 127.0.0.1 могут использовать микрофон
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return false;
    }
    // Частные IP-адреса также могут использовать микрофон (браузер разрешает)
    if (hostname.startsWith('192.168.') || hostname.startsWith('10.') || hostname.startsWith('172.')) {
        return false;
    }
    // Другие HTTP-доступы считаются не через localhost
    return true;
}
