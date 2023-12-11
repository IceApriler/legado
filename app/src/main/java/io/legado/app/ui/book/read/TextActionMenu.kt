package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import androidx.annotation.RequiresApi
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.core.view.isVisible
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ItemTextBinding
import io.legado.app.databinding.PopupActionMenuBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.collect.CollectSelectDialog
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.utils.*
import io.legado.app.utils.LogUtils.getCurrentDateStr
import io.legado.app.utils.showDialogFragment
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

@SuppressLint("RestrictedApi")
class TextActionMenu(private val context: Context, private val callBack: CallBack, private val activity: ReadBookActivity) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val binding = PopupActionMenuBinding.inflate(LayoutInflater.from(context))
    private val adapter = Adapter(context).apply {
        setHasStableIds(true)
    }
    private val menuItems: List<MenuItemImpl>
    private val visibleMenuItems = arrayListOf<MenuItemImpl>()
    private val moreMenuItems = arrayListOf<MenuItemImpl>()
    private val expandTextMenu get() = context.getPrefBoolean(PreferKey.expandTextMenu)

    init {
        @SuppressLint("InflateParams")
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = false

        val myMenu = MenuBuilder(context)
        val otherMenu = MenuBuilder(context)
        SupportMenuInflater(context).inflate(R.menu.content_select_action, myMenu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onInitializeMenu(otherMenu)
        }
        menuItems = myMenu.visibleItems + otherMenu.visibleItems
        visibleMenuItems.addAll(menuItems.subList(0, 5))
        moreMenuItems.addAll(menuItems.subList(5, menuItems.size))
        binding.recyclerView.adapter = adapter
        binding.recyclerViewMore.adapter = adapter
        setOnDismissListener {
            if (!context.getPrefBoolean(PreferKey.expandTextMenu)) {
                binding.ivMenuMore.setImageResource(R.drawable.ic_more_vert)
                binding.recyclerViewMore.gone()
                adapter.setItems(visibleMenuItems)
                binding.recyclerView.visible()
            }
        }
        binding.ivMenuMore.setOnClickListener {
            if (binding.recyclerView.isVisible) {
                binding.ivMenuMore.setImageResource(R.drawable.ic_arrow_back)
                adapter.setItems(moreMenuItems)
                binding.recyclerView.gone()
                binding.recyclerViewMore.visible()
            } else {
                binding.ivMenuMore.setImageResource(R.drawable.ic_more_vert)
                binding.recyclerViewMore.gone()
                adapter.setItems(visibleMenuItems)
                binding.recyclerView.visible()
            }
        }
        upMenu()
    }

    fun upMenu() {
        if (expandTextMenu) {
            adapter.setItems(menuItems)
            binding.ivMenuMore.gone()
        } else {
            adapter.setItems(visibleMenuItems)
            binding.ivMenuMore.visible()
        }
    }

    fun show(
        view: View,
        windowHeight: Int,
        startX: Int,
        startTopY: Int,
        startBottomY: Int,
        endX: Int,
        endBottomY: Int
    ) {
        if (expandTextMenu) {
            when {
                startTopY > 500 -> {
                    showAtLocation(
                        view,
                        Gravity.BOTTOM or Gravity.START,
                        startX,
                        windowHeight - startTopY
                    )
                }
                endBottomY - startBottomY > 500 -> {
                    showAtLocation(view, Gravity.TOP or Gravity.START, startX, startBottomY)
                }
                else -> {
                    showAtLocation(view, Gravity.TOP or Gravity.START, endX, endBottomY)
                }
            }
        } else {
            contentView.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED,
            )
            val popupHeight = contentView.measuredHeight
            when {
                startBottomY > 500 -> {
                    showAtLocation(
                        view,
                        Gravity.TOP or Gravity.START,
                        startX,
                        startTopY - popupHeight
                    )
                }
                endBottomY - startBottomY > 500 -> {
                    showAtLocation(
                        view,
                        Gravity.TOP or Gravity.START,
                        startX,
                        startBottomY
                    )
                }
                else -> {
                    showAtLocation(
                        view,
                        Gravity.TOP or Gravity.START,
                        endX,
                        endBottomY
                    )
                }
            }
        }
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<MenuItemImpl, ItemTextBinding>(context) {

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getViewBinding(parent: ViewGroup): ItemTextBinding {
            return ItemTextBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemTextBinding,
            item: MenuItemImpl,
            payloads: MutableList<Any>
        ) {
            with(binding) {
                textView.text = item.title
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemTextBinding) {
            holder.itemView.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    if (!callBack.onMenuItemSelected(it.itemId)) {
                        onMenuItemSelected(it)
                    }
                }
                callBack.onMenuActionFinally()
            }
            holder.itemView.setOnLongClickListener {
                if (AppConfig.contentSelectSpeakMod == 0) {
                    AppConfig.contentSelectSpeakMod = 1
                    context.toastOnUi("切换为从选择的地方开始一直朗读")
                } else {
                    AppConfig.contentSelectSpeakMod = 0
                    context.toastOnUi("切换为朗读选择内容")
                }
                true
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItemImpl) {
        when (item.itemId) {
            R.id.menu_copy -> context.sendToClip(callBack.selectedText)
            R.id.menu_share_str -> context.share(callBack.selectedText)
            R.id.menu_browser -> {
                kotlin.runCatching {
                    val intent = if (callBack.selectedText.isAbsUrl()) {
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse(callBack.selectedText)
                        }
                    } else {
                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, callBack.selectedText)
                        }
                    }
                    context.startActivity(intent)
                }.onFailure {
                    it.printOnDebug()
                    context.toastOnUi(it.localizedMessage ?: "ERROR")
                }
            }
            R.id.menu_collect_str -> {
                Log.d("collect", "onMenuItemSelected: "+callBack.selectedText)

                context.toastOnUi("收藏："+callBack.selectedText)

                activity.showDialogFragment(CollectSelectDialog(callBack.selectedText))

//                val customDirectory = "AMyDocument"
//                // 获取CSV文件路径
//                val csvFilePath = File(Environment.getExternalStorageDirectory(), "$customDirectory/my_data.csv").absolutePath
//
//                Log.d("collect", csvFilePath)
//                // 保存CSV文件
//                saveCsvFile(csvFilePath)
//
//                // 追加数据到CSV文件
//                val newData = "New Data, "+ getCurrentDateStr("yy-MM-dd HH:mm:ss.SSS") +", "+ callBack.selectedText +"\n"
//                appendDataToCsvFile(csvFilePath, newData)
//
//                // 修改CSV文件中的数据
//                modifyCsvFile(csvFilePath, "John Doe", "Updated Age")
            }
            else -> item.intent?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.putExtra(Intent.EXTRA_PROCESS_TEXT, callBack.selectedText)
                    context.startActivity(it)
                }
            }
        }
    }

    private fun saveCsvFile(filePath: String) {
        Log.d("collect", "saveCsvFile")
        try {
            val file = File(filePath)
            val fileWriter = FileWriter(file)

            val writer = CSVWriter(fileWriter)
            val data = arrayOf("Name", "Age", "Email")
            writer.writeNext(data)

            // 添加示例数据
            val sampleData = arrayOf("John Doe", "25", "john@example.com")
            writer.writeNext(sampleData)

            writer.close()

            Log.d("collect", "saveCsvFile----success")

            // 文件保存成功
            context.toastOnUi("CSV file saved successfully")
        } catch (e: IOException) {
            Log.d("collect", "saveCsvFile----error")
            e.printStackTrace()
            // 处理保存文件失败的情况
            context.toastOnUi("Failed to save CSV file")
        }
    }

    private fun appendDataToCsvFile(filePath: String, newData: String) {
        try {
            val fileWriter = FileWriter(filePath, true)
            val writer = CSVWriter(fileWriter)

            // 将新数据追加到文件末尾
            writer.writeNext(newData.split(",").toTypedArray())

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


    @RequiresApi(Build.VERSION_CODES.M)
    private fun createProcessTextIntent(): Intent {
        return Intent()
            .setAction(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSupportedActivities(): List<ResolveInfo> {
        @Suppress("DEPRECATION")
        return context.packageManager
            .queryIntentActivities(createProcessTextIntent(), 0)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createProcessTextIntentForResolveInfo(info: ResolveInfo): Intent {
        return createProcessTextIntent()
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
            .setClassName(info.activityInfo.packageName, info.activityInfo.name)
    }

    /**
     * Start with a menu Item order value that is high enough
     * so that your "PROCESS_TEXT" menu items appear after the
     * standard selection menu items like Cut, Copy, Paste.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun onInitializeMenu(menu: Menu) {
        kotlin.runCatching {
            var menuItemOrder = 100
            for (resolveInfo in getSupportedActivities()) {
                menu.add(
                    Menu.NONE, Menu.NONE,
                    menuItemOrder++, resolveInfo.loadLabel(context.packageManager)
                ).intent = createProcessTextIntentForResolveInfo(resolveInfo)
            }
        }.onFailure {
            context.toastOnUi("获取文字操作菜单出错:${it.localizedMessage}")
        }
    }

    interface CallBack {
        val selectedText: String

        fun onMenuItemSelected(itemId: Int): Boolean

        fun onMenuActionFinally()
    }
}