# Руководство по использованию IndexStreamTTS

## Подготовка окружения
### 1. Клонируйте проект 
```bash 
git clone https://github.com/Ksuriuri/index-tts-vllm.git
```
Перейдите в распакованный каталог
```bash
cd index-tts-vllm
```
Переключитесь на указанную версию (используется историческая версия VLLM-0.10.2)
```bash
git checkout 224e8d5e5c8f66801845c66b30fa765328fd0be3
```

### 2. Создайте и активируйте conda-окружение
```bash 
conda create -n index-tts-vllm python=3.12
conda activate index-tts-vllm
```

### 3. Установите PyTorch — требуется версия 2.8.0 (последняя)
#### Посмотрите максимальную поддерживаемую версию видеокарты и фактически установленную версию
```bash
nvidia-smi
nvcc --version
``` 
#### Максимальная версия CUDA, поддерживаемая драйвером
```bash
CUDA Version: 12.8
```
#### Фактически установленная версия компилятора CUDA
```bash
Cuda compilation tools, release 12.8, V12.8.89
```
#### Соответствующая команда установки (по умолчанию pytorch даёт версию под драйвер 12.8)
```bash
pip install torch torchvision
```
Требуется версия pytorch 2.8.0 (соответствует vllm 0.10.2). Конкретную команду установки смотрите на [официальном сайте pytorch](https://pytorch.org/get-started/locally/)

### 4. Установите зависимости
```bash 
pip install -r requirements.txt
```

### 5. Скачайте веса модели
### Вариант 1: скачать официальные веса и сконвертировать их
Это официальные файлы весов — достаточно скачать их в любой локальный каталог. Поддерживаются веса IndexTTS-1.5.  
| HuggingFace                                                   | ModelScope                                                          |
|---------------------------------------------------------------|---------------------------------------------------------------------|
| [IndexTTS](https://huggingface.co/IndexTeam/Index-TTS)        | [IndexTTS](https://modelscope.cn/models/IndexTeam/Index-TTS)        |
| [IndexTTS-1.5](https://huggingface.co/IndexTeam/IndexTTS-1.5) | [IndexTTS-1.5](https://modelscope.cn/models/IndexTeam/IndexTTS-1.5) |

Ниже в качестве примера показана установка через ModelScope.  
#### Обратите внимание: git должен быть установлен, а lfs инициализирован и включён (если уже установлен, можно пропустить)
```bash
sudo apt-get install git-lfs
git lfs install
```
Создайте каталог модели и скачайте модель
```bash 
mkdir model_dir
cd model_dir
git clone https://www.modelscope.cn/IndexTeam/IndexTTS-1.5.git
```

#### Конвертация весов модели
```bash 
bash convert_hf_format.sh /path/to/your/model_dir
```
Например: если скачанная вами модель IndexTTS-1.5 находится в каталоге model_dir, выполните следующую команду:
```bash
bash convert_hf_format.sh model_dir/IndexTTS-1.5
```
Эта операция конвертирует официальные веса модели в версию, совместимую с библиотекой transformers, и сохраняет их в папку vllm внутри каталога с весами модели — так библиотеке vllm будет проще загрузить веса.

### 6. Подстройте интерфейс под проект
Данные, возвращаемые интерфейсом, не совместимы с проектом, поэтому нужно немного подстроить их, чтобы интерфейс сразу возвращал аудиоданные.
```bash
vi api_server.py
```
```bash 
@app.post("/tts", responses={
    200: {"content": {"application/octet-stream": {}}},
    500: {"content": {"application/json": {}}}
})
async def tts_api(request: Request):
    try:
        data = await request.json()
        text = data["text"]
        character = data["character"]

        global tts
        sr, wav = await tts.infer_with_ref_audio_embed(character, text)

        return Response(content=wav.tobytes(), media_type="application/octet-stream")
        
    except Exception as ex:
        tb_str = ''.join(traceback.format_exception(type(ex), ex, ex.__traceback__))
        print(tb_str)
        return JSONResponse(
            status_code=500,
            content={
                "status": "error",
                "error": str(tb_str)
            }
        )
```

### 7. Напишите sh-скрипт запуска (убедитесь, что он выполняется в соответствующем conda-окружении)
```bash 
vi start_api.sh
```
### Вставьте приведённое ниже содержимое и нажмите :, введите wq для сохранения  
#### Путь /home/system/index-tts-vllm/model_dir/IndexTTS-1.5 в скрипте замените на фактический
```bash
# 激活conda环境
conda activate index-tts-vllm 
echo "激活项目conda环境"
sleep 2
# 查找占用11996端口的进程号
PID_VLLM=$(sudo netstat -tulnp | grep 11996 | awk '{print $7}' | cut -d'/' -f1)

# 检查是否找到进程号
if [ -z "$PID_VLLM" ]; then
  echo "没有找到占用11996端口的进程"
else
  echo "找到占用11996端口的进程，进程号为: $PID_VLLM"
  # 先尝试普通kill，等待2秒
  kill $PID_VLLM
  sleep 2
  # 检查进程是否还在
  if ps -p $PID_VLLM > /dev/null; then
    echo "进程仍在运行，强制终止..."
    kill -9 $PID_VLLM
  fi
  echo "已终止进程 $PID_VLLM"
fi

# 查找占用VLLM::EngineCore进程
GPU_PIDS=$(ps aux | grep -E "VLLM|EngineCore" | grep -v grep | awk '{print $2}')

# 检查是否找到进程号
if [ -z "$GPU_PIDS" ]; then
  echo "没有找到VLLM相关进程"
else
  echo "找到VLLM相关进程，进程号为: $GPU_PIDS"
  # 先尝试普通kill，等待2秒
  kill $GPU_PIDS
  sleep 2
  # 检查进程是否还在
  if ps -p $GPU_PIDS > /dev/null; then
    echo "进程仍在运行，强制终止..."
    kill -9 $GPU_PIDS
  fi
  echo "已终止进程 $GPU_PIDS"
fi

# 创建tmp目录（如果不存在）
mkdir -p tmp

# 后台运行api_server.py，日志重定向到tmp/server.log
nohup python api_server.py --model_dir /home/system/index-tts-vllm/model_dir/IndexTTS-1.5 --port 11996 > tmp/server.log 2>&1 &
echo "api_server.py 已在后台运行，日志请查看 tmp/server.log"
```
Дайте скрипту права на выполнение и запустите его
```bash 
chmod +x start_api.sh
./start_api.sh
```
Журнал выводится в tmp/server.log; посмотреть его можно командой
```bash
tail -f tmp/server.log
```
Если памяти видеокарты достаточно, в скрипт можно добавить параметр запуска ----gpu_memory_utilization для регулировки доли занимаемой видеопамяти. Значение по умолчанию — 0.25.

## Настройка тембра голоса
index-tts-vllm поддерживает регистрацию пользовательских тембров через конфигурационный файл; доступны как одиночный, так и смешанный тембр.  
Пользовательские тембры настраиваются в файле assets/speaker.json в корне проекта.
### Описание формата конфигурации
```bash
{
    "说话人名称1": [
        "音频文件路径1.wav",
        "音频文件路径2.wav"
    ],
    "说话人名称2": [
        "音频文件路径3.wav"
    ]
}
```
### Внимание (после настройки роли нужно перезапустить сервис для регистрации тембра)
После добавления нужно добавить соответствующего диктора в 智控台 (при одиночном модуле — заменить соответствующий voice).
