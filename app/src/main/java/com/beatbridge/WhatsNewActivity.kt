package com.beatbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beatbridge.databinding.ActivityWhatsNewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WhatsNewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWhatsNewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWhatsNewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "What's New"

        val changelogs = loadChangelogs()
        binding.rvChangelogs.layoutManager = LinearLayoutManager(this)
        binding.rvChangelogs.adapter = ChangelogAdapter(changelogs)
        binding.rvChangelogs.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL),
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadChangelogs(): List<ChangelogEntry> {
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return (assets.list("changelogs") ?: emptyArray())
            .mapNotNull { filename ->
                val timestamp = filename.removeSuffix(".txt").toLongOrNull() ?: return@mapNotNull null
                val content = assets.open("changelogs/$filename").bufferedReader().readText().trim()
                if (content.isEmpty()) return@mapNotNull null
                ChangelogEntry(
                    date = dateFormat.format(Date(timestamp * 1000L)),
                    content = content,
                    timestamp = timestamp,
                )
            }
            .sortedByDescending { it.timestamp }
    }
}

data class ChangelogEntry(val date: String, val content: String, val timestamp: Long)

class ChangelogAdapter(private val items: List<ChangelogEntry>) :
    RecyclerView.Adapter<ChangelogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tv_changelog_date)
        val tvContent: TextView = view.findViewById(R.id.tv_changelog_content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_changelog, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDate.text = item.date
        holder.tvContent.text = item.content
    }

    override fun getItemCount() = items.size
}
