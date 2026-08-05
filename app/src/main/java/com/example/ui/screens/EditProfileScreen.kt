package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isSaving by viewModel.isProfileSaving.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    
    val scrollState = rememberScrollState()

    // Temporary local states for editing
    var localUsername by remember { mutableStateOf(userProfile?.username ?: "") }
    var localImageUri by remember { mutableStateOf<Uri?>(null) }
    var localUsernameError by remember { mutableStateOf<String?>(null) }

    // Sync state if userProfile finishes loading in background
    LaunchedEffect(userProfile) {
        userProfile?.let {
            if (localUsername.isEmpty()) {
                localUsername = it.username
            }
        }
    }

    // Modern photo/gallery picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            localImageUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("edit_profile_back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile photo clickable update panel
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = !isSaving) {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("edit_profile_photo_box"),
                contentAlignment = Alignment.Center
            ) {
                if (localImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = localImageUri),
                        contentDescription = "New Profile Picture Chosen",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val firestoreImageUrl = userProfile?.profileImage ?: ""
                    if (firestoreImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = firestoreImageUrl),
                            contentDescription = "Current Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Camera Alt overlay to signal to the user it is clickable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pick Image",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = "Tap to change photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Validation or Error feedback
            profileError?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Lock Input: UID
            OutlinedTextField(
                value = userProfile?.uid ?: "",
                onValueChange = {},
                label = { Text("UID (Protected)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("edit_profile_uid_input"),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Lock Input: Email
            OutlinedTextField(
                value = userProfile?.email ?: "",
                onValueChange = {},
                label = { Text("Email (Locked)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag("edit_profile_email_input"),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            )

            // Dynamic Input: Username
            OutlinedTextField(
                value = localUsername,
                onValueChange = {
                    localUsername = it
                    if (localUsernameError != null && it.trim().length >= 3) {
                        localUsernameError = null
                    }
                },
                label = { Text("Username") },
                isError = localUsernameError != null,
                supportingText = {
                    localUsernameError?.let { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("edit_profile_username_input"),
                enabled = !isSaving
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("edit_profile_loader")
                )
            }

            // Actions Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_edit_profile_cancel"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val currentName = localUsername.trim()
                        if (currentName.length < 3) {
                            localUsernameError = "Username must be at least 3 characters"
                        } else {
                            localUsernameError = null
                            viewModel.updateUserProfile(currentName, localImageUri) { success ->
                                if (success) {
                                    onNavigateBack()
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_edit_profile_save"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
