package com.orange.molkky

//https://stackoverflow.com/a/48769967
infix fun <T> Collection<T>.sameContentWith(collection: Collection<T>?)
        = collection?.let { this.size == it.size && this.containsAll(it) }