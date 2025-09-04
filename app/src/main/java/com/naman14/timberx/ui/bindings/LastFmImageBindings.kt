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
package com.naman14.timberx.ui.bindings

import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.view.View
import android.widget.ImageView
import androidx.annotation.DimenRes
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.naman14.timberx.R
import com.naman14.timberx.extensions.disposeOnDetach
import com.naman14.timberx.extensions.ioToMain
import com.naman14.timberx.extensions.subscribeForOutcome
import com.naman14.timberx.network.Outcome
import com.naman14.timberx.network.api.LastFmRestService
import com.naman14.timberx.network.models.ArtworkSize
import com.naman14.timberx.network.models.ArtworkSize.MEGA
import com.naman14.timberx.network.models.ofSize
import com.naman14.timberx.ui.bindings.EXTRA_LARGE_IMAGE_ROUND_CORNERS_TRANSFORMER
import com.naman14.timberx.ui.bindings.LARGE_IMAGE_ROUND_CORNERS_TRANSFORMER
import com.naman14.timberx.util.Utils.getAlbumArtUri
import org.koin.core.context.GlobalContext
import timber.log.Timber

// Matches keys in preferences.xml
private const val LASTFM_ARTIST_IMAGE = "lastfm_artist_image"
private const val LASTFM_ALBUM_IMAGE = "lastfm_album_image"

data class CacheKey(
    val artist: String,
    val album: String = "",
    val size: ArtworkSize
)

val imageUrlCache = mutableMapOf<CacheKey, String>()
const val CROSS_FADE_DIRATION = 400

@BindingAdapter("artistName", "artworkSize", requireAll = true)
fun setLastFmArtistImage(
    view: ImageView,
    artistName: String?,
    artworkSize: ArtworkSize
) {
    if (artistName == null) return

    if (view.useLastFmArtistImages()) {
        Timber.d("""setLastFmArtistImage("$artistName", ${artworkSize.apiValue})""")
        val cacheKey = CacheKey(artistName, "", artworkSize)
        val cachedUrl = imageUrlCache[cacheKey]
        val resizeTo =
                view.px(if (artworkSize == MEGA) R.dimen.album_art_mega else R.dimen.album_art_large)
        val transformation = artworkSize.transformation()
        val options = RequestOptions()
                .centerCrop()
                .override(resizeTo, resizeTo)
                .transform(transformation)

        if (cachedUrl != null) {
            Glide.with(view)
                    .load(cachedUrl)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DIRATION))
                    .into(view)
            return
        }

        fetchArtistImage(view, artistName, artworkSize, callback = { url ->
            if (url.isEmpty()) return@fetchArtistImage
            Glide.with(view)
                    .load(url)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DIRATION))
                    .into(view)
        })
    }
}

@BindingAdapter("albumArtist", "albumName", "artworkSize", "albumId", requireAll = true)
fun setLastFmAlbumImage(
    view: ImageView,
    albumArtist: String?,
    albumName: String?,
    artworkSize: ArtworkSize,
    albumId: Long?
) {

    if (albumArtist == null || albumName == null || albumId == null) return

    // First try to load local album art, if it fails then try Last.fm
    Glide.with(view)
        .load(getAlbumArtUri(albumId))
        .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DIRATION))
        .into(view)
        
    // Also try to get Last.fm image if enabled
    if (view.useLastFmAlbumImages()) {
        fetchAlbumImage(view, albumArtist, albumName, artworkSize) { url ->
            if (url.isNotEmpty()) {
                val resizeTo = view.px(if (artworkSize == MEGA) R.dimen.album_art_mega else R.dimen.album_art_large)
                val transformation = artworkSize.transformation()
                val options = RequestOptions()
                    .centerCrop()
                    .override(resizeTo, resizeTo)
                    .transform(transformation)
                
                Glide.with(view)
                    .load(url)
                    .apply(options)
                    .transition(DrawableTransitionOptions.withCrossFade(CROSS_FADE_DIRATION))
                    .into(view)
            }
        }
    }
}

private fun fetchArtistImage(
    view: View,
    artistName: String,
    artworkSize: ArtworkSize,
    callback: (url: String) -> Unit
) {
    val lastFmService = GlobalContext.get().get<LastFmRestService>()
    lastFmService.getArtistInfo(artistName)
            .ioToMain()
            .subscribeForOutcome { outcome ->
                Timber.d("""getArtistInfo("$artistName") outcome: $outcome""")
                when (outcome) {
                    is Outcome.Success -> {
                        val artistResult = outcome.data.artist ?: return@subscribeForOutcome
                        val url = artistResult.artwork.ofSize(artworkSize).url
                        val cacheKey = CacheKey(artistName, "", artworkSize)
                        imageUrlCache[cacheKey] = url
                        Timber.d("""getArtistInfo("$artistName") image URL: $url""")
                        callback(url)
                    }
                    is Outcome.Failure -> {
                        Timber.e(outcome.e, "Failed to fetch artist image for $artistName")
                    }
                    is Outcome.ApiError -> {
                        Timber.e(outcome.e, "API error fetching artist image for $artistName")
                    }
                }
            }
            .disposeOnDetach(view)
}

private fun fetchAlbumImage(
    view: View,
    artistName: String,
    albumName: String,
    artworkSize: ArtworkSize,
    callback: (url: String) -> Unit
) {
    val lastFmService = GlobalContext.get().get<LastFmRestService>()
    lastFmService.getAlbumInfo(artistName, albumName)
            .ioToMain()
            .subscribeForOutcome { outcome ->
                Timber.d("""getAlbumInfo("$albumName") outcome: $outcome""")
                when (outcome) {
                    is Outcome.Success -> {
                        val albumResult = outcome.data.album ?: return@subscribeForOutcome
                        val url = albumResult.artwork.ofSize(artworkSize).url
                        val cacheKey = CacheKey(artistName, albumName, artworkSize)
                        imageUrlCache[cacheKey] = url
                        Timber.d("""getAlbumInfo("$albumName") image URL: $url""")
                        callback(url)
                    }
                    is Outcome.Failure -> {
                        Timber.e(outcome.e, "Failed to fetch album image for $albumName")
                    }
                    is Outcome.ApiError -> {
                        Timber.e(outcome.e, "API error fetching album image for $albumName")
                    }
                }
            }
            .disposeOnDetach(view)
}

private fun ArtworkSize.transformation() = if (this == MEGA) {
    EXTRA_LARGE_IMAGE_ROUND_CORNERS_TRANSFORMER
} else {
    LARGE_IMAGE_ROUND_CORNERS_TRANSFORMER
}

private fun View.px(@DimenRes dimen: Int) = resources.getDimensionPixelSize(dimen)

private fun View.useLastFmAlbumImages(): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(LASTFM_ALBUM_IMAGE, true)
}

private fun View.useLastFmArtistImages(): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(LASTFM_ARTIST_IMAGE, true)
}
