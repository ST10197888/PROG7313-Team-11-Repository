package com.budget.app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.budget.app.R
import com.budget.app.activities.MainActivity
import com.budget.app.models.Transaction
import com.budget.app.models.TransactionType
import com.budget.app.utils.AppData
import com.budget.app.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionDetailsFragment : Fragment() {

    private var transactionId: Int = -1

    companion object {
        fun newInstance(id: Int): TransactionDetailsFragment {
            val fragment = TransactionDetailsFragment()
            val args = Bundle()
            args.putInt("transaction_id", id)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transactionId = arguments?.getInt("transaction_id") ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_transaction_details, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tx = AppData.getAllTransactions().find { it.id == transactionId }
        if (tx == null) {
            Toast.makeText(requireContext(), "Transaction not found", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.navigateTo(R.id.nav_transactions)
            return
        }

        view.findViewById<TextView>(R.id.tvValTitle).text = tx.title
        view.findViewById<TextView>(R.id.tvValAmount).text = CurrencyFormatter.format(tx.amount)
        view.findViewById<TextView>(R.id.tvValType).text = tx.type.name
        view.findViewById<TextView>(R.id.tvValCategory).text = tx.category
        view.findViewById<TextView>(R.id.tvValNotes).text = tx.notes.ifEmpty { "No notes provided" }
        
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        view.findViewById<TextView>(R.id.tvValDate).text = sdf.format(tx.date)

        val amountTv = view.findViewById<TextView>(R.id.tvValAmount)
        amountTv.setTextColor(requireContext().getColor(
            if (tx.type == TransactionType.INCOME) R.color.income_green else R.color.expense_red
        ))

        // Attachment section
        val layoutAttach = view.findViewById<View>(R.id.layoutDetailsAttachment)
        if (tx.attachmentUri != null) {
            layoutAttach.visibility = View.VISIBLE
            view.findViewById<Button>(R.id.btnViewDetailsAttach).setOnClickListener {
                openAttachment(tx.attachmentUri)
            }
        }

        view.findViewById<Button>(R.id.btnBackToAll).setOnClickListener {
            (activity as? MainActivity)?.loadFragment(TransactionsFragment())
        }
    }

    private fun openAttachment(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, requireContext().contentResolver.getType(uri) ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open document", Toast.LENGTH_SHORT).show()
        }
    }
}
