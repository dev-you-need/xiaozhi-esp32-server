Войдите в AutoDL и арендуйте образ
Выберите образ:
```
PyTorch / 2.1.0 / 3.10(ubuntu22.04) / cuda 12.1
```

После включения сервера настройте академическое ускорение
```
source /etc/network_turbo
```

Перейдите в рабочую директорию
```
cd autodl-tmp/
```

Склонируйте проект
```
git clone https://gitclone.com/github.com/fishaudio/fish-speech.git ; cd fish-speech
```

Установите зависимости
```
pip install -e.
```

Если возникает ошибка, установите portaudio
```
apt-get install portaudio19-dev -y
```

После установки выполните
```
pip install torch==2.3.1 torchvision==0.18.1 torchaudio==2.3.1 --index-url https://download.pytorch.org/whl/cu121
```

Скачайте модель
```
cd tools
python download_models.py 
```

После завершения загрузки модели запустите интерфейс API
```
python -m tools.api_server --listen 0.0.0.0:6006 
```

Затем откройте в браузере страницу инстансов AutoDL
```
https://autodl.com/console/instance/list
```

Как показано на рисунке ниже, нажмите кнопку `自定义服务` («Пользовательский сервис») рядом с вашим сервером, чтобы включить проброс портов
![Пользовательский сервис](../images/fishspeech/autodl-01.png)

После настройки проброса портов откройте на локальном компьютере адрес `http://localhost:6006/` — теперь вы можете обращаться к интерфейсу fish-speech
![Предпросмотр сервиса](../images/fishspeech/autodl-02.png)


Если вы развёртываете одиночный модуль, основная конфигурация выглядит так
```
selected_module:
  TTS: FishSpeech
TTS:
  FishSpeech:
    reference_audio: ["config/assets/wakeup_words.wav",]
    reference_text: ["哈啰啊，我是小智啦，声音好听的台湾女孩一枚，超开心认识你耶，最近在忙啥，别忘了给我来点有趣的料哦，我超爱听八卦的啦",]
    api_key: "123"
    api_url: "http://127.0.0.1:6006/v1/tts"
```

Затем перезапустите сервис
