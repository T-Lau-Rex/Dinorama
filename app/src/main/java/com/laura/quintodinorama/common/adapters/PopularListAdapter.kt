package com.laura.quintodinorama.common.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.ItemFavoriteBinding
import com.laura.quintodinorama.retrofit.entities.Dinosaur
import com.laura.quintodinorama.ui.listener.OnPopularClickListener

class PopularListAdapter(private var listener: OnPopularClickListener): ListAdapter<Dinosaur,RecyclerView.ViewHolder>(PopularDiffCallback()) {

    private lateinit var context: Context
    private lateinit var mFirebaseListAdapter: FirebaseRecyclerAdapter<Dinosaur, ViewHolder>

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val  binding = ItemFavoriteBinding.bind(view)

        fun setListener(dinosaur: Dinosaur){
            binding.root.setOnClickListener { listener.onClick(dinosaur) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dinosaur = getItem(position)

        with(holder as ViewHolder) {
            setListener(dinosaur)

            with(binding) {
                tvNameFav.text = dinosaur.name
                Glide.with(context)
                    .load(dinosaur.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(imgDinoFav)
            }
        }
    }
}


class PopularDiffCallback: DiffUtil.ItemCallback<Dinosaur>()
{
    override fun areItemsTheSame(oldItem: Dinosaur, newItem: Dinosaur): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Dinosaur, newItem: Dinosaur): Boolean {
        return oldItem == newItem
    }
}