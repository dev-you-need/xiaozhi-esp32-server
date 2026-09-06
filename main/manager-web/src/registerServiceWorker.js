/* eslint-disable no-console */

export const register = () => {
  if (process.env.NODE_ENV === 'production' && 'serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      const swUrl = `${process.env.BASE_URL}service-worker.js`;
      
      console.info(`[Сервис Сяочжи] Попытка регистрации Service Worker, URL: ${swUrl}`);
      
      // Сначала проверка, зарегистрирован ли уже Service Worker
      navigator.serviceWorker.getRegistrations().then(registrations => {
        if (registrations.length > 0) {
          console.info('[Сервис Сяочжи] Обнаружена существующая регистрация Service Worker, проверка обновлений');
        }
        
        // Продолжение регистрации Service Worker
        navigator.serviceWorker
          .register(swUrl)
          .then(registration => {
            console.info('[Сервис Сяочжи] Service Worker успешно зарегистрирован');
            
            // Обработка обновлений
            registration.onupdatefound = () => {
              const installingWorker = registration.installing;
              if (installingWorker == null) {
                return;
              }
              installingWorker.onstatechange = () => {
                if (installingWorker.state === 'installed') {
                  if (navigator.serviceWorker.controller) {
                    // Контент обновлен, уведомление пользователя об обновлении
                    console.log('[Сервис Сяочжи] Доступен новый контент, обновите страницу');
                    // Здесь можно показать уведомление об обновлении
                    const updateNotification = document.createElement('div');
                    updateNotification.style.cssText = `
                      position: fixed;
                      bottom: 20px;
                      right: 20px;
                      background: #409EFF;
                      color: white;
                      padding: 12px 20px;
                      border-radius: 4px;
                      box-shadow: 0 2px 12px 0 rgba(0,0,0,.1);
                      z-index: 9999;
                    `;
                    updateNotification.innerHTML = `
                      <div style="display: flex; align-items: center;">
                        <span style="margin-right: 10px;">Обнаружена новая версия, нажмите для обновления</span>
                        <button style="background: white; color: #409EFF; border: none; padding: 5px 10px; border-radius: 3px; cursor: pointer;">Обновить</button>
                      </div>
                    `;
                    document.body.appendChild(updateNotification);
                    updateNotification.querySelector('button').addEventListener('click', () => {
                      window.location.reload();
                    });
                  } else {
                    // Все в порядке, Service Worker успешно установлен
                    console.log('[Сервис Сяочжи] Контент кэширован для офлайн использования');
                    
                    // Инициализация кэша
                    setTimeout(() => {
                      // Прогрев CDN кэша
                      const cdnUrls = [
                        'https://unpkg.com/element-ui@2.15.14/lib/theme-chalk/index.css',
                        'https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.min.css',
                        'https://unpkg.com/vue@2.6.14/dist/vue.min.js',
                        'https://unpkg.com/vue-router@3.6.5/dist/vue-router.min.js',
                        'https://unpkg.com/vuex@3.6.2/dist/vuex.min.js',
                        'https://unpkg.com/element-ui@2.15.14/lib/index.js',
                        'https://unpkg.com/axios@0.27.2/dist/axios.min.js',
                        'https://unpkg.com/opus-decoder@0.7.7/dist/opus-decoder.min.js'
                      ];
                      
                      // Прогрев кэша
                      cdnUrls.forEach(url => {
                        fetch(url, { mode: 'no-cors' }).catch(err => {
                          console.log(`Ошибка прогрева кэша ${url}`, err);
                        });
                      });
                    }, 2000);
                  }
                }
              };
            };
          })
          .catch(error => {
            console.error('Ошибка регистрации Service Worker:', error);
            
            if (error.name === 'TypeError' && error.message.includes('Failed to register a ServiceWorker')) {
              console.warn('[Сервис Сяочжи] Ошибка сети при регистрации Service Worker, CDN ресурсы могут быть не кэшированы');
              if (process.env.NODE_ENV === 'production') {
                console.info(
                  'Возможные причины: 1. Сервер не настроен с правильными MIME типами 2. Проблемы с SSL сертификатом сервера 3. Сервер не возвращает файл service-worker.js'
                );
              }
            }
          });
      });
    });
  }
};

export const unregister = () => {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.ready
      .then(registration => {
        registration.unregister();
      })
      .catch(error => {
        console.error(error.message);
      });
  }
}; 
