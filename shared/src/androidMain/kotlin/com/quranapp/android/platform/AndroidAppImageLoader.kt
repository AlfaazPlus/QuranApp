package com.quranapp.android.platform

import android.content.Context
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.quranapp.platform.AppImageLoader
import com.quranapp.platform.AppImageRequest
import com.quranapp.platform.AppImageRequestBuilder
import com.quranapp.platform.CoilAppImageLoader
import okio.Path

fun createAppImageLoader(
    context: Context,
): AppImageLoader {
    return CoilAppImageLoader(
        loaderFactory = {
            ImageLoader.Builder(context).build()
        },
        requestBuilder = AndroidAppImageRequestBuilder(context),
    )
}

class AndroidAppImageRequestBuilder(
    private val context: Context,
) : AppImageRequestBuilder {

    override fun build(
        request: AppImageRequest,
    ): ImageRequest {
        val policy = if (request.cache) {
            CachePolicy.ENABLED
        } else {
            CachePolicy.DISABLED
        }

        val data = when (val source = request.data) {
            is Path -> source.toFile()
            else -> source
        }

        return ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (request.width != null && request.height != null) {
                    size(request.width, request.height)
                }

                allowHardware(request.allowHardware)

                memoryCachePolicy(policy)
                diskCachePolicy(policy)
            }
            .build()
    }
}
