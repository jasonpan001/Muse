/*
 * Copyright (c) 2019 Naman Dwivedi.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package com.naman14.timberx.db


import org.koin.dsl.module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf

val roomModule = module {
    single {
        Room.databaseBuilder(get<Application>(), TimberDatabase::class.java, "queue.db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    factory<QueueDao> { get<TimberDatabase>().queueDao() }

    // 用 builder 写法
    factoryOf(::RealQueueHelper) { bind<QueueHelper>() }
    // 或者 singleOf(::RealQueueHelper) { bind<QueueHelper>() }
}