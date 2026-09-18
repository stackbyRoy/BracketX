package com.bracketx.ui

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.bracketx.data.repository.RepositoryProvider
import com.bracketx.ui.screens.AuthScreen
import com.bracketx.ui.screens.CreateTournamentScreen
import com.bracketx.ui.screens.HomeScreen
import com.bracketx.ui.screens.ProfileScreen
import com.bracketx.ui.screens.TournamentDetailScreen
import com.bracketx.ui.theme.AccentBlue
import com.bracketx.ui.theme.BackgroundDark
import com.bracketx.ui.theme.PrimaryText
import com.bracketx.ui.theme.SecondaryText
import com.bracketx.ui.theme.SurfaceDark

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Create : Screen("create", "Create")
    object Profile : Screen("profile", "Profile")
    object Auth : Screen("auth", "Sign In")
    object TournamentDetail : Screen("tournament/{tournamentId}", "Tournament") {
        fun createRoute(tournamentId: String) = "tournament/$tournamentId"
    }
}

@Composable
fun BracketXApp(
    incomingIntent: Intent? = null,
    navController: NavHostController = rememberNavController()
) {
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()

    val deepLinkTournamentId = remember(incomingIntent) {
        incomingIntent?.data?.let { uri ->
            when {
                uri.scheme == "bracketx" && (uri.host == "tournament" || uri.host == "t") -> {
                    uri.pathSegments.firstOrNull() ?: uri.lastPathSegment
                }
                uri.path?.contains("/tournament/") == true -> {
                    uri.lastPathSegment
                }
                uri.path?.contains("/t/") == true -> {
                    uri.lastPathSegment
                }
                else -> null
            }
        }
    }

    // Handle runtime intent navigation (e.g. from onNewIntent or initial intent)
    LaunchedEffect(deepLinkTournamentId) {
        if (!deepLinkTournamentId.isNullOrBlank()) {
            navController.navigate(Screen.TournamentDetail.createRoute(deepLinkTournamentId)) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.Create.route, Screen.Profile.route)

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = SurfaceDark) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = SurfaceDark
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Create") },
                        label = { Text("Create") },
                        selected = currentRoute == Screen.Create.route,
                        onClick = {
                            if (currentUser == null) {
                                navController.navigate(Screen.Auth.route)
                            } else {
                                navController.navigate(Screen.Create.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = SurfaceDark
                        )
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text(if (currentUser == null) "Sign In" else "Profile") },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            if (currentUser == null) {
                                navController.navigate(Screen.Auth.route)
                            } else {
                                navController.navigate(Screen.Profile.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = SurfaceDark
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (currentUser == null && deepLinkTournamentId == null) Screen.Auth.route else Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen()
                // When authentication completes, navigate back or to Home
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTournament = { tournamentId ->
                        navController.navigate(Screen.TournamentDetail.createRoute(tournamentId))
                    },
                    onNavigateToCreate = {
                        if (currentUser == null) {
                            navController.navigate(Screen.Auth.route)
                        } else {
                            navController.navigate(Screen.Create.route)
                        }
                    }
                )
            }

            composable(Screen.Create.route) {
                CreateTournamentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTournamentCreated = { tournamentId ->
                        navController.navigate(Screen.TournamentDetail.createRoute(tournamentId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen()
            }

            composable(
                route = Screen.TournamentDetail.route,
                arguments = listOf(navArgument("tournamentId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://bracketx.vercel.app/t/{tournamentId}" },
                    navDeepLink { uriPattern = "https://*.vercel.app/t/{tournamentId}" },
                    navDeepLink { uriPattern = "http://bracketx.vercel.app/t/{tournamentId}" },
                    navDeepLink { uriPattern = "http://*.vercel.app/t/{tournamentId}" },
                    navDeepLink { uriPattern = "bracketx://t/{tournamentId}" },
                    navDeepLink { uriPattern = "https://bracketx.vercel.app/tournament/{tournamentId}" },
                    navDeepLink { uriPattern = "https://*.vercel.app/tournament/{tournamentId}" },
                    navDeepLink { uriPattern = "http://bracketx.vercel.app/tournament/{tournamentId}" },
                    navDeepLink { uriPattern = "http://*.vercel.app/tournament/{tournamentId}" },
                    navDeepLink { uriPattern = "bracketx://tournament/{tournamentId}" }
                )
            ) { backStackEntry ->
                val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                TournamentDetailScreen(
                    tournamentId = tournamentId,
                    onNavigateBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onRequireAuth = {
                        navController.navigate(Screen.Auth.route)
                    }
                )
            }
        }
    }
}
