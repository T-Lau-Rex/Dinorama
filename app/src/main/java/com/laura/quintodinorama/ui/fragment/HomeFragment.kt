package com.laura.quintodinorama.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.FragmentHomeBinding
import com.laura.quintodinorama.databinding.ItemFavoriteBinding
import com.laura.quintodinorama.retrofit.entities.Dinosaur
import com.laura.quintodinorama.ui.activity.DinosaurDetailsActivity
import java.util.Calendar

class HomeFragment : Fragment() {

    // Variable initialization
    private var _binding: FragmentHomeBinding? = null
    private val mBinding: FragmentHomeBinding get() = _binding ?: throw IllegalStateException("Binding should not be null")

    private lateinit var mDinoramaRef: DatabaseReference
    private lateinit var mCuriositiesRef: DatabaseReference
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>
    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFirebase()
        setupAdapter()
        setupCuriosity()
        setupRecyclerView()
    }

    // Firebase database setup
    private fun setupFirebase() {
        mDinoramaRef = FirebaseDatabase.getInstance().reference.child(DinosaursApplication.PATH_DINOSAURS)
        mCuriositiesRef = FirebaseDatabase.getInstance().reference.child(DinosaursApplication.CURIOSITIES)
    }

    // Adapter setup
    private fun setupAdapter() {
        val query = mDinoramaRef.orderByChild("favoriteCount").limitToFirst(10)

        val options = FirebaseRecyclerOptions.Builder<Dinosaur>().setQuery(query) {
            val dinosaur = it.getValue(Dinosaur::class.java)
            dinosaur!!.id = it.key!!
            dinosaur
        }.build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DinosaurHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(R.layout.item_favorite, parent,false)

                return  DinosaurHolder(view)
            }

            override fun onBindViewHolder(holder: DinosaurHolder, position: Int, model: Dinosaur) {
                val dinosaur = getItem(position)

                with(holder) {
                    setListener(dinosaur)

                    with(binding) {
                        tvNameFav.text = dinosaur.name
                        cbFav.text = dinosaur.favorite.keys.size.toString()
                        FirebaseAuth.getInstance().currentUser?.let {
                            cbFav.isChecked = dinosaur.favorite.containsKey(it.uid)
                        }

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

            override fun onError(error: DatabaseError) {
                super.onError(error)

                Snackbar.make(mBinding.root, error.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Curiosity of the day setup
    private fun setupCuriosity() {
        mCuriositiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val curiosity = snapshot.children.sortedBy { it.key?.toIntOrNull() }
                    val totalCuriosities = curiosity.size

                    if (totalCuriosities > 0) {
                        // Calculates the index based on the day of the year
                        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                        val curiosityIndex = dayOfYear % totalCuriosities

                        val dayCuriosity = curiosity[curiosityIndex]
                        val description = dayCuriosity.child("description").value.toString()
                        val title = dayCuriosity.child("title").value.toString()
                        val image = dayCuriosity.child("image").value.toString()

                        mBinding.tvTitleCurioso.text = title
                        mBinding.tvInfoCurioso.text = description
                        Glide.with(this@HomeFragment)
                            .load(image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(mBinding.imgCurioso)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(mBinding.root, "Error: ${error.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    // Set up the RecyclerView
    private fun setupRecyclerView() {
        mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        mBinding.rcvFavs.apply {
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
            setHasFixedSize(true)
        }
    }

    override fun onStart() {
        super.onStart()
        mFirebaseAdapter.startListening()
    }

    // Function to mark/unmark a dinosaur as favorite
    private fun setFavorite(dinosaur: Dinosaur, checked: Boolean) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = mDinoramaRef.child(dinosaur.id)

        if (checked) {
            databaseReference.child("favorite").child(userId).setValue(true)
        } else {
            databaseReference.child("favorite").child(userId).setValue(null)

        }
        val favoriteRef = databaseReference.child("favorite")

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

    inner class DinosaurHolder(view: View): RecyclerView.ViewHolder(view){
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