package ru.saikodev.initial.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reaction(
    val emoji: String = "",
    val count: Int = 0,
    val byMe: Boolean = false
) : Parcelable
