package com.budget.app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.models.Transaction
import com.budget.app.utils.AppData
import com.budget.app.utils.CurrencyFormatter

class AttachmentsFragment : Fragment(), MainActivity.OnBackPressedListener {

    private lateinit var rv: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_attachments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvAttachments)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyAttachments)

        val attachments = AppData.getTransactionsWithAttachments()

        if (attachments.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = AttachmentsAdapter(attachments) { transaction ->
                viewAttachment(transaction)
            }
        }
    }

    override fun onBackPressed(): Boolean {
        if (::rv.isInitialized && rv.computeVerticalScrollOffset() > 0) {
            rv.smoothScrollToPosition(0)
            return true
        }
        return false
    }

    private fun viewAttachment(transaction: Transaction) {
        try {
            val uri = Uri.parse(transaction.attachmentUri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, requireContext().contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private class AttachmentsAdapter(
        private val items: List<Transaction>,
        private val onItemClick: (Transaction) -> Unit
    ) : RecyclerView.Adapter<AttachmentsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFileName: TextView = view.findViewById(R.id.tvAttachFileName)
            val tvInfo: TextView = view.findViewById(R.id.tvAttachTransactionInfo)
            val btnView: View = view.findViewById(R.id.btnViewAttachment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attachment, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvFileName.text = item.attachmentName ?: "Unnamed File"
            holder.tvInfo.text = "${item.title} - ${CurrencyFormatter.format(item.amount)}"
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.btnView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
