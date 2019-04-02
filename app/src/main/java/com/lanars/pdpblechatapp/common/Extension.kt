package com.lanars.pdpblechatapp.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

fun ViewGroup.inflate(layoutID: Int, attachToRoot: Boolean = false): View =
        LayoutInflater.from(context).inflate(layoutID, this, attachToRoot)