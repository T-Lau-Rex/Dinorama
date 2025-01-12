package com.laura.quintodinorama.ui.fragment

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
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
import com.google.firebase.database.ValueEventListener
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.FragmentDinosaursBinding
import com.laura.quintodinorama.databinding.ItemDinosaursBinding
import com.laura.quintodinorama.retrofit.entities.Dinosaur
import com.laura.quintodinorama.ui.activity.DinosaurDetailsActivity

class DinosaursFragment : Fragment() {

    // Variable initialization
    private var _binding: FragmentDinosaursBinding? = null
    private val mBinding: FragmentDinosaursBinding get() = _binding ?: throw IllegalStateException("Binding should not be null")

    private lateinit var mDinoramaRef: DatabaseReference
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>
    private lateinit var mLayoutManager: LayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDinosaursBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUIComponents()
        setupFirebase()
        setupAdapter()
        setupRcvDinos()
        setupSearch() // TODO
    }

    // Setup views and UI control behavior
    private fun setupUIComponents() {
        // Setup chips to control the visibility of filters
        mBinding.chipEpoca.setOnCheckedChangeListener { _, isChecked ->
            mBinding.clLineaTemporal.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateConstraints()
        }
        mBinding.chipOrden.setOnCheckedChangeListener { _, isChecked ->
            mBinding.clOptionsOrden.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateConstraints()
        }
        mBinding.chipDieta.setOnCheckedChangeListener { _, isChecked ->
            mBinding.llSwitchDiet.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateConstraints()
        }

        // Setup switch to show suborders
        mBinding.switchOrden.setOnCheckedChangeListener { _, isChecked ->
            mBinding.llBtnsSaurischia.visibility = if (isChecked) View.GONE else View.VISIBLE
            mBinding.llBtnsOrnitischia.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }
    // Update layout constraints
    private fun updateConstraints() {
        val constraintBottom = mBinding.root.findViewById<ConstraintLayout>(R.id.contenedor)
        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintBottom)

        if (mBinding.chipEpoca.isChecked && !mBinding.chipDieta.isChecked) {
            constraintSet.clear(R.id.clOptionsOrden, ConstraintSet.BOTTOM)
        } else {
            constraintSet.connect(
                R.id.clOptionsOrden,
                ConstraintSet.BOTTOM,
                R.id.chipDieta,
                ConstraintSet.TOP
            )
        }
        constraintSet.applyTo(constraintBottom)
    }
    // Firebase database setup
    private fun setupFirebase() {
        mDinoramaRef = FirebaseDatabase.getInstance().reference.child(DinosaursApplication.PATH_DINOSAURS)
    }

    // RecyclerView adapter setup
    private fun setupAdapter() {
        val query = mDinoramaRef.orderByChild("name")

        val options = FirebaseRecyclerOptions.Builder<Dinosaur>()
            .setQuery(query) {
                val dinosaur = it.getValue(Dinosaur::class.java)
                dinosaur!!.id = it.key!!
                dinosaur
            }.build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Dinosaur, DinosaurHolder>(options) {
            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DinosaurHolder {
                mContext = parent.context
                val view = LayoutInflater.from(mContext).inflate(R.layout.item_dinosaurs, parent,false)
                return DinosaurHolder(view)
            }

            override fun onBindViewHolder(holder: DinosaurHolder, position: Int, model: Dinosaur) {
                val dinosaur = getItem(position)
                holder.bind(dinosaur)
            }
            override fun onError(error: DatabaseError) {
                super.onError(error)
                Snackbar.make(mBinding.root, error.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // RecyclerView setup
    private fun setupRcvDinos() {
        mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        mBinding.rcvDinosaurs.apply {
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
            setHasFixedSize(true)
        }
    }

    // TODO: Search setup. Not yet functional
    private fun setupSearch() {
        val textColor = mBinding.etSearch.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        textColor.setTextColor(ContextCompat.getColor(requireContext(), R.color.main_light))

        mBinding.etSearch.setOnQueryTextListener(object  : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("onQueryTextSubmit", "Query: $query")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("SearchView", "Query: $newText")
                return true
            }
        })
        mBinding.etSearch.setOnCloseListener {
            filterDinosaursByName("")
            true
        }
    }

    // TODO: Dinosaur filtering by name. Not yet functional
    private fun filterDinosaursByName(queryText: String) {
        val query = if (queryText.isEmpty()) {
            mDinoramaRef.orderByChild("name")
        } else {
            mDinoramaRef.orderByChild("name").startAt(queryText).endAt(queryText + "\uf8ff")

        }

        val options = FirebaseRecyclerOptions.Builder<Dinosaur>()
            .setQuery(query) { snapshot ->
                val dinosaur = snapshot.getValue(Dinosaur::class.java)
                dinosaur?.id = snapshot.key ?: ""
                dinosaur!!
            }.build()

        mFirebaseAdapter.updateOptions(options)
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
        val binding = ItemDinosaursBinding.bind(view)

        fun bind(dinosaur: Dinosaur) {
            setListener(dinosaur)

            with(binding) {
                tvNameDino.text = dinosaur.name
                tvDietDino.text = dinosaur.feeding
                tvEraDino.text = dinosaur.era
                cbFav.text = dinosaur.favorite.keys.size.toString()
                FirebaseAuth.getInstance().currentUser?.let {
                    cbFav.isChecked = dinosaur.favorite.containsKey(it.uid)
                }

                val colorBackground = DinosaursApplication.suborderColor[dinosaur.suborder]
                    ?: DinosaursApplication.suborderColor["eraDefault"]!!

                val bkgrDrawable = viewBackground.background as? GradientDrawable
                bkgrDrawable?.setColor(requireContext().getColor(colorBackground))

                Glide.with(this@DinosaursFragment)
                    .load(dinosaur.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(imgDinoFav)
            }
        }

        fun setListener(dinosaur: Dinosaur) {
            binding.cbFav.setOnCheckedChangeListener { _, checked ->
                setFavorite(dinosaur, checked)
            }
            binding.clItemDino.setOnClickListener {
                val context = it.context
                val intent = Intent(context, DinosaurDetailsActivity::class.java).apply {
                    putExtra("DINOSAUR_ID", dinosaur.id)
                }
                context.startActivity(intent)
            }
        }
    }

    // Stop the adapter's listener when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        mFirebaseAdapter.stopListening()
        _binding = null
    }
}