package com.peke.hex.editor.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.peke.hex.editor.R
import com.peke.hex.editor.databinding.DialogEditTextBinding
import com.peke.hex.editor.utils.RegexInputFilter
import com.peke.hex.editor.utils.StringUtils

class HexDataSearchDialog(private val mContext: Context) : Dialog(mContext, R.style.CustomDialog) {

    private val mBinding by lazy { DialogEditTextBinding.inflate(layoutInflater) }

    var onSureClick:((bytes:ByteArray)->Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        setCanceledOnTouchOutside(false)

        mBinding.apply {
            tvTitle.text = mContext.getString(R.string.search_data)
            editText.hint = mContext.getString(R.string.input_hex_data)

            //设置只能输入0-9和A-F
            editText.filters = arrayOf(RegexInputFilter("[0-9a-fA-F\\s]+"))

            btnCancel.setOnClickListener {
                dismiss()
            }

            btnOk.setOnClickListener {
                dismiss()
                val text = editText.text.toString()
                if (text.isNotEmpty()){
                    val bytes: ByteArray = StringUtils.hexDataToBytes(text.replace("\\s".toRegex(), ""))
                    onSureClick?.invoke(bytes)
                }
            }

        }

    }

}