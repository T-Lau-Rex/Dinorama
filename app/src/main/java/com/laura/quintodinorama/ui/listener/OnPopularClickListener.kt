package com.laura.quintodinorama.ui.listener

import com.laura.quintodinorama.retrofit.entities.Dinosaur

interface OnPopularClickListener {
    fun onClick(dinosaur: Dinosaur)
}