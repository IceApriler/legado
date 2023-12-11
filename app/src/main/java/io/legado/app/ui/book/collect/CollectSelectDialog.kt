package io.legado.app.ui.book.collect

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.CollectTag
import io.legado.app.databinding.DialogBookGroupPickerBinding
import io.legado.app.databinding.ItemGroupSelectBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.LogUtils.getCurrentDateStr
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


class CollectSelectDialog() : BaseDialogFragment(R.layout.dialog_book_group_picker),
    Toolbar.OnMenuItemClickListener {

    constructor(selectedText: String, requestCode: Int = -1) : this() {
        arguments = Bundle().apply {
            putString("selectedText", selectedText)
            putInt("requestCode", requestCode)
        }
    }

    private val binding by viewBinding(DialogBookGroupPickerBinding::bind)
    private var requestCode: Int = -1

    //    private val viewModel: GroupViewModel by viewModels()
    private val adapter by lazy { GroupAdapter(requireContext()) }
    private val callBack get() = (activity as? CallBack)
    private var tagId: Long = 0
    private var selectedText: String = ""

    private val TAG = "CollectSelectDialog"

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        arguments?.let {
            selectedText = it.getString("selectedText", "")
            requestCode = it.getInt("requestCode", -1)
        }
        initView()
        initData()
    }

    private fun initView() {
        binding.toolBar.title = "收藏"
//        binding.toolBar.inflateMenu(R.menu.book_group_manage)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = false
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.setTextColor(requireContext().accentColor)
        binding.tvOk.setOnClickListener {
            Log.d(TAG, "initView: tvOk")
//            callBack?.upGroup(requestCode, groupId)

            val customDirectory = "AMyDocument"
            // 获取CSV文件路径
            val csvFilePath = File(
                Environment.getExternalStorageDirectory(),
                "$customDirectory/my_data.csv"
            ).absolutePath

            Log.d(TAG, csvFilePath)
            val csvFile = File(csvFilePath)
            if (csvFile.exists()) {
                // 文件已存在，追加数据到CSV文件
                val newData = arrayOf(getCurrentDateStr("yy-MM-dd HH:mm:ss"), selectedText, "", "", "", "")
                appendDataToCsvFile(csvFilePath, newData)
            } else {
                // 文件不存在，初始化数据
                initializeCsvFile(csvFilePath)
            }
            // 修改CSV文件中的数据
//            modifyCsvFile(csvFilePath, "John Doe", "Updated Age")
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
//        launch {
//            appDb.bookGroupDao.flowSelect().conflate().collect {
//                adapter.setItems(it)
//            }
//        }
        // 预定义一些书籍分组数据
        val predefinedData = listOf(
            CollectTag(1, "灵感、桥段套路、笔记" /* 其他属性 */),
            CollectTag(2, "设定集锦" /* 其他属性 */),
            CollectTag(21, "设定集锦-功法" /* 其他属性 */),
            CollectTag(22, "设定集锦-修炼" /* 其他属性 */),
            CollectTag(3, "描写" /* 其他属性 */),
            CollectTag(31, "描写-人" /* 其他属性 */),
            CollectTag(311, "描写-人-静态外表" /* 其他属性 */),
            CollectTag(312, "描写-人-动态动作" /* 其他属性 */),
            CollectTag(32, "描写-景" /* 其他属性 */),
            CollectTag(33, "描写-物" /* 其他属性 */),
            CollectTag(34, "描写-战斗" /* 其他属性 */),
            CollectTag(4, "形容词" /* 其他属性 */),
            CollectTag(41, "形容词-景" /* 其他属性 */),
            CollectTag(42, "形容词-物" /* 其他属性 */),
            CollectTag(43, "形容词-人" /* 其他属性 */),
            CollectTag(5, "名词、名称" /* 其他属性 */),
            CollectTag(7, "短语、比喻" /* 其他属性 */),
        )

        // 将预定义数据设置到适配器中
        adapter.setItems(predefinedData)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
//        when (item?.itemId) {
//            R.id.menu_add -> showDialogFragment(
//                GroupEditDialog()
//            )
//        }
        return true
    }

    private inner class GroupAdapter(context: Context) :
        RecyclerAdapter<CollectTag, ItemGroupSelectBinding>(context),
        ItemTouchCallback.Callback {

        private var isMoved: Boolean = false

        override fun getViewBinding(parent: ViewGroup): ItemGroupSelectBinding {
            return ItemGroupSelectBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemGroupSelectBinding,
            item: CollectTag,
            payloads: MutableList<Any>
        ) {
            binding.run {
                root.setBackgroundColor(context.backgroundColor)
                cbGroup.text = item.tagName
                cbGroup.isChecked = (tagId and item.tagId) > 0
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemGroupSelectBinding) {
            binding.run {
                cbGroup.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            tagId = if (isChecked) {
                                tagId + it.tagId
                            } else {
                                tagId - it.tagId
                            }
                        }
                    }
                }
//                tvEdit.setOnClickListener {
//                    showDialogFragment(
//                        GroupEditDialog(getItem(holder.layoutPosition))
//                    )
//                }
            }
        }

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
//            swapItem(srcPosition, targetPosition)
//            isMoved = true
            return true
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            if (isMoved) {
//                for ((index, item) in getItems().withIndex()) {
//                    item.order = index + 1
//                }
//                viewModel.upGroup(*getItems().toTypedArray())
            }
            isMoved = false
        }
    }

    private fun initializeCsvFile(filePath: String) {
        Log.d(TAG, "initializeCsvFile")
        try {
            val file = File(filePath)
            val fileWriter = FileWriter(file)

            val writer = CSVWriter(fileWriter)
            val data = arrayOf("收藏时间", "收藏内容", "分类", "标签", "备注", "来源")
            writer.writeNext(data)

            // 添加示例数据
//            val sampleData = arrayOf("John Doe", "25", "john@example.com")
//            writer.writeNext(sampleData)

            writer.close()

            Log.d(TAG, "Initialize CSV file successfully")

            // 文件保存成功
            toastOnUi("Initialize CSV file successfully")
        } catch (e: IOException) {
            Log.d(TAG, "Failed to initialize CSV file")
            e.printStackTrace()
            // 处理保存文件失败的情况
            toastOnUi("Failed to initialize CSV file")
        }
    }

    private fun appendDataToCsvFile(filePath: String, newData: Array<String>) {
        try {
            val fileWriter = FileWriter(filePath, true)
            val writer = CSVWriter(fileWriter)

            // 将新数据追加到文件末尾
            writer.writeNext(newData)

            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // 处理异常
        }
    }

    private fun modifyCsvFile(filePath: String, targetName: String, newAge: String) {
        val csvEntries = readCsvFile(filePath)
        for (entry in csvEntries) {
            if (entry[0] == targetName) {
                entry[1] = newAge
            }
        }

        writeCsvFile(filePath, csvEntries)
    }

    private fun deleteDataFromCsvFile(filePath: String, targetName: String) {
        val csvEntries = readCsvFile(filePath)
        csvEntries.removeIf { entry -> entry[0] == targetName }
        writeCsvFile(filePath, csvEntries)
    }

    private fun queryCsvFile(filePath: String) {
        val csvEntries = readCsvFile(filePath)
        for (entry in csvEntries) {
            // 处理查询到的数据
            println("Name: ${entry[0]}, Age: ${entry[1]}, Email: ${entry[2]}")
        }
    }

    private fun readCsvFile(filePath: String): MutableList<Array<String>> {
        val csvEntries = mutableListOf<Array<String>>()

        try {
            val reader = CSVReader(FileReader(filePath))
            var nextLine: Array<String>?

            // 逐行读取CSV文件
            while (reader.readNext().also { nextLine = it } != null) {
                csvEntries.add(nextLine!!)
            }

            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理异常
        }

        return csvEntries
    }

    private fun writeCsvFile(filePath: String, data: List<Array<String>>) {
        try {
            val fileWriter = FileWriter(filePath)
            val writer = CSVWriter(fileWriter)

            for (entry in data) {
                writer.writeNext(entry)
            }

            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
            // 处理异常
        }
    }

    interface CallBack {
        fun upGroup(requestCode: Int, groupId: Long)
    }
}