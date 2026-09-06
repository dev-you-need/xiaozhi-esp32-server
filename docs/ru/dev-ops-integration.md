# Автоматическое обновление при развёртывании всех модулей из исходного кода

Это руководство предназначено для энтузиастов, развёртывающих все модули из исходного кода. Оно показывает, как с помощью автоматических команд автоматически получать исходный код, автоматически компилировать и автоматически запускать сервисы на нужных портах. Так достигается максимально эффективное обновление системы.

Тестовая платформа проекта `https://2662r3426b.vicp.fun` с момента открытия использует именно этот метод, и результаты отличные.

С этим руководством можно ознакомиться по видеоуроку от блогера Bilibili `毕乐labs`: [《开源小智服务器xiaozhi-server自动更新以及最新版本MCP接入点配置保姆教程》](https://www.bilibili.com/video/BV15H37zHE7Q)

# Условия для начала
- Ваш компьютер/сервер работает под управлением Linux
- Вы уже полностью запустили весь процесс
- Вам нравится следить за новыми функциями, но ручное развёртывание каждый раз кажется утомительным, и вы хотели бы способ автоматического обновления

Второе условие обязательно: некоторые файлы, затронутые в этом руководстве (окружение JDK, Node.js, Conda и т.п.), появляются только после того, как вы полностью запустили процесс. Если вы его не запустили, то, когда я буду говорить о каком-то файле, вы можете не понять, о чём речь.

# Результат руководства
- Решение проблемы невозможности получения последнего исходного кода проекта из Китая
- Автоматическое получение кода и компиляция файлов фронтенда
- Автоматическое получение кода и компиляция java-файлов, автоматическое завершение процесса на порту 8002 и его автоматический запуск
- Автоматическое получение python-кода, автоматическое завершение процесса на порту 8000 и его автоматический запуск

# Шаг 1. Выберите каталог проекта

Например, я спланировал каталог проекта так (это новый пустой каталог; если не хотите ошибиться, сделайте так же):
```
/home/system/xiaozhi
```

# Шаг 2. Клонируйте этот проект

Сначала выполните первую команду — получение исходного кода. Эта команда подходит для серверов и компьютеров в китайской сети, обход прокси не требуется.

```
cd /home/system/xiaozhi
git clone https://ghproxy.net/https://github.com/xinnan-tech/xiaozhi-esp32-server.git
```

После выполнения в каталоге проекта появится новая папка `xiaozhi-esp32-server` — это исходный код проекта.

# Шаг 3. Скопируйте базовые файлы

Если вы раньше уже полностью запустили весь процесс, вам знакомы файлы модели funasr `xiaozhi-server/models/SenseVoiceSmall/model.pt` и ваш личный конфигурационный файл `xiaozhi-server/data/.config.yaml`.

Теперь нужно скопировать файл `model.pt` в новый каталог. Можно сделать так:
```
# 创建需要的目录
mkdir -p /home/system/xiaozhi/xiaozhi-esp32-server/main/xiaozhi-server/data/

cp 你原来的.config.yaml完整路径 /home/system/xiaozhi/xiaozhi-esp32-server/main/xiaozhi-server/data/.config.yaml
cp 你原来的model.pt完整路径 /home/system/xiaozhi/xiaozhi-esp32-server/main/xiaozhi-server/models/SenseVoiceSmall/model.pt
```

# Шаг 4. Создайте три файла автоматической компиляции

## 4.1 Автоматическая компиляция модуля manager-web

В каталоге `/home/system/xiaozhi/` создайте файл с именем `update_8001.sh` и следующим содержимым:

```
cd /home/system/xiaozhi/xiaozhi-esp32-server
git fetch --all
git reset --hard
git pull origin main


cd /home/system/xiaozhi/xiaozhi-esp32-server/main/manager-web
npm install
npm run build
rm -rf /home/system/xiaozhi/manager-web
mv /home/system/xiaozhi/xiaozhi-esp32-server/main/manager-web/dist /home/system/xiaozhi/manager-web
```

После сохранения выполните команду выдачи прав:
```
chmod 777 update_8001.sh
```
После выполнения двигайтесь дальше.

## 4.2 Автоматическая компиляция и запуск модуля manager-api

В каталоге `/home/system/xiaozhi/` создайте файл с именем `update_8002.sh` и следующим содержимым:

```
cd /home/system/xiaozhi/xiaozhi-esp32-server
git pull origin main


cd /home/system/xiaozhi/xiaozhi-esp32-server/main/manager-api
rm -rf target
mvn clean package -Dmaven.test.skip=true
cd /home/system/xiaozhi/

# 查找占用8002端口的进程号
PID=$(sudo netstat -tulnp | grep 8002 | awk '{print $7}' | cut -d'/' -f1)

rm -rf /home/system/xiaozhi/xiaozhi-esp32-api.jar
mv /home/system/xiaozhi/xiaozhi-esp32-server/main/manager-api/target/xiaozhi-esp32-api.jar /home/system/xiaozhi/xiaozhi-esp32-api.jar

# 检查是否找到进程号
if [ -z "$PID" ]; then
  echo "没有找到占用8002端口的进程"
else
  echo "找到占用8002端口的进程，进程号为: $PID"
  # 杀掉进程
  kill -9 $PID
  kill -9 $PID
  echo "已杀掉进程 $PID"
fi

nohup java -jar xiaozhi-esp32-api.jar --spring.profiles.active=dev &

tail -f nohup.out
```

После сохранения выполните команду выдачи прав:
```
chmod 777 update_8002.sh
```
После выполнения двигайтесь дальше.

## 4.3 Автоматическая компиляция и запуск Python-проекта

В каталоге `/home/system/xiaozhi/` создайте файл с именем `update_8000.sh` и следующим содержимым:

```
cd /home/system/xiaozhi/xiaozhi-esp32-server
git pull origin main

# 查找占用8000端口的进程号
PID=$(sudo netstat -tulnp | grep 8000 | awk '{print $7}' | cut -d'/' -f1)

# 检查是否找到进程号
if [ -z "$PID" ]; then
  echo "没有找到占用8000端口的进程"
else
  echo "找到占用8000端口的进程，进程号为: $PID"
  # 杀掉进程
  kill -9 $PID
  kill -9 $PID
  echo "已杀掉进程 $PID"
fi
cd main/xiaozhi-server
# 初始化conda环境
source ~/.bashrc
conda activate xiaozhi-esp32-server
pip install -r requirements.txt
nohup python app.py >/dev/null &
tail -f /home/system/xiaozhi/xiaozhi-esp32-server/main/xiaozhi-server/tmp/server.log
```

После сохранения выполните команду выдачи прав:
```
chmod 777 update_8000.sh
```
После выполнения двигайтесь дальше.

# Ежедневное обновление

После того как все вышеуказанные скрипты созданы, для ежедневного обновления достаточно последовательно выполнить следующие команды — произойдёт автоматическое обновление и запуск.

```
cd /home/system/xiaozhi
# 更新并启动Java程序
./update_8001.sh
# 更新web程序
./update_8002.sh
# 更新并启动python程序
./update_8000.sh


# 后期想查看java日志，执行以下命令
tail -f nohup.out
# 后期想查看python日志，执行以下命令
tail -f /home/system/xiaozhi/xiaozhi-esp32-server/main/xiaozhi-server/tmp/server.log
```

# Важные замечания

Тестовая платформа `https://2662r3426b.vicp.fun` использует nginx в качестве обратного прокси. Подробную конфигурацию nginx.conf можно [посмотреть здесь](https://github.com/xinnan-tech/xiaozhi-esp32-server/issues/791).

## Частые вопросы

### 1. Почему я не вижу порт 8001?
Ответ: порт 8001 используется в среде разработки для запуска фронтенда. При развёртывании на сервере не рекомендуется запускать фронтенд на порту 8001 через `npm run serve`; вместо этого, как в этом руководстве, фронтенд компилируется в html-файлы, а доступ к ним управляется через nginx.

### 2. Нужно ли при каждом обновлении вручную выполнять SQL-выражения?
Ответ: нет, потому что проект использует **Liquibase** для управления версиями базы данных, и новые sql-скрипты выполняются автоматически.
