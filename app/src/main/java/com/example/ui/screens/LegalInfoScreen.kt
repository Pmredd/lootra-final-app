package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LegalDefaults
import com.example.data.repository.LegalRepository
import kotlinx.coroutines.launch

data class LegalCategoryItem(
    val docId: String,
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val learnMoreUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalInfoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    legalRepository: LegalRepository = remember { LegalRepository() }
) {
    var activeViewerDocId by remember { mutableStateOf<String?>(null) }
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val onLearnMore: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Unable to open the website. Please try again.")
            }
        }
    }

    val categories = remember {
        listOf(
            LegalCategoryItem(
                docId = "privacy_policy",
                title = "Privacy Policy",
                icon = Icons.Default.Lock,
                accentColor = Color(0xFF10B981),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#privacy"
            ),
            LegalCategoryItem(
                docId = "terms_conditions",
                title = "Terms & Conditions",
                icon = Icons.Default.Gavel,
                accentColor = Color(0xFF38BDF8),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#terms"
            ),
            LegalCategoryItem(
                docId = "delete_account",
                title = "Delete Account",
                icon = Icons.Default.Delete,
                accentColor = Color(0xFFEF4444),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#delete-account"
            ),
            LegalCategoryItem(
                docId = "contact_us",
                title = "Contact",
                icon = Icons.Default.Call,
                accentColor = Color(0xFF6366F1),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#contact"
            ),
            LegalCategoryItem(
                docId = "community_guidelines",
                title = "Community Guidelines",
                icon = Icons.Default.Group,
                accentColor = Color(0xFFA855F7),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#community"
            ),
            LegalCategoryItem(
                docId = "about_lootra",
                title = "About Lootra",
                icon = Icons.Default.Info,
                accentColor = Color(0xFFF59E0B),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#about"
            ),
            LegalCategoryItem(
                docId = "help_support",
                title = "Help & Support",
                icon = Icons.Default.Help,
                accentColor = Color(0xFFEC4899),
                learnMoreUrl = "https://lootra-official-website.ai.studio/#help"
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Legal & Information",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Official Lootra campus policies & support",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("legal_info_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Live Status Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x1210B981) else Color(0x120F172A)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color(0x3310B981) else Color(0x1F0F172A)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF10B981), shape = RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synchronized live with Lootra Firestore Legal Center",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF0F172A)
                    )
                }
            }

            // List of Legal Pages
            categories.forEach { item ->
                LegalInfoCard(
                    category = item,
                    isDark = isDark,
                    legalRepository = legalRepository,
                    onOpenViewer = { activeViewerDocId = item.docId },
                    onLearnMore = { onLearnMore(item.learnMoreUrl) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Document Reader Dialog
    activeViewerDocId?.let { docId ->
        DynamicLegalDocumentViewer(
            docId = docId,
            onClose = { activeViewerDocId = null },
            legalRepository = legalRepository
        )
    }
}

@Composable
private fun LegalInfoCard(
    category: LegalCategoryItem,
    isDark: Boolean,
    legalRepository: LegalRepository,
    onOpenViewer: () -> Unit,
    onLearnMore: () -> Unit
) {
    val pageEntity by legalRepository.getLegalPageFlow(category.docId)
        .collectAsState(initial = LegalDefaults.getDefault(category.docId))

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x12FFFFFF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0x1AFFFFFF) else Color(0x0F000000)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenViewer() }
            .testTag("legal_info_card_${category.docId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = category.accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.title,
                        tint = category.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pageEntity.title.ifEmpty { category.title },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "VERSION ${pageEntity.version}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = category.accentColor
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open ${category.title}",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = pageEntity.summary,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                ),
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isNarrow = maxWidth < 340.dp
                if (isNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenViewer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("view_in_app_${category.docId}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "View ${category.title} in app",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View in App",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        OutlinedButton(
                            onClick = onLearnMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("learn_more_${category.docId}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) category.accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Learn more about ${category.title} on website",
                                tint = if (isDark) category.accentColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Learn More",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isDark) category.accentColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenViewer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_in_app_${category.docId}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Article,
                                contentDescription = "View ${category.title} in app",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View in App",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        OutlinedButton(
                            onClick = onLearnMore,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("learn_more_${category.docId}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDark) category.accentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Learn more about ${category.title} on website",
                                tint = if (isDark) category.accentColor else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Learn More",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isDark) category.accentColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
