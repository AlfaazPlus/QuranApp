package com.quranapp.platform

import coil3.Image
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.CancellationException


data class AppImageRequest(
    val data: Any,
    val width: Int? = null,
    val height: Int? = null,
    val cache: Boolean = true,
    val allowHardware: Boolean = true,
    val retryOnOutOfMemory: Boolean = false,
) {
    init {
        require((width == null) == (height == null)) {
            "Width and height must either both be specified or both be null."
        }

        require(width == null || width > 0)
        require(height == null || height > 0)
    }
}

interface AppImageLoader {
    suspend fun load(
        request: AppImageRequest,
    ): Image?

    fun clear()
}

fun interface AppImageRequestBuilder {
    fun build(
        request: AppImageRequest,
    ): ImageRequest
}


class CoilAppImageLoader(
    private val loaderFactory: () -> ImageLoader,
    private val requestBuilder: AppImageRequestBuilder,
) : AppImageLoader {

    private val loaderDelegate = lazy(
        LazyThreadSafetyMode.SYNCHRONIZED,
        loaderFactory,
    )

    private fun loader(): ImageLoader = loaderDelegate.value

    override suspend fun load(
        request: AppImageRequest,
    ): Image? {
        return execute(
            request = request,
            allowRetry = request.retryOnOutOfMemory,
        )
    }

    private suspend fun execute(
        request: AppImageRequest,
        allowRetry: Boolean,
    ): Image? {
        return try {
            val imageRequest = requestBuilder.build(request)

            when (val result = loader().execute(imageRequest)) {
                is SuccessResult -> result.image

                is ErrorResult -> {
                    if (
                        allowRetry &&
                        result.throwable.isOutOfMemory()
                    ) {
                        retry(request)
                    } else {
                        null
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (oom: OutOfMemoryError) {
            if (allowRetry) retry(request) else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun retry(
        request: AppImageRequest,
    ): Image? {
        val width = request.width ?: return null
        val height = request.height ?: return null

        if (width <= 1 && height <= 1) return null

        return execute(
            request = request.copy(
                width = (width / 2).coerceAtLeast(1),
                height = (height / 2).coerceAtLeast(1),
                retryOnOutOfMemory = false,
            ),
            allowRetry = false,
        )
    }

    override fun clear() {
        if (loaderDelegate.isInitialized()) {
            loader().memoryCache?.clear()
        }
    }

    private fun Throwable.isOutOfMemory(): Boolean {
        var current: Throwable? = this

        repeat(8) {
            if (current is OutOfMemoryError) return true
            current = current?.cause
        }

        return false
    }
}
