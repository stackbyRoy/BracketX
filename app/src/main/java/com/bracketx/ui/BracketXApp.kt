package com.bracketx.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    object TournamentDetail : Screen("tournament/{tournamentId}", "Tournament") {
        fun createRoute(tournamentId: String) = "tournament/$tournamentId"
    }
}

@Composable
fun BracketXApp(navController: NavHostController = rememberNavController()) {
    val currentUser by RepositoryProvider.authRepository.currentUser.collectAsState()

    if (currentUser == null) {
        AuthScreen()
    } else {
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
                                navController.navigate(Screen.Create.route) {
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
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile") },
                            selected = currentRoute == Screen.Profile.route,
                            onClick = {
                                navController.navigate(Screen.Profile.route) {
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
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToTournament = { tournamentId ->
                            navController.navigate(Screen.TournamentDetail.createRoute(tournamentId))
                        },
                        onNavigateToCreate = {
                            navController.navigate(Screen.Create.route)
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
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                    TournamentDetailScreen(
                        tournamentId = tournamentId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
