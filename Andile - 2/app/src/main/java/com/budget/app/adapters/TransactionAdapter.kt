package com.budget.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.budget.app.R
import com.budget.app.models.Transaction
import com.budget.app.models.TransactionType
import com.budget.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    initialData: List<Transaction>,
    private val onDelete: ((Transaction) -> Unit)?,
    private val onDetails: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private val data: MutableList<Transaction> = initialData.toMutableList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle    : TextView    = view.findViewById(R.id.tvTitle)
        val tvAmount   : TextView    = view.findViewById(R.id.tvAmount)
        val tvCategory : TextView    = view.findViewById(R.id.tvCategory)
        val tvDate     : TextView    = view.findViewById(R.id.tvDate)
        val btnDelete  : ImageButton = view.findViewById(R.id.btnDelete)
        val btnDetails : ImageButton = view.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = data[position]
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        holder.tvTitle.text    = tx.title
        holder.tvCategory.text = tx.category
        holder.tvDate.text     = sdf.format(tx.date)

        val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
        holder.tvAmount.text = "$sign ${CurrencyFormatter.format(tx.amount)}"
        holder.tvAmount.setTextColor(
            holder.itemView.context.getColor(
                if (tx.type == TransactionType.INCOME) R.color.income_green else R.color.expense_red
            )
        )

        if (onDelete != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete.invoke(tx) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        if (onDetails != null) {
            holder.btnDetails.visibility = View.VISIBLE
            holder.btnDetails.setOnClickListener { onDetails.invoke(tx) }
        } else {
            holder.btnDetails.visibility = View.GONE
        }
    }

    fun updateData(newList: List<Transaction>) {
        data.clear()
        data.addAll(newList)
        notifyDataSetChanged()
    }
}
