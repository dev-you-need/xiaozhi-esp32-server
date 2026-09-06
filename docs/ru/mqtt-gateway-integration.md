# Руководство по развёртыванию MQTT-шлюза

Проект `xiaozhi-esp32-server` можно с небольшой доработкой использовать вместе с открытым проектом 虾哥 [xiaozhi-mqtt-gateway](https://github.com/78/xiaozhi-mqtt-gateway), что позволит реализовать подключение устройств 小智 по MQTT+UDP.

Это руководство состоит из трёх частей. В зависимости от того, развёрнуты ли у вас все модули или только один, выберите соответствующий раздел для подключения MQTT-шлюза:
- Часть первая: развёртывание MQTT-шлюза
- Часть вторая: подключение устройств 小智 по MQTT+UDP при запуске всех модулей
- Часть третья: подключение устройств 小智 по MQTT+UDP при одиночном запуске xiaozhi-server

## Подготовительный этап

Подготовьте адрес подключения `mqtt-websocket` вашего `xiaozhi-server`. Для этого к вашему исходному `websocket-адресу` нужно добавить строку `?from=mqtt_gateway` — так вы получите адрес подключения `mqtt-websocket`.

1. Если вы развёртываете из исходного кода, ваш адрес `mqtt-websocket` таков:
```
ws://127.0.0.1:8000/xiaozhi/v1/?from=mqtt_gateway
```

2. Если вы развёртываете через docker, ваш адрес `mqtt-websocket` таков:
```
ws://你宿主机局域网IP:8000/xiaozhi/v1/?from=mqtt_gateway
```

## Важное замечание

Если вы развёртываете на сервере, убедитесь, что порты `1883`, `8884` и `8007` открыты наружу. Для порта `8884` тип протокола — `UDP`, для остальных — `TCP`.

Если вы развёртываете на сервере, убедитесь, что порты `1883`, `8884` и `8007` открыты наружу. Для порта `8884` тип протокола — `UDP`, для остальных — `TCP`.

Если вы развёртываете на сервере, убедитесь, что порты `1883`, `8884` и `8007` открыты наружу. Для порта `8884` тип протокола — `UDP`, для остальных — `TCP`.

## Часть первая: развёртывание MQTT-шлюза

1. Клонируйте [доработанный проект xiaozhi-mqtt-gateway](https://github.com/xinnan-tech/xiaozhi-mqtt-gateway.git):
```bash
git clone https://ghfast.top/https://github.com/xinnan-tech/xiaozhi-mqtt-gateway.git
cd xiaozhi-mqtt-gateway
```

2. Установите зависимости:
```bash
npm install
npm install -g pm2
```

3. Настройте `config.json`:
```bash
cp config/mqtt.json.example config/mqtt.json
```

4. Отредактируйте файл конфигурации config/mqtt.json, заменив адрес `mqtt-websocket` из «Подготовительного этапа» этого руководства в `chat_servers`. Например, для `xiaozhi-server`, развёрнутого из исходного кода, конфигурация будет такой:

``` 
{
    "production": {
        "chat_servers": [
            "ws://127.0.0.1:8000/xiaozhi/v1/?from=mqtt_gateway"
        ]
    },
    "debug": false,
    "max_mqtt_payload_size": 8192,
    "mcp_client": {
        "capabilities": {
        },
        "client_info": {
            "name": "xiaozhi-mqtt-client",
            "version": "1.0.0"
        },
        "max_tools_count": 128
    }
}
```
5. Создайте в корне проекта файл `.env` и задайте следующие переменные окружения:
```
PUBLIC_IP=your-ip         # 服务器公网IP
MQTT_PORT=1883            # MQTT服务器端口
UDP_PORT=8884             # UDP服务器端口
API_PORT=8007             # 管理API端口
MQTT_SIGNATURE_KEY=test   # MQTT签名密钥
SERVER_SECRET=Te1st12134  # 服务器密钥，请保持和智控台（server.secret）一致或者和xiaozhi-server里（server.auth_key）保持一致
```
Обратите внимание на параметр `PUBLIC_IP`: убедитесь, что он совпадает с реальным публичным IP, а если есть домен — укажите домен.

`MQTT_SIGNATURE_KEY` — ключ, используемый для аутентификации MQTT-подключений. Лучше задать его посложнее: желательно не менее 8 символов, содержащих как заглавные, так и строчные буквы. Этот ключ ещё пригодится позже.

- Не используйте простые пароли, например `123456` или `test`.
- Не используйте простые пароли, например `123456` или `test`.
- Не используйте простые пароли, например `123456` или `test`.

`SERVER_SECRET` — ключ для генерации данных аутентификации websocket-подключений.

1. Если вы развёртываете все модули и в управлении параметрами 智控台 у вас `server.auth.enabled` установлен в `true`, то `SERVER_SECRET` должен совпадать с 智控台 (`server.secret`).

2. Если вы развёртываете один модуль и в конфигурационном файле у вас `server.auth.enabled` установлен в `true`, то `SERVER_SECRET` должен совпадать со значением в конфигурационном файле (`server.auth_key`).

6. Запустите MQTT-шлюз
```
# 启动服务
pm2 start ecosystem.config.js

# 查看日志
pm2 logs xz-mqtt
```

Когда вы увидите следующий журнал, значит, MQTT-шлюз успешно запущен:
```
0|xz-mqtt  | 2025-09-11T12:14:48: MQTT 服务器正在监听端口 1883
0|xz-mqtt  | 2025-09-11T12:14:48: UDP 服务器正在监听 x.x.x.x:8884
```

Если потребуется перезапустить MQTT-шлюз, выполните команду:
```
pm2 restart xz-mqtt
```

## Часть вторая: подключение устройств 小智 по MQTT+UDP при запуске всех модулей

Посмотрите номер версии внизу главной страницы 智控台 и убедитесь, что версия 智控台 — `0.7.7` или выше. Если нет — обновите 智控台.

1. Вверху 智控台 нажмите `参数管理`, найдите `server.mqtt_gateway`, нажмите «Изменить» и введите значение `PUBLIC_IP` из файла `.env` + `:` + `MQTT_PORT`. Например:
```
192.168.0.7:1883
```
2. Вверху 智控台 нажмите `参数管理`, найдите `server.mqtt_signature_key`, нажмите «Изменить» и введите значение `MQTT_SIGNATURE_KEY` из файла `.env`.

3. Вверху 智控台 нажмите `参数管理`, найдите `server.udp_gateway`, нажмите «Изменить» и введите значение `PUBLIC_IP` из файла `.env` + `:` + `UDP_PORT`. Например:
```
192.168.0.7:8884
```
4. Вверху 智控台 нажмите `参数管理`, найдите `server.mqtt_manager_api`, нажмите «Изменить» и введите значение `PUBLIC_IP` из файла `.env` + `:` + `API_PORT`. Например:
```
192.168.0.7:8007
```

После завершения настройки вы можете проверить командой curl, выдаёт ли ваш ota-адрес mqtt-конфигурацию. Замените `http://localhost:8002/xiaozhi/ota/` на ваш ota-адрес:
```
curl 'http://localhost:8002/xiaozhi/ota/' \
  -H 'Content-Type: application/json' \
  -H 'Client-Id: 7b94d69a-9808-4c59-9c9b-704333b38aff' \
  -H 'Device-Id: 11:22:33:44:55:66' \
  --data-raw $'{\n  "application": {\n    "version": "1.0.1",\n    "elf_sha256": "1"\n  },\n  "board": {\n    "mac": "11:22:33:44:55:66"\n  }\n}'
```

Если возвращённое содержимое включает конфигурацию, связанную с `mqtt`, значит, настройка прошла успешно. Например:

```
{"server_time":{"timestamp":1757567894012,"timeZone":"Asia/Shanghai","timezone_offset":480},"activation":{"code":"460609","message":"http://xiaozhi.server.com\n460609","challenge":"11:22:33:44:55:66"},"firmware":{"version":"1.0.1","url":"http://xiaozhi.server.com:8002/xiaozhi/otaMag/download/NOT_ACTIVATED_FIRMWARE_THIS_IS_A_INVALID_URL"},"websocket":{"url":"ws://192.168.4.23:8000/xiaozhi/v1/"},"mqtt":{"endpoint":"192.168.0.7:1883","client_id":"GID_default@@@11_22_33_44_55_66@@@7b94d69a-9808-4c59-9c9b-704333b38aff","username":"eyJpcCI6IjA6MDowOjA6MDowOjA6MSJ9","password":"Y8XP9xcUhVIN9OmbCHT9ETBiYNE3l3Z07Wk46wV9PE8=","publish_topic":"device-server","subscribe_topic":"devices/p2p/11_22_33_44_55_66"}}
```

Поскольку mqtt-информация выдаётся через ota-адрес, достаточно убедиться, что устройство нормально подключается к ota-адресу сервера, а затем перезагрузить его для активации.

После активации следите за журналом mqtt-gateway и проверяйте, есть ли записи об успешном подключении.
```
pm2 logs xz-mqtt
```

## Часть третья: подключение устройств 小智 по MQTT+UDP при одиночном запуске xiaozhi-server

Откройте файл `data/.config.yaml`, найдите в разделе `server` параметр `mqtt_gateway` и введите значение `PUBLIC_IP` из файла `.env` + `:` + `MQTT_PORT`. Например:
```
192.168.0.7:1883
```
В разделе `server` найдите параметр `mqtt_signature_key` и введите значение `MQTT_SIGNATURE_KEY` из файла `.env`.

В разделе `server` найдите параметр `udp_gateway` и введите значение `PUBLIC_IP` из файла `.env` + `:` + `UDP_PORT`. Например:
```
192.168.0.7:8884
```

После завершения настройки вы можете проверить командой curl, выдаёт ли ваш ota-адрес mqtt-конфигурацию. Замените `http://localhost:8002/xiaozhi/ota/` на ваш ota-адрес:
```
curl 'http://localhost:8002/xiaozhi/ota/' \
  -H 'Device-Id: 11:22:33:44:55:66' \
  --data-raw $'{\n  "application": {\n    "version": "1.0.1",\n    "elf_sha256": "1"\n  },\n  "board": {\n    "mac": "11:22:33:44:55:66"\n  }\n}'
```

Если возвращённое содержимое включает конфигурацию, связанную с `mqtt`, значит, настройка прошла успешно. Например:
```
{"server_time":{"timestamp":1758781561083,"timeZone":"GMT+08:00","timezone_offset":480},"activation":{"code":"527111","message":"http://xiaozhi.server.com\n527111","challenge":"11:22:33:44:55:66"},"firmware":{"version":"1.0.1","url":"http://xiaozhi.server.com:8002/xiaozhi/otaMag/download/NOT_ACTIVATED_FIRMWARE_THIS_IS_A_INVALID_URL"},"websocket":{"url":"ws://192.168.1.15:8000/xiaozhi/v1/"},"mqtt":{"endpoint":"192.168.1.15:1883","client_id":"GID_default@@@11_22_33_44_55_66@@@11_22_33_44_55_66","username":"eyJpcCI6IjE5Mi4xNjguMS4xNSJ9","password":"fjAYs49zTJecWqJ3jBt+kqxVn/x7vkXRAc85ak/va7Y=","publish_topic":"device-server","subscribe_topic":"devices/p2p/11_22_33_44_55_66"}}
```

Поскольку mqtt-информация выдаётся через ota-адрес, достаточно убедиться, что устройство нормально подключается к ota-адресу сервера, а затем перезагрузить его для активации.

После активации следите за журналом mqtt-gateway и проверяйте, есть ли записи об успешном подключении.
```
pm2 logs xz-mqtt
```
