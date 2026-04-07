package ru.saikodev.initial.domain.model

data class LinkPreview(
    val url: String = "",
    val title: String? = null,
    val description: String? = null,
    val image: String? = null,
    val domain: String? = null,
    val siteName: String? = null,
    val embedType: String? = null
)
