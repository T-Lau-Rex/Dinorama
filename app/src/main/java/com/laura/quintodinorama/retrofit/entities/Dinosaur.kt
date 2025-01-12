package com.laura.quintodinorama.retrofit.entities

import androidx.room.PrimaryKey
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Dinosaur(@PrimaryKey(autoGenerate = true) var id: String = "",
    var name: String = "",
    var era: String = "",
    var order: String = "",
    var suborder: String = "",
    var feeding: String = "",
    var description: String = "",
    var size: String = "",
    var weight: String = "",
    val image: String = "",
    val detail_image: String = "",
    val favoriteCount: Int = 0 ,
    val favorite: Map<String, Boolean> = mutableMapOf()
)