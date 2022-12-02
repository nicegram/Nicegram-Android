package app.nicegram

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.appvillis.nicegram.R
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.databinding.ItemAddNewBinding
import org.telegram.messenger.databinding.ItemQuickReplyBinding
import org.telegram.messenger.databinding.ViewQuickRepliesBinding
import org.telegram.ui.ActionBar.Theme

class QuickRepliesView(context: Context) : FrameLayout(context) {

    val binding: ViewQuickRepliesBinding

    init {
        binding = ViewQuickRepliesBinding.inflate(LayoutInflater.from(context), this, true)

        binding.desc.text = AndroidUtilities.replaceTags(LocaleController.getString(R.string.QuickRepliesDesc))

        binding.recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding.recyclerView.itemAnimator = null

        applyTheme()
    }

    private fun applyTheme() {
        binding.desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, null, true))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.recyclerView.backgroundTintList = ColorStateList.valueOf(Theme.getColor(Theme.key_windowBackgroundWhite, null, true))
        }
    }

    fun setQuickReplies(replies: List<QuickReply>, listener: (allItems: List<QuickReply>) -> Unit) {
        binding.recyclerView.adapter = QuickRepliesAdapter(LayoutInflater.from(context), replies, {
            (binding.recyclerView.adapter as QuickRepliesAdapter).removeAt(it)
        }, listener)
    }

    private class QuickRepliesAdapter(val inflater: LayoutInflater, items: List<QuickReply>, val deleteListener: (pos: Int) -> Unit, val listener: (newItems: List<QuickReply>) -> Unit) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val VIEW_TYPE_ADD = 0
            const val VIEW_TYPE_REPLY = 1
        }

        private val items = mutableListOf<QuickReply>()
        var requestFocusPosition = -1
        var currentFocusPos = -1

        init {
            this.items.addAll(items)
        }

        fun removeAt(i: Int) {
            items.removeAt(i - 1)
            //notifyItemRemoved(i)
            notifyDataSetChanged()
            listener.invoke(items)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_ADD) QuickReplyAddVH(
                ItemAddNewBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            ).apply {
                binding.divider.setBackgroundColor(Theme.getColor(Theme.key_divider))
                itemView.setOnClickListener {
                    if (items.size == 0 || (items[0].text != null && !items[0].text.equals(""))) {
                        items.add(0, QuickReply(-1, null))
                        requestFocusPosition = 1
                        currentFocusPos = -1
                        //notifyItemInserted(1)
                        notifyDataSetChanged()
                    }
                }
            } else QuickReplyVH(ItemQuickReplyBinding.inflate(inflater, parent, false)).apply {
                binding.editText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
                binding.editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3))
                binding.divider.setBackgroundColor(Theme.getColor(Theme.key_divider))
                binding.editText.addTextChangedListener {
                    item?.text = it.toString()
                    listener.invoke(items)
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is QuickReplyVH) {
                val item = items[position - 1]
                holder.bind(item)
                holder.binding.divider.isVisible = position != itemCount - 1

                holder.binding.removeBtn.setOnClickListener {
                    if (holder.adapterPosition == currentFocusPos) {
                        requestFocusPosition = 1
                        currentFocusPos = -1
                    } else {
                        if (holder.adapterPosition < currentFocusPos) currentFocusPos--
                    }
                    deleteListener(holder.adapterPosition)
                }

                if (position == requestFocusPosition) {
                    requestFocusPosition = -1
                    holder.binding.editText.requestFocus()
                    holder.binding.editText.setSelection(holder.binding.editText.length())
                } else if (position == currentFocusPos) {
                    holder.binding.editText.requestFocus()
                    holder.binding.editText.setSelection(holder.binding.editText.length())
                }

                holder.binding.editText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && holder.adapterPosition > 0) currentFocusPos = holder.adapterPosition
                }
            } else if (holder is QuickReplyAddVH) {
                holder.binding.divider.isVisible = position != itemCount - 1
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_REPLY
        }

        override fun getItemCount() = items.size + 1
    }

    private class QuickReplyVH(val binding: ItemQuickReplyBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var item: QuickReply? = null

        fun bind(item: QuickReply) {
            this.item = item
            binding.editText.setText(item.text)
        }
    }

    private class QuickReplyAddVH(val binding: ItemAddNewBinding) :
        RecyclerView.ViewHolder(binding.root)

    class QuickReply(val id: Int, var text: String?)
}