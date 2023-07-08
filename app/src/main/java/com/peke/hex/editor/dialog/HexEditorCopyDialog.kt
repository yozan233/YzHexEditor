package com.peke.hex.editor.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.peke.hex.editor.R
import com.peke.hex.editor.databinding.DialogHexeditorCopyBinding
import com.peke.hex.editor.utils.DisplayUtils

class HexEditorCopyDialog(private val mContext: Context) : Dialog(mContext, R.style.CustomDialog) {

    private val mBinding by lazy { DialogHexeditorCopyBinding.inflate(layoutInflater) }

    var onCopy1Click:(()->Unit)? = null
    var onCopy2Click:(()->Unit)? = null
    var onCopy3Click:(()->Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setCanceledOnTouchOutside(false)
        window?.let {
            val windowAttributes = it.attributes
            windowAttributes.width = ((DisplayUtils.getScreenWidth(mContext) * 0.85f).toInt())
            it.attributes = windowAttributes
        }


        mBinding.apply {
            btnCopy1.setOnClickListener {
                dismiss()
                onCopy1Click?.invoke()
            }

            btnCopy2.setOnClickListener {
                dismiss()
                onCopy2Click?.invoke()
            }

            btnCopy3.setOnClickListener {
                dismiss()
                onCopy3Click?.invoke()
            }

            btnClose.setOnClickListener {
                dismiss()
            }

        }
    }
}