# Руководство по использованию инструмента тестирования производительности распознавания речи, больших языковых моделей, непотокового и потокового синтеза речи, а также визуальных моделей

1. В каталоге main/xiaozhi-server создайте каталог data
2. В каталоге data создайте файл .config.yaml
3. В файле .data/config.yaml укажите параметры вашей модели распознавания речи, большой языковой модели, потокового синтеза речи и визуальной модели
Например:
```
LLM:
  ChatGLMLLM:
    # 定义LLM API类型
    type: openai
    # glm-4-flash 是免费的，但是还是需要注册填写api_key的
    # 可在这里找到你的api key https://bigmodel.cn/usercenter/proj-mgmt/apikeys
    model_name: glm-4-flash
    url: https://open.bigmodel.cn/api/paas/v4/
    api_key: 你的chat-glm web key

TTS:

VLLM:

ASR:
```
4. В каталоге main/xiaozhi-server запустите performance_tester.py: 
```
python performance_tester.py
```
