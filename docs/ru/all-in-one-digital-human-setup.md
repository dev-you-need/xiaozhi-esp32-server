# Руководство по настройке цифрового человека на моноблоке

Этот проект предназначен для развертывания полноценной системы отображения цифрового человека на устройствах с архитектурой x86 (например, мини-ПК, промышленные компьютеры, обычные компьютеры и т.д.) и реализует следующие функции:
- При загрузке система автоматически входит в полноэкранный браузер Kiosk, отображающий заданную веб-страницу
- В фоне работает сервис обнаружения слов активации, поддерживающий голосовое взаимодействие

> **Примечание**: в этом документе в качестве примера развертывания используется **мини-ПК Intel N100 (Tianhong QN10-100B4)**, другие устройства x86 могут быть настроены по аналогии (обратите внимание на различия в сетевой конфигурации и звуковом оборудовании).

## Подходящее окружение

| Пункт | Описание |
|------|------|
| Пример оборудования | Tianhong QN10-100B4 (Intel N100) |
| Операционная система | Ubuntu 24.04 LTS (Noble Numbat) |
| Пример пользователя | xz (замените на свой в зависимости от ситуации) |
| Сеть | Подключение по Wi-Fi, фиксированный IP (при необходимости можно перейти на проводное подключение) |

## Порядок развертывания

1. Инициализация системы (смена репозиториев, подключение к сети)
2. Установка графических компонентов и браузера Kiosk
3. Настройка автоматического входа и графического интерфейса
4. Развертывание сервиса слов активации (окружение Python + микрофон)
5. Оптимизация скорости загрузки и скрытие информации при запуске

---


### Инициализация системы (смена репозиториев, подключение к сети)

```
sudo cp /etc/apt/sources.list /etc/apt/sources.list.bak

sudo tee /etc/apt/sources.list > /dev/null <<EOF
deb http://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse
# deb-src http://mirrors.aliyun.com/ubuntu/ noble main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse
# deb-src http://mirrors.aliyun.com/ubuntu/ noble-security main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse
# deb-src http://mirrors.aliyun.com/ubuntu/ noble-updates main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ noble-proposed main restricted universe multiverse
# deb-src http://mirrors.aliyun.com/ubuntu/ noble-proposed main restricted universe multiverse

deb http://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse
# deb-src http://mirrors.aliyun.com/ubuntu/ noble-backports main restricted universe multiverse
EOF

echo 'Acquire::ForceIPv4 "true";' | sudo tee /etc/apt/apt.conf.d/99force-ipv4
```


Установите инструменты управления сетью (если они уже установлены, этот шаг можно пропустить)

Bash

```
sudo apt update
sudo apt install network-manager -y
sudo systemctl start NetworkManager
sudo systemctl enable NetworkManager
```


Задайте пароль Wi-Fi и фиксированный IP

> **Напоминание**: имена Wi-Fi сети, пароль и IP-адреса в приведенных ниже командах являются примерами — обязательно замените их на свои реальные данные.

Bash

```
sudo nmcli device wifi connect "MERCURY_1812" password "12345678"

sudo nmcli connection modify "MERCURY_1812" ipv4.addresses "192.168.0.86/24" ipv4.gateway "192.168.0.1" ipv4.dns "8.8.8.8,114.114.114.114" ipv4.method "manual"

sudo nmcli connection up "MERCURY_1812"
```


### Шаг 1: Установка основных графических компонентов и браузера

Здесь мы придерживаемся «минимализма» и решительно не устанавливаем лишние окружения рабочего стола (такие как GNOME/KDE), а ставим только базовые драйверы, самый легковесный оконный менеджер (Openbox), инструмент скрытия курсора мыши и браузер Chromium.

Bash

```
sudo timedatectl set-timezone Asia/Shanghai


sudo apt install net-tools vim fonts-wqy-microhei fonts-wqy-zenhei alsa-utils pulseaudio -y
sudo apt install --no-install-recommends xserver-xorg x11-xserver-utils xinit openbox unclutter -y

wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt install ./google-chrome-stable_current_amd64.deb -y
rm google-chrome-stable_current_amd64.deb

sudo apt purge snapd -y

```

### Шаг 2: Настройка автоматического входа без пароля на TTY1

Чтобы избежать неловкости ручного ввода имени пользователя и пароля, мы изменим службу systemd так, чтобы система автоматически входила под пользователем `xz` сразу после загрузки. Здесь используется команда однократной записи, полностью избавляющая от проблем с сохранением файла из-за неправильного использования `nano` или `vi`.

**1. Создайте каталог конфигурации:**

Bash

```
sudo mkdir -p /etc/systemd/system/getty@tty1.service.d/
```

**2. Запишите правило автоматического входа:**

Bash

```
echo -e "[Service]\nExecStart=\nExecStart=-/sbin/agetty --autologin xz --noclear %I \$TERM" | sudo tee /etc/systemd/system/getty@tty1.service.d/override.conf
```

**3. Перезагрузите службы и задайте целевой режим загрузки по умолчанию:**

Bash

```
sudo systemctl daemon-reload
sudo systemctl set-default multi-user.target
```

### Шаг 3: Настройка автоматического запуска графического интерфейса после входа

После автоматического входа система по умолчанию остается в командной строке с белым текстом на черном фоне, нам нужно настроить скрипт, чтобы сразу после входа запускалась графическая среда X11.

**1. Логика запуска `startx`:**

Напрямую допишите код запуска в ваш личный файл конфигурации окружения:

Bash

```
cat << 'EOF' >> ~/.bash_profile
if [ -z "$DISPLAY" ] && [ "$(fgconsole)" -eq 1 ]; then
    exec startx
fi
EOF
```

**2. Укажите `startx` запускать Openbox:**

Bash

```
echo "exec openbox-session" > ~/.xinitrc
```

### Шаг 4: Настройка «несокрушимого» Openbox и браузера

Это самый важный шаг: отключить засыпание экрана, скрыть курсор мыши, заблокировать браузер в полноэкранном режиме и написать «бесконечный цикл», который мгновенно перезапускает браузер, даже если его случайно закроют.

**1. Создайте каталог конфигурации Openbox:**

Bash

```
mkdir -p ~/.config/openbox
```

**2. Запишите скрипт автозапуска (`autostart`):**

Скопируйте весь приведенный ниже код и нажмите Enter (это автоматически запишет все правила защиты в файл):

Bash

```
cat << 'EOF' > ~/.config/openbox/autostart
# 关闭屏幕保护
xset -dpms
xset s noblank
xset s off

# 隐藏鼠标
unclutter -idle 0.1 -root &

# 死循环启动 Chromium（崩溃或被关也能秒重启）
while true; do
    google-chrome \
        --kiosk \
        --no-first-run \
        --no-default-browser-check \
        --disable-infobars \
        --disable-session-crashed-bubble \
        --disable-translate \
        --disable-external-intent-requests \
        --autoplay-policy=no-user-gesture-required \
        --use-fake-ui-for-media-stream \
        "https://www.douyin.com"
    sleep 2
done &
EOF
```

**3. Заблокируйте комбинацию клавиш выхода `Alt+F4`:**

Чтобы никто не мог подключить клавиатуру и принудительно закрыть окно, мы удалим системные комбинации клавиш по умолчанию в Openbox.

Bash

```
cp /etc/xdg/openbox/rc.xml ~/.config/openbox/
sed -i '/<keybind key="A-F4">/,/<\/keybind>/d' ~/.config/openbox/rc.xml
```

### Шаг 5: Перезагрузка и проверка результата

Если вы отключили сетевой кабель или не хотите ждать полного подключения к сети, можно отключить службу ожидания сети, чтобы избежать зависания при загрузке

Bash

```
sudo systemctl mask systemd-networkd-wait-online.service
sudo systemctl mask NetworkManager-wait-online.service
```

Скройте информацию при загрузке (GRUB)

Bash

```
sudo sed -i 's/GRUB_CMDLINE_LINUX_DEFAULT=.*/GRUB_CMDLINE_LINUX_DEFAULT="quiet loglevel=3 systemd.show_status=false vt.global_cursor_default=0"/g' /etc/default/grub

echo 'GRUB_TIMEOUT_STYLE="hidden"' | sudo tee -a /etc/default/grub
echo 'GRUB_RECORDFAIL_TIMEOUT=0' | sudo tee -a /etc/default/grub

sudo update-grub
```

Установите громкость на 100% и перезагрузитесь:

Bash

```
amixer -q sset Master 100% unmute
sudo reboot
```

### Развертывание сервиса слов активации

Для развертывания сервиса обнаружения слов активации на моноблоке необходимо установить окружение Python, загрузить файлы проекта, настроить микрофон Camera и автозапуск при загрузке.

#### 1. Установка Miniconda

```bash
wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
bash Miniconda3-latest-Linux-x86_64.sh -b -p $HOME/miniconda3
~/miniconda3/bin/conda init bash
source ~/.bashrc
rm Miniconda3-latest-Linux-x86_64.sh
```


Убедитесь, что при входе происходит автоматический переход в окружение conda

```bash
if ! grep -q '.bashrc' ~/.bash_profile; then
    cat << 'EOF' >> ~/.bash_profile

if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi
EOF
fi
```


#### 2. Создание виртуального окружения Python

```bash
conda create -n test python=3.10 -y
conda activate test
```

Если возникает ошибка Terms of Service have not been accepted, выполните:

```bash
conda tos accept --override-channels --channel https://repo.anaconda.com/pkgs/main
conda tos accept --override-channels --channel https://repo.anaconda.com/pkgs/r
```

#### 3. Загрузка файлов проекта

Загрузите весь каталог `main/digital-human/` с машины разработки в каталог `~/digital-human/` на моноблоке:

```bash
# 在开发机上执行（将 <一体机IP> 替换为实际 IP）
scp -r main/digital-human/ xz@<一体机IP>:~/digital-human/
```

#### 4. Установка системных зависимостей

Сервису слов активации нужны библиотека аудиозахвата и плагины ALSA PulseAudio:

```bash
sudo apt install libportaudio2 portaudio19-dev libasound2-plugins -y
```

#### 5. Установка зависимостей Python

```bash
cd ~/digital-human/wakeword_runtime
pip install numpy
pip install -r requirements.txt
```

#### 6. Загрузка модели слов активации

Файлы модели не включены в проект, их нужно загрузить и настроить отдельно, подробнее см. раздел «Загрузка модели» в [docs/digital-human-wakeword.md](digital-human-wakeword.md).

#### 7. Изменение скрипта автозапуска Openbox

В autostart нужно добавить конфигурацию PulseAudio и микрофона Camera, а адрес Chrome заменить на тестовую страницу.

Сначала определите имя устройства микрофона Camera в PulseAudio:

```bash
pulseaudio --start
pactl list sources short
```

Найдите строку, содержащую `USB_Camera`, и запомните полное имя, например:

```
alsa_input.usb-SN0002_2K_USB_Camera_46435000_P030D00_SN0002-02.mono-fallback
```

Затем полностью перезапишите autostart следующим содержимым (замените `TARGET_MIC` на имя вашего реального устройства):

```bash
cat << 'EOF' > ~/.config/openbox/autostart
# 1. 启动声音服务并稍作等待
pulseaudio --start
sleep 1

# 2. 锁定 Camera 的麦克风（请替换为你的实际设备名）
TARGET_MIC="alsa_input.usb-SN0002_2K_USB_Camera_46435000_P030D00_SN0002-02.mono-fallback"

# 3. 设为系统默认麦克风
pactl set-default-source "$TARGET_MIC"

# 4. 解除静音
pactl set-source-mute "$TARGET_MIC" 0

# 5. 音量拉到 100%
pactl set-source-volume "$TARGET_MIC" 100%

# --- 极简桌面与浏览器环境配置 ---

# 关闭屏幕保护
xset -dpms
xset s noblank
xset s off

# 隐藏鼠标
unclutter -idle 0.1 -root &

# 死循环启动浏览器（崩溃或被关也能秒重启）
while true; do
    google-chrome \
        --kiosk \
        --no-first-run \
        --no-default-browser-check \
        --disable-infobars \
        --disable-session-crashed-bubble \
        --disable-translate \
        --disable-external-intent-requests \
        --autoplay-policy=no-user-gesture-required \
        --use-fake-ui-for-media-stream \
        "http://127.0.0.1:8006/index.html"
    sleep 2
done &
EOF
```

#### 8. Настройка автозапуска сервиса слов активации при загрузке

Создайте файл службы systemd, чтобы сервис слов активации запускался автоматически при загрузке.

Сначала узнайте UID текущего пользователя:

```bash
id -u $(whoami)
```

Затем замените `1000` в приведенном ниже коде на полученный UID (обычно первый пользователь имеет UID 1000):

```bash
sudo tee /etc/systemd/system/digital-human.service << 'EOF'
[Unit]
Description=Digital Human Runtime
After=network.target sound.target

[Service]
Type=simple
User=xz
Environment=XDG_RUNTIME_DIR=/run/user/1000
Environment=PULSE_SERVER=unix:/run/user/1000/pulse/native
WorkingDirectory=/home/xz/digital-human
ExecStartPre=/bin/sleep 10
ExecStart=/home/xz/miniconda3/envs/test/bin/python start.py
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
```

> **Важные примечания**:
> - `User=xz` — замените на имя вашего реального пользователя
> - `/run/user/1000` — замените на ваш реальный UID
> - Пути в `WorkingDirectory` и `ExecStart` — замените на ваш реальный путь развертывания
> - Переменные окружения PulseAudio в `Environment` **должны остаться**, иначе сервис слов активации и браузер не смогут одновременно использовать микрофон Camera

Включите и запустите службу:

```bash
sudo systemctl daemon-reload
sudo systemctl enable digital-human
sudo systemctl start digital-human
```

#### 9. Часто используемые команды управления службой

```bash
sudo systemctl start digital-human     # 立即启动
sudo systemctl stop digital-human      # 停止
sudo systemctl restart digital-human   # 重启
sudo systemctl status digital-human    # 查看状态
journalctl -u digital-human -f         # 查看实时日志
```
