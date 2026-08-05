package com.beatbridge

import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

/**
 * Draws inset list dividers while leaving selected cards visually isolated.
 *
 * A divider next to a selected row competes with the row's rounded outline, so
 * both the divider above and below a selected row are intentionally omitted.
 */
class SelectionAwareDividerDecoration(
    @ColorInt color: Int,
    density: Float,
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    private val thickness = density.coerceAtLeast(1f)
    private val horizontalInset = 20f * density

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return

        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION || position >= adapter.itemCount - 1) continue

            val next = parent.findViewHolderForAdapterPosition(position + 1)?.itemView
            if (child.isSelected || next?.isSelected == true) continue

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin + (thickness / 2f)
            canvas.drawRect(
                parent.paddingLeft + horizontalInset,
                top,
                parent.width - parent.paddingRight - horizontalInset,
                top + thickness,
                paint,
            )
        }
    }
}
