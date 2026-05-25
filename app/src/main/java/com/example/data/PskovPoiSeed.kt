package com.example.data

import com.example.domain.*

object PskovPoiSeed {

    fun getAllPskovPOIs(): List<PointOfInterest> = listOf(

        // ══════════════════════════════════════════════════════
        // БЛОК 1 — КРЕМЛЬ И КРОМ (ABYSSAL_GATE / NEXUS_POINT)
        // Зона: центр, ул. Кремль
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_krom_kremlin",
            name = "Псковский Кром",
            realName = "Псковский Кремль",
            type = PoiType.ABYSSAL_GATE,
            element = Element.MIST,
            latitude = 57.8203, longitude = 28.3301,
            minLevel = 20, maxLevel = 40,
            osmTags = mapOf("historic" to "castle", "name" to "Псковский Кремль", "wikidata" to "Q581677")
        ),
        PointOfInterest(
            id = "pskov_trinity_cathedral",
            name = "Собор Живоначальной Троицы",
            realName = "Троицкий кафедральный собор",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8206, longitude = 28.3296,
            minLevel = 18, maxLevel = 35,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "building" to "cathedral")
        ),
        PointOfInterest(
            id = "pskov_prikaz_palaty",
            name = "Приказные Палаты",
            realName = "Приказные палаты (музей)",
            type = PoiType.NEXUS_POINT,
            element = Element.MIST,
            latitude = 57.8201, longitude = 28.3298,
            minLevel = 12, maxLevel = 25,
            osmTags = mapOf("tourism" to "museum", "historic" to "yes")
        ),
        PointOfInterest(
            id = "pskov_dovmontov_city",
            name = "Довмонтов Город",
            realName = "Довмонтов город (руины)",
            type = PoiType.RIFT,
            element = Element.MIST,
            latitude = 57.8199, longitude = 28.3308,
            minLevel = 15, maxLevel = 30,
            osmTags = mapOf("historic" to "ruins", "name" to "Довмонтов город")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 2 — МОНАСТЫРИ (SANCTUM / ABYSSAL_GATE)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_mirozhsky_monastery",
            name = "Мирожский монастырь",
            realName = "Спасо-Преображенский Мирожский монастырь",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8142, longitude = 28.3234,
            minLevel = 18, maxLevel = 36,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "historic" to "monastery")
        ),
        PointOfInterest(
            id = "pskov_snetogorsky_monastery",
            name = "Снетогорский монастырь",
            realName = "Снетогорский женский монастырь",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8023, longitude = 28.2974,
            minLevel = 16, maxLevel = 32,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "historic" to "monastery")
        ),
        PointOfInterest(
            id = "pskov_ioanno_predtechensky",
            name = "Иоанно-Предтеченский монастырь",
            realName = "Иоанно-Предтеченский монастырь с Завеличья",
            type = PoiType.SANCTUM,
            element = Element.MIST,
            latitude = 57.8172, longitude = 28.3162,
            minLevel = 14, maxLevel = 28,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "historic" to "monastery")
        ),
        PointOfInterest(
            id = "pskov_starovoznesensky_monastery",
            name = "Старовознесенский монастырь",
            realName = "Старовознесенский монастырь",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8258, longitude = 28.3312,
            minLevel = 10, maxLevel = 22,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "historic" to "monastery")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 3 — ЦЕРКВИ ЮНЕСКО И СТАРЕЙШИЕ (SANCTUM)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_church_vasily_gorka",
            name = "Церковь Василия на Горке",
            realName = "Церковь Василия Великого на Горке (ЮНЕСКО)",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8192, longitude = 28.3371,
            minLevel = 12, maxLevel = 24,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "heritage" to "unesco")
        ),
        PointOfInterest(
            id = "pskov_church_nikola_usokha",
            name = "Николы со Усохи",
            realName = "Церковь Николы со Усохи (ЮНЕСКО)",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8181, longitude = 28.3365,
            minLevel = 11, maxLevel = 22,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "heritage" to "unesco")
        ),
        PointOfInterest(
            id = "pskov_church_kozma_damian_gremyachaya",
            name = "Козьмы и Дамиана с Гремячей",
            realName = "Церковь Козьмы и Дамиана с Гремячей горы",
            type = PoiType.SANCTUM,
            element = Element.BLAZE,
            latitude = 57.8220, longitude = 28.3444,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_bogoyavleniya_zapskova",
            name = "Богоявления с Запсковья",
            realName = "Церковь Богоявления с Запсковья",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8230, longitude = 28.3464,
            minLevel = 9, maxLevel = 18,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_uspeniya_paromenya",
            name = "Успения с Пароменья",
            realName = "Церковь Успения Пресвятой Богородицы с Пароменья",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8165, longitude = 28.3200,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_anastasia_kuznetsakh",
            name = "Анастасии в Кузнецах",
            realName = "Церковь Анастасии Римлянки в Кузнецах",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8177, longitude = 28.3388,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_kozma_primostye",
            name = "Козьмы и Дамиана с Примостья",
            realName = "Церковь Козьмы и Дамиана с Примостья",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8160, longitude = 28.3252,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_mikhail_arkhangel",
            name = "Михаила Архангела с Городца",
            realName = "Церковь Михаила Архангела с Городца",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8210, longitude = 28.3340,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_varlaam",
            name = "Варлаама Хутынского",
            realName = "Церковь Варлаама Хутынского на Званице",
            type = PoiType.SANCTUM,
            element = Element.MIST,
            latitude = 57.8241, longitude = 28.3479,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_georgiy_vzvoz",
            name = "Георгия со Взвоза",
            realName = "Церковь Георгия со Взвоза (ЮНЕСКО)",
            type = PoiType.SANCTUM,
            element = Element.BLAZE,
            latitude = 57.8155, longitude = 28.3210,
            minLevel = 11, maxLevel = 22,
            osmTags = mapOf("amenity" to "place_of_worship", "heritage" to "unesco")
        ),
        PointOfInterest(
            id = "pskov_church_ioann_bogoslov_misharina",
            name = "Иоанна Богослова на Мишариной",
            realName = "Церковь Иоанна Богослова с Мишариной горы",
            type = PoiType.SANCTUM,
            element = Element.MIST,
            latitude = 57.8261, longitude = 28.3503,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_voskreseniya_stadishche",
            name = "Воскресения со Стадища",
            realName = "Церковь Воскресения Христова со Стадища",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8252, longitude = 28.3391,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_petra_pavla_bui",
            name = "Петра и Павла с Буя",
            realName = "Церковь Петра и Павла с Буя",
            type = PoiType.SANCTUM,
            element = Element.BLAZE,
            latitude = 57.8195, longitude = 28.3288,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_odygitrii",
            name = "Одигитрии",
            realName = "Церковь Одигитрии",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8188, longitude = 28.3350,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_pokrova_proloma",
            name = "Покрова от Пролома",
            realName = "Церковь Покрова и Рождества от Пролома",
            type = PoiType.SANCTUM,
            element = Element.ICE,
            latitude = 57.8225, longitude = 28.3468,
            minLevel = 9, maxLevel = 18,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_klimenta",
            name = "Климента Папы Римского",
            realName = "Церковь Климента Папы Римского",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8186, longitude = 28.3378,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_nikolay_torgu",
            name = "Николы от Торга",
            realName = "Церковь Николы от Торга",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8197, longitude = 28.3352,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_vvedeniya",
            name = "Введенская церковь",
            realName = "Введенская церковь",
            type = PoiType.SANCTUM,
            element = Element.BLOOM,
            latitude = 57.8231, longitude = 28.3320,
            minLevel = 5, maxLevel = 10,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_varvarinsk",
            name = "Варваринская церковь",
            realName = "Варваринская церковь с Петровского посада",
            type = PoiType.SANCTUM,
            element = Element.MIST,
            latitude = 57.8248, longitude = 28.3285,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_ilyinskaya",
            name = "Ильинская церковь",
            realName = "Ильинская церковь на Свином Волоке",
            type = PoiType.SANCTUM,
            element = Element.BLAZE,
            latitude = 57.8270, longitude = 28.3420,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_alexnevsky",
            name = "Церковь Александра Невского (76 дивизия)",
            realName = "Церковь Александра Невского при 76-й ДШД",
            type = PoiType.SANCTUM,
            element = Element.BLAZE,
            latitude = 57.8091, longitude = 28.3551,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian")
        ),
        PointOfInterest(
            id = "pskov_church_blagoveshcheniya",
            name = "Благовещенский собор",
            realName = "Благовещенский собор",
            type = PoiType.SANCTUM,
            element = Element.AETHER,
            latitude = 57.8204, longitude = 28.3310,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("amenity" to "place_of_worship", "religion" to "christian", "building" to "cathedral")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 4 — БАШНИ И УКРЕПЛЕНИЯ (CHAOS_SPIKE / RIFT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_tower_gremyachaya",
            name = "Гремячья башня",
            realName = "Гремячья башня (Космодемьянская)",
            type = PoiType.CHAOS_SPIKE,
            element = Element.BLAZE,
            latitude = 57.8219, longitude = 28.3452,
            minLevel = 14, maxLevel = 28,
            osmTags = mapOf("historic" to "tower", "man_made" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_pokrovskaya",
            name = "Покровская башня",
            realName = "Покровская башня (крупнейшая в России)",
            type = PoiType.CHAOS_SPIKE,
            element = Element.MIST,
            latitude = 57.8228, longitude = 28.3488,
            minLevel = 16, maxLevel = 32,
            osmTags = mapOf("historic" to "tower", "man_made" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_ploskaya",
            name = "Плоская башня",
            realName = "Плоская башня (угловая)",
            type = PoiType.RIFT,
            element = Element.ICE,
            latitude = 57.8236, longitude = 28.3458,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("historic" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_vysokaya",
            name = "Высокая башня",
            realName = "Высокая башня крепости",
            type = PoiType.RIFT,
            element = Element.BLAZE,
            latitude = 57.8245, longitude = 28.3420,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("historic" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_varlaamskaya",
            name = "Варлаамская башня",
            realName = "Варлаамская башня",
            type = PoiType.RIFT,
            element = Element.MIST,
            latitude = 57.8240, longitude = 28.3477,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("historic" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_vlasievskaya",
            name = "Власьевская башня",
            realName = "Власьевская башня",
            type = PoiType.RIFT,
            element = Element.ICE,
            latitude = 57.8196, longitude = 28.3355,
            minLevel = 9, maxLevel = 18,
            osmTags = mapOf("historic" to "tower")
        ),
        PointOfInterest(
            id = "pskov_tower_snetnaya",
            name = "Снетная башня",
            realName = "Башня Снетной горы",
            type = PoiType.CHAOS_SPIKE,
            element = Element.ICE,
            latitude = 57.8019, longitude = 28.2960,
            minLevel = 12, maxLevel = 24,
            osmTags = mapOf("historic" to "tower")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 5 — МУЗЕИ И КУЛЬТУРНЫЕ ОБЪЕКТЫ (NEXUS_POINT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_pogankin_chambers",
            name = "Поганкины Палаты",
            realName = "Поганкины палаты (Псковский музей-заповедник)",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8183, longitude = 28.3327,
            minLevel = 12, maxLevel = 25,
            osmTags = mapOf("tourism" to "museum", "historic" to "yes")
        ),
        PointOfInterest(
            id = "pskov_menshikov_chambers",
            name = "Меньшиковы Палаты",
            realName = "Палаты Меньшиковых (музей)",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8187, longitude = 28.3341,
            minLevel = 9, maxLevel = 18,
            osmTags = mapOf("tourism" to "museum", "historic" to "yes")
        ),
        PointOfInterest(
            id = "pskov_museum_dva_kapitana",
            name = "Музей Два Капитана",
            realName = "Музей «Два капитана»",
            type = PoiType.NEXUS_POINT,
            element = Element.ICE,
            latitude = 57.8191, longitude = 28.3356,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("tourism" to "museum")
        ),
        PointOfInterest(
            id = "pskov_museum_kuznetsy",
            name = "Кузнечный двор",
            realName = "Кузнечный двор (ремесленный музей)",
            type = PoiType.NEXUS_POINT,
            element = Element.BLAZE,
            latitude = 57.8179, longitude = 28.3382,
            minLevel = 5, maxLevel = 10,
            osmTags = mapOf("tourism" to "museum", "craft" to "blacksmith")
        ),
        PointOfInterest(
            id = "pskov_museum_pskov_history",
            name = "Краеведческий музей Пскова",
            realName = "Псковский государственный объединённый историко-архитектурный музей-заповедник",
            type = PoiType.NEXUS_POINT,
            element = Element.MIST,
            latitude = 57.8185, longitude = 28.3330,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("tourism" to "museum")
        ),
        PointOfInterest(
            id = "pskov_gallery_art",
            name = "Картинная галерея Пскова",
            realName = "Псковская областная картинная галерея",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8188, longitude = 28.3324,
            minLevel = 5, maxLevel = 10,
            osmTags = mapOf("tourism" to "museum", "artwork" to "yes")
        ),
        PointOfInterest(
            id = "pskov_museum_korolenko",
            name = "Дом-музей Короленко",
            realName = "Дом-музей писателя В. Г. Короленко",
            type = PoiType.NEXUS_POINT,
            element = Element.BLOOM,
            latitude = 57.8203, longitude = 28.3391,
            minLevel = 4, maxLevel = 8,
            osmTags = mapOf("tourism" to "museum", "historic" to "house")
        ),
        PointOfInterest(
            id = "pskov_museum_ww2",
            name = "Музей ВОВ",
            realName = "Псковский военно-исторический музей (ВОВ)",
            type = PoiType.NEXUS_POINT,
            element = Element.BLAZE,
            latitude = 57.8175, longitude = 28.3310,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("tourism" to "museum", "military" to "yes")
        ),
        PointOfInterest(
            id = "pskov_museum_rimsky_korsakov",
            name = "Музей-усадьба Р-Корсакова",
            realName = "Музей-усадьба Н. А. Римского-Корсакова (пригород)",
            type = PoiType.NEXUS_POINT,
            element = Element.BLOOM,
            latitude = 57.8220, longitude = 28.3500,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("tourism" to "museum", "historic" to "house")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 6 — ПАМЯТНИКИ И СКУЛЬПТУРЫ (CHAOS_SPIKE / RIFT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_monument_princess_olga",
            name = "Памятник Ольге",
            realName = "Памятник княгине Ольге (ул. Советская)",
            type = PoiType.CHAOS_SPIKE,
            element = Element.AETHER,
            latitude = 57.8194, longitude = 28.3353,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("tourism" to "artwork", "historic" to "monument")
        ),
        PointOfInterest(
            id = "pskov_monument_nevsky_sokolikha",
            name = "Памятник Невскому на Соколихе",
            realName = "Памятник Александру Невскому на горе Соколиха",
            type = PoiType.CHAOS_SPIKE,
            element = Element.BLAZE,
            latitude = 57.8468, longitude = 28.3121,
            minLevel = 12, maxLevel = 24,
            osmTags = mapOf("tourism" to "artwork", "historic" to "monument")
        ),
        PointOfInterest(
            id = "pskov_monument_6_rota",
            name = "Памятник 6-й Роте",
            realName = "Памятник десантникам 6-й роты (ВДВ)",
            type = PoiType.CHAOS_SPIKE,
            element = Element.BLAZE,
            latitude = 57.8079, longitude = 28.3368,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("tourism" to "artwork", "historic" to "memorial")
        ),
        PointOfInterest(
            id = "pskov_monument_knyaz_vsevolod",
            name = "Памятник князю Довмонту",
            realName = "Памятник псковскому князю Довмонту",
            type = PoiType.RIFT,
            element = Element.MIST,
            latitude = 57.8200, longitude = 28.3303,
            minLevel = 9, maxLevel = 18,
            osmTags = mapOf("tourism" to "artwork", "historic" to "monument")
        ),
        PointOfInterest(
            id = "pskov_monument_pushkin",
            name = "Памятник Пушкину",
            realName = "Памятник А. С. Пушкину (Октябрьская пл.)",
            type = PoiType.RIFT,
            element = Element.AETHER,
            latitude = 57.8192, longitude = 28.3388,
            minLevel = 5, maxLevel = 10,
            osmTags = mapOf("tourism" to "artwork")
        ),
        PointOfInterest(
            id = "pskov_monument_300_oborona",
            name = "Памятник 300-летию обороны",
            realName = "Памятник в честь 300-летия обороны Пскова 1581 года",
            type = PoiType.RIFT,
            element = Element.ICE,
            latitude = 57.8236, longitude = 28.3491,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("tourism" to "artwork", "historic" to "monument")
        ),
        PointOfInterest(
            id = "pskov_monument_memorial_vov",
            name = "Мемориал воинской славы",
            realName = "Мемориал Вечный огонь (пл. Победы)",
            type = PoiType.CHAOS_SPIKE,
            element = Element.BLAZE,
            latitude = 57.8197, longitude = 28.3438,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("historic" to "memorial", "memorial" to "war_memorial")
        ),
        PointOfInterest(
            id = "pskov_stele_goroda_slavy",
            name = "Стела «Город воинской славы»",
            realName = "Стела «Город воинской славы»",
            type = PoiType.CHAOS_SPIKE,
            element = Element.BLAZE,
            latitude = 57.8194, longitude = 28.3430,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("tourism" to "artwork", "historic" to "monument")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 7 — ПРИРОДА И РЕКИ (SACRED_GROVE / CHAOS_SPIKE)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_reka_velikaya_confluence",
            name = "Слияние Великой и Псковы",
            realName = "Место слияния рек Великой и Псковы",
            type = PoiType.SACRED_GROVE,
            element = Element.ICE,
            latitude = 57.8210, longitude = 28.3280,
            minLevel = 6, maxLevel = 12,
            osmTags = mapOf("natural" to "water", "waterway" to "river")
        ),
        PointOfInterest(
            id = "pskov_snetnaya_gora",
            name = "Снятная гора",
            realName = "Снятная гора (природный холм у Снетогорского монастыря)",
            type = PoiType.SACRED_GROVE,
            element = Element.ICE,
            latitude = 57.8028, longitude = 28.2968,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("natural" to "peak", "name" to "Снятная гора")
        ),
        PointOfInterest(
            id = "pskov_gremyachaya_gora",
            name = "Гремячья гора",
            realName = "Гремячья гора (исторический холм)",
            type = PoiType.SACRED_GROVE,
            element = Element.BLAZE,
            latitude = 57.8225, longitude = 28.3461,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("natural" to "peak")
        ),
        PointOfInterest(
            id = "pskov_park_kuopio",
            name = "Парк Куопио",
            realName = "Сквер Куопио (набережная Великой)",
            type = PoiType.SACRED_GROVE,
            element = Element.BLOOM,
            latitude = 57.8167, longitude = 28.3188,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("leisure" to "park")
        ),
        PointOfInterest(
            id = "pskov_botanical_garden",
            name = "Ботанический сад",
            realName = "Псковский ботанический сад",
            type = PoiType.SACRED_GROVE,
            element = Element.BLOOM,
            latitude = 57.8155, longitude = 28.3450,
            minLevel = 4, maxLevel = 8,
            osmTags = mapOf("leisure" to "garden", "tourism" to "attraction")
        ),
        PointOfInterest(
            id = "pskov_detsky_park",
            name = "Детский парк",
            realName = "Детский парк (ул. Советская)",
            type = PoiType.SACRED_GROVE,
            element = Element.BLOOM,
            latitude = 57.8202, longitude = 28.3412,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("leisure" to "park")
        ),
        PointOfInterest(
            id = "pskov_park_pobedy",
            name = "Парк Победы",
            realName = "Парк Победы (Псков)",
            type = PoiType.SACRED_GROVE,
            element = Element.BLOOM,
            latitude = 57.8185, longitude = 28.3442,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("leisure" to "park")
        ),
        PointOfInterest(
            id = "pskov_letny_sad",
            name = "Летний сад",
            realName = "Летний сад (набережная р. Великой)",
            type = PoiType.SACRED_GROVE,
            element = Element.BLOOM,
            latitude = 57.8170, longitude = 28.3210,
            minLevel = 3, maxLevel = 7,
            osmTags = mapOf("leisure" to "garden")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 8 — РЫНКИ, ТОРГОВЫЕ ПЛОЩАДИ (MERCHANT_CARAVAN)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_rynok_centralny",
            name = "Центральный рынок",
            realName = "Центральный рынок Пскова",
            type = PoiType.MERCHANT_CARAVAN,
            element = Element.AETHER,
            latitude = 57.8198, longitude = 28.3413,
            minLevel = 2, maxLevel = 8,
            osmTags = mapOf("shop" to "market", "amenity" to "marketplace")
        ),
        PointOfInterest(
            id = "pskov_oktyabrsky_prospekt_shopping",
            name = "Торговый ряд на Октябрьском",
            realName = "Октябрьский проспект (торговая ул.)",
            type = PoiType.MERCHANT_CARAVAN,
            element = Element.AETHER,
            latitude = 57.8192, longitude = 28.3374,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("shop" to "mall")
        ),
        PointOfInterest(
            id = "pskov_shop_pskovskie_suveniry",
            name = "Псковские сувениры",
            realName = "Магазин псковских сувениров и ремёсел",
            type = PoiType.MERCHANT_CARAVAN,
            element = Element.AETHER,
            latitude = 57.8189, longitude = 28.3341,
            minLevel = 2, maxLevel = 5,
            osmTags = mapOf("shop" to "gift")
        ),
        PointOfInterest(
            id = "pskov_shop_posadnik",
            name = "Торговый центр Посадник",
            realName = "ТЦ «Посадник»",
            type = PoiType.MERCHANT_CARAVAN,
            element = Element.AETHER,
            latitude = 57.8201, longitude = 28.3402,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("shop" to "mall")
        ),
        PointOfInterest(
            id = "pskov_kuznetsy_craft",
            name = "Ремесленные лавки Кузнецов",
            realName = "Ремесленные лавки у Кузнечного двора",
            type = PoiType.MERCHANT_CARAVAN,
            element = Element.BLAZE,
            latitude = 57.8178, longitude = 28.3385,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("shop" to "craft", "craft" to "blacksmith")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 9 — РЕСТОРАНЫ И ТАВЕРНЫ (TAVERN)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_tavern_posadnik",
            name = "Трактир Посадника",
            realName = "Ресторан «Посадник» (традиционная кухня)",
            type = PoiType.TAVERN,
            element = Element.BLOOM,
            latitude = 57.8199, longitude = 28.3346,
            minLevel = 1, maxLevel = 5,
            osmTags = mapOf("amenity" to "restaurant")
        ),
        PointOfInterest(
            id = "pskov_cafe_karavan",
            name = "Кафе Псковское",
            realName = "Кафе «Псковское»",
            type = PoiType.TAVERN,
            element = Element.BLOOM,
            latitude = 57.8195, longitude = 28.3396,
            minLevel = 1, maxLevel = 4,
            osmTags = mapOf("amenity" to "cafe")
        ),
        PointOfInterest(
            id = "pskov_bar_old_pskov",
            name = "Таверна Старый Псков",
            realName = "Бар «Старый Псков»",
            type = PoiType.TAVERN,
            element = Element.BLAZE,
            latitude = 57.8188, longitude = 28.3362,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("amenity" to "bar")
        ),
        PointOfInterest(
            id = "pskov_restoran_rechnoy",
            name = "Речной ресторан",
            realName = "Ресторан на набережной р. Великой",
            type = PoiType.TAVERN,
            element = Element.ICE,
            latitude = 57.8166, longitude = 28.3198,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("amenity" to "restaurant")
        ),
        PointOfInterest(
            id = "pskov_pub_vikingen",
            name = "Паб Викинг",
            realName = "Паб «Викинг»",
            type = PoiType.TAVERN,
            element = Element.ICE,
            latitude = 57.8205, longitude = 28.3406,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("amenity" to "pub")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 10 — БАНКИ И ХРАНИЛИЩА (GUILD_VAULT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_bank_sber_central",
            name = "Сбербанк (центральный)",
            realName = "Сбербанк, главное отделение",
            type = PoiType.GUILD_VAULT,
            element = Element.AETHER,
            latitude = 57.8193, longitude = 28.3388,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("amenity" to "bank")
        ),
        PointOfInterest(
            id = "pskov_bank_vtb",
            name = "ВТБ Банк",
            realName = "ВТБ Банк, отделение",
            type = PoiType.GUILD_VAULT,
            element = Element.ICE,
            latitude = 57.8200, longitude = 28.3400,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("amenity" to "bank")
        ),
        PointOfInterest(
            id = "pskov_bank_pskovoblbank",
            name = "Псковобласть банк",
            realName = "Псковский областной банк",
            type = PoiType.GUILD_VAULT,
            element = Element.AETHER,
            latitude = 57.8188, longitude = 28.3370,
            minLevel = 4, maxLevel = 9,
            osmTags = mapOf("amenity" to "bank")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 11 — ПЛОЩАДИ И УЗЛОВЫЕ ТОЧКИ (NEXUS_POINT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_ploshad_lenina",
            name = "Площадь Ленина",
            realName = "Площадь Ленина (центральная площадь)",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8193, longitude = 28.3387,
            minLevel = 1, maxLevel = 10,
            osmTags = mapOf("place" to "square", "name" to "Площадь Ленина")
        ),
        PointOfInterest(
            id = "pskov_ploshad_pobedy",
            name = "Площадь Победы",
            realName = "Площадь Победы",
            type = PoiType.NEXUS_POINT,
            element = Element.BLAZE,
            latitude = 57.8196, longitude = 28.3432,
            minLevel = 2, maxLevel = 8,
            osmTags = mapOf("place" to "square")
        ),
        PointOfInterest(
            id = "pskov_ploshad_mira",
            name = "Площадь Мира",
            realName = "Площадь Мира",
            type = PoiType.NEXUS_POINT,
            element = Element.BLOOM,
            latitude = 57.8209, longitude = 28.3358,
            minLevel = 2, maxLevel = 8,
            osmTags = mapOf("place" to "square")
        ),
        PointOfInterest(
            id = "pskov_vokzal",
            name = "Псковский вокзал",
            realName = "Железнодорожный вокзал Псков",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8129, longitude = 28.3576,
            minLevel = 3, maxLevel = 10,
            osmTags = mapOf("railway" to "station", "name" to "Псков")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 12 — ИСТОРИЧЕСКИЕ ЗДАНИЯ И РУИНЫ (RIFT / ABYSSAL_GATE)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_yamskoy_dvor",
            name = "Ямской двор",
            realName = "Ямской двор (палаты XVII в.)",
            type = PoiType.RIFT,
            element = Element.MIST,
            latitude = 57.8193, longitude = 28.3362,
            minLevel = 8, maxLevel = 16,
            osmTags = mapOf("historic" to "yes", "building" to "historic")
        ),
        PointOfInterest(
            id = "pskov_dom_pechenka",
            name = "Дом Печенко",
            realName = "Жилой дом Печенко XVII века",
            type = PoiType.RIFT,
            element = Element.MIST,
            latitude = 57.8182, longitude = 28.3336,
            minLevel = 7, maxLevel = 14,
            osmTags = mapOf("historic" to "yes", "building" to "historic")
        ),
        PointOfInterest(
            id = "pskov_ruiny_steny_sredнего",
            name = "Руины Среднего города",
            realName = "Руины крепостной стены Среднего города (XIV в.)",
            type = PoiType.ABYSSAL_GATE,
            element = Element.MIST,
            latitude = 57.8235, longitude = 28.3330,
            minLevel = 14, maxLevel = 28,
            osmTags = mapOf("historic" to "ruins")
        ),
        PointOfInterest(
            id = "pskov_ruiny_okol_stena",
            name = "Руины Окольного города",
            realName = "Фрагменты стены Окольного города (XV в.)",
            type = PoiType.ABYSSAL_GATE,
            element = Element.MIST,
            latitude = 57.8250, longitude = 28.3450,
            minLevel = 12, maxLevel = 24,
            osmTags = mapOf("historic" to "ruins")
        ),
        PointOfInterest(
            id = "pskov_ruiny_mirozhsky_naberezhnaya",
            name = "Руины у Мирожи",
            realName = "Руины у Мирожского монастыря (берег Великой)",
            type = PoiType.RIFT,
            element = Element.BLOOM,
            latitude = 57.8138, longitude = 28.3225,
            minLevel = 10, maxLevel = 20,
            osmTags = mapOf("historic" to "ruins")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 13 — СЛУЧАЙНЫЕ ВСТРЕЧИ У МОСТОВ И ПУСТЫРЕЙ (RANDOM_ENCOUNTER)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_most_olginskiy",
            name = "Ольгинский мост",
            realName = "Ольгинский мост через р. Великую",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.ICE,
            latitude = 57.8163, longitude = 28.3200,
            minLevel = 4, maxLevel = 10,
            osmTags = mapOf("man_made" to "bridge")
        ),
        PointOfInterest(
            id = "pskov_most_sovetskiy",
            name = "Советский мост",
            realName = "Советский (Троицкий) мост",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.ICE,
            latitude = 57.8199, longitude = 28.3274,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("man_made" to "bridge")
        ),
        PointOfInterest(
            id = "pskov_most_vysokiy",
            name = "Высокий мост",
            realName = "Высокий мост через реку Пскову",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.ICE,
            latitude = 57.8228, longitude = 28.3340,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("man_made" to "bridge")
        ),
        PointOfInterest(
            id = "pskov_naberezhnaya_velikoy",
            name = "Набережная Великой",
            realName = "Набережная реки Великой",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.ICE,
            latitude = 57.8172, longitude = 28.3220,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("leisure" to "promenade")
        ),
        PointOfInterest(
            id = "pskov_zapskove_district",
            name = "Запсковье (квартал)",
            realName = "Исторический квартал Запсковье",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.MIST,
            latitude = 57.8246, longitude = 28.3490,
            minLevel = 4, maxLevel = 10,
            osmTags = mapOf("place" to "neighbourhood")
        ),
        PointOfInterest(
            id = "pskov_zavelchye_district",
            name = "Завеличье",
            realName = "Исторический квартал Завеличье",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.BLOOM,
            latitude = 57.8155, longitude = 28.3145,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("place" to "neighbourhood")
        ),
        PointOfInterest(
            id = "pskov_elektrostanciya_retro",
            name = "Старая электростанция",
            realName = "Здание старой электростанции (XIX в.)",
            type = PoiType.RANDOM_ENCOUNTER,
            element = Element.BLAZE,
            latitude = 57.8210, longitude = 28.3441,
            minLevel = 5, maxLevel = 12,
            osmTags = mapOf("historic" to "industrial")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 14 — ГОСТИНИЦЫ КАК ХАБЫ ГИЛЬДИИ (GUILD_VAULT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_hotel_oktyabrskaya",
            name = "Гостиница Октябрьская",
            realName = "Гостиница «Октябрьская»",
            type = PoiType.GUILD_VAULT,
            element = Element.AETHER,
            latitude = 57.8195, longitude = 28.3393,
            minLevel = 2, maxLevel = 8,
            osmTags = mapOf("tourism" to "hotel")
        ),
        PointOfInterest(
            id = "pskov_hotel_rizhskaya",
            name = "Гостиница Рижская",
            realName = "Гостиница «Рижская»",
            type = PoiType.GUILD_VAULT,
            element = Element.ICE,
            latitude = 57.8202, longitude = 28.3460,
            minLevel = 2, maxLevel = 8,
            osmTags = mapOf("tourism" to "hotel")
        ),
        PointOfInterest(
            id = "pskov_hostel_krom",
            name = "Хостел у Крома",
            realName = "Хостел рядом с Кремлём",
            type = PoiType.GUILD_VAULT,
            element = Element.AETHER,
            latitude = 57.8204, longitude = 28.3316,
            minLevel = 1, maxLevel = 5,
            osmTags = mapOf("tourism" to "hostel")
        ),

        // ══════════════════════════════════════════════════════
        // БЛОК 15 — УЧЕБНЫЕ И КУЛЬТУРНЫЕ ЦЕНТРЫ (NEXUS_POINT)
        // ══════════════════════════════════════════════════════

        PointOfInterest(
            id = "pskov_psgu_university",
            name = "Псковский госуниверситет",
            realName = "Псковский государственный университет",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8218, longitude = 28.3440,
            minLevel = 3, maxLevel = 10,
            osmTags = mapOf("grid" to "education", "amenity" to "university")
        ),
        PointOfInterest(
            id = "pskov_library_central",
            name = "Центральная библиотека",
            realName = "Псковская областная универсальная библиотека",
            type = PoiType.NEXUS_POINT,
            element = Element.MIST,
            latitude = 57.8208, longitude = 28.3395,
            minLevel = 3, maxLevel = 8,
            osmTags = mapOf("amenity" to "library")
        ),
        PointOfInterest(
            id = "pskov_teatr_pushkina",
            name = "Псковский драматический театр",
            realName = "Псковский академический театр драмы им. Пушкина",
            type = PoiType.NEXUS_POINT,
            element = Element.MIST,
            latitude = 57.8210, longitude = 28.3401,
            minLevel = 4, maxLevel = 10,
            osmTags = mapOf("amenity" to "theatre")
        ),
        PointOfInterest(
            id = "pskov_filarmonia",
            name = "Псковская филармония",
            realName = "Псковская областная филармония",
            type = PoiType.NEXUS_POINT,
            element = Element.AETHER,
            latitude = 57.8205, longitude = 28.3375,
            minLevel = 4, maxLevel = 10,
            osmTags = mapOf("amenity" to "theatre")
        ),
        PointOfInterest(
            id = "pskov_kino_pobeda",
            name = "Кинотеатр Победа",
            realName = "Кинотеатр «Победа»",
            type = PoiType.TAVERN,
            element = Element.BLOOM,
            latitude = 57.8198, longitude = 28.3418,
            minLevel = 2, maxLevel = 6,
            osmTags = mapOf("amenity" to "cinema")
        )
    )
}
