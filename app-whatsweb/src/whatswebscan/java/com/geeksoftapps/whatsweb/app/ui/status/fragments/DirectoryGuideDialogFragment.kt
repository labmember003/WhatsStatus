package com.geeksoftapps.whatsweb.app.ui.status.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.geeksoftapps.whatsweb.app.R

/**
 * A full-screen-style dialog that shows a 2-step visual guide explaining
 * how to select the WhatsApp directory in the system file picker.
 *
 * Step 1 – top 50% of the screenshot (arrow pointing to the "WhatsApp" folder)
 * Step 2 – bottom 50% of the screenshot (arrow pointing to "USE THIS FOLDER" button)
 *
 * When the user finishes reading, [onGuideComplete] fires so the caller
 * can launch the actual SAF picker.
 */
class DirectoryGuideDialogFragment : DialogFragment() {

    private var currentStep = 0
    private var onGuideComplete: (() -> Unit)? = null

    companion object {
        private const val ARG_IS_BUSINESS = "is_business"

        fun newInstance(isBusiness: Boolean, onComplete: () -> Unit): DirectoryGuideDialogFragment {
            return DirectoryGuideDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_BUSINESS, isBusiness)
                }
                onGuideComplete = onComplete
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = false
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_directory_guide, container, false)
    }

    /**
     * Scales the image to fill the ImageView width and shows either the
     * top half ([showTopHalf] = true) or bottom half ([showTopHalf] = false).
     */
    private fun applyCrop(iv: ImageView, showTopHalf: Boolean) {
        val drawable = iv.drawable ?: return
        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        val vWidth = iv.width.toFloat()
        val vHeight = iv.height.toFloat()

        if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) return

        val scale = vWidth / dWidth
        val scaledHeight = dHeight * scale

        val matrix = Matrix()
        if (showTopHalf) {
            // Scale to fit width, origin top-left — shows the top portion
            matrix.setScale(scale, scale)
        } else {
            // Scale to fit width, shift up so the bottom of the image aligns with the bottom of the view
            val dy = vHeight - scaledHeight
            matrix.setScale(scale, scale)
            matrix.postTranslate(0f, dy)
        }
        iv.imageMatrix = matrix
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvGuideTitle)
        val tvStep = view.findViewById<TextView>(R.id.tvStepIndicator)
        val ivScreenshot = view.findViewById<ImageView>(R.id.ivGuideScreenshot)
        val tvInstruction = view.findViewById<TextView>(R.id.tvGuideInstruction)
        val dot1 = view.findViewById<View>(R.id.dot1)
        val dot2 = view.findViewById<View>(R.id.dot2)
        val btnPrevious = view.findViewById<MaterialButton>(R.id.btnGuidePrevious)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnGuideNext)

        val isBusiness = arguments?.getBoolean(ARG_IS_BUSINESS, false) ?: false

        val folderName = if (isBusiness) "WhatsApp Business" else "WhatsApp"
        tvTitle.text = getString(R.string.guide_title)

        fun updateStep() {
            when (currentStep) {
                0 -> {
                    tvStep.text = getString(R.string.guide_step_1_of_2)
                    ivScreenshot.setImageResource(R.drawable.guide_step1_select_folder)
                    tvInstruction.text = Html.fromHtml(
                        "Find and tap on the <b>\"$folderName\"</b> folder in the list.",
                        Html.FROM_HTML_MODE_COMPACT
                    )
                    dot1.setBackgroundResource(R.drawable.bg_guide_dot_active)
                    dot2.setBackgroundResource(R.drawable.bg_guide_dot_inactive)
                    btnPrevious.visibility = View.GONE
                    btnNext.text = getString(R.string.guide_next)
                    // Show top 50% of the screenshot
                    ivScreenshot.post { applyCrop(ivScreenshot, showTopHalf = true) }
                }
                1 -> {
                    tvStep.text = getString(R.string.guide_step_2_of_2)
                    ivScreenshot.setImageResource(R.drawable.guide_step2_use_folder)
                    tvInstruction.text = Html.fromHtml(
                        "Now tap the <b>\"USE THIS FOLDER\"</b> button at the bottom to grant access.",
                        Html.FROM_HTML_MODE_COMPACT
                    )
                    dot1.setBackgroundResource(R.drawable.bg_guide_dot_inactive)
                    dot2.setBackgroundResource(R.drawable.bg_guide_dot_active)
                    btnPrevious.visibility = View.VISIBLE
                    btnNext.text = getString(R.string.guide_got_it)
                    // Show bottom 50% of the screenshot
                    ivScreenshot.post { applyCrop(ivScreenshot, showTopHalf = false) }
                }
            }
        }

        updateStep()

        btnNext.setOnClickListener {
            if (currentStep == 0) {
                currentStep = 1
                updateStep()
            } else {
                dismiss()
                onGuideComplete?.invoke()
            }
        }

        btnPrevious.setOnClickListener {
            if (currentStep > 0) {
                currentStep = 0
                updateStep()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog nearly full-width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
