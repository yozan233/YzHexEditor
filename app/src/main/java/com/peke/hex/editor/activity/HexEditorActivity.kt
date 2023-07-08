package com.peke.hex.editor.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import com.peke.hex.editor.R
import com.peke.hex.editor.adapter.HexEditorItemAdapter
import com.peke.hex.editor.databinding.ActivityHexEditorBinding
import com.peke.hex.editor.dialog.HexDataSearchDialog
import com.peke.hex.editor.dialog.HexEditorCopyDialog
import com.peke.hex.editor.dialog.HexEditorOffsetDialog
import com.peke.hex.editor.dialog.HexEditorPasteDialog
import com.peke.hex.editor.ext.toHexString
import com.peke.hex.editor.ext.toUnsignedInt
import com.peke.hex.editor.utils.ClipboardUtils
import com.peke.hex.editor.utils.DisplayUtils
import com.peke.hex.editor.utils.StringUtils
import com.peke.hex.editor.widget.CustomKeyBoardView
import com.peke.hex.editor.widget.HexEditorView
import java.io.File
import java.util.regex.Pattern

class HexEditorActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context,file:File?) {
            if(file == null || !file.exists()) return
            val intent = android.content.Intent(context, HexEditorActivity::class.java)
            intent.putExtra("file",file)
            context.startActivity(intent)
        }

    }

    private val mBinding: ActivityHexEditorBinding by lazy { ActivityHexEditorBinding.inflate(layoutInflater) }

    private val mHexEditorView by lazy {
        HexEditorView(this).apply {
            initHexEditorView(this)
            val layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
            layoutParams.weight = 1f
            mBinding.layoutEditor.addView(this, layoutParams)
        }

    }

    /**
     * 源文件
     */
    private val mFile:File by lazy { intent.getSerializableExtra("file") as File }

    /**
     * 剪贴板工具类
     */
    private val clipboardUtils by lazy {
        ClipboardUtils(this).apply {
            clearMyClipboardData()
        }
    }

    /**
     * 十六进制编辑器输入地址弹窗
     */
    private val mHexEditorOffsetDialog by lazy {
        HexEditorOffsetDialog(this).apply {
            fileSize = mHexEditorView.dataSize

            onSureClick = { offset->
                var intercept = false
                when(mode){
                    HexEditorOffsetDialog.Mode.GOTO -> {
                        mHexEditorView.setSelectIndex(offset)
                    }
                    HexEditorOffsetDialog.Mode.START -> {
                        val endIndex = mBinding.etDataIndex2.tag as Int
                        if (offset > endIndex) {
                            Toast.makeText(this@HexEditorActivity, R.string.start_address_error, Toast.LENGTH_SHORT).show()
                            intercept = true
                        } else {
                            setEditIndexRange(offset, -1)
                            mHexEditorView.setSelectStartIndex(offset)
                        }
                    }
                    HexEditorOffsetDialog.Mode.END -> {
                        val startIndex = mBinding.etDataIndex1.tag as Int
                        if (offset < startIndex) {
                            Toast.makeText(this@HexEditorActivity, R.string.end_address_error, Toast.LENGTH_SHORT).show()
                            intercept = true
                        } else {
                            setEditIndexRange(-1, offset)
                            mHexEditorView.setSelectEndIndex(offset + 1)
                        }
                    }
                }

                intercept
            }

            setOnDismissListener {
                mHexEditorView.recoveryCursorState()
            }
        }
    }

    /**
     * 输入搜索数据弹窗
     */
    private val mHexDataSearchDialog by lazy {
        HexDataSearchDialog(this).apply {
            onSureClick = {
                mHexEditorView.searchData(it)
            }
            setOnDismissListener {
                mHexEditorView.recoveryCursorState()
            }
        }
    }

    /**
     * 复制弹窗
     */
    private val mHexEditorCopyDialog by lazy {
        HexEditorCopyDialog(this).apply {
            onCopy1Click = {
                clipboardUtils.writeMyClipboardBytes(getSelectedBytes())
                Toast.makeText(this@HexEditorActivity, R.string.copy_success, Toast.LENGTH_SHORT).show()
            }

            onCopy2Click = {
                val bytes = getSelectedBytes()
                val stringBuilder = StringBuilder()
                for (b in bytes) {
                    val ib = b.toUnsignedInt()
                    if(ib < 0x10)
                        stringBuilder.append('0')
                    stringBuilder.append(ib.toHexString())
                }
                clipboardUtils.writeSysClipText(stringBuilder.toString()){
                    if (it){
                        Toast.makeText(this@HexEditorActivity, R.string.copy_success, Toast.LENGTH_SHORT).show()
                    }
                    else{
                        Toast.makeText(this@HexEditorActivity, R.string.copy_failed, Toast.LENGTH_SHORT).show()
                    }
                }

            }

            onCopy3Click = {
                val startIndex = mBinding.etDataIndex1.tag as Int
                val endIndex = mBinding.etDataIndex2.tag as Int
                val bytes = getSelectedBytes()

                val builder = StringBuilder()
                val startPosition: Int = startIndex / 8
                val endPosition: Int = endIndex / 8
                var position = startPosition
                var pos = 0
                do {
                    val offset = position * 8
                    val offsetStr: String = offset.toHexString(8)
                    if (builder.isNotEmpty())
                        builder.append('\n')
                    builder.append(offsetStr)
                    val chars = CharArray(8)
                    for (i in 0..7) {
                        val index = offset + i
                        if (index in startIndex..endIndex) {
                            val hex: Int = bytes[pos].toUnsignedInt()
                            val hexStr: String = hex.toHexString(2)
                            builder.append(" ").append(hexStr)
                            chars[i] = if (hex <= 0x20 || hex == 0x7F) ' ' else hex.toChar()
                            pos++
                        } else {
                            chars[i] = ' '
                            builder.append("   ")
                        }
                    }
                    builder.append(' ').append(String(chars))
                    position++
                } while (position <= endPosition)

                clipboardUtils.writeSysClipText(builder.toString()){
                    if (it){
                        Toast.makeText(this@HexEditorActivity, R.string.copy_success, Toast.LENGTH_SHORT).show()
                    }
                    else{
                        Toast.makeText(this@HexEditorActivity, R.string.copy_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            setOnDismissListener {
                mHexEditorView.recoveryCursorState()
            }
        }
    }

    /**
     * 粘贴弹窗
     */
    private val mHexEditorPasteDialog by lazy {
        HexEditorPasteDialog(this).apply {
            onPaste1Click = {
                val bytes = clipboardUtils.readMyClipboardBytes()
                if (bytes.isNotEmpty()) {
                    val startIndex: Int = mHexEditorView.selectedIndex
                    mHexEditorView.writeBytes(startIndex, bytes)
                    updateUndoRedoButton()
                }
                else{
                    Toast.makeText(this@HexEditorActivity, R.string.clipboard_is_empty, Toast.LENGTH_SHORT).show()
                }
            }

            onPaste2Click = {
                clipboardUtils.readSysClipText{ text->
                    var bytes: ByteArray? = null
                    if (!text.isNullOrEmpty()) {
                        val builder = java.lang.StringBuilder()
                        val regex = Pattern.compile("[\\dA-Fa-f]+")
                        val matcher = regex.matcher(text)
                        while (matcher.find()) {
                            builder.append(matcher.group())
                        }
                        if (builder.length % 2 != 0) {
                            builder.insert(builder.length - 1, '0')
                        }
                        bytes = StringUtils.hexDataToBytes(builder.toString())
                    }

                    if(bytes != null && bytes!!.isNotEmpty()) {
                        val startIndex: Int = mHexEditorView.selectedIndex
                        mHexEditorView.writeBytes(startIndex, bytes)
                        updateUndoRedoButton()
                    }
                    else{
                        Toast.makeText(this@HexEditorActivity, R.string.clipboard_is_empty, Toast.LENGTH_SHORT).show()
                    }

                }
            }

            setOnDismissListener {
                mHexEditorView.recoveryCursorState()
            }
        }
    }

    /**
     * 头部菜单弹窗
     */
    private val mMenuPopup by lazy {
        val options = arrayOf(
            getString(R.string.go_to_offset),
            getString(R.string.copy),
            getString(R.string.paste),
            getString(R.string.search),
            getString(R.string.save)
        )
        val listPopupWindow = ListPopupWindow(this)
        listPopupWindow.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1,options))
        listPopupWindow.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            lastClickMenuIndex = position
            when(position){
                0 -> {
                    //跳转到
                    mHexEditorOffsetDialog.mode = HexEditorOffsetDialog.Mode.GOTO
                    mHexEditorOffsetDialog.setText("")
                    mHexEditorOffsetDialog.show()
                }
                1 -> {
                    //复制
                    if (mHexEditorView.isDataRangeSelected) {
                        mHexEditorCopyDialog.show()
                    } else {
                        mHexEditorView.recoveryCursorState()
                        Toast.makeText(this@HexEditorActivity, R.string.no_selected_data, Toast.LENGTH_SHORT).show()
                    }
                }
                2 -> {
                    //粘贴
                    if (mHexEditorView.isDataSelected) {
                        mHexEditorPasteDialog.show()
                    } else {
                        mHexEditorView.recoveryCursorState()
                        Toast.makeText(this@HexEditorActivity, R.string.no_paste_position, Toast.LENGTH_SHORT).show()
                    }
                }
                3 -> {
                    //搜索
                    mHexDataSearchDialog.show()
                }
                4 -> {
                    //保存
                    mHexEditorView.saveFile{
                        updateUndoRedoButton()
                    }
                }
            }
            dismissMenuPopup()
        })
        listPopupWindow.anchorView = mBinding.btnMore
        listPopupWindow.width = DisplayUtils.dp2px(this@HexEditorActivity, 120f)
        listPopupWindow.verticalOffset = -DisplayUtils.dp2px(this@HexEditorActivity,8f)
        listPopupWindow.setOnDismissListener {
            menuPopupDismissTimeMillis = System.currentTimeMillis()
            if (lastClickMenuIndex == -1){
                mHexEditorView.recoveryCursorState()
            }
        }

        listPopupWindow
    }

    /**
     * 最后点击的菜单索引
     */
    private var lastClickMenuIndex = -1

    /**
     * 菜单弹窗消失时间戳
     */
    private var menuPopupDismissTimeMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mBinding.apply {
            initHexEditorKeyBoard()

            btnUndo.setOnClickListener(View.OnClickListener {
                val undo: Boolean = mHexEditorView.undo()
                if (undo) {
                    updateUndoRedoButton()
                }
            })

            btnRedo.setOnClickListener(View.OnClickListener {
                val redo: Boolean = mHexEditorView.redo()
                if (redo) {
                    updateUndoRedoButton()
                }
            })

            btnMore.setOnClickListener {
                if (mHexEditorView.hasTaskRunning() || System.currentTimeMillis() - menuPopupDismissTimeMillis < 500)
                    return@setOnClickListener
                mHexEditorView.hideCursor()
                lastClickMenuIndex = -1
                mMenuPopup.show()
            }

            etDataIndex1.setOnClickListener {
                mHexEditorOffsetDialog.mode = HexEditorOffsetDialog.Mode.START
                mHexEditorOffsetDialog.setText(etDataIndex1.text.replace("^0+".toRegex(),""))
                mHexEditorOffsetDialog.show()
            }

            etDataIndex2.setOnClickListener {
                mHexEditorOffsetDialog.mode = HexEditorOffsetDialog.Mode.END
                mHexEditorOffsetDialog.setText(etDataIndex2.text.replace("^0+".toRegex(),""))
                mHexEditorOffsetDialog.show()
            }

        }


        loadFile()
    }

    /**
     * 初始化十六进制编辑器键盘
     */
    private fun initHexEditorKeyBoard() {
        mBinding.mKeyBoard.setOnInputListener { inputKey ->
            if (inputKey.isHexNumber) {
                mHexEditorView.writeHalfByte(inputKey.value)
                updateUndoRedoButton()
            } else {
                when (inputKey) {
                    CustomKeyBoardView.InputKey.KEY_START -> mHexEditorView.moveSelectedOffsetToFirst()
                    CustomKeyBoardView.InputKey.KEY_END -> mHexEditorView.moveSelectedOffsetToLast()
                    CustomKeyBoardView.InputKey.KEY_LEFT -> mHexEditorView.moveSelectedOffsetLeft()
                    CustomKeyBoardView.InputKey.KEY_RIGHT -> mHexEditorView.moveSelectedOffsetRight()
                    CustomKeyBoardView.InputKey.KEY_UP -> mHexEditorView.moveSelectedPositionUp()
                    CustomKeyBoardView.InputKey.KEY_DOWN -> mHexEditorView.moveSelectedPositionDown()
                    CustomKeyBoardView.InputKey.KEY_ENTRY -> mHexEditorView.clearSelectRange()
                    else -> { }
                }
            }
        }
    }

    /**
     * 初始化十六进制编辑器视图
     */
    private fun initHexEditorView(hexEditorView: HexEditorView) {
        hexEditorView.setListener(object : HexEditorView.Listener {
            override fun onSelectModeChange(selectMode: HexEditorItemAdapter.SelectMode) {
                when (selectMode) {
                    HexEditorItemAdapter.SelectMode.NONE -> {
                        mBinding.layoutDataIndex.isVisible = false
                        mBinding.layoutKeyboard.isVisible = false
                    }

                    HexEditorItemAdapter.SelectMode.SingleSelect -> {
                        mBinding.layoutDataIndex.isVisible = false
                        mBinding.layoutKeyboard.isVisible = true
                    }

                    HexEditorItemAdapter.SelectMode.MultipleSelect -> {
                        mBinding.layoutDataIndex.isVisible = true
                        mBinding.layoutKeyboard.isVisible = false
                    }
                }
            }

            override fun onIndexRangeSelected(startIndex: Int, endIndex: Int) {
                setEditIndexRange(startIndex, endIndex)
            }

            override fun onDataChange(size: Int) {
                showEmptyFileLayout(size == 0)
            }

            override fun onFileLoadFinish(isSuccess: Boolean) {
                displayFileName()
            }
        })
    }

    /**
     * 加载文件
     */
    private fun loadFile(){
        mHexEditorView.srcFile = mFile
    }

    /**
     * 获取选择的字节数据
     */
    private fun getSelectedBytes(): ByteArray {
        val startIndex = mBinding.etDataIndex1.tag as Int
        val endIndex = mBinding.etDataIndex2.tag as Int
        val dataLength = endIndex - startIndex + 1
        return mHexEditorView.readBytes(startIndex, dataLength)
    }

    /**
     * 更新撤销重做按钮
     */
    private fun updateUndoRedoButton() {
        mBinding.btnUndo.alpha = if (mHexEditorView.canUndo()) 1f else 0.4f
        mBinding.btnRedo.alpha = if (mHexEditorView.canRedo()) 1f else 0.4f
    }

    /**
     * 设置显示的选择范围
     */
    private fun setEditIndexRange(startIndex: Int, endIndex: Int) {
        if (startIndex >= 0) {
            mBinding.etDataIndex1.tag = startIndex
            mBinding.etDataIndex1.text = startIndex.toHexString(8)
        }
        if (endIndex >= 0) {
            mBinding.etDataIndex2.tag = endIndex
            mBinding.etDataIndex2.text = endIndex.toHexString(8)
        }
    }

    /**
     * 显示空文件布局
     */
    private fun showEmptyFileLayout(show: Boolean) {
        if (show){
            mBinding.layoutNotFile.isVisible = true
            mHexEditorView.isVisible = false
        }
        else{
            mBinding.layoutNotFile.isVisible = false
            mHexEditorView.isVisible = true
        }
    }

    /**
     * 显示文件名称
     */
    private fun displayFileName() {
        val srcFile = mHexEditorView.srcFile
        if (srcFile == null) {
            mBinding.tvFileName.text = getString(R.string.no_import_file)
        } else {
            mBinding.tvFileName.text = srcFile.name
        }
    }

    /**
     * 关闭菜单弹窗
     */
    private fun dismissMenuPopup(){
        if (mMenuPopup.isShowing){
            mMenuPopup.dismiss()
        }
    }

    private var mBackPressedTime: Long = 0
    override fun onBackPressed() {
        if (mHexEditorView.hasTaskRunning()) {
            return
        }

        if (mMenuPopup.isShowing){
            mMenuPopup.dismiss()
            return
        }

        if (mHexEditorView.isDataSelected) {
            mHexEditorView.clearSelectRange()
            return
        }

        if (System.currentTimeMillis() - mBackPressedTime > 2000) {
            Toast.makeText(this, R.string.double_click_back_key_exit, Toast.LENGTH_SHORT).show()
            mBackPressedTime = System.currentTimeMillis()
            return
        }
        super.onBackPressed()
    }

}