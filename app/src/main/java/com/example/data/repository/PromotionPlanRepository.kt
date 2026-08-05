package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.PromotionPlan
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PromotionPlanRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val _plansFlow = MutableStateFlow<List<PromotionPlan>>(emptyList())
    val plansFlow: StateFlow<List<PromotionPlan>> = _plansFlow.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        listenToPromotionPlans()
    }

    fun listenToPromotionPlans() {
        Log.d("PromotionPlanRepo", "Setting up real-time snapshot listener for promotion_plans collection")
        listenerRegistration?.remove()
        listenerRegistration = firestore.collection("promotion_plans")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PromotionPlanRepo", "Firestore Listener Error on promotion_plans: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val plansList = mutableListOf<PromotionPlan>()
                    for (doc in snapshot.documents) {
                        try {
                            val plan = PromotionPlan.fromDocument(doc)
                            plansList.add(plan)
                        } catch (e: Exception) {
                            Log.e("PromotionPlanRepo", "Error parsing promotion plan document ${doc.id}", e)
                        }
                    }
                    val sortedPlans = plansList.sortedBy { it.displayOrder }
                    _plansFlow.value = sortedPlans
                    Log.d("PromotionPlanRepo", "Realtime Snapshot Updated! Total plans loaded: ${sortedPlans.size}")
                }
            }
    }

    fun addPlan(plan: PromotionPlan, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val calculatedBudget = if (plan.budget > 0) plan.budget else (plan.targetViews * plan.rewardPerView)
        val docRef = if (plan.planId.isNotEmpty()) {
            firestore.collection("promotion_plans").document(plan.planId)
        } else {
            firestore.collection("promotion_plans").document()
        }
        val finalPlan = plan.copy(
            planId = docRef.id,
            budget = calculatedBudget,
            campaignBudget = calculatedBudget,
            createdAt = if (plan.createdAt > 0) plan.createdAt else System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        Log.d("PromotionPlanRepo", "Writing plan to Firestore [ID: ${docRef.id}, Title: ${finalPlan.displayTitle}]")
        docRef.set(finalPlan.toMap())
            .addOnSuccessListener {
                Log.d("PromotionPlanRepo", "Successfully saved plan to Firestore: ${docRef.id}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("PromotionPlanRepo", "Failed to save plan to Firestore: ${exception.message}", exception)
                onFailure(exception)
            }
    }

    fun updatePlan(plan: PromotionPlan, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (plan.planId.isEmpty()) {
            val error = IllegalArgumentException("Cannot update plan with empty planId")
            Log.e("PromotionPlanRepo", error.message!!)
            onFailure(error)
            return
        }
        val calculatedBudget = if (plan.budget > 0) plan.budget else (plan.targetViews * plan.rewardPerView)
        val finalPlan = plan.copy(
            budget = calculatedBudget,
            campaignBudget = calculatedBudget,
            updatedAt = System.currentTimeMillis()
        )

        Log.d("PromotionPlanRepo", "Updating plan in Firestore [ID: ${plan.planId}]")
        firestore.collection("promotion_plans").document(plan.planId)
            .set(finalPlan.toMap())
            .addOnSuccessListener {
                Log.d("PromotionPlanRepo", "Successfully updated plan in Firestore: ${plan.planId}")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("PromotionPlanRepo", "Failed to update plan in Firestore: ${exception.message}", exception)
                onFailure(exception)
            }
    }

    fun deletePlan(planId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        if (planId.isEmpty()) {
            val error = IllegalArgumentException("Cannot delete plan with empty planId")
            Log.e("PromotionPlanRepo", error.message!!)
            onFailure(error)
            return
        }

        Log.d("PromotionPlanRepo", "Deleting plan from Firestore [ID: $planId]")
        firestore.collection("promotion_plans").document(planId)
            .delete()
            .addOnSuccessListener {
                Log.d("PromotionPlanRepo", "Successfully deleted plan from Firestore: $planId")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("PromotionPlanRepo", "Failed to delete plan from Firestore: ${exception.message}", exception)
                onFailure(exception)
            }
    }
}
