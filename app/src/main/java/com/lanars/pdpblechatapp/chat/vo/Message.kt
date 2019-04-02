package com.lanars.pdpblechatapp.chat.vo

import java.util.*

data class Message(val id: UUID = UUID.randomUUID(),
                   val message: String,
                   val isOutComing: Boolean)