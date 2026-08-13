package com.example.data.repository

import com.example.data.model.LegalPageEntity

object LegalDefaults {
    fun getDefault(docId: String): LegalPageEntity {
        return when (docId) {
            "privacy_policy" -> LegalPageEntity(
                docId = "privacy_policy",
                title = "Privacy Policy",
                summary = "Learn how Lootra protects your personal data, digital wellbeing telemetry, wallet information, and privacy rights.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Legal System",
                content = """
                    LOOTRA PRIVACY POLICY
                    Effective Date: July 2026 | Version: 1.0.0

                    1. DATA COLLECTION
                    Lootra collects minimal required data to deliver our campus digital wellbeing and reward experience. This includes user account credentials, email, profile details, and device identifiers (1 Device = 1 Wallet binding).

                    2. FIREBASE AUTHENTICATION
                    We utilize Google Firebase Authentication for secure sign-in, account management, and password security. Your password is never stored in plaintext and is handled exclusively by encrypted Firebase auth servers.

                    3. USAGE STATISTICS & DIGITAL WELLBEING DATA
                    With your explicit permission, Lootra monitors screen time, app usage statistics, and focus durations to calculate daily digital wellbeing scores and award Lootra Coins. Detailed raw application telemetry remains local to your device whenever possible and aggregate metrics are processed securely.

                    4. WALLET & REWARD INFORMATION
                    Your virtual wallet ledger tracks Lootra Coins, lifetime coin earnings, and redemption history. Wallet transactions are cryptographically bound to your authenticated user identity and device fingerprint to prevent fraud.

                    5. REWARD TV & REELS TV
                    Interaction history with Reward TV videos and Reels TV short clips (including watch durations, likes, and comments) is recorded to personalize recommendations and grant promotional watch rewards.

                    6. CAMPUS ORDERS & COMMERCE
                    When you place store orders or redeem campus offers, we collect order item details, total coins spent, pickup status, and campus location to fulfill transactions with campus merchants.

                    7. CLOUDINARY & MEDIA STORAGE
                    Profile images, uploaded video reels, and promotional banners are hosted securely via Cloudinary CDN infrastructure with content safety filters enabled.

                    8. GOOGLE ADMOB
                    Optional rewarded video ads are served via Google AdMob SDK in compliance with Google Privacy Principles. Non-personalized advertising identifiers may be processed by AdMob to deliver rewarded video ads.

                    9. PUSH NOTIFICATIONS
                    Firebase Cloud Messaging (FCM) delivers transactional notifications, order status alerts, coin reward updates, and wellbeing reminders. You can adjust notification preferences in device settings at any time.

                    10. DATA SECURITY & ENCRYPTION
                    All data in transit and at rest is secured with AES-256 encryption using Google Cloud Security Rules and Firestore access policies.

                    11. USER RIGHTS & ACCOUNT DELETION
                    You possess full ownership of your data. You may inspect your stored profile at any time or request permanent account deletion through in-app support or email.

                    12. CONTACT US
                    For privacy inquiries or data removal requests:
                    Email: lootra143@gmail.com
                """.trimIndent()
            )

            "terms_conditions" -> LegalPageEntity(
                docId = "terms_conditions",
                title = "Terms & Conditions",
                summary = "Review user responsibilities, Lootra Coin rules, reward TV guidelines, marketplace policies, and prohibited activities.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Legal System",
                content = """
                    LOOTRA TERMS & CONDITIONS
                    Effective Date: July 2026 | Version: 1.0.0

                    1. USER RESPONSIBILITIES
                    By registering for Lootra, you agree to provide truthful registration information, maintain a single account per physical device, and safeguard your account security.

                    2. REWARD & LOOTRA COIN RULES
                    • Lootra Coins are utility tokens granted for digital wellbeing achievements, daily app usage goals, and video interaction.
                    • Coins hold no monetary value outside Lootra's authorized in-app shop and partner redemptions. Coins cannot be exchanged for cash or transferred between external accounts.

                    3. 1 DEVICE = 1 WALLET SECURITY RULE
                    Each physical device is permanently registered to a single Lootra wallet. Utilizing emulators, device spoofing, or bot scripts to artificially inflate coin rewards is strictly prohibited and results in immediate account forfeiture.

                    4. ORDER & SHOPPING POLICIES
                    Orders placed using Lootra Coins or promotional offers are subject to campus store availability. Pickup confirmations must be presented in person at authorized campus pickup stations.

                    5. REELS TV & PROMOTIONAL RULES
                    Users who create or upload content to Reels TV grant Lootra a non-exclusive license to feature their content across campus feeds. Uploaded content must adhere strictly to copyright and intellectual property standards.

                    6. PROHIBITED ACTIVITIES
                    Prohibited activities include:
                    • Fraudulent coin generation or exploiting software vulnerabilities.
                    • Harassment, hate speech, or impersonating other campus students or faculty.
                    • Placing fake order reservations or spamming vendor channels.

                    7. ACCOUNT SUSPENSION & LIABILITY
                    Lootra reserves the right to suspend or terminate accounts that violate our security policies or terms of service. Lootra is not liable for indirect damages arising from service interruptions.

                    8. REFUND & RETURN POLICY (FUTURE READY)
                    For items ordered in Lootra Shop, coin refunds or item replacements are handled according to store vendor return terms. Digital rewards and vouchers are final upon issuance.
                """.trimIndent()
            )

            "community_guidelines" -> LegalPageEntity(
                docId = "community_guidelines",
                title = "Community Guidelines",
                summary = "Standards for respectful campus interaction, Reels TV safety, fraud prevention, and spam-free commerce.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Legal System",
                content = """
                    LOOTRA COMMUNITY GUIDELINES
                    Effective Date: July 2026 | Version: 1.0.0

                    1. RESPECTFUL & SAFE INTERACTION
                    Lootra is built for a vibrant, supportive campus community. We strictly enforce zero tolerance for harassment, bullying, discrimination, or offensive behavior in chats, comments, or public reels.

                    2. PROMOTION & ADVERTISING RULES
                    Students and campus merchants may promote legitimate events and offers through authorized promotion plans. Unsolicited commercial spam or unauthorized product sales are prohibited.

                    3. NO FRAUD & NO FAKE CONTENT
                    Users must not upload misleading videos, clickbait, or deceptive offers. Fraudulent attempts to trick other students or manipulate video view counters will lead to account suspension.

                    4. REWARD ABUSE PREVENTION
                    Creating multiple dummy accounts, using auto-clickers for Reward TV, or exploiting referral links violates community trust. System checks monitor coin distribution to guarantee fairness.

                    5. SAFE PLATFORM USAGE
                    Keep our campus safe. Report any inappropriate reels content, harmful interactions, or suspicious activities directly using in-app flag controls.
                """.trimIndent()
            )

            "about_lootra" -> LegalPageEntity(
                docId = "about_lootra",
                title = "About Lootra",
                summary = "Discover Lootra's mission, vision, campus wellbeing ecosystem, feature suite, and future roadmap.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Engineering",
                content = """
                    ABOUT LOOTRA - DIGITAL WELLBEING & CAMPUS REWARDS

                    1. MISSION
                    Lootra empowers students to build healthier digital habits by transforming phone screen time discipline into tangible campus rewards, discounts, and entertainment.

                    2. VISION
                    To become the primary student ecosystem connecting focus tracking, campus commerce, video creation, and community rewards seamlessly across universities nationwide.

                    3. CORE FEATURES
                    • Digital Wellbeing Dashboard: Track screen time, app usage, and set focus goals.
                    • Lootra Virtual Wallet: Earn Lootra Coins for screen discipline and spend them in the Lootra Shop.
                    • Reward TV & Reels TV: Watch inspiring campus shorts and rewarded video content.
                    • Campus Shop & Pickup: Browse products and redeem exclusive student rewards.
                    • Super Admin & Campus Control: Enterprise management for campus admins and store vendors.

                    4. APP VERSION & BUILD
                    Current Platform Version: v1.0.0 (Production Build)

                    5. FUTURE ROADMAP
                    • Multi-university campus expansion and custom college hubs.
                    • Peer-to-peer campus marketplace and student study groups.
                    • Advanced AI-driven focus insights and personalized habit recommendations.
                """.trimIndent()
            )

            "help_support" -> LegalPageEntity(
                docId = "help_support",
                title = "Help & Support",
                summary = "Find answers to frequently asked questions regarding Lootra Coins, Wallet, Reward TV, Orders, and troubleshooting.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Support Team",
                content = """
                    LOOTRA HELP & SUPPORT CENTER

                    1. FREQUENTLY ASKED QUESTIONS (FAQS)
                    Q: How do I earn Lootra Coins?
                    A: You earn coins by maintaining daily focus goals, tracking screen time discipline, watching Reward TV, and participating in campus activities.

                    Q: Why is my device bound to my wallet?
                    A: To ensure fair reward distribution for all students, Lootra enforces a strictly monitored "1 Device = 1 Wallet" security rule.

                    2. WALLET HELP & TROUBLESHOOTING
                    • If your coin balance does not update immediately, pull down to refresh your wallet screen.
                    • Coin transaction history is logged in real-time under Wallet History.

                    3. REWARD TV & REELS TV HELP
                    • Complete the full duration of rewarded video clips to unlock coin credits.
                    • If a video fails to play, check your internet connection or update the app.

                    4. ORDERS & CAMPUS SHOP HELP
                    • After placing an order, show your digital QR or Order ID at your designated campus store for pickup.
                    • Order status updates (Pending, Processing, Delivered) appear live under My Orders.

                    5. CONTACT SUPPORT
                    Need direct assistance? Our campus support team is ready to help at lootra143@gmail.com.
                """.trimIndent()
            )

            "contact_us" -> LegalPageEntity(
                docId = "contact_us",
                title = "Contact Us",
                summary = "Get in touch with the Lootra team for support, partnerships, or official campus inquiries.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Team",
                content = """
                    CONTACT LOOTRA TEAM

                    We are always here to assist you with support, campus partnerships, or feedback.

                    1. OFFICIAL SUPPORT EMAIL
                    Direct Support: lootra143@gmail.com

                    2. CAMPUS PARTNERSHIPS & VENDORS
                    Campus Store Onboarding: lootra143@gmail.com

                    3. SOCIAL & COMMUNITY CHANNELS
                    • Website: https://lootra.app (Coming Soon)
                    • WhatsApp Support: Available for registered campus partners
                    • Instagram: @lootra_app
                    • Support Line: Available during business hours

                    4. HEADQUARTERS & ADDRESS
                    Lootra Campus Technologies
                    Student Wellbeing & Commerce Division
                    Primary Email: lootra143@gmail.com
                """.trimIndent()
            )

            "delete_account" -> LegalPageEntity(
                docId = "delete_account",
                title = "Delete Account",
                summary = "Information and steps for permanently deleting your Lootra account, data, wallet, and settings.",
                version = "1.0.0",
                status = "published",
                published = true,
                updatedAt = System.currentTimeMillis(),
                updatedBy = "Lootra Privacy Team",
                content = """
                    LOOTRA ACCOUNT DELETION POLICY
                    Effective Date: July 2026 | Version: 1.0.0

                    1. ACCOUNT DELETION OVERVIEW
                    You have the right to request the permanent deletion of your Lootra account and all associated personal data at any time.

                    2. WHAT DATA IS REMOVED
                    Upon account deletion, the following data will be permanently purged:
                    • Profile credentials and personal information.
                    • Lootra wallet history, coins balance, and redemption vouchers.
                    • Digital wellbeing history and usage logs.
                    • Uploaded reels, comments, and interactions.

                    3. HOW TO DELETE YOUR ACCOUNT IN-APP
                    You can initiate account deletion directly inside Lootra:
                    1. Go to Profile Screen.
                    2. Scroll to the bottom and select "Delete Account".
                    3. Confirm deletion via re-authentication.

                    4. EXTERNAL DELETION REQUESTS
                    You may also submit a deletion request online at https://lootra-official-website.ai.studio/#delete-account or via email at lootra143@gmail.com.
                """.trimIndent()
            )

            else -> LegalPageEntity(docId = docId, title = docId.replace("_", " ").uppercase())
        }
    }

    val ALL_DOC_IDS = listOf(
        "privacy_policy",
        "terms_conditions",
        "delete_account",
        "community_guidelines",
        "about_lootra",
        "help_support",
        "contact_us"
    )
}
