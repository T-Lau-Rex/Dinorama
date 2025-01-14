package com.laura.quintodinorama.ui.fragment

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.FragmentFavoriteBinding
import com.laura.quintodinorama.databinding.ItemFavoriteBinding
import com.laura.quintodinorama.retrofit.entities.Dinosaur
import com.laura.quintodinorama.ui.activity.DinosaurDetailsActivity

class FavoriteFragment : Fragment() {

    // Variable initialization
    private var _binding: FragmentFavoriteBinding? = null
    private val binding: FragmentFavoriteBinding get() = _binding ?: throw IllegalStateException("Binding should not be null")

    private lateinit var mDinoramaRef: DatabaseReference
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>
    private lateinit var mGridLayoutManager: GridLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFirebase()
        setupAdapter()
        setupRecyclerView()
    }

    // Firebase database setup
    private fun setupFirebase() {
        mDinoramaRef = FirebaseDatabase.getInstance().reference.child(DinosaursApplication.PATH_DINOSAURS)
    }

    // Adapter setup
    private fun setupAdapter() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val query = mDinoramaRef.orderByChild("favorite/$userId").equalTo(true)

        val option = FirebaseRecyclerOptions.Builder<Dinosaur>().setQuery(query) {
            val dinosaur = it.getValue(Dinosaur::class.java)
            dinosaur!!.id = it.key!!
            dinosaur
        }.build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>(option) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DinosaurHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(R.layout.item_favorite, parent, false)

                return DinosaurHolder(view)
            }

            override fun onBindViewHolder(holder: DinosaurHolder, position: Int, model: Dinosaur) {
                val dinosaur = getItem(position)

                with(holder) {
                    setListener(dinosaur)

                    with(binding) {
                        tvNameFav.text = dinosaur.name
                        cbFav.isChecked = true

                        // Change the dinosaur's background according to its suborder
                        val colorBackground = DinosaursApplication.suborderColor[dinosaur.suborder]
                            ?: DinosaursApplication.suborderColor["eraDefault"]!!

                        val bkgrDrawable = viewBackground.background as? GradientDrawable
                        bkgrDrawable?.setColor(requireContext().getColor(colorBackground))

                        Glide.with(mContext)
                            .load(dinosaur.image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .fitCenter()
                            .into(imgDinoFav)
                    }
                }
            }
        }
    }

    // Set up the RecyclerView with grid layout
    private fun setupRecyclerView() {
        mGridLayoutManager = GridLayoutManager(context, 2)
        binding.rcvFavs.apply {
            layoutManager = mGridLayoutManager
            adapter = mFirebaseAdapter
        }
    }

    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    // Function to mark/unmark a dinosaur as favorite
    private fun setFavorite(dinosaur: Dinosaur, checked: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = mDinoramaRef.child(dinosaur.id)//.child("favorite")

        if (checked) {
            databaseReference.child("favorite").child(userId).setValue(true)
        } else {
            databaseReference.child("favorite").child(userId).setValue(null)
        }

        val favoriteRef = databaseReference.child("favorite")

        // Recalculate the favorites counter
        favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favoriteLength = snapshot.childrenCount
                databaseReference.child("favoriteCount").setValue(-favoriteLength)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("setFavorite", "Error counting favorites: ${error.message}")
            }
        })
    }

    inner class DinosaurHolder(view: View): RecyclerView.ViewHolder(view) {
        val binding = ItemFavoriteBinding.bind(view)

        fun setListener(dinosaur: Dinosaur) {
            binding.cbFav.setOnCheckedChangeListener { _, checked ->
                setFavorite(dinosaur, checked)
            }
            binding.cardFavorite.setOnClickListener {
                val context = it.context
                val intent = Intent(context, DinosaurDetailsActivity::class.java).apply {
                    putExtra("DINOSAUR_ID", dinosaur.id)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mFirebaseAdapter.stopListening()
        _binding = null
    }
}