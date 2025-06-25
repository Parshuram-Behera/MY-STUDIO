import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.*
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap


/**
 * Data class to represent a file item in our list.
 */
data class FileItem(
    val id: String, // Unique ID for deletion
    val icon: ImageVector,
    val name: String,
    val size: String
)

/**
 * The main Composable function for the application.
 * It sets up the Material Design theme, a Modal Navigation Drawer,
 * a Scaffold with a TopAppBar, and displays different screens based on navigation selection.
 */


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Home") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5),
            surface = Color.White,
            background = Color(0xFFF5F5F5)
        )
    ) {
        ModalNavigationDrawer(
            drawerContent = {
                DrawerContent(
                    selectedScreen = currentScreen,
                    onScreenSelected = { screen ->
                        currentScreen = screen
                        scope.launch { drawerState.close() }
                    }
                )
            },
            drawerState = drawerState,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    // CONDITIONALLY RENDER THE MAIN APP TOP APP BAR
                    if (currentScreen != "Send" && currentScreen != "Settings" && currentScreen != "Profile") { // Only show if not on the SendScreen
                        TopAppBar(
                            title = { Text("Dashboard", style = MaterialTheme.typography.headlineSmall) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    scope.launch {
                                        if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Toggle Navigation Drawer")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    when (currentScreen) {
                        "Home" -> HomeScreen(
                            onNavigateToSend = { currentScreen = "Send" }
                        )

                        "Send" -> SendScreen(
                            onNavigateBack = { currentScreen = "Home" }
                        )
                        //"Settings" -> Text("Settings Screen", style = MaterialTheme.typography.bodyLarge)
                        "Settings" -> SettingScreen(
                            onNavigateBack = { currentScreen = "Home" }
                        )

                        "Profile" -> ProfileScreen(
                            onNavigateBack = { currentScreen = "Home" }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable function for the content of the navigation drawer.
 * Displays a list of navigation items.
 *
 * @param selectedScreen The currently selected screen, used to highlight the active item.
 * @param onScreenSelected Callback function invoked when a navigation item is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(selectedScreen: String, onScreenSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            //.padding(16.dp) tool bar padding
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // "Menu" text removed as per previous request
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = selectedScreen == "Home",
            onClick = { onScreenSelected("Home") },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFFFFC1CC)
            )
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = selectedScreen == "Settings",
            onClick = { onScreenSelected("Settings") },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFFFFC1CC)
            )
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = selectedScreen == "Profile",
            onClick = { onScreenSelected("Profile") },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFFFFC1CC)
            )
        )
    }
}

/**
 * Composable function for the Home Screen, demonstrating a TabRow with HorizontalPager.
 * It now includes a Floating Action Button on "Tab 1" and toggles two circular buttons with slide animations.
 * @param onNavigateToSend Callback to navigate to the SendScreen.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(onNavigateToSend: () -> Unit) {
    val tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4")
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    var showCircles by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, style = MaterialTheme.typography.bodyMedium) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (page == 0) {
                    FloatingActionButton(
                        onClick = {
                            showCircles = !showCircles
                            println("FAB clicked! showCircles: $showCircles")
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add new item")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Send Circle (Deep Green) with slide-in from top
                        AnimatedVisibility(
                            visible = showCircles,
                            enter = slideInVertically { -it } + fadeIn(),
                            exit = slideOutVertically { -it } + fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF006400))
                                        .clickable {
                                            onNavigateToSend()
                                            showCircles = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Send", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Receive Circle (Light Blue) with slide-in from bottom
                        AnimatedVisibility(
                            visible = showCircles,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFADD8E6))
                                        .clickable { /* Handle Receive click */ println("Receive clicked!") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = "Receive",
                                        tint = Color.Black,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Receive", color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn()
@Composable
fun SettingScreen(onNavigateBack: () -> Unit) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "back icon")
                    }
                },

                )
        }

    ) {

        // Space for Settings Screen
    }
}

/*@Composable
fun ProfileScreen(onNavigateBack: () -> Unit){


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile" , style = MaterialTheme.typography.headlineSmall)},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack){
                        Icon(Icons.Default.ArrowBack,"back Icon")
                    }
                }
            )
        }

    ){

        // Space for Profile Screen
    }
}*/


/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Box (50% width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder content
                Text(
                    text = "Left Section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // Right Box (50% width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder content
                Text(
                    text = "Right Section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
    }
}*/


/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Box (50% width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Left Section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // Right Section (50% width, divided vertically)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Box (50% height)
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Top Right Section",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }

                // Bottom Box (50% height)
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bottom Right Section",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}*/


/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    // State to manage WebSocket server
    var serverStatus by remember { mutableStateOf("Server Stopped") }
    var receivedMessage by remember { mutableStateOf("") }
    var server: ApplicationEngine? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    // Function to start the WebSocket server
    fun startWebSocketServer() {
        server?.stop() // Stop any existing server
        server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                webSocket("/receiver") {
                    serverStatus = "Server Started on ${InetAddress.getLocalHost().hostAddress}:8080"
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    receivedMessage = "Received: $text"
                                    send(Frame.Text("Echo: $text"))
                                }
                                else -> {
                                    // Ignore non-text frames for simplicity
                                }
                            }
                        }
                    } finally {
                        serverStatus = "Server Stopped"
                        receivedMessage = ""
                    }
                }
            }
        }.start(wait = false)
    }

    // Function to stop the WebSocket server
    fun stopWebSocketServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 1000)
        server = null
        serverStatus = "Server Stopped"
        receivedMessage = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Box (50% width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Left Section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // Right Section (50% width, divided vertically)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Box (50% height) - Search for Receiver Button
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                if Arrays.asList([serverStatus, receivedMessage])
                                if (server == null) {
                                    startWebSocketServer()
                                } else {
                                    stopWebSocketServer()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (server == null) "Search for Receiver" else "Stop Server",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Bottom Box (50% height) - Server Status and Messages
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = serverStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (receivedMessage.isNotEmpty()) {
                            Text(
                                text = receivedMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}*/


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    // State to manage WebSocket server
    var serverStatus by remember { mutableStateOf("Server Stopped") }
    var server: ApplicationEngine? by remember { mutableStateOf(null) }
    val connectedDevices = remember { mutableStateListOf<String>() }
    val statusMessages = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val clientSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    // App-specific identifier for authentication
    val APP_IDENTIFIER = "MY_STUDIO_APP"

    // Function to get the local IP address for the Wi-Fi hotspot
    fun getLocalIpAddress(): String {
        try {
            return InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            return "Unknown IP"
        }
    }

    // Function to send a pairing request to a specific device
    fun sendPairingRequest(deviceId: String) {
        scope.launch {
            clientSessions[deviceId]?.let { session ->
                try {
                    session.send(Frame.Text("PAIR_REQUEST"))
                    statusMessages.add("Sent pairing request to $deviceId")
                } catch (e: Exception) {
                    statusMessages.add("Failed to send pairing request to $deviceId: ${e.message}")
                }
            } ?: statusMessages.add("Device $deviceId not found")
        }
    }

    // Function to start the WebSocket server
    fun startWebSocketServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 1000) // Stop any existing server
        try {
            server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
                install(WebSockets)
                routing {
                    webSocket("/receiver") {
                        val sessionId = "device-${System.currentTimeMillis()}-${(1000..9999).random()}"
                        statusMessages.add("Connection attempt from a device")
                        try {
                            // Wait for the client to send the app identifier
                            val firstFrame = incoming.receive()
                            if (firstFrame is Frame.Text && firstFrame.readText() == APP_IDENTIFIER) {
                                // Client authenticated
                                clientSessions[sessionId] = this
                                connectedDevices.add(sessionId)
                                serverStatus =
                                    "Server Started on ${getLocalIpAddress()}:8080 (${connectedDevices.size} device(s) connected)"
                                statusMessages.add("Device $sessionId connected")
                                send(Frame.Text("Authenticated: Welcome, $sessionId"))

                                // Listen for further messages
                                for (frame in incoming) {
                                    when (frame) {
                                        is Frame.Text -> {
                                            val text = frame.readText()
                                            statusMessages.add("[$sessionId]: $text")
                                            send(Frame.Text("Echo: $text"))
                                        }

                                        else -> {
                                            // Ignore non-text frames
                                        }
                                    }
                                }
                            } else {
                                // Invalid identifier, close connection
                                statusMessages.add("Invalid app identifier from a device")
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid app identifier"))
                            }
                        } catch (e: Exception) {
                            statusMessages.add("Error with $sessionId: ${e.message}")
                        } finally {
                            clientSessions.remove(sessionId)
                            connectedDevices.remove(sessionId)
                            statusMessages.add("Device $sessionId disconnected")
                            serverStatus = if (connectedDevices.isEmpty()) {
                                "Server Started on ${getLocalIpAddress()}:8080 (No devices connected)"
                            } else {
                                "Server Started on ${getLocalIpAddress()}:8080 (${connectedDevices.size} device(s) connected)"
                            }
                        }
                    }
                }
            }.start(wait = false)
            serverStatus = "Server Started on ${getLocalIpAddress()}:8080 (No devices connected)"
            statusMessages.add("WebSocket server started")
        } catch (e: Exception) {
            serverStatus = "Failed to start server: ${e.message}"
            statusMessages.add("Server start failed: ${e.message}")
            server = null
        }
    }

    // Function to stop the WebSocket server
    fun stopWebSocketServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 1000)
        server = null
        connectedDevices.clear()
        statusMessages.clear()
        clientSessions.clear()
        serverStatus = "Server Stopped"
        statusMessages.add("WebSocket server stopped")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Icon")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Box (50% width)
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Left Section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }

            // Right Section (50% width, divided vertically)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Box (50% height) - Search for Receiver Button
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                if (server == null) {
                                    startWebSocketServer()
                                } else {
                                    stopWebSocketServer()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (server == null) "Search for Receiver" else "Stop Server",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Bottom Box (50% height) - Server Status, Connected Devices, and Messages
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            text = serverStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (connectedDevices.isNotEmpty()) {
                            Text(
                                text = "Connected Devices:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(connectedDevices) { deviceId ->
                                    Text(
                                        text = deviceId,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFF5F5F5))
                                            .padding(8.dp)
                                            .clickable { sendPairingRequest(deviceId) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (statusMessages.isNotEmpty()) {
                            Text(
                                text = "Status Log:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(statusMessages) { message ->
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SendScreen(onNavigateBack: () -> Unit) {
    // State to hold the list of file items

    val fileItems = remember {
        mutableStateListOf<FileItem>()
    }

    // State to track the selected file for expansion
    var selectedFileId by remember { mutableStateOf<String?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Screen", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Rectangle (30% width) - File List with Expandable Details
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(fileItems, key = { it.id }) { item ->
                        FileListItem(
                            item = item,
                            isExpanded = item.id == selectedFileId,
                            onToggleExpand = {
                                selectedFileId = if (selectedFileId == item.id) null else item.id
                            },
                            onDeleteClick = { clickedItem ->
                                if (selectedFileId == clickedItem.id) selectedFileId = null
                                fileItems.remove(clickedItem)
                            }
                        )
                    }
                }
            }

            // Right Rectangle (70% width) - File Chooser Area
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .clickable {
                        val fileDialog = FileDialog(null as java.awt.Frame?, "Choose Files", FileDialog.LOAD).apply {
                            isMultipleMode = true
                        }
                        fileDialog.isVisible = true
                        fileDialog.files.forEach { file ->
                            if (file != null) {
                                val fileSizeKB = file.length() / 1024
                                fileItems.add(
                                    FileItem(
                                        id = file.absolutePath,
                                        icon = Icons.Default.Description,
                                        name = file.name,
                                        size = "$fileSizeKB KB"
                                    )
                                )
                            }
                        }
                    }
            ) {
                // Inner bordered area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Choose Files",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose Files",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun FileListItem(
    item: FileItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteClick: (FileItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp)
    ) {
        // File Item Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Middle: File Name and Size
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(item.size, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Right: Delete Icon
            IconButton(
                onClick = { onDeleteClick(item) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${item.name}",
                    tint = Color.Red
                )
            }
        }

        // Expandable Details Section
        AnimatedVisibility(
            visible = isExpanded,
            // Expand from the bottom of the item
            enter = expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 400)),
            // Shrink back towards the bottom of the item
            exit = shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 400))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Size: ${item.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Simulate sending file */ println("Sending ${item.name}") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Send File", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
/**
 * Composable for a single item in the file list.
 * @param item The FileItem data to display.
 * @param onDeleteClick Callback when the delete icon is clicked.
 */


/**
 * Main entry point for the Compose Desktop application.
 * Creates a desktop window and hosts the App Composable.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "My Studio",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        App()
    }
}