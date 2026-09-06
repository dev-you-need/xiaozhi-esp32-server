# Руководство по интеграции компонента памяти PowerMem

## Введение

[PowerMem](https://www.powermem.ai/) — это компонент памяти для агентов с открытым исходным кодом от OceanBase. Он выполняет обобщение памяти и интеллектуальный поиск через локальную LLM, обеспечивая эффективное управление памятью для ИИ-агентов.

Описание стоимости: сам PowerMem бесплатен и имеет открытый исходный код, фактические расходы зависят от выбранных вами LLM и базы данных:
- Использование SQLite + бесплатная LLM (например, Zhipu glm-4-flash) = **полностью бесплатно**
- Использование облачной LLM или облачной базы данных = оплата по тарифам соответствующего сервиса

> 💡 **Совет по максимальной производительности**: максимальную производительность PowerMem раскрывает в связке с OceanBase, SQLite рекомендуется использовать только при нехватке ресурсов.

- **GitHub**: https://github.com/oceanbase/powermem
- **Официальный сайт**: https://www.powermem.ai/
- **Примеры использования**: https://github.com/oceanbase/powermem/tree/main/examples

## Функциональные возможности

- **Локальное обобщение**: обобщение и извлечение памяти через LLM на локальной машине
- **Профиль пользователя**: автоматическое извлечение информации о пользователе (имя, профессия, интересы и т.д.) через `UserMemory`, постоянное обновление профиля
- **Интеллектуальное забывание**: автоматическое «забывание» устаревшей шумовой информации на основе кривой забывания Эббингауза
- **Несколько бэкендов хранения**: поддержка OceanBase (рекомендуется, лучшая производительность), SeekDB (рекомендуется, единое хранилище для ИИ-приложений), PostgreSQL, SQLite (легковесный вариант)
- **Поддержка нескольких LLM**: Tongyi Qianwen, Zhipu (glm-4-flash бесплатна), OpenAI и др.
- **Интеллектуальный поиск**: семантический поиск на основе векторного поиска
- **Приватное развертывание**: полная поддержка локального приватного развертывания
- **Асинхронные операции**: эффективное асинхронное управление памятью

## Установка

PowerMem уже добавлен в зависимости проекта; при необходимости ручной установки:

```bash
pip install powermem
```

## Описание конфигурации

### Базовая конфигурация

Настройте PowerMem в `config.yaml`:

```yaml
selected_module:
  Memory: powermem

Memory:
  powermem:
    type: powermem
    # 是否启用用户画像功能
    # 用户画像支持: oceanbase、seekdb、sqlite (powermem 0.3.0+)
    enable_user_profile: true
    
    # ========== LLM 配置 ==========
    llm:
      provider: openai  # 可选: qwen, openai, zhipu 等
      config:
        api_key: 你的LLM API密钥
        model: qwen-plus
        # openai_base_url: https://api.openai.com/v1  # 可选，自定义服务地址
    
    # ========== Embedding 配置 ==========
    embedder:
      provider: openai  # 可选: qwen, openai 等
      config:
        api_key: 你的嵌入模型API密钥
        model: text-embedding-v4
        openai_base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
        # embedding_dims: 1024  # 向量维度，非1536时需配置
    
    # ========== Database 配置 ==========
    vector_store:
      provider: sqlite  # 可选: oceanbase(推荐), seekdb(推荐), postgres, sqlite(轻量)
      config: {}  # SQLite 无需额外配置
```

### Подробное описание параметров конфигурации

#### Конфигурация LLM

| Параметр | Описание | Возможные значения |
|------|------|--------|
| `llm.provider` | Провайдер LLM | `qwen`, `openai`, `zhipu` и др. |
| `llm.config.api_key` | API-ключ | - |
| `llm.config.model` | Название модели | выбирается в зависимости от провайдера |
| `llm.config.openai_base_url` | Пользовательский адрес сервиса (необязательно) | - |

#### Конфигурация Embedding

| Параметр | Описание | Возможные значения |
|------|------|--------|
| `embedder.provider` | Провайдер модели эмбеддингов | `qwen`, `openai` и др. |
| `embedder.config.api_key` | API-ключ | - |
| `embedder.config.model` | Название модели | выбирается в зависимости от провайдера |
| `embedder.config.openai_base_url` | Пользовательский адрес сервиса (необязательно) | - |

#### Конфигурация базы данных

| Параметр | Описание | Возможные значения |
|------|------|--------|
| `vector_store.provider` | Тип бэкенда хранения | `oceanbase`(рекомендуется), `seekdb`(рекомендуется), `postgres`, `sqlite`(легковесный вариант) |
| `vector_store.config` | Конфигурация подключения к базе данных | настраивается в зависимости от provider |

### Описание режимов памяти

PowerMem поддерживает два режима памяти:

| Режим | Конфигурация | Функция | Требования к хранилищу |
|------|------|------|----------|
| **Обычная память** | `enable_user_profile: false` | Хранение и поиск диалоговой памяти | поддерживаются все базы данных |
| **Профиль пользователя** | `enable_user_profile: true` | Память + автоматическое извлечение профиля пользователя | oceanbase, seekdb, sqlite |

> 📌 **Примечание о версии**: в PowerMem 0.3.0+ функция профиля пользователя поддерживает три бэкенда хранения: OceanBase, SeekDB, SQLite.

### Использование Tongyi Qianwen (рекомендуется)

1. Зайдите на [платформу Alibaba Cloud Bailian](https://bailian.console.aliyun.com/) и зарегистрируйте аккаунт
2. Получите API-ключ на странице [управления API Key](https://bailian.console.aliyun.com/?apiKey=1#/api-key)
3. Настройте конфигурацию:

```yaml
Memory:
  powermem:
    type: powermem
    enable_user_profile: true
    llm:
      provider: qwen
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: qwen-plus
    embedder:
      provider: openai
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: text-embedding-v4
        openai_base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
    vector_store:
      provider: sqlite
      config: {}
```

### Использование бесплатной LLM Zhipu (полностью бесплатное решение)

Zhipu предоставляет бесплатную модель glm-4-flash, в связке с SQLite это полностью бесплатное решение:

1. Зайдите на [платформу Zhipu AI Open Platform](https://bigmodel.cn/) и зарегистрируйте аккаунт
2. Получите API-ключ на странице [API Keys](https://bigmodel.cn/usercenter/proj-mgmt/apikeys)
3. Настройте конфигурацию:

```yaml
Memory:
  powermem:
    type: powermem
    enable_user_profile: true
    llm:
      provider: openai  # 使用 openai 兼容模式
      config:
        api_key: xxxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx
        model: glm-4-flash
        openai_base_url: https://open.bigmodel.cn/api/paas/v4/
    embedder:
      provider: openai
      config:
        api_key: xxxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx
        model: embedding-3
        openai_base_url: https://open.bigmodel.cn/api/paas/v4/
    vector_store:
      provider: sqlite
      config: {}
```

### Использование OpenAI

```yaml
Memory:
  powermem:
    type: powermem
    enable_user_profile: true
    llm:
      provider: openai
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: gpt-4o-mini
        openai_base_url: https://api.openai.com/v1
    embedder:
      provider: openai
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: text-embedding-3-small
        openai_base_url: https://api.openai.com/v1
    vector_store:
      provider: sqlite
      config: {}
```

### Использование OceanBase (максимальная производительность)

OceanBase — лучший партнер для PowerMem, обеспечивающий максимальную производительность:

1. Разверните базу данных OceanBase (поддерживается локальное развертывание с открытым исходным кодом или облачный сервис)
   - Развертывание с открытым исходным кодом: https://github.com/oceanbase/oceanbase
   - Облачный сервис: https://www.oceanbase.com/
2. Настройте конфигурацию:

```yaml
Memory:
  powermem:
    type: powermem
    enable_user_profile: true
    llm:
      provider: qwen
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: qwen-plus
    embedder:
      provider: openai
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: text-embedding-v4
        openai_base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
    vector_store:
      provider: oceanbase
      config:
        host: 127.0.0.1
        port: 2881
        user: root@test
        password: your_password
        db_name: powermem
        collection_name: memories  # 默认值
        embedding_model_dims: 1536  # 嵌入向量维度，必需参数
```

## Изоляция памяти устройств

PowerMem автоматически использует ID устройства (`device_id`) в качестве `user_id` для изоляции памяти. Это означает:

- Каждое устройство имеет изолированное пространство памяти
- Память разных устройств полностью изолирована друг от друга
- Несколько диалогов на одном устройстве могут совместно использовать контекст памяти

## Профиль пользователя (UserMemory)

PowerMem предоставляет класс `UserMemory`, который автоматически извлекает информацию профиля пользователя из диалогов.

> 📌 **Примечание о версии**: в PowerMem 0.3.0+ функция профиля пользователя поддерживает три бэкенда хранения: OceanBase, SeekDB, SQLite.

### Включение профиля пользователя

Достаточно установить `enable_user_profile: true` в конфигурации:

```yaml
Memory:
  powermem:
    type: powermem
    enable_user_profile: true  # 启用用户画像
    llm:
      provider: qwen
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: qwen-plus
    embedder:
      provider: openai
      config:
        api_key: sk-xxxxxxxxxxxxxxxx
        model: text-embedding-v4
        openai_base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
    vector_store:
      provider: sqlite  # 用户画像支持: oceanbase、seekdb、sqlite
      config: {}
```

### Возможности профиля пользователя

| Возможность | Описание |
|------|------|
| **Извлечение информации** | Автоматическое извлечение имени, возраста, профессии, интересов и т.д. из диалогов |
| **Постоянное обновление** | Постоянное совершенствование профиля пользователя по мере диалога |
| **Поиск по профилю** | Сочетание профиля пользователя с поиском по памяти для повышения релевантности |
| **Интеллектуальное забывание** | Ослабление устаревшей информации на основе кривой забывания Эббингауза |

### Как это работает

После включения профиля пользователя Xiaozhi при запросе памяти автоматически возвращает:
1. **Профиль пользователя**: основная информация о пользователе, интересы и т.д.
2. **Релевантные воспоминания**: историческая память, относящаяся к текущему диалогу

> ✅ **Примечание о версии**: в PowerMem 0.3.0+ функция профиля пользователя поддерживает три бэкенда хранения: OceanBase, SeekDB, SQLite.

## Сравнение с другими компонентами памяти

| Характеристика | PowerMem | mem0ai | mem_local_short |
|------|----------|--------|-----------------|
| Принцип работы | Локальное обобщение | Облачный интерфейс | Локальное обобщение |
| Место хранения | Локальная/облачная БД | Облако | Локальный YAML |
| Стоимость | зависит от LLM и БД | 1000 запросов/мес бесплатно | полностью бесплатно |
| Интеллектуальный поиск | ✅ Векторный поиск | ✅ Векторный поиск | ❌ Полный возврат |
| Профиль пользователя | ✅ UserMemory | ❌ | ❌ |
| Интеллектуальное забывание | ✅ Кривая забывания | ❌ | ❌ |
| Приватное развертывание | ✅ Поддерживается | ❌ Только облако | ✅ Поддерживается |
| Поддержка баз данных | OceanBase(рекомендуется)/SeekDB/PostgreSQL/SQLite | - | YAML-файл |

## Часто задаваемые вопросы

### 1. Ошибка API-ключа

Если возникает ошибка `API key is required`, проверьте:
- правильно ли заполнены `llm_api_key` и `embedding_api_key`
- действителен ли API-ключ

### 2. Модель не существует

Если возникает ошибка, что модель не существует, убедитесь:
- правильны ли названия `llm_model` и `embedding_model`
- активирован ли соответствующий сервис модели

### 3. Превышение времени ожидания подключения

Если возникает превышение времени ожидания подключения, можно попробовать:
- проверить сетевое подключение
- если используется прокси, настроить `llm_base_url` и `embedding_base_url`

## Проверка и тестирование

Можно проверить работу PowerMem в виртуальном окружении:

```bash
# 激活虚拟环境
source .venv/bin/activate

# 测试 PowerMem 导入
python -c "from powermem import AsyncMemory; print('PowerMem 导入成功')"

# 测试 UserMemory 导入（用户画像功能）
python -c "from powermem import UserMemory; print('UserMemory 导入成功')"
```

## Дополнительные ресурсы

- [Официальная документация PowerMem](https://www.powermem.ai/)
- [Репозиторий PowerMem на GitHub](https://github.com/oceanbase/powermem)
- [Примеры использования PowerMem](https://github.com/oceanbase/powermem/tree/main/examples)
- [Официальный сайт OceanBase](https://www.oceanbase.com/)
- [OceanBase на GitHub](https://github.com/oceanbase/oceanbase)
- [SeekDB на GitHub](https://github.com/oceanbase/seekdb) (ИИ-нативная поисковая база данных)
- [Платформа Alibaba Cloud Bailian](https://bailian.console.aliyun.com/)
