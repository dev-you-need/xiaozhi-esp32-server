# Как получить информацию об устройстве с помощью метода MCP

В этом руководстве объясняется, как получить информацию об устройстве с помощью метода MCP.

Шаг 1: настройте свой файл `agent-base-prompt.txt`

Скопируйте содержимое файла `agent-base-prompt.txt` из каталога xiaozhi-server в свой каталог `data` и переименуйте его в `.agent-base-prompt.txt`.

Шаг 2: отредактируйте файл `data/.agent-base-prompt.txt`, найдите тег `<context>` и добавьте в его содержимое следующий код:
```
- **设备ID：** {{device_id}}
```

После добавления содержимое тега `<context>` в вашем файле `data/.agent-base-prompt.txt` будет выглядеть примерно так:
```
<context>
【重要！以下信息已实时提供，无需调用工具查询，请直接使用：】
- **设备ID：** {{device_id}}
- **当前时间：** {{current_time}}
- **今天日期：** {{today_date}} ({{today_weekday}})
- **今天农历：** {{lunar_date}}
- **用户所在城市：** {{local_address}}
- **当地未来7天天气：** {{weather_info}}
</context>
```

Шаг 3: отредактируйте файл `data/.config.yaml`, найдите настройку `agent-base-prompt`; до изменения её содержимое выглядит так:
```
prompt_template: agent-base-prompt.txt
```
Измените на
```
prompt_template: data/.agent-base-prompt.txt
```

Шаг 4: перезапустите ваш сервис xiaozhi-server.

Шаг 5: в вашем методе mcp добавьте параметр с именем `device_id`, типом `string` и описанием `设备ID`.

Шаг 6: снова разбудите Xiaozhi и попросите его вызвать метод mcp, затем проверьте, может ли ваш метод mcp получить `设备ID`.
