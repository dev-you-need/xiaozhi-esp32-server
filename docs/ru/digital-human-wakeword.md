# Способ запуска цифрового человека digital-human

## Обзор

Тестовая страница включает высокоточную функцию голосового пробуждения на основе **Sherpa-ONNX** с поддержкой пользовательских слов активации и обнаружения в реальном времени. Используется лёгкая модель обнаружения ключевых слов, обеспечивающая отклик за миллисекунды.

## Модель слов активации

### Загрузка модели (обязательно)

**Важное замечание**: модель не входит в состав проекта и должна быть заранее загружена и настроена.

### Официальные адреса загрузки моделей

- **Официальный список моделей**: <https://csukuangfj.github.io/sherpa/onnx/kws/pretrained_models/index.html>
- **Рекомендуемая модель**: `sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01`

### Шаги загрузки и настройки

#### 1. Загрузите пакет модели

```bash
# 方法1：直接下载（推荐）
cd main/digital-human/wakeword_runtime/
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2

# 解压
tar xvf sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01.tar.bz2

# 方法2：使用ModelScope
pip install modelscope
python -c "
from modelscope import snapshot_download
snapshot_download('pkufool/sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01', cache_dir='./models')
"
```

#### 2. Настройте файлы модели

После загрузки пакет модели содержит следующие файлы:

```
sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01/
├── encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx    # 速度优先
├── encoder-epoch-12-avg-2-chunk-16-left-64.onnx
├── encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx    # 速度优先
├── encoder-epoch-99-avg-1-chunk-16-left-64.onnx         # 精度优先
├── decoder-epoch-12-avg-2-chunk-16-left-64.onnx
├── decoder-epoch-99-avg-1-chunk-16-left-64.onnx         # 精度优先
├── joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx     # 速度优先
├── joiner-epoch-12-avg-2-chunk-16-left-64.onnx
├── joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx     # 速度优先
├── joiner-epoch-99-avg-1-chunk-16-left-64.onnx          # 精度优先
├── tokens.txt                    # Token映射表（必需）
├── keywords_raw.txt              # 模型包里可能附带（可选，runtime 不依赖）
├── keywords.txt                  # 现成的
├── test_wavs/                    # 测试音频（可选）
├── configuration.json            # 模型元信息（可选）
└── README.md                     # 说明文档（可选）
```

#### 3. Выберите схему настройки

**Вариант 1: приоритет точности (рекомендуется)**

```bash
cd sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01

# 创建模型目录
mkdir -p ../models

# 复制精度优先的epoch-99 fp32三件套
cp encoder-epoch-99-avg-1-chunk-16-left-64.onnx ../models/encoder.onnx
cp decoder-epoch-99-avg-1-chunk-16-left-64.onnx ../models/decoder.onnx
cp joiner-epoch-99-avg-1-chunk-16-left-64.onnx ../models/joiner.onnx

# 复制配套文件
cp tokens.txt ../models/tokens.txt
# keywords_raw.txt 如果模型包里附带，可自行保留；runtime 不依赖它
```

**Вариант 2: приоритет скорости**

```bash
cd sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01

# 创建模型目录
mkdir -p ../models

# 复制速度优先的epoch-99 int8三件套
cp encoder-epoch-99-avg-1-chunk-16-left-64.int8.onnx ../models/encoder.onnx
cp decoder-epoch-99-avg-1-chunk-16-left-64.onnx ../models/decoder.onnx
cp joiner-epoch-99-avg-1-chunk-16-left-64.int8.onnx ../models/joiner.onnx

# 复制配套文件
cp tokens.txt ../models/tokens.txt
```

**Примечания**:

- **Не смешивайте fp32 и int8**: все три файла модели должны быть одной и той же точности
- **Предпочитайте epoch-99**: она обучена тщательнее, чем epoch-12, и даёт более высокую точность
- **Обязательные файлы**: `encoder.onnx` + `decoder.onnx` + `joiner.onnx` + `tokens.txt` + `keywords.txt`

### Итоговая структура файлов модели

После настройки файлы модели должны находиться в каталоге `wakeword_runtime/models/`, полный путь — `main/digital-human/wakeword_runtime/models/`:

```
wakeword_runtime/models/
├── encoder.onnx      # 编码器模型（重命名后）
├── decoder.onnx      # 解码器模型（重命名后）
├── joiner.onnx       # 连接器模型（重命名后）
├── tokens.txt        # 拼音 Token 映射表（228行版本）
├── keywords.txt      # 关键词配置文件（首次启动自动生成）
└── keywords_raw.txt  # 可选，runtime 不依赖
```

## Способ запуска

Выполните в каталоге `main/digital-human`:

```bash
pip install -r wakeword_runtime/requirements.txt
python start.py
```

Адреса по умолчанию после запуска:

- Страница: `http://127.0.0.1:8006/index.html`
- Мост событий: `ws://127.0.0.1:8006/wakeword-ws`
- Проверка работоспособности: `http://127.0.0.1:8006/health`

Способ остановки:

- Нажмите `Ctrl+C` в терминале, где выполняется запуск
- При этом одновременно остановятся служба статических страниц, мост событий и процесс обнаружения слов активации

## Описание конфигурационного файла

Конфигурационный файл находится по адресу [main/digital-human/wakeword_runtime/config.json](../../main/digital-human/wakeword_runtime/config.json).

Основные настройки в настоящее время:

```json
{
  "wakeword": {
    "enabled": true
  },
  "model_dir": "models",
  "audio": {
    "input_device": null,
    "sample_rate": 16000,
    "channels": 1
  },
  "detector": {
    "num_threads": 4,
    "provider": "cpu",
    "max_active_paths": 2,
    "keywords_score": 1.8,
    "keywords_threshold": 0.1,
    "num_trailing_blanks": 1,
    "cooldown_seconds": 1.5
  },
  "logging": {
    "level": "INFO",
    "dir": "logs",
    "file": "wakeword-runtime.log"
  }
}
```

Значения полей:

| Параметр | Описание |
| --- | --- |
| `wakeword.enabled` | Включать ли локальное обнаружение слов активации |
| `model_dir` | Каталог с моделью и словарём |
| `audio.input_device` | Устройство ввода микрофона; по умолчанию используется системное устройство по умолчанию |
| `audio.sample_rate` | Частота дискретизации, по умолчанию `16000` |
| `audio.channels` | Число каналов, по умолчанию `1` |
| `detector.num_threads` | Число потоков детектора |
| `detector.provider` | Провайдер вывода, в настоящее время обычно `cpu` |
| `detector.max_active_paths` | Число путей поиска |
| `detector.keywords_score` | Бонусный балл ключевых слов |
| `detector.keywords_threshold` | Порог обнаружения |
| `detector.num_trailing_blanks` | Число завершающих пробелов |
| `detector.cooldown_seconds` | Время охлаждения между последовательными срабатываниями |
| `logging.level` | Уровень журналирования |
| `logging.dir` | Каталог журналов |
| `logging.file` | Имя файла журнала |

## Рекомендуемый порядок использования

### Первое использование

1. Подготовьте файлы модели и `tokens.txt` в каталоге `models/`
2. Убедитесь, что `models/keywords.txt` существует
3. В каталоге `digital-human` запустите `python start.py`
4. Откройте в браузере `http://127.0.0.1:8006/index.html`
5. Перейдите на страницу настроек и проверьте конфигурацию «слова активации»

### Изменение слов активации

1. Откройте настройки страницы цифрового человека
2. Перейдите на вкладку «Слова активации»
3. Измените состояние включения или список слов активации
4. Нажмите «Применить слова активации»
5. Следуя подсказке, решите, нужно ли перезапустить немедленно

### Отключение слов активации

1. Измените «Включить локальные слова активации» на отключено
2. Нажмите «Применить слова активации»
3. Рекомендуется один раз сразу перезапустить

После отключения:

- Страница и мост событий остаются доступными
- Обнаружение слов активации не выполняется
