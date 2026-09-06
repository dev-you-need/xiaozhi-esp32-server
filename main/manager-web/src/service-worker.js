/* глобальный self, workbox */

// Пользовательская логика обработки установки и активации Service Worker
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// Список CDN ресурсов
const CDN_CSS = [
  'https://unpkg.com/element-ui@2.15.14/lib/theme-chalk/index.css',
  'https://cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.min.css'
];

const CDN_JS = [
  'https://unpkg.com/vue@2.6.14/dist/vue.min.js',
  'https://unpkg.com/vue-router@3.6.5/dist/vue-router.min.js',
  'https://unpkg.com/vuex@3.6.2/dist/vuex.min.js',
  'https://unpkg.com/element-ui@2.15.14/lib/index.js',
  'https://unpkg.com/axios@0.27.2/dist/axios.min.js',
  'https://unpkg.com/opus-decoder@0.7.7/dist/opus-decoder.min.js'
];

// Автоматическое выполнение при инъекции manifest в Service Worker
const manifest = self.__WB_MANIFEST || [];

// Проверка включения CDN режима
const isCDNEnabled = manifest.some(entry => 
  entry.url === 'cdn-mode' && entry.revision === 'enabled'
);

console.log(`Service Worker инициализирован, CDN режим: ${isCDNEnabled ? 'включен' : 'выключен'}`);

// Инъекция кода workbox
importScripts('https://storage.googleapis.com/workbox-cdn/releases/7.0.0/workbox-sw.js');
workbox.setConfig({ debug: false });

// Запуск workbox
workbox.core.skipWaiting();
workbox.core.clientsClaim();

// Предварительное кэширование офлайн страницы
const OFFLINE_URL = '/offline.html';
workbox.precaching.precacheAndRoute([
  { url: OFFLINE_URL, revision: null }
]);

// Добавление обработчика события установки, вывод сообщения об установке в консоль
self.addEventListener('install', event => {
  if (isCDNEnabled) {
    console.log('Service Worker установлен, начало кэширования CDN ресурсов');
  } else {
    console.log('Service Worker установлен, CDN режим выключен, кэширование только локальных ресурсов');
  }
  
  // Гарантия кэширования офлайн страницы
  event.waitUntil(
    caches.open('offline-cache').then((cache) => {
      return cache.add(OFFLINE_URL);
    })
  );
});

// Добавление обработчика события активации
self.addEventListener('activate', event => {
  console.log('Service Worker активирован, теперь управляет страницей');
  
  // Очистка кэшей старых версий
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.filter(cacheName => {
          // Очистка кэшей кроме текущей версии
          return cacheName.startsWith('workbox-') && !workbox.core.cacheNames.runtime.includes(cacheName);
        }).map(cacheName => {
          return caches.delete(cacheName);
        })
      );
    })
  );
});

// Добавление перехватчика fetch событий для просмотра попадания CDN ресурсов в кэш
self.addEventListener('fetch', event => {
  // Мониторинг кэширования CDN ресурсов только при включенном CDN режиме
  if (isCDNEnabled) {
    const url = new URL(event.request.url);
    
    // Вывод информации о попадании в кэш для CDN ресурсов
    if ([...CDN_CSS, ...CDN_JS].includes(url.href)) {
      // Без нарушения нормального процесса fetch, только добавление логов
      console.log(`Запрос CDN ресурса: ${url.href}`);
    }
  }
});

// Кэширование CDN ресурсов только в CDN режиме
if (isCDNEnabled) {
  // Кэширование CDN CSS ресурсов
  workbox.routing.registerRoute(
    ({ url }) => CDN_CSS.includes(url.href),
    new workbox.strategies.CacheFirst({
      cacheName: 'cdn-stylesheets',
      plugins: [
        new workbox.expiration.ExpirationPlugin({
          maxAgeSeconds: 365 * 24 * 60 * 60, // Увеличение до 1 года кэширования
          maxEntries: 10, // Максимум 10 CSS файлов
        }),
        new workbox.cacheableResponse.CacheableResponsePlugin({
          statuses: [0, 200], // Кэширование успешных ответов
        }),
      ],
    })
  );

  // Кэширование CDN JS ресурсов
  workbox.routing.registerRoute(
    ({ url }) => CDN_JS.includes(url.href),
    new workbox.strategies.CacheFirst({
      cacheName: 'cdn-scripts',
      plugins: [
        new workbox.expiration.ExpirationPlugin({
          maxAgeSeconds: 365 * 24 * 60 * 60, // Увеличение до 1 года кэширования
          maxEntries: 20, // Максимум 20 JS файлов
        }),
        new workbox.cacheableResponse.CacheableResponsePlugin({
          statuses: [0, 200], // Кэширование успешных ответов
        }),
      ],
    })
  );
}

// Кэширование локальных статических ресурсов независимо от CDN режима
workbox.routing.registerRoute(
  /\.(?:js|css|png|jpg|jpeg|svg|gif|ico|woff|woff2|eot|ttf|otf)$/,
  new workbox.strategies.StaleWhileRevalidate({
    cacheName: 'static-resources',
    plugins: [
      new workbox.expiration.ExpirationPlugin({
        maxAgeSeconds: 7 * 24 * 60 * 60, // 7 дней кэширования
        maxEntries: 50, // Максимум 50 файлов
      }),
    ],
  })
);

// Кэширование HTML страниц
workbox.routing.registerRoute(
  /\.html$/,
  new workbox.strategies.NetworkFirst({
    cacheName: 'html-cache',
    plugins: [
      new workbox.expiration.ExpirationPlugin({
        maxAgeSeconds: 1 * 24 * 60 * 60, // 1 день кэширования
        maxEntries: 10, // Максимум 10 HTML файлов
      }),
    ],
  })
);

// Офлайн страница - использование более надежного способа обработки
workbox.routing.setCatchHandler(async ({ event }) => {
  // Возврат соответствующей страницы по умолчанию в зависимости от типа запроса
  switch (event.request.destination) {
    case 'document':
      // Если запрос веб-страницы, возврат офлайн страницы
      return caches.match(OFFLINE_URL);
    default:
      // Все остальные запросы возвращают ошибку
      return Response.error();
  }
}); 
