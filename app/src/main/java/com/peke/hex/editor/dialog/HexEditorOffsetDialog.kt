package com.peke.hex.editor.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import com.peke.hex.editor.R
import com.peke.hex.editor.databinding.DialogEditTextBinding
import com.peke.hex.editor.ext.toHexString
import com.peke.hex.editor.utils.RegexInputFilter

class HexEditorOffsetDialog(private val mContext: Context): Dialog(mContext, R.style.CustomDialog) {

    private val mBinding by lazy { DialogEditTextBinding.inflate(layoutInflater) }

    var onSureClick:((offset:Int)->Boolean)? = null
    var fileSize:Int = Int.MAX_VALUE
        set(value) {
            field = value
            val maxStr = fileSize.toHexString()
            mBinding.editText.hint = "0x0 - 0x$maxStr"
        }

    var mode: Mode = Mode.GOTO
        set(value) {
            field = value
            when(value){
                Mode.GOTO -> mBinding.tvTitle.text = mContext.getString(R.string.go_to_offset)
                Mode.START -> mBinding.tvTitle.text = mContext.getString(R.string.input_start_offset)
                Mode.END -> mBinding.tvTitle.text = mContext.getString(R.string.input_end_offset)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setCanceledOnTouchOutside(false)

        mBinding.apply {
            //设置只能输入0-9和A-F
            editText.filters = arrayOf(RegexInputFilter("[0-9a-fA-F]+"))
            editText.maxLines = 1

            editText.doAfterTextChanged {
                if (it.isNullOrEmpty())
                    return@doAfterTextChanged
                var offset = Int.MAX_VALUE
                try {
                    offset = it.toString().toInt(16)
                }catch (_:Exception) { }
                if (offset > fileSize){
                    val maxStr = fileSize.toHexString()
                    editText.setText(maxStr)
                    editText.setSelection(maxStr.length)
                }
            }

            btnCancel.setOnClickListener {
                dismiss()
            }

            btnOk.setOnClickListener {
                var isIntercept = false
                val text = editText.text.toString()
                if (text.isNotEmpty()){
                    val offset = text.toInt(16)
                    isIntercept = onSureClick?.invoke(offset) ?: false
                }
                if (!isIntercept)
                    dismiss()
            }

        }

    }

    fun setText(text:CharSequence){
        mBinding.editText.setText(text)
    }

    enum class Mode {
        GOTO, START, END
    }

}