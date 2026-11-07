package com.bakerylabel.pro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.data.ThemeMode
import com.bakerylabel.pro.ui.screens.BackupScreen
import com.bakerylabel.pro.ui.screens.BulkScreen
import com.bakerylabel.pro.ui.screens.CategoriesScreen
import com.bakerylabel.pro.ui.screens.DashboardScreen
import com.bakerylabel.pro.ui.screens.EditorScreen
import com.bakerylabel.pro.ui.screens.PrintScreen
import com.bakerylabel.pro.ui.screens.ProductsScreen
import com.bakerylabel.pro.ui.screens.SavedLabelsScreen
import com.bakerylabel.pro.ui.screens.SettingsScreen
import com.bakerylabel.pro.ui.screens.ShopScreen
import com.bakerylabel.pro.ui.screens.TemplatesScreen
import com.bakerylabel.pro.ui.theme.BakeryTheme

val LocalApp = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

object Routes {
    const val DASHBOARD = "dashboard"
    const val PRODUCTS = "products?filter={filter}"
    const val CATEGORIES = "categories"
    const val EDITOR = "editor?labelId={labelId}&productId={productId}&template={template}&tab={tab}"
    const val SAVED = "saved?select={select}"
    const val BULK = "bulk"
    const val PRINT = "print"
    const val TEMPLATES = "templates"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"

    fun products(filter: String = "all") = "products?filter=$filter"
    fun editor(labelId: Long = 0, productId: Long = 0, template: String = "", tab: Int = 0) =
        "editor?labelId=$labelId&productId=$productId&template=$template&tab=$tab"
    fun saved(select: Boolean = false) = "saved?select=$select"
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BakeryApp).container
        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CompositionLocalProvider(LocalApp provides container) {
                BakeryTheme(dark = dark) { AppNav() }
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    NavHost(
        navController = nav,
        startDestination = Routes.DASHBOARD,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) + fadeIn(tween(280)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260)) },
    ) {
        composable(Routes.DASHBOARD) { DashboardScreen(nav) }
        composable(
            Routes.PRODUCTS,
            arguments = listOf(navArgument("filter") { type = NavType.StringType; defaultValue = "all" }),
        ) { ProductsScreen(nav, it.arguments?.getString("filter") ?: "all", back) }
        composable(Routes.CATEGORIES) { CategoriesScreen(back) }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("labelId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("productId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("template") { type = NavType.StringType; defaultValue = "" },
                navArgument("tab") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) {
            val a = it.arguments
            EditorScreen(
                nav = nav,
                labelId = a?.getLong("labelId") ?: 0L,
                productId = a?.getLong("productId") ?: 0L,
                templateId = a?.getString("template") ?: "",
                initialTab = a?.getInt("tab") ?: 0,
                onBack = back,
            )
        }
        composable(
            Routes.SAVED,
            arguments = listOf(navArgument("select") { type = NavType.BoolType; defaultValue = false }),
        ) { SavedLabelsScreen(nav, it.arguments?.getBoolean("select") ?: false, back) }
        composable(Routes.BULK) { BulkScreen(nav, back) }
        composable(Routes.PRINT) { PrintScreen(nav, back) }
        composable(Routes.TEMPLATES) { TemplatesScreen(nav, back) }
        composable(Routes.SHOP) { ShopScreen(back) }
        composable(Routes.SETTINGS) { SettingsScreen(nav, back) }
        composable(Routes.BACKUP) { BackupScreen(back) }
    }
}
