# Руководство по использованию источников контекста

## Обзор

`Источник контекста` — это способ добавить [источник данных] в контекст системного промпта Сяочжи.

`Источник контекста` в момент пробуждения Сяочжи получает данные из внешних систем и динамически внедряет их в системный промпт (System Prompt) большой языковой модели.
Так Сяочжи получает возможность «чувствовать» состояние того или иного объекта в мире в момент пробуждения.

По сути он отличается от MCP и памяти: `источник контекста` принудительно заставляет Сяочжи воспринимать данные о мире; `память (Mem)` позволяет ему знать, о чём шла речь в предыдущих разговорах; `MCP (function_call)` используется, когда нужно вызвать какое-то действие или получить знания.

С помощью этой функции в момент пробуждения Сяочжи «чувствует»:
- Состояние датчиков здоровья человека (температура тела, артериальное давление, сатурация крови и т.д.)
- Данные бизнес-систем в реальном времени (нагрузка на серверы, списки дел, информацию об акциях и т.д.)
- Любую текстовую информацию, доступную через HTTP API

**Внимание**: эта функция лишь позволяет Сяочжи воспринять состояние объектов в момент пробуждения. Если же нужно, чтобы Сяочжи получал состояние объектов в реальном времени уже после пробуждения, рекомендуется дополнительно использовать вызовы инструментов MCP.

## Принцип работы

1. **Настройка источника**: пользователь настраивает один или несколько адресов HTTP API.
2. **Запрос при срабатывании**: при формировании промпта система, обнаружив в шаблоне плейсхолдер `{{ dynamic_context }}`, отправляет запросы ко всем настроенным API.
3. **Автоматическое внедрение**: система автоматически форматирует данные, возвращённые API, в виде Markdown-списка и заменяет плейсхолдер `{{ dynamic_context }}`.

## Спецификация интерфейса

Чтобы Сяочжи корректно разбирал данные, ваш API должен соответствовать следующим требованиям:

- **Метод запроса**: `GET`
- **Заголовки запроса**: система автоматически добавляет поле `device-id` в заголовок запроса (Request Header).
- **Формат ответа**: должен возвращаться JSON, содержащий поля `code` и `data`.

### Пример ответа

**Случай 1: возврат пар «ключ-значение»**
```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "客厅温度": "26℃",
    "客厅湿度": "45%",
    "大门状态": "已关闭"
  }
}
```
*Эффект внедрения:*
```markdown
<context>
- **客厅温度：** 26℃
- **客厅湿度：** 45%
- **大门状态：** 已关闭
</context>
```

**Случай 2: возврат списка**
```json
{
  "code": 0,
  "data": [
    "您有10个待办事项",
    "当前汽车的行驶速度是100km每小时"
  ]
}
```
*Эффект внедрения:*
```markdown
<context>
- 您有10个待办事项
- 当前汽车的行驶速度是100km每小时
</context>
```

## Руководство по настройке

### Способ 1: настройка через панель управления (полное развёртывание модулей)

1. Войдите в панель управления и перейдите на страницу **настройки роли**.
2. Найдите пункт настройки **источников контекста** (нажмите кнопку «Редактировать источники»).
3. Нажмите **Добавить** и введите адрес вашего API.
4. Если API требует аутентификации, в разделе **заголовков запроса** можно добавить `Authorization` или другие заголовки.
5. Сохраните настройку.

### Способ 2: настройка через конфигурационный файл (развёртывание одного модуля)

Отредактируйте файл `xiaozhi-server/data/.config.yaml`, добавив секцию `context_providers`:

```yaml
# 上下文源配置
context_providers:
  - url: "http://api.example.com/data"
    headers:
      Authorization: "Bearer your-token"
  - url: "http://another-api.com/data"
```

## Включение функции

По умолчанию в файле шаблона системного промпта (`data/.agent-base-prompt.txt`) уже предустановлен плейсхолдер `{{ dynamic_context }}`, добавлять его вручную не нужно.

**Пример:**

```markdown
<context>
【重要！以下信息已实时提供，无需调用工具查询，请直接使用：】
- **设备ID：** {{device_id}}
- **当前时间：** {{current_time}}
...
{{ dynamic_context }}
</context>
```

**Внимание**: если эта функция вам не нужна, вы можете либо **не настраивать ни одного источника контекста**, либо **удалить** плейсхолдер `{{ dynamic_context }}` из файла шаблона промпта.

## Приложение: пример Mock-сервера для тестирования

Для удобства тестирования и разработки мы предоставляем простой скрипт Mock-сервера на Python. Вы можете запустить этот скрипт, чтобы локально эмулировать API-интерфейс.

**mock_api_server.py**

```python
import http.server
import socketserver
import json
from urllib.parse import urlparse, parse_qs

# 设置端口号
PORT = 8081

class MockRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        # 解析路径和参数
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query = parse_qs(parsed_path.query)

        response_data = {}
        status_code = 200

        print(f"收到请求: {path}, 参数: {query}")

        # Case 1: 模拟健康数据 (返回字典 Dict)
        # 路径参数风格: /health
        # device_id 从 Header 获取
        if path == "/health":
            device_id = self.headers.get("device-id", "unknown_device")
            print(f"device_id: {device_id}")
            response_data = {
                "code": 0,
                "msg": "success",
                "data": {
                    "测试设备ID": device_id,
                    "心率": "80 bpm",
                    "血压": "120/80 mmHg",
                    "状态": "良好"
                }
            }

        # Case 2: 模拟新闻列表 (返回列表 List)
        # 无参数: /news/list
        elif path == "/news/list":
            response_data = {
                "code": 0,
                "msg": "success",
                "data": [
                    "今日头条：Python 3.14 发布",
                    "科技新闻：AI 助手改变生活",
                    "本地新闻：明日有大雨，记得带伞"
                ]
            }

        # Case 3: 模拟天气简报 (返回字符串 String)
        # 无参数: /weather/simple
        elif path == "/weather/simple":
            response_data = {
                "code": 0,
                "msg": "success",
                "data": "今日晴转多云，气温 20-25 度，空气质量优，适合出行。"
            }

        # Case 4: 模拟设备详情 (Query参数风格)
        # 参数风格: /device/info
        # device_id 从 Header 获取
        elif path == "/device/info":
            device_id = self.headers.get("device-id", "unknown_device")
            response_data = {
                "code": 0,
                "msg": "success",
                "data": {
                    "查询方式": "Header参数",
                    "设备ID": device_id,
                    "电量": "85%",
                    "固件": "v2.0.1"
                }
            }
        
        # Case 5: 404 Not Found
        else:
            status_code = 404
            response_data = {"error": "接口不存在"}

        # 发送响应
        self.send_response(status_code)
        self.send_header('Content-type', 'application/json; charset=utf-8')
        self.end_headers()
        self.wfile.write(json.dumps(response_data, ensure_ascii=False).encode('utf-8'))

# 启动服务
# 允许地址重用，防止快速重启报错
socketserver.TCPServer.allow_reuse_address = True
with socketserver.TCPServer(("", PORT), MockRequestHandler) as httpd:
    print(f"==================================================")
    print(f"Mock API Server 已启动: http://localhost:{PORT}")
    print(f"可用接口列表:")
    print(f"1. [字典] http://localhost:{PORT}/health")
    print(f"2. [列表] http://localhost:{PORT}/news/list")
    print(f"3. [文本] http://localhost:{PORT}/weather/simple")
    print(f"4. [参数] http://localhost:{PORT}/device/info")
    print(f"==================================================")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n服务已停止")
```
