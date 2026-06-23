package com.z2ray.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

interface Strings {
    val tabHome: String
    val tabConfigs: String
    val tabSecurity: String
    val tabSubs: String
    val tabConsole: String
    
    // HomeScreen
    val protectedTunnel: String
    val connected: String
    val connecting: String
    val shieldInactive: String
    val latencyLabel: String
    val encryptionLabel: String
    val downloadLabel: String
    val uploadLabel: String
    val modeLabel: String
    val noServerSelected: String
    val tapChooseConfig: String
    val securityShieldOffline: String
    val smartRoute: String
    val globalProxy: String
    val directRoute: String
    
    // ServerListScreen
    val activeGateways: String
    val activeGatewaysDesc: String
    val importClip: String
    val speedTestAll: String
    val clearAll: String
    val noServersFound: String
    val noServersFoundDesc: String
    val searchPlaceholder: String
    val protocol: String
    val latency: String
    val enterV2rayLink: String
    val importLinkTitle: String
    val importLinkDesc: String
    val cancel: String
    val importBtn: String
    val success: String
    val invalidLink: String
    
    // SecurityScreen
    val advProtocols: String
    val advProtocolsDesc: String
    val routingEngine: String
    val bypassIranDesc: String
    val globalProxyDesc: String
    val directDesc: String
    val stealthSni: String
    val stealthSniDesc: String
    val sniLabel: String
    val sniHint: String
    val antiDpi: String
    val antiDpiDesc: String
    val sizeRange: String
    val delayRange: String
    val secureDns: String
    val dnsDescCloudflare: String
    val dnsDescGoogle: String
    val dnsDescShecan: String
    val dnsDescSystem: String
    val perAppProxyTitle: String
    val perAppProxyDesc: String
    val bypassIranApps: String
    val bypassIranAppsDesc: String
    val selectApps: String
    
    // SubsScreen
    val subManager: String
    val subManagerDesc: String
    val addSub: String
    val groupName: String
    val subUrl: String
    val noSubs: String
    val noSubsDesc: String
    val fetchSuccess: String
    val fetchFail: String
    
    // ConsoleScreen
    val diagnosticLog: String
    val diagnosticLogDesc: String
    val clearLogs: String
    val exportLogs: String
}

object EnglishStrings : Strings {
    override val tabHome = "Home"
    override val tabConfigs = "Configs"
    override val tabSecurity = "Settings"
    override val tabSubs = "Subs"
    override val tabConsole = "Console"
    
    override val protectedTunnel = "Protected Tunnel"
    override val connected = "Connected"
    override val connecting = "Connecting..."
    override val shieldInactive = "Not Connected"
    override val latencyLabel = "LATENCY"
    override val encryptionLabel = "ENCRYPTION"
    override val downloadLabel = "DOWNLOAD"
    override val uploadLabel = "UPLOAD"
    override val modeLabel = "MODE"
    override val noServerSelected = "No Server Selected"
    override val tapChooseConfig = "Tap to choose a config"
    override val securityShieldOffline = "Stealth Shield Offline"
    override val smartRoute = "Smart Route"
    override val globalProxy = "Global Proxy"
    override val directRoute = "Direct Bypass"
    
    override val activeGateways = "ACTIVE SHIELD GATEWAYS"
    override val activeGatewaysDesc = "Import configurations easily or ping live servers to find fast secure bypass corridors."
    override val importClip = "Import from Clipboard"
    override val speedTestAll = "Speed Test All"
    override val clearAll = "Clear Configs"
    override val noServersFound = "No secure configs stored locally"
    override val noServersFoundDesc = "Import vless://, vmess://, trojan://, ss://, hy2://, socks://, Clash/Mihomo YAML, SIP008 JSON, or raw Xray JSON configs."
    override val searchPlaceholder = "Search gate name / protocol..."
    override val protocol = "PROTOCOL"
    override val latency = "LATENCY"
    override val enterV2rayLink = "Enter v2ray link (vless/vmess/trojan/ss)"
    override val importLinkTitle = "Import Manual Config"
    override val importLinkDesc = "Paste a share link, Clash/Mihomo YAML, SIP008 JSON, or raw Xray JSON profile."
    override val cancel = "Cancel"
    override val importBtn = "Import Config"
    override val success = "Import Success!"
    override val invalidLink = "Invalid connection uri structure!"
    
    override val advProtocols = "ADVANCED CIRCUMVENTION PROTOCOLS"
    override val advProtocolsDesc = "Optimize anti-filtering and active traffic encryption specifically configured for domestic and restricted networks."
    override val routingEngine = "Intelligent Routing Rule Engine"
    override val bypassIranDesc = "Bypasses domestic websites (.ir) and local IPs so local traffic stays direct and fast."
    override val globalProxyDesc = "Routes all device applications and connection interfaces through the secure proxy tunnel."
    override val directDesc = "Temporarily suspends routing controls and exits tunnel safely."
    override val stealthSni = "Stealth SNI (Active Payload Cloaking)"
    override val stealthSniDesc = "Modifies the TLS ClientHello Server Name Indication header to bypass deep packet filters on restricted networks."
    override val sniLabel = "Stealth SNI Overrides"
    override val sniHint = "Popular bypass SNIs: images.apple.com, assets.github.com, speedtest.net"
    override val antiDpi = "Anti-DPI Packet Fragmentation"
    override val antiDpiDesc = "Splits secure ClientHello handshake TLS records into mini segments, neutralizing SNI detection scanners."
    override val sizeRange = "Size Range (bytes)"
    override val delayRange = "Delay Range (ms)"
    override val secureDns = "Secure DNS (DNS-over-HTTPS)"
    override val dnsDescCloudflare = "Encrypted queries to Cloudflare securely (highly unblockable)."
    override val dnsDescGoogle = "Queries Google DoH endpoints anonymously."
    override val dnsDescShecan = "Popular anti-sanction DNS resolving foreign tech restrictions."
    override val dnsDescSystem = "Requests default cellular network service routers."
    
    override val perAppProxyTitle = "Per-App Proxy (Split Tunneling)"
    override val perAppProxyDesc = "Bypass blockades or selectively secure individual target apps on your device."
    override val bypassIranApps = "Bypass Domestic Apps (Iran)"
    override val bypassIranAppsDesc = "Automatically route banking, shopping, and Snapp apps directly to avoid payment interference."
    override val selectApps = "Choose Selected App Corridors"
    
    override val subManager = "SUBSCRIPTION LIST GROUP MANAGER"
    override val subManagerDesc = "Manage secure auto-updating gateway config packages. Decodes base64 subscription links dynamically."
    override val addSub = "Register New Subscription"
    override val groupName = "Subscription Group Name"
    override val subUrl = "Subscription Group Link URL"
    override val noSubs = "No subscription channels added"
    override val noSubsDesc = "Add an auto-updating subscription to download lists of active servers automatically."
    override val fetchSuccess = "Successfully fetched servers!"
    override val fetchFail = "Failed to update configuration link!"
    
    override val diagnosticLog = "DIAGNOSTIC LOG SECURITY ENGINE"
    override val diagnosticLogDesc = "Real-time tunnel state changes, latency metrics, handshake details, and bypass rules."
    override val clearLogs = "CLEAR LOGS"
    override val exportLogs = "EXPORT LOGS"
}

object PersianStrings : Strings {
    override val tabHome = "خانه"
    override val tabConfigs = "کانفیگ‌ها"
    override val tabSecurity = "تنظیمات"
    override val tabSubs = "اشتراک‌ها"
    override val tabConsole = "کنسول"
    
    override val protectedTunnel = "تونل امن متصل است"
    override val connected = "متصل شد"
    override val connecting = "در حال اتصال..."
    override val shieldInactive = "متصل نیست"
    override val latencyLabel = "تاخیر (پینگ)"
    override val encryptionLabel = "رمزنگاری"
    override val downloadLabel = "دانلود"
    override val uploadLabel = "آپلود"
    override val modeLabel = "حالت"
    override val noServerSelected = "کانفیگی انتخاب نشده است"
    override val tapChooseConfig = "برای انتخاب کانفیگ لمس کنید"
    override val securityShieldOffline = "سپر مخفی غیرفعال است"
    override val smartRoute = "مسیر هوشمند"
    override val globalProxy = "پراکسی کل"
    override val directRoute = "اتصال مستقیم"
    
    override val activeGateways = "دروازه‌های عبور فعال"
    override val activeGatewaysDesc = "پیکربندی‌ها را به راحتی وارد کنید یا کانفیگ‌ها را پینگ کنید تا سریع‌ترین کوریدور امن و ضدفیلتر را بیابید."
    override val importClip = "کپی از کلیپ‌بورد"
    override val speedTestAll = "تست پینگ همه"
    override val clearAll = "پاک کردن کانفیگ‌ها"
    override val noServersFound = "هیچ کانفیگ امنی ذخیره نشده است"
    override val noServersFoundDesc = "لینک‌های vless://، vmess://، trojan://، ss://، hy2://، socks:// یا کانفیگ Clash/Mihomo، SIP008 و JSON خام Xray را وارد کنید."
    override val searchPlaceholder = "جستجو در کانفیگ‌ها یا پروتکل‌ها..."
    override val protocol = "پروتکل"
    override val latency = "پینگ"
    override val enterV2rayLink = "آدرس کانفیگ را وارد کنید"
    override val importLinkTitle = "افزودن دستی کانفیگ"
    override val importLinkDesc = "لینک کانفیگ، Clash/Mihomo YAML، SIP008 JSON یا JSON خام Xray را وارد کنید."
    override val cancel = "لغو"
    override val importBtn = "افزودن کانفیگ"
    override val success = "کانفیگ با موفقیت اضافه شد!"
    override val invalidLink = "پیکربندی لینک وارد شده نامعتبر است!"
    
    override val advProtocols = "پروتکل‌های پیشرفته ضد فیلتر"
    override val advProtocolsDesc = "بهینه‌سازی ضدفیلترینگ و لایه‌های رمزنگاری ترافیک متناسب با شبکه‌های همراه اول، ایرانسل و اینترنت خانگی."
    override val routingEngine = "موتور هوشمند مدیریت مسیر"
    override val bypassIranDesc = "دور زدن وب‌سایت‌های ایرانی (ir.) و آی‌پی‌های داخلی برای اجرای سریع و مستقیم برنامه‌های بانکی."
    override val globalProxyDesc = "انتقال کل ترامادول و ترافیک تمام برنامه‌ها و اتصالات از بستر تونل عبور امن."
    override val directDesc = "قطع موقت تونل‌زنی ترافیک و خروج ایمن."
    override val stealthSni = "پوشش مخفی SNI (استتار پلود)"
    override val stealthSniDesc = "تغییر هدر Server Name Indication در دست‌دهی TLS برای عبور از فیلترینگ پیشرفته وب‌سایت‌ها."
    override val sniLabel = "هدرهای SNI سفارشی متصل"
    override val sniHint = "آدرس‌های پیشنهادی: images.apple.com, assets.github.com, speedtest.net"
    override val antiDpi = "تکه‌تکه‌کردن پکت‌ها (Anti-DPI)"
    override val antiDpiDesc = "تقسیم رکوردهای TLS ClientHello به قطعات ریز برای خنثی‌سازی اسکنرهای تشخیص دامنه فیلترینگ."
    override val sizeRange = "محدوده اندازه (بایت)"
    override val delayRange = "محدوده تاخیر (میلی‌ثانیه)"
    override val secureDns = "دی‌ان‌اس امن (DNS-over-HTTPS)"
    override val dnsDescCloudflare = "ارسال درخواست‌های دی‌ان‌اس به کلودفلر به صورت رمزگذاری شده بدون انسداد."
    override val dnsDescGoogle = "اتصال به سرویس‌های دی‌ان‌اس گوگل بدون افشای حریم خصوصی."
    override val dnsDescShecan = "سرویس شکن جهت دور زدن تحریم‌های فناوری بین‌المللی علیه کاربران ایرانی."
    override val dnsDescSystem = "استفاده از دی‌ان‌اس پیش‌فرض شبکه اپراتور همراه."
    
    override val perAppProxyTitle = "پراکسی برنامه‌ها (Per-App Proxy)"
    override val perAppProxyDesc = "فقط برنامه‌های خاصی را از فیلترشکن عبور دهید یا برنامه‌های خاصی را مستثنی کنید."
    override val bypassIranApps = "دور زدن برنامه‌های داخلی ایران"
    override val bypassIranAppsDesc = "به طور خودکار برنامه‌های بانکی، خرید و اسنپ مستقیماً بدون فیلترشکن باز می‌شوند تا تداخلی ایجاد نشود."
    override val selectApps = "انتخاب مسیر عبور برنامه‌ها"
    
    override val subManager = "مدیریت لینک‌های اشتراکی کانال"
    override val subManagerDesc = "بروزرسانی خودکار پکیج کانفیگ‌ها. پشتیبانی از رمزگشایی کانی‌های بیس۶۴ اشتراک به صورت پویا."
    override val addSub = "ثبت لینک اشتراک جدید"
    override val groupName = "نام کانال یا اشتراک"
    override val subUrl = "لینک یا آدرس URL اشتراک"
    override val noSubs = "هیچ گروه اشتراکی تعریف نشده است"
    override val noSubsDesc = "یک لینک کانال اشتراکی اضافه کنید تا به طور خودکار لیست آخرین کانفیگ‌ها را بارگذاری و بروزرسانی کنید."
    override val fetchSuccess = "لیست کانفیگ‌ها با موفقیت بروزرسانی شد!"
    override val fetchFail = "بروزرسانی کانال با خطا مواجه شد!"
    
    override val diagnosticLog = "کنسول عیب‌یابی و مانیتورینگ امنیتی"
    override val diagnosticLogDesc = "نمایش زنده لاگ سیستم، وضعیت دست‌دهی کانال، پینگ دروازه‌ها و قوانین هدایت مسیریاب ترافیک."
    override val clearLogs = "پاک کردن لاگ‌ها"
    override val exportLogs = "خروجی گرفتن از لاگ"
}

object RussianStrings : Strings {
    override val tabHome = "Главная"
    override val tabConfigs = "Конфиги"
    override val tabSecurity = "Настройки"
    override val tabSubs = "Подписки"
    override val tabConsole = "Консоль"
    
    override val protectedTunnel = "Защищенный туннель активен"
    override val connected = "Подключено"
    override val connecting = "Подключение..."
    override val shieldInactive = "Не подключено"
    override val latencyLabel = "ЗАДЕРЖКА"
    override val encryptionLabel = "ШИФРОВАНИЕ"
    override val downloadLabel = "СКАЧАТЬ"
    override val uploadLabel = "ОТПРАВКА"
    override val modeLabel = "РЕЖИМ"
    override val noServerSelected = "Сервер не выбран"
    override val tapChooseConfig = "Выберите конфиг для подключения"
    override val securityShieldOffline = "Стелс-щит неактивен"
    override val smartRoute = "Умный обход"
    override val globalProxy = "Прокси РФ/Мир"
    override val directRoute = "Прямой обход"
    
    override val activeGateways = "АКТИВНЫЕ ШЛЮЗЫ ЗАЩИТЫ"
    override val activeGatewaysDesc = "Просто импортируйте конфигурации или проверяйте пинг, чтобы найти быстрые коридоры."
    override val importClip = "Импорт из буфера"
    override val speedTestAll = "Проверить все"
    override val clearAll = "Очистить шлюзы"
    override val noServersFound = "Нет локальных шлюзов"
    override val noServersFoundDesc = "Импортируйте vless://, vmess://, trojan://, ss://, hy2://, socks://, Clash/Mihomo YAML, SIP008 JSON или raw Xray JSON."
    override val searchPlaceholder = "Поиск шлюза или протокола..."
    override val protocol = "ПРОТОКОЛ"
    override val latency = "ПИНГ"
    override val enterV2rayLink = "Вставьте ссылку"
    override val importLinkTitle = "Импортировать шлюз вручную"
    override val importLinkDesc = "Вставьте share link, Clash/Mihomo YAML, SIP008 JSON или raw Xray JSON профиль."
    override val cancel = "Отмена"
    override val importBtn = "Добавить конфиг"
    override val success = "Импортировано успешно!"
    override val invalidLink = "Неверная ссылка!"
    
    override val advProtocols = "РАСШИРЕННЫЙ ОБХОД ЦЕНЗУРЫ"
    override val advProtocolsDesc = "Оптимизация шифрования трафика для стабильной работы от DPI блокировок."
    override val routingEngine = "Умный механизм маршрутизации"
    override val bypassIranDesc = "Обходит локальные сайты (.ir) напрямую для быстрой работы без туннелирования."
    override val globalProxyDesc = "Весь трафик приложений устройства перенаправляется через защищенный туннель."
    override val directDesc = "Временно приостановить туннель."
    override val stealthSni = "Стелс SNI (Маскировка заголовков)"
    override val stealthSniDesc = "Изменяет заголовки TLS ClientHello Server Name Indication для обхода глубокого инспектирования пакетов DPI."
    override val sniLabel = "Подмена заголовка SNI"
    override val sniHint = "Полезные SNI: images.apple.com, assets.github.com, speedtest.net"
    override val antiDpi = "Фрагментация пакетов Anti-DPI"
    override val antiDpiDesc = "Разделяет TLS ClientHello на мини-фрагменты, что прекращает обнаружение запрещенных ресурсов фильтром."
    override val sizeRange = "Размеры сегментов (байт)"
    override val delayRange = "Интервал задержки (мс)"
    override val secureDns = "Безопасный DNS (DNS-over-HTTPS)"
    override val dnsDescCloudflare = "Зашифрованные DNS запросы к Cloudflare (предотвращает подмену IP)."
    override val dnsDescGoogle = "Запросы к DoH серверам Google анонимно."
    override val dnsDescShecan = "Специализированный анти-санкционный DNS Shecan."
    override val dnsDescSystem = "Использовать стандартный DNS вашего сотового провайдера."
    
    override val perAppProxyTitle = "Прокси для приложений"
    override val perAppProxyDesc = "Выберите, какие приложения отправлять в туннель, а какие оставить напрямую."
    override val bypassIranApps = "Обходить национальные приложения"
    override val bypassIranAppsDesc = "Автоматический обход государственных, платежных и банковских веб-сервисов страны."
    override val selectApps = "Список приложений для прокси"
    
    override val subManager = "УПРАВЛЕНИЕ ГРУППАМИ ПОДПИСОК"
    override val subManagerDesc = "Автоматическое обновление пакетов конфигураций из сторонних подписок."
    override val addSub = "Добавить новую подписку"
    override val groupName = "Название подписки"
    override val subUrl = "Ссылка на подписку"
    override val noSubs = "Группы подписок отсутствуют"
    override val noSubsDesc = "Добавьте рабочую подписку, чтобы всегда иметь свежий пул серверов."
    override val fetchSuccess = "Конфиги обновлены!"
    override val fetchFail = "Ошибка получения конфигураций!"
    
    override val diagnosticLog = "ДИАГНОСТИЧЕСКАЯ КОНСОЛЬ АНАЛИЗА"
    override val diagnosticLogDesc = "История рукопожатий TLS, логинов, метрики задержки и правил DPI обхода."
    override val clearLogs = "ОЧИСТИТЬ ЛОГИ"
    override val exportLogs = "ЭКСПОРТ ЛОГОВ"
}

val LocalAppStrings = staticCompositionLocalOf<Strings> { EnglishStrings }

object AppStrings {
    val current: Strings
        @Composable
        get() = LocalAppStrings.current
}
