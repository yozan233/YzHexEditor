package com.peke.hex.editor.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.peke.hex.editor.R
import com.peke.hex.editor.databinding.DialogHexeditorPasteBinding
import com.peke.hex.editor.utils.DisplayUtils

class HexEditorPasteDialog(private val mContext: Context) : Dialog(mContext, R.style.CustomDialog) {

    private val mBinding by lazy { DialogHexeditorPasteBinding.inflate(layoutInflater) }


    var onPaste1Click:(()->Unit)? = null
    var onPaste2Click:(()->Unit)? = null

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
            btnPaste1.setOnClickListener {
                dismiss()
                onPaste1Click?.invoke()
            }

            btnPaste2.setOnClickListener {
                dismiss()
                onPaste2Click?.invoke()
            }

            btnClose.setOnClickListener {
                dismiss()
            }

        }

    }

}