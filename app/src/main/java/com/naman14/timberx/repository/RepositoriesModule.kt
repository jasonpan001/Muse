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
package com.naman14.timberx.repository

import com.naman14.timberx.PREF_ALBUM_SORT_ORDER
import com.naman14.timberx.PREF_SONG_SORT_ORDER
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.module.dsl.bind

val repositoriesModule = module {
    factory<SongsRepository> { RealSongsRepository(get(), get(qualifier = named(PREF_SONG_SORT_ORDER))) }
    factory<AlbumRepository> { RealAlbumRepository(get(), get(qualifier = named(PREF_ALBUM_SORT_ORDER))) }
    factory<ArtistRepository> { RealArtistRepository(get()) }
    factory<GenreRepository> { RealGenreRepository(get()) }
    factory<PlaylistRepository> { RealPlaylistRepository(get()) }
    factory<FoldersRepository> { RealFoldersRepository() }
}