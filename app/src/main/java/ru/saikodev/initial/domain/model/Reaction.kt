package ru.saikodev.initial.domain.model

data class Reaction(
    val emoji: String = "",
    val count: Int = 0,
    val byMe: Boolean = false
)
