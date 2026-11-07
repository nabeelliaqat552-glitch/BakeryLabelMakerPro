package com.bakerylabel.pro

import android.content.Context
import com.bakerylabel.pro.data.AppDatabase
import com.bakerylabel.pro.data.Repository
import com.bakerylabel.pro.data.SettingsRepository
import com.bakerylabel.pro.print.PrintQueue

/** Simple manual dependency container shared across the app. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val settings = SettingsRepository(context)
    val repo = Repository(context, db, settings)
    val printQueue = PrintQueue()
}
