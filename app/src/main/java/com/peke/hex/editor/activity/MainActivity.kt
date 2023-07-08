package com.peke.hex.editor.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.peke.hex.editor.databinding.ActivityMainBinding
import com.peke.hex.editor.utils.FilePathUtils
import java.io.File
import java.net.URI


class MainActivity : AppCompatActivity() {
    private val mBinding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var fileManagerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        permissionLauncher = registerForActivityResult(RequestMultiplePermissions()){ result ->
            if (result.filterValues { it == false }.isEmpty()){
                toFileManager()
            }
        }

        fileManagerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if (result.resultCode == RESULT_OK){
                val uri = result.data?.data
                val path = FilePathUtils.getPathFromUri(this@MainActivity,uri)
                path?.let {
                    HexEditorActivity.start(this@MainActivity, File(it))
                }
            }
        }

        mBinding.apply {
            btnSelectFile.setOnClickListener {
                if (hasStoragePermission()){
                    toFileManager()
                }
                else{
                    permissionLauncher.launch(permissions)
                }
            }

        }

    }

    private fun hasStoragePermission():Boolean{
        permissions.forEach {
            if (PermissionChecker.checkSelfPermission(this,it) != PermissionChecker.PERMISSION_GRANTED){
                return false
            }
        }
        return true
    }

    private fun toFileManager() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        fileManagerLauncher.launch(intent)
    }

}