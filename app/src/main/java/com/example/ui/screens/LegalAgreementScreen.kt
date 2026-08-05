package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LegalPageEntity
import com.example.data.repository.LegalDefaults
import com.example.data.repository.LegalRepository

enum class LegalDocType(
    val docId: String,
    val title: String,
    val icon: ImageVector,
    val summary: String
) {
    PRIVACY_POLICY(
        docId = "privacy_policy",
        title = "Privacy Policy",
        icon = Icons.Default.Lock,
        summary = "Learn how Lootra protects your data, personal information, and usage telemetry with a strict zero-sell policy."
    ),
    TERMS_AND_CONDITIONS(
        docId = "terms_conditions",
        title = "Terms & Conditions",
        icon = Icons.Default.Gavel,
        summary = "Review user rights, account responsibilities, 1-Device = 1-Wallet rules, and virtual reward terms."
    ),
    COMMUNITY_GUIDELINES(
        docId = "community_guidelines",
        title = "Community Guidelines",
        icon = Icons.Default.Group,
        summary = "Discover standards for respectful interactions, campus marketplace safety, and video reel guidelines."
    )
}

@Composable
fun LegalAgreementScreen(
    onAcceptAndContinue: (privacyVer: String, termsVer: String, communityVer: String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier,
    legalRepository: LegalRepository = remember { LegalRepository() }
) {
    var isAgreed by remember { mutableStateOf(false) }
    var activeViewerDocId by remember { mutableStateOf<String?>(null) }
    val isDark = isSystemInDarkTheme()

    // Fetch live pages from Firestore
    val privacyPage by legalRepository.getLegalPageFlow("privacy_policy").collectAsState(initial = LegalDefaults.getDefault("privacy_policy"))
    val termsPage by legalRepository.getLegalPageFlow("terms_conditions").collectAsState(initial = LegalDefaults.getDefault("terms_conditions"))
    val communityPage by legalRepository.getLegalPageFlow("community_guidelines").collectAsState(initial = LegalDefaults.getDefault("community_guidelines"))

    val pageMap = remember(privacyPage, termsPage, communityPage) {
        mapOf(
            "privacy_policy" to privacyPage,
            "terms_conditions" to termsPage,
            "community_guidelines" to communityPage
        )
    }

    val bgBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF020617),
                    Color(0xFF0F172A),
                    Color(0xFF020617)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFE2E8F0),
                    Color(0xFFF1F5F9)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0x0A06B6D4), // Cyan glow
                    radius = 350.dp.toPx(),
                    center = Offset(size.width, size.height * 0.15f)
                )
                drawCircle(
                    color = Color(0x0A10B981), // Emerald glow
                    radius = 300.dp.toPx(),
                    center = Offset(0f, size.height * 0.85f)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0x12FFFFFF) else Color(0xCCFFFFFF)
            ),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(Color(0x33FFFFFF), Color(0x08FFFFFF))
                        } else {
                            listOf(Color(0x660F172A), Color(0x0F0F172A))
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("legal_agreement_container_card"),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Illustration / Icon Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (isDark) Color(0x1A10B981) else Color(0x1A0F172A),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0x3310B981) else Color(0x330F172A),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Lootra Legal Protection",
                        tint = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Lootra",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.testTag("legal_agreement_title")
                )

                Text(
                    text = "Please review and accept our legal agreements before continuing.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF94A3B8),
                    modifier = Modifier
                        .padding(top = 6.dp, bottom = 20.dp)
                        .testTag("legal_agreement_subtitle")
                )

                // 3 Legal Document Expandable Cards
                LegalDocType.values().forEach { docType ->
                    val page = pageMap[docType.docId] ?: LegalDefaults.getDefault(docType.docId)
                    ExpandableLegalDocCard(
                        docType = docType,
                        page = page,
                        isDark = isDark,
                        onReadFullContent = { activeViewerDocId = docType.docId }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Consent Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color(0x0AFFFFFF) else Color(0x08000000),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isAgreed) Color(0xFF10B981) else Color(0x1AFFFFFF),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { isAgreed = !isAgreed }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { isAgreed = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF10B981),
                            uncheckedColor = Color(0xFF64748B),
                            checkmarkColor = Color(0xFF020617)
                        ),
                        modifier = Modifier.testTag("legal_consent_checkbox")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I have read and agree to the Privacy Policy, Terms & Conditions and Community Guidelines.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onAcceptAndContinue(privacyPage.version, termsPage.version, communityPage.version)
                    },
                    enabled = isAgreed && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("legal_continue_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF0F172A),
                        contentColor = if (isDark) Color(0xFF020617) else Color.White,
                        disabledContainerColor = Color(0x3364748B),
                        disabledContentColor = Color(0x6694A3B8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = if (isDark) Color(0xFF020617) else Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Saving...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("legal_back_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }

    // Error Dialog with Retry/Cancel
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = onClearError,
            title = {
                Text(
                    text = "Unable to complete setup",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = errorMessage.ifEmpty { "Unable to complete setup. Please try again." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearError()
                        if (isAgreed) {
                            onAcceptAndContinue(privacyPage.version, termsPage.version, communityPage.version)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onClearError) {
                    Text("Cancel")
                }
            },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            titleContentColor = if (isDark) Color.White else Color(0xFF0F172A),
            textContentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        )
    }

    // In-App Full Document Viewer Modal
    activeViewerDocId?.let { docId ->
        DynamicLegalDocumentViewer(
            docId = docId,
            onClose = { activeViewerDocId = null },
            legalRepository = legalRepository
        )
    }
}

@Composable
fun ExpandableLegalDocCard(
    docType: LegalDocType,
    page: LegalPageEntity,
    isDark: Boolean,
    onReadFullContent: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x0DFFFFFF) else Color(0x05000000)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isExpanded) Color(0xFF10B981) else if (isDark) Color(0x1F38BDF8) else Color(0x1F0F172A),
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("legal_card_${docType.docId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = Color(0x1A38BDF8),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = docType.icon,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = page.title.ifEmpty { docType.title },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "VERSION ${page.version}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF38BDF8)
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = page.summary.ifEmpty { docType.summary },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = Color(0xFF94A3B8)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    HorizontalDivider(
                        color = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(
                                color = if (isDark) Color(0x1A000000) else Color(0x0A000000),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = page.content,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { onReadFullContent() }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .testTag("btn_read_full_${docType.docId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Read Full Content",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Read Full Content",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { onReadFullContent() }
                        .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                        .testTag("btn_read_more_${docType.docId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read Full Content",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Read More",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicLegalDocumentViewer(
    docId: String,
    onClose: () -> Unit,
    legalRepository: LegalRepository = remember { LegalRepository() }
) {
    val isDark = isSystemInDarkTheme()
    val pageEntity by legalRepository.getLegalPageFlow(docId).collectAsState(initial = LegalDefaults.getDefault(docId))

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF0F172A) else Color.White,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color(0x1A10B981),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (docId) {
                                    "privacy_policy" -> Icons.Default.Lock
                                    "terms_conditions" -> Icons.Default.Gavel
                                    "community_guidelines" -> Icons.Default.Group
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = pageEntity.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "Lootra Legal Framework v${pageEntity.version}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_doc_viewer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000)
                )

                // Scrollable Document Text
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color(0x0AFFFFFF) else Color(0x05000000),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = pageEntity.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Default,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                        modifier = Modifier.testTag("doc_text_content")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Done Button
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_close_doc_reader"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF020617)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "I Have Reviewed This Document",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyUpdateDialog(
    onReviewAndAccept: () -> Unit,
    onLogout: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = { /* Modal: user must review or logout */ },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color(0x1A38BDF8),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Legal documents have been updated.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.testTag("policy_update_dialog_title")
            )
        },
        text = {
            Text(
                text = "We have updated our Privacy Policy, Terms & Conditions, and Community Guidelines. Please review and accept the updated terms to continue using Lootra.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = Color(0xFF94A3B8),
                modifier = Modifier.testTag("policy_update_dialog_text")
            )
        },
        confirmButton = {
            Button(
                onClick = onReviewAndAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color(0xFF020617)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_review_and_accept")
            ) {
                Text(
                    text = "Review & Accept",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = BorderStroke(1.dp, Color(0x33EF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_policy_logout")
            ) {
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
        shape = RoundedCornerShape(20.dp),
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}
