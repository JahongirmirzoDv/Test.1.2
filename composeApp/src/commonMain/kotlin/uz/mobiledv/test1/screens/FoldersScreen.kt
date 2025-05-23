// Located in: jahongirmirzodv/test.1.2/Test.1.2-e8bc22d6ec882d29fdc4fa507b210d7398d64cde/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersScreen.kt
package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import uz.mobiledv.test1.AppViewModel
import uz.mobiledv.test1.model.Folder
// import uz.mobiledv.test1.util.PlatformType // Not directly used here, AppViewModel handles platform distinction
// import uz.mobiledv.test1.util.getCurrentPlatform // Not directly used here
import uz.mobiledv.test1.util.isValidEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    appViewModel: AppViewModel = koinViewModel(),
    viewModel: FoldersViewModel = koinViewModel(),
    onFolderClick: (Folder) -> Unit,
    onLogout: () -> Unit,
    navController: NavController // Keep if other navigation is needed from here
) {
    val isManager = appViewModel.isManager // Get from AppViewModel
    println("is admin: ${isManager}")


    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showEditFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showDeleteFolderDialog by remember { mutableStateOf<Folder?>(null) }
    var showCreateUserDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    // val scope = rememberCoroutineScope() // Not used directly, AppViewModel handles alerts

    val foldersUiState by viewModel.foldersUiState.collectAsStateWithLifecycle()
    val folderOperationStatus by viewModel.operationStatus.collectAsStateWithLifecycle()
    val userCreationAlert by appViewModel.operationAlert.collectAsStateWithLifecycle() // Listen to AppViewModel's alert

    LaunchedEffect(folderOperationStatus) {
        folderOperationStatus?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearOperationStatus()
        }
    }

    // This handles alerts from AppViewModel (e.g., for user creation)
    LaunchedEffect(userCreationAlert) {
        userCreationAlert?.let { message ->
            snackbarHostState.showSnackbar(message)
            appViewModel.operationAlert.value = null // Clear AppViewModel's alert
            if (message.startsWith("User") && (message.contains("created successfully") || message.contains("Error creating user"))) {
                if (!message.contains("already exists", ignoreCase = true) &&
                    !message.contains("Invalid", ignoreCase = true) && // Keep dialog for client-side errors
                    !message.contains("Password", ignoreCase = true) &&
                    !message.contains("cannot be empty", ignoreCase = true) ) {
                    showCreateUserDialog = false // Close dialog on successful creation or definitive server error
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isManager) "Admin Folders" else "My Folders") },
                actions = {
                    if (isManager) {
                        IconButton(onClick = { showAddFolderDialog = true }) {
                            Icon(Icons.Filled.Add, "Add Folder")
                        }
                        IconButton(onClick = {
                            showCreateUserDialog = true
                        }) {
                            Icon(Icons.Filled.ManageAccounts, "Create User")
                        }
                    }
                    IconButton(onClick = {
                        onLogout() // This will trigger AppViewModel.logout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = foldersUiState) {
                is FoldersUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Loading folders...")
                    }
                }

                is FoldersUiState.Success -> {
                    if (state.folders.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(if (isManager) "No folders yet. Tap '+' to create one!" else "No folders available.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            items(state.folders, key = { it.id }) { folder ->
                                FolderListItem(
                                    folder = folder,
                                    onClick = { onFolderClick(folder) },
                                    isManager = isManager, // Pass isManager status
                                    onEdit = { if (isManager) showEditFolderDialog = folder },
                                    onDelete = { if (isManager) showDeleteFolderDialog = folder }
                                )
                            }
                        }
                    }
                }

                is FoldersUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error: ${state.message}", maxLines = 4)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadFolders() }) {
                            Text("Retry")
                        }
                    }
                }

                is FoldersUiState.Idle -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Initializing...")
                    }
                }
            }
        }
    }

    if (isManager) {
        if (showAddFolderDialog) {
            FolderDialog(
                onDismiss = { showAddFolderDialog = false },
                onConfirm = { name, description ->
                    viewModel.createFolder(name, description)
                    showAddFolderDialog = false
                }
            )
        }

        showEditFolderDialog?.let { folder ->
            FolderDialog(
                folder = folder,
                onDismiss = { showEditFolderDialog = null },
                onConfirm = { name, description ->
                    viewModel.updateFolder(folder.id, name, description)
                    showEditFolderDialog = null
                }
            )
        }

        showDeleteFolderDialog?.let { folder ->
            AlertDialog(
                onDismissRequest = { showDeleteFolderDialog = null },
                title = { Text("Delete Folder") },
                text = { Text("Are you sure you want to delete folder \"${folder.name}\"? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteFolder(folder.id)
                            showDeleteFolderDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteFolderDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (showCreateUserDialog) {
            CreateUserDialog(
                onDismiss = { showCreateUserDialog = false },
                onConfirm = { username, email, password ->
                    // Admin creating a regular (non-admin) user by default
                    appViewModel.adminCreateUser(username, email, password, isAdmin = false)
                    // Dialog closure is handled by LaunchedEffect observing userCreationAlert
                }
            )
        }
    }
}

@Composable
private fun FolderListItem( // No changes needed here if it only depends on isManager for UI elements
    folder: Folder,
    onClick: () -> Unit,
    isManager: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (folder.description.isNotEmpty()) {
                    Text(
                        text = folder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                folder.createdAt?.let {
                    Text(
                        text = "Created: $it", // You might want to format this date/time
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (isManager) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Edit, "Edit Folder", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            "Delete Folder",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDialog( // No changes needed
    folder: Folder? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit
) {
    var name by remember(folder) { mutableStateOf(folder?.name ?: "") }
    var description by remember(folder) { mutableStateOf(folder?.description ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (folder == null) "Add New Folder" else "Edit Folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Folder Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true
                )
                if (nameError != null) {
                    Text(
                        nameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Folder name cannot be empty."
                    } else {
                        nameError = null
                        onConfirm(name.trim(), description.trim())
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, email: String, pass: String) -> Unit
) {
    var username by remember { mutableStateOf("") } // Added username
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf<String?>(null) } // Added
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New User") },
        text = {
            Column {
                OutlinedTextField( // Username field
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = if (it.isNotBlank()) null else "Username cannot be empty."
                    },
                    label = { Text("Username*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = usernameError != null,
                    singleLine = true
                )
                usernameError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = if (isValidEmail(it)) null else "Invalid email format."
                    },
                    label = { Text("User Email*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = emailError != null,
                    singleLine = true
                )
                emailError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = if (it.length >= 6) null else "Password must be at least 6 characters."
                    },
                    label = { Text("Password*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = passwordError != null,
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, if (passwordVisible) "Hide password" else "Show password")
                        }
                    }
                )
                passwordError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isUsernameValid = username.isNotBlank()
                    val isEmailCurrentlyValid = isValidEmail(email)
                    val isPasswordCurrentlyValid = password.length >= 6

                    usernameError = if (isUsernameValid) null else "Username cannot be empty."
                    emailError = if (isEmailCurrentlyValid) null else "Invalid email format."
                    passwordError = if (isPasswordCurrentlyValid) null else "Password must be at least 6 characters."

                    if (isUsernameValid && isEmailCurrentlyValid && isPasswordCurrentlyValid) {
                        onConfirm(username.trim(), email.trim(), password)
                    }
                }
            ) { Text("Create User") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}