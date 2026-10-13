package com.example.s1client

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.s1client.util.S1Notify
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.example.s1client.databinding.ActivityImageViewerBinding
import com.example.s1client.databinding.ItemImageViewerBinding
import com.example.s1client.util.ImageDownloader
import kotlinx.coroutines.launch

class ImageViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var urls: List<String> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        urls = intent.getStringArrayListExtra("urls") ?: emptyList()
        currentIndex = intent.getIntExtra("position", 0).coerceIn(0, maxOf(0, urls.size - 1))

        if (urls.isEmpty()) { finish(); return }

        binding.vpImages.adapter = ImagePagerAdapter()
        binding.vpImages.setCurrentItem(currentIndex, false)
        updatePageInfo(currentIndex)

        binding.vpImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                updatePageInfo(position)
            }
        })

        binding.btnClose.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { saveCurrentImage() }
        binding.btnShare.setOnClickListener { shareCurrentImage() }
    }

    private fun updatePageInfo(index: Int) {
        binding.tvPageInfo.text = "${index + 1} / ${urls.size}"
    }

    private fun saveCurrentImage() {
        val url = urls.getOrNull(currentIndex) ?: return
        toast("正在保存...")
        lifecycleScope.launch {
            val filename = ImageDownloader.downloadToGallery(this@ImageViewerActivity, url)
            if (filename != null) {
                toast("已保存到相册：$filename", S1Notify.Type.SUCCESS)
            } else {
                toast("保存失败", S1Notify.Type.ERROR)
            }
        }
    }

    private fun shareCurrentImage() {
        val url = urls.getOrNull(currentIndex) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "分享链接"))
    }

    private inner class ImagePagerAdapter : RecyclerView.Adapter<ImagePagerAdapter.VH>() {
        inner class VH(val binding: ItemImageViewerBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemImageViewerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val url = urls[position]
            holder.binding.progressBar.visibility = View.VISIBLE
            holder.binding.tvLoadError.visibility = View.GONE
            holder.binding.ivZoom.load(url) {
                listener(
                    onSuccess = { _, _ ->
                        holder.binding.progressBar.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        holder.binding.progressBar.visibility = View.GONE
                        holder.binding.tvLoadError.visibility = View.VISIBLE
                    }
                )
            }
        }

        override fun getItemCount() = urls.size
    }
}
