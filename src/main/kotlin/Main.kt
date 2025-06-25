
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
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
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
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
                    if (currentScreen != "Send" && currentScreen != "Settings" && currentScreen != "Profile") {
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
 */
@Composable
fun DrawerContent(selectedScreen: String, onScreenSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(250.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
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
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onNavigateToSend: () -> Unit) {
    val tabs = listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
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
                                        .clickable { println("Receive clicked!") },
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

/**
 * Composable for the Settings Screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back icon")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings Screen", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Composable for the Profile Screen with WebSocket server and client.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateBack: () -> Unit) {
    var serverStatus by remember { mutableStateOf("Server Stopped") }
    var server: ApplicationEngine? by remember { mutableStateOf(null) }
    val connectedDevices = remember { mutableStateListOf<String>() }
    val statusMessages = remember { mutableStateListOf<String>() }
    val clientSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    var clientStatus by remember { mutableStateOf("Disconnected from mobile server") }
    var mobileIp by remember { mutableStateOf("") }
    var client: HttpClient? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val APP_IDENTIFIER = "MY_STUDIO_APP"

    fun getLocalIpAddress(): String {
        try {
            return InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            return "Unknown IP"
        }
    }

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

    fun startWebSocketServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 1000)
        try {
            server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
                install(io.ktor.server.websocket.WebSockets)
                routing {
                    webSocket("/receiver") {
                        val sessionId = "device-${System.currentTimeMillis()}-${(1000..9999).random()}"
                        statusMessages.add("Connection attempt from a device")
                        try {
                            val firstFrame = incoming.receive()
                            if (firstFrame is Frame.Text && firstFrame.readText() == APP_IDENTIFIER) {
                                clientSessions[sessionId] = this
                                connectedDevices.add(sessionId)
                                serverStatus = "Server Started on ${getLocalIpAddress()}:8080 (${connectedDevices.size} device(s) connected)"
                                statusMessages.add("Device $sessionId connected")
                                send(Frame.Text("Authenticated: Welcome, $sessionId"))
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        statusMessages.add("[$sessionId]: $text")
                                        send(Frame.Text("Echo: $text"))
                                    }
                                }
                            } else {
                                statusMessages.add("Invalid identifier from a device")
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid identifier"))
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

    fun stopWebSocketServer() {
        server?.stop(gracePeriodMillis = 1000, timeoutMillis = 1000)
        server = null
        connectedDevices.clear()
        statusMessages.clear()
        clientSessions.clear()
        serverStatus = "Server Stopped"
        statusMessages.add("WebSocket server stopped")
    }

    fun startWebSocketClient(ip: String) {
        if (ip.isBlank()) {
            statusMessages.add("Please enter a valid mobile IP address")
            return
        }
        client?.close()
        client = HttpClient(io.ktor.client.engine.cio.CIO) {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }
        scope.launch {
            try {
                client!!.webSocket(urlString = "ws://$ip:8081/receiver") {
                    clientStatus = "Connected to mobile server at $ip:8081"
                    statusMessages.add("Connected to mobile server at $ip:8081")
                    send(Frame.Text(APP_IDENTIFIER))
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            statusMessages.add("Mobile server: $text")
                            if (text == "PAIR_REQUEST") {
                                statusMessages.add("Received pairing request from mobile")
                                send(Frame.Text("PAIR_RESPONSE"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                clientStatus = "Failed to connect to mobile server: ${e.message}"
                statusMessages.add("Client error: ${e.message}")
            } finally {
                client?.close()
                client = null
                clientStatus = "Disconnected from mobile server"
                statusMessages.add("Disconnected from mobile server")
            }
        }
    }

    fun stopWebSocketClient() {
        client?.close()
        client = null
        clientStatus = "Disconnected from mobile server"
        statusMessages.add("WebSocket client stopped")
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
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Connect to Mobile Server",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mobileIp,
                        onValueChange = { mobileIp = it },
                        label = { Text("Mobile IP Address") },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (client == null) {
                                    startWebSocketClient(mobileIp)
                                } else {
                                    stopWebSocketClient()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (client == null) "Connect" else "Disconnect",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
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
                        Text(
                            text = clientStatus,
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

/**
 * Composable for the Send Screen with file selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(onNavigateBack: () -> Unit) {
    val fileItems = remember { mutableStateListOf<FileItem>() }
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

/**
 * Composable for a single item in the file list.
 */
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(item.size, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
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

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 400)),
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
                    onClick = { println("Sending ${item.name}") },
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
 * Main entry point for the Compose Desktop application.
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