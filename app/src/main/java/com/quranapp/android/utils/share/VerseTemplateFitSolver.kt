package com.quranapp.android.utils.share

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class VerseFitStage {
    IDEAL,
    SCALED,
    TIGHTENED,
    SQUEEZED,
    REJECTED;

    val fits: Boolean
        get() = this != REJECTED
}

enum class FlowBlockKind {
    ARABIC,
    TRANSLATION,
    REFERENCE;

    val isBodyBlock: Boolean
        get() = this == ARABIC || this == TRANSLATION
}

data class ScriptFactors(
    val sizeFactor: Float = 1f,
    val lineHeightFactor: Float = 1f,
)

fun interface VerseBlockMeasurer {
    fun measureHeight(
        kind: FlowBlockKind,
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        maxWidthPx: Float,
    ): Float
}

fun interface FreeReferenceMeasurer {
    fun fitsInBox(
        fontSizePx: Float,
        lineHeightMultiplier: Float,
        boxWidthPx: Float,
        boxHeightPx: Float,
    ): Boolean
}

data class SolvedFlowBlock(
    val kind: FlowBlockKind,
    val fontSizePx: Float,
    val lineHeightMultiplier: Float,
    val gapBeforePx: Float,
    val measuredHeightPx: Float,
)

data class SolvedFreeReference(
    val boxLeftPx: Float,
    val boxTopPx: Float,
    val boxWidthPx: Float,
    val boxHeightPx: Float,
    val fontSizePx: Float,
    val lineHeightMultiplier: Float,
)

data class VerseFitResult(
    val stage: VerseFitStage,
    val scaleRatio: Float,
    val fillRatio: Float,
    val safeLeftPx: Float,
    val safeTopPx: Float,
    val safeWidthPx: Float,
    val safeHeightPx: Float,
    val totalFlowHeightPx: Float,
    val flowBlocks: List<SolvedFlowBlock>,
    val freeReference: SolvedFreeReference?,
)

private class FlowBlockSpec(
    val kind: FlowBlockKind,
    val floorSizePx: Float,
    val prefSizePx: Float,
    val floorLineHeight: Float,
    val prefLineHeight: Float,
    val gapMinPx: Float,
    val gapMaxPx: Float,
)

object VerseTemplateFitSolver {
    const val REF_PREF_LINE_HEIGHT = 1.25f
    const val REF_FLOOR_LINE_HEIGHT = 1.15f
    const val FREE_REF_LINE_HEIGHT = 1.25f

    private const val BINARY_SEARCH_STEPS = 16
    private const val TIGHTEN_GAP_MIN_FACTOR = 0.55f
    private const val SQUEEZE_GAP_MIN_FACTOR = 0.40f
    private const val SQUEEZE_SIZE_FACTOR = 0.85f
    private const val DOMINANT_STACK_SHARE = 0.42f

    fun solve(
        layout: VerseTemplateLayout,
        canvasWidth: Float,
        canvasHeight: Float,
        hasArabic: Boolean,
        hasTranslation: Boolean,
        hasReference: Boolean,
        scriptFactors: ScriptFactors = ScriptFactors(),
        flowMeasurer: VerseBlockMeasurer,
        freeRefMeasurer: FreeReferenceMeasurer? = null,
    ): VerseFitResult {
        val clampedScale = 1f
        val eps = max(0.5f, canvasHeight * 0.0005f)

        val leftFrac = layout.safeArea.left.coerceAtLeast(0f)
        val rightFrac = layout.safeArea.right.coerceAtLeast(0f)
        val topFrac = layout.safeArea.top.coerceAtLeast(0f)
        val bottomFrac = layout.safeArea.bottom.coerceAtLeast(0f)

        val safeW = max(1f, canvasWidth * max(0.05f, 1f - leftFrac - rightFrac))
        val safeH = max(1f, canvasHeight * max(0.05f, 1f - topFrac - bottomFrac))
        val safeLeft = (leftFrac * canvasWidth).coerceIn(0f, max(0f, canvasWidth - safeW))
        val safeTop = (topFrac * canvasHeight).coerceIn(0f, max(0f, canvasHeight - safeH))

        val specs = buildFlowSpecs(
            layout = layout,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            hasArabic = hasArabic,
            hasTranslation = hasTranslation,
            hasFlowReference = hasReference && layout.reference.mode == ReferenceMode.FLOW,
            userScale = clampedScale,
            scriptFactors = scriptFactors,
        )

        val freeRef =
            if (hasReference && layout.reference.mode == ReferenceMode.FREE && freeRefMeasurer != null) {
                solveFreeReference(
                    ref = layout.reference,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    userScale = clampedScale,
                    measurer = freeRefMeasurer,
                )
            } else {
                null
            }

        if (specs.isEmpty()) {
            return VerseFitResult(
                stage = VerseFitStage.IDEAL,
                scaleRatio = 1f,
                fillRatio = 0f,
                safeLeftPx = safeLeft,
                safeTopPx = safeTop,
                safeWidthPx = safeW,
                safeHeightPx = safeH,
                totalFlowHeightPx = 0f,
                flowBlocks = emptyList(),
                freeReference = freeRef,
            )
        }

        val count = specs.size
        val sizes = FloatArray(count)
        val lineHeights = FloatArray(count)
        val gaps = FloatArray(count)
        val heights = FloatArray(count)

        fun evaluateCurrent(): Float {
            var total = 0f
            for (i in 0 until count) {
                val h = flowMeasurer.measureHeight(
                    kind = specs[i].kind,
                    fontSizePx = sizes[i],
                    lineHeightMultiplier = lineHeights[i],
                    maxWidthPx = safeW,
                )
                heights[i] = h
                total += gaps[i] + h
            }
            return total
        }

        fun applyScaleStage(t: Float) {
            for (i in 0 until count) {
                val s = specs[i]
                sizes[i] = lerp(s.floorSizePx, s.prefSizePx, t)
                lineHeights[i] = s.prefLineHeight
                gaps[i] = lerp(s.gapMinPx, s.gapMaxPx, t)
            }
        }

        fun applyTightenStage(u: Float) {
            for (i in 0 until count) {
                val s = specs[i]
                sizes[i] = s.floorSizePx
                lineHeights[i] = lerp(s.floorLineHeight, s.prefLineHeight, u)
                gaps[i] = lerp(TIGHTEN_GAP_MIN_FACTOR * s.gapMinPx, s.gapMinPx, u)
            }
        }

        fun buildSolvedList(): List<SolvedFlowBlock> {
            return List(count) { i ->
                SolvedFlowBlock(
                    kind = specs[i].kind,
                    fontSizePx = sizes[i],
                    lineHeightMultiplier = lineHeights[i],
                    gapBeforePx = gaps[i],
                    measuredHeightPx = heights[i],
                )
            }
        }

        // Stage 1: ideal (t = 1)
        applyScaleStage(1f)
        val idealHeight = evaluateCurrent()
        if (idealHeight <= safeH + eps) {
            return VerseFitResult(
                stage = VerseFitStage.IDEAL,
                scaleRatio = 1f,
                fillRatio = idealHeight / safeH,
                safeLeftPx = safeLeft,
                safeTopPx = safeTop,
                safeWidthPx = safeW,
                safeHeightPx = safeH,
                totalFlowHeightPx = idealHeight,
                flowBlocks = buildSolvedList(),
                freeReference = freeRef,
            )
        }

        // Stage 2: scaled (t in [0, 1])
        applyScaleStage(0f)
        val floorScaleHeight = evaluateCurrent()
        if (floorScaleHeight <= safeH + eps) {
            var lo = 0f
            var hi = 1f
            var bestT = 0f

            repeat(BINARY_SEARCH_STEPS) {
                val mid = (lo + hi) * 0.5f
                applyScaleStage(mid)
                if (evaluateCurrent() <= safeH + eps) {
                    bestT = mid
                    lo = mid
                } else {
                    hi = mid
                }
            }

            applyScaleStage(bestT)
            val finalH = evaluateCurrent()
            val ratio = (sizes[0] / specs[0].prefSizePx).coerceIn(0f, 1f)

            return VerseFitResult(
                stage = VerseFitStage.SCALED,
                scaleRatio = ratio,
                fillRatio = finalH / safeH,
                safeLeftPx = safeLeft,
                safeTopPx = safeTop,
                safeWidthPx = safeW,
                safeHeightPx = safeH,
                totalFlowHeightPx = finalH,
                flowBlocks = buildSolvedList(),
                freeReference = freeRef,
            )
        }

        // Stage 3: tightened (u in [0, 1], sizes at floor, lineHeights -> minLineHeight)
        applyTightenStage(0f)
        val maxTightenHeight = evaluateCurrent()
        if (maxTightenHeight <= safeH + eps) {
            var lo = 0f
            var hi = 1f
            var bestU = 0f

            repeat(BINARY_SEARCH_STEPS) {
                val mid = (lo + hi) * 0.5f
                applyTightenStage(mid)
                if (evaluateCurrent() <= safeH + eps) {
                    bestU = mid
                    lo = mid
                } else {
                    hi = mid
                }
            }

            applyTightenStage(bestU)
            val finalH = evaluateCurrent()
            val ratio = (specs[0].floorSizePx / specs[0].prefSizePx).coerceIn(0f, 1f)

            return VerseFitResult(
                stage = VerseFitStage.TIGHTENED,
                scaleRatio = ratio,
                fillRatio = finalH / safeH,
                safeLeftPx = safeLeft,
                safeTopPx = safeTop,
                safeWidthPx = safeW,
                safeHeightPx = safeH,
                totalFlowHeightPx = finalH,
                flowBlocks = buildSolvedList(),
                freeReference = freeRef,
            )
        }

        // Determine dominant body blocks from max-tighten floor heights
        val dominantMask = BooleanArray(count)
        var anyDominant = false
        if (maxTightenHeight > 0f) {
            for (i in 0 until count) {
                if (specs[i].kind.isBodyBlock && heights[i] >= DOMINANT_STACK_SHARE * maxTightenHeight) {
                    dominantMask[i] = true
                    anyDominant = true
                }
            }
        }
        if (!anyDominant) {
            val firstBodyIdx = specs.indexOfFirst { it.kind.isBodyBlock }
            if (firstBodyIdx >= 0) {
                dominantMask[firstBodyIdx] = true
            }
        }

        fun applySqueezeStage(s: Float) {
            for (i in 0 until count) {
                val spec = specs[i]
                sizes[i] = if (dominantMask[i]) {
                    lerp(SQUEEZE_SIZE_FACTOR * spec.floorSizePx, spec.floorSizePx, s)
                } else {
                    spec.floorSizePx
                }
                lineHeights[i] = spec.floorLineHeight
                gaps[i] = lerp(
                    SQUEEZE_GAP_MIN_FACTOR * spec.gapMinPx,
                    TIGHTEN_GAP_MIN_FACTOR * spec.gapMinPx,
                    s,
                )
            }
        }

        // Stage 4: squeezed (s in [0, 1])
        applySqueezeStage(0f)
        val maxSqueezeHeight = evaluateCurrent()
        if (maxSqueezeHeight <= safeH + eps) {
            var lo = 0f
            var hi = 1f
            var bestS = 0f

            repeat(BINARY_SEARCH_STEPS) {
                val mid = (lo + hi) * 0.5f
                applySqueezeStage(mid)
                if (evaluateCurrent() <= safeH + eps) {
                    bestS = mid
                    lo = mid
                } else {
                    hi = mid
                }
            }

            applySqueezeStage(bestS)
            val finalH = evaluateCurrent()
            val floorRatio = specs[0].floorSizePx / specs[0].prefSizePx
            val squeezeFactor = lerp(SQUEEZE_SIZE_FACTOR, 1f, bestS)

            return VerseFitResult(
                stage = VerseFitStage.SQUEEZED,
                scaleRatio = (floorRatio * squeezeFactor).coerceIn(0f, 1f),
                fillRatio = finalH / safeH,
                safeLeftPx = safeLeft,
                safeTopPx = safeTop,
                safeWidthPx = safeW,
                safeHeightPx = safeH,
                totalFlowHeightPx = finalH,
                flowBlocks = buildSolvedList(),
                freeReference = freeRef,
            )
        }

        // Stage 5: rejected (retain max squeeze layout for preview diagnostics)
        val floorRatio = specs[0].floorSizePx / specs[0].prefSizePx
        return VerseFitResult(
            stage = VerseFitStage.REJECTED,
            scaleRatio = (floorRatio * SQUEEZE_SIZE_FACTOR).coerceIn(0f, 1f),
            fillRatio = maxSqueezeHeight / safeH,
            safeLeftPx = safeLeft,
            safeTopPx = safeTop,
            safeWidthPx = safeW,
            safeHeightPx = safeH,
            totalFlowHeightPx = maxSqueezeHeight,
            flowBlocks = buildSolvedList(),
            freeReference = freeRef,
        )
    }

    private fun buildFlowSpecs(
        layout: VerseTemplateLayout,
        canvasWidth: Float,
        canvasHeight: Float,
        hasArabic: Boolean,
        hasTranslation: Boolean,
        hasFlowReference: Boolean,
        userScale: Float,
        scriptFactors: ScriptFactors,
    ): List<FlowBlockSpec> {
        val list = ArrayList<FlowBlockSpec>(3)

        if (hasArabic) {
            val sizeFactor = scriptFactors.sizeFactor.coerceAtLeast(0.1f)
            val lhFactor = scriptFactors.lineHeightFactor.coerceAtLeast(0.1f)

            val floorFrac = layout.arabic.minSize * sizeFactor
            val prefFrac = max(floorFrac, layout.arabic.maxSize * sizeFactor * userScale)
            val floorLH = layout.arabic.minLineHeight * lhFactor
            val prefLH = max(floorLH, layout.arabic.lineHeight * lhFactor)

            list.add(
                FlowBlockSpec(
                    kind = FlowBlockKind.ARABIC,
                    floorSizePx = floorFrac * canvasWidth,
                    prefSizePx = prefFrac * canvasWidth,
                    floorLineHeight = floorLH,
                    prefLineHeight = prefLH,
                    gapMinPx = 0f,
                    gapMaxPx = 0f,
                )
            )
        }

        if (hasTranslation) {
            val floorFrac = layout.translation.minSize
            val prefFrac = max(floorFrac, layout.translation.maxSize * userScale)
            val floorLH = layout.translation.minLineHeight
            val prefLH = max(floorLH, layout.translation.lineHeight)

            val hasPrior = list.isNotEmpty()
            val gapMin = if (hasPrior) layout.spacing.arabicTranslation.min * canvasHeight else 0f
            val gapMax = if (hasPrior) layout.spacing.arabicTranslation.max * canvasHeight else 0f

            list.add(
                FlowBlockSpec(
                    kind = FlowBlockKind.TRANSLATION,
                    floorSizePx = floorFrac * canvasWidth,
                    prefSizePx = prefFrac * canvasWidth,
                    floorLineHeight = floorLH,
                    prefLineHeight = prefLH,
                    gapMinPx = gapMin,
                    gapMaxPx = gapMax,
                )
            )
        }

        if (hasFlowReference) {
            val floorFrac = layout.reference.minSize
            val prefFrac = max(floorFrac, layout.reference.maxSize * sqrt(userScale))

            val hasPrior = list.isNotEmpty()
            val gapMin =
                if (hasPrior) layout.spacing.translationReference.min * canvasHeight else 0f
            val gapMax =
                if (hasPrior) layout.spacing.translationReference.max * canvasHeight else 0f

            list.add(
                FlowBlockSpec(
                    kind = FlowBlockKind.REFERENCE,
                    floorSizePx = floorFrac * canvasWidth,
                    prefSizePx = prefFrac * canvasWidth,
                    floorLineHeight = REF_FLOOR_LINE_HEIGHT,
                    prefLineHeight = REF_PREF_LINE_HEIGHT,
                    gapMinPx = gapMin,
                    gapMaxPx = gapMax,
                )
            )
        }

        return list
    }

    private fun solveFreeReference(
        ref: ReferenceBlockStyle,
        canvasWidth: Float,
        canvasHeight: Float,
        userScale: Float,
        measurer: FreeReferenceMeasurer,
    ): SolvedFreeReference {
        val boxLeft = ref.left * canvasWidth
        val boxTop = ref.top * canvasHeight
        val boxWidth = max(1f, (1f - ref.left - ref.right) * canvasWidth)
        val boxHeight = max(1f, (1f - ref.top - ref.bottom) * canvasHeight)

        val floorPx = max(1f, ref.minSize * canvasWidth)
        val prefPx = max(floorPx, ref.maxSize * sqrt(userScale) * canvasWidth)

        if (measurer.fitsInBox(prefPx, FREE_REF_LINE_HEIGHT, boxWidth, boxHeight)) {
            return SolvedFreeReference(
                boxLeftPx = boxLeft,
                boxTopPx = boxTop,
                boxWidthPx = boxWidth,
                boxHeightPx = boxHeight,
                fontSizePx = prefPx,
                lineHeightMultiplier = FREE_REF_LINE_HEIGHT,
            )
        }

        if (measurer.fitsInBox(floorPx, FREE_REF_LINE_HEIGHT, boxWidth, boxHeight)) {
            var lo = floorPx
            var hi = prefPx
            var best = floorPx

            repeat(BINARY_SEARCH_STEPS) {
                val mid = (lo + hi) * 0.5f
                if (measurer.fitsInBox(mid, FREE_REF_LINE_HEIGHT, boxWidth, boxHeight)) {
                    best = mid
                    lo = mid
                } else {
                    hi = mid
                }
            }

            return SolvedFreeReference(
                boxLeftPx = boxLeft,
                boxTopPx = boxTop,
                boxWidthPx = boxWidth,
                boxHeightPx = boxHeight,
                fontSizePx = best,
                lineHeightMultiplier = FREE_REF_LINE_HEIGHT,
            )
        }

        // Spec §5.2: may go below minSize only if the box itself is too small to hold minSize
        val hardFloorPx = min(floorPx, max(4f, floorPx * 0.4f))
        var lo = hardFloorPx
        var hi = floorPx
        var best = hardFloorPx

        repeat(BINARY_SEARCH_STEPS) {
            val mid = (lo + hi) * 0.5f
            
            if (measurer.fitsInBox(mid, FREE_REF_LINE_HEIGHT, boxWidth, boxHeight)) {
                best = mid
                lo = mid
            } else {
                hi = mid
            }
        }

        return SolvedFreeReference(
            boxLeftPx = boxLeft,
            boxTopPx = boxTop,
            boxWidthPx = boxWidth,
            boxHeightPx = boxHeight,
            fontSizePx = best,
            lineHeightMultiplier = FREE_REF_LINE_HEIGHT,
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
