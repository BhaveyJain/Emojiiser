package com.example.emojiiser

import android.view.LayoutInflater
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions

class UserAdapter(private val context: Context, private val options: FirestoreRecyclerOptions<User>) : FirestoreRecyclerAdapter<User, UserAdapter.ViewHolder>(options) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(android.R.id.text1)
        val tvEmojis: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(model: User) {
            tvName.text = model.displayName
            tvEmojis.text = model.emojis
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false))

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: User) {
        holder.bind(model)
    }
}
