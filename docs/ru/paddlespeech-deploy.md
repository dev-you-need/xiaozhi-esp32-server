# Интеграция PaddleSpeechTTS с сервисом xiaozhi

## Важные замечания
- Плюсы: локальное офлайн-развёртывание, высокая скорость
- Минусы: по состоянию на 25 сентября 2025 года модель по умолчанию — китайская, английский текст в речь не преобразуется. Если в тексте есть английские слова, звука не будет; для поддержки китайского и английского одновременно модель нужно обучать самостоятельно.

## 1. Базовые требования к окружению
Операционная система: Windows / Linux / WSL 2

Версия Python: 3.9 и выше (сверьтесь с официальным руководством Paddle)

Версия Paddle: последняя официальная версия   ```https://www.paddlepaddle.org.cn/install```

Инструмент управления зависимостями: conda или venv

## 2. Запуск сервиса paddlespeech
### 1. Клонируйте исходный код из официального репозитория paddlespeech
```bash 
git clone https://github.com/PaddlePaddle/PaddleSpeech.git
```
### 2. Создайте виртуальное окружение
```bash

conda create -n paddle_env python=3.10 -y
conda activate paddle_env
```
### 3. Установите paddle
В зависимости от архитектуры CPU и GPU создайте окружение в соответствии с версиями Python, поддерживаемыми официально Paddle  
```
https://www.paddlepaddle.org.cn/install
```

### 4. Перейдите в каталог paddlespeech
```bash
cd PaddleSpeech
```
### 5. Установите paddlespeech
```bash
pip install pytest-runner -i https://pypi.tuna.tsinghua.edu.cn/simple

# используйте любую из команд ниже
pip install paddlepaddle -i https://mirror.baidu.com/pypi/simple
pip install paddlespeech -i https://pypi.tuna.tsinghua.edu.cn/simple
```
### 6. Автоматически загрузите речевую модель командой
```bash
paddlespeech tts --input "你好，这是一次测试"
```
На этом шаге модель автоматически загрузится и будет закэширована локально в каталоге .paddlespeech/models

### 7. Измените конфигурацию tts_online_application.yaml
Справочный каталог ```"PaddleSpeech\demos\streaming_tts_server\conf\tts_online_application.yaml"```
Откройте файл ```tts_online_application.yaml``` в редакторе и установите ```protocol``` равным ```websocket```

### 8. Запустите сервис
```yaml
paddlespeech_server start --config_file ./demos/streaming_tts_server/conf/tts_online_application.yaml
# официальная команда запуска по умолчанию:
paddlespeech_server start --config_file ./conf/tts_online_application.yaml
```
Запустите команду с учётом фактического расположения вашего файла ```tts_online_application.yaml```. Если в логах появится следующее, значит сервис запущен успешно
```
Prefix dict has been built successfully.
[2025-08-07 10:03:11,312] [   DEBUG] __init__.py:166 - Prefix dict has been built successfully.
INFO:     Started server process [2298]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8092 (Press CTRL+C to quit)
```

## 3. Изменение конфигурации xiaozhi
### 1.```main/xiaozhi-server/core/providers/tts/paddle_speech.py```

### 2.```main/xiaozhi-server/data/.config.yaml```
При развёртывании одного модуля
```yaml
selected_module:
  TTS: PaddleSpeechTTS
TTS:
  PaddleSpeechTTS:
      type: paddle_speech
      protocol: websocket 
      url:  ws://127.0.0.1:8092/paddlespeech/tts/streaming  # TTS 服务的 URL 地址，指向本地服务器 [websocket默认ws://127.0.0.1:8092/paddlespeech/tts/streaming]
      spk_id: 0  # 发音人 ID，0 通常表示默认的发音人
      sample_rate: 24000  # 采样率 [websocket默认24000，http默认0 自动选择]
      speed: 1.0  # 语速，1.0 表示正常语速，>1 表示加快，<1 表示减慢
      volume: 1.0  # 音量，1.0 表示正常音量，>1 表示增大，<1 表示减小
      save_path:   # 保存路径
```
### 3. Запустите сервис xiaozhi
```py
python app.py
```
После запуска `python start.py` в каталоге `main/digital-human` откройте `http://127.0.0.1:8006/index.html`, проверьте подключение и отправку сообщений, и посмотрите, появляются ли логи на стороне paddlespeech

Пример вывода логов:
```
INFO:     127.0.0.1:44312 - "WebSocket /paddlespeech/tts/streaming" [accepted]
INFO:     connection open
[2025-08-07 11:16:33,355] [    INFO] - sentence: 哈哈，怎么突然找我聊天啦？
[2025-08-07 11:16:33,356] [    INFO] - The durations of audio is: 2.4625 s
[2025-08-07 11:16:33,356] [    INFO] - first response time: 0.1143045425415039 s
[2025-08-07 11:16:33,356] [    INFO] - final response time: 0.4777836799621582 s
[2025-08-07 11:16:33,356] [    INFO] - RTF: 0.19402382942625715
[2025-08-07 11:16:33,356] [    INFO] - Other info: front time: 0.06514096260070801 s, first am infer time: 0.008037090301513672 s, first voc infer time: 0.04112648963928223 s,
[2025-08-07 11:16:33,356] [    INFO] - Complete the synthesis of the audio streams
INFO:     connection closed

```
