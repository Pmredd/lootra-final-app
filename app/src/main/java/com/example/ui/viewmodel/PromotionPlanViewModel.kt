package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.WellbeingApplication
import com.example.data.model.PromotionPlan
import com.example.data.repository.PromotionPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class PromotionUiState {
    object Idle : PromotionUiState()
    object Loading : PromotionUiState()
    data class Success(val message: String) : PromotionUiState()
    data class Error(val message: String) : PromotionUiState()
}

class PromotionPlanViewModel(
    private val promotionPlanRepository: PromotionPlanRepository
) : ViewModel() {

    val plansFlow: StateFlow<List<PromotionPlan>> = promotionPlanRepository.plansFlow

    private val _uiState = MutableStateFlow<PromotionUiState>(PromotionUiState.Idle)
    val uiState: StateFlow<PromotionUiState> = _uiState.asStateFlow()

    fun clearUiState() {
        _uiState.value = PromotionUiState.Idle
    }

    fun addPlan(plan: PromotionPlan, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        Log.d("PromotionPlanVM", "Adding promotion plan: ${plan.displayTitle}")
        _uiState.value = PromotionUiState.Loading
        promotionPlanRepository.addPlan(
            plan = plan,
            onSuccess = {
                _uiState.value = PromotionUiState.Success("Promotion plan added successfully")
                onSuccess()
            },
            onFailure = { e ->
                _uiState.value = PromotionUiState.Error("Failed to add plan: ${e.message}")
                onFailure(e)
            }
        )
    }

    fun updatePlan(plan: PromotionPlan, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        Log.d("PromotionPlanVM", "Updating promotion plan: ${plan.planId}")
        _uiState.value = PromotionUiState.Loading
        promotionPlanRepository.updatePlan(
            plan = plan,
            onSuccess = {
                _uiState.value = PromotionUiState.Success("Promotion plan updated successfully")
                onSuccess()
            },
            onFailure = { e ->
                _uiState.value = PromotionUiState.Error("Failed to update plan: ${e.message}")
                onFailure(e)
            }
        )
    }

    fun deletePlan(planId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        Log.d("PromotionPlanVM", "Deleting promotion plan with ID: $planId")
        _uiState.value = PromotionUiState.Loading
        promotionPlanRepository.deletePlan(
            planId = planId,
            onSuccess = {
                _uiState.value = PromotionUiState.Success("Promotion plan deleted successfully")
                onSuccess()
            },
            onFailure = { e ->
                _uiState.value = PromotionUiState.Error("Failed to delete plan: ${e.message}")
                onFailure(e)
            }
        )
    }

    fun duplicatePlan(plan: PromotionPlan, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        // Find maximum displayOrder to place the duplicate at the end
        val maxOrder = plansFlow.value.maxOfOrNull { it.displayOrder } ?: 0
        val titleCopy = "${plan.displayTitle} (Copy)"
        val duplicated = plan.copy(
            planId = "",
            title = titleCopy,
            planName = titleCopy,
            displayOrder = maxOrder + 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        addPlan(duplicated, onSuccess, onFailure)
    }

    fun movePlanUp(plan: PromotionPlan) {
        val list = plansFlow.value.toMutableList()
        val index = list.indexOfFirst { it.planId == plan.planId }
        if (index > 0) {
            val prev = list[index - 1]
            val currentOrder = plan.displayOrder
            val prevOrder = prev.displayOrder
            updatePlan(plan.copy(displayOrder = prevOrder, updatedAt = System.currentTimeMillis()))
            updatePlan(prev.copy(displayOrder = currentOrder, updatedAt = System.currentTimeMillis()))
        }
    }

    fun movePlanDown(plan: PromotionPlan) {
        val list = plansFlow.value.toMutableList()
        val index = list.indexOfFirst { it.planId == plan.planId }
        if (index >= 0 && index < list.size - 1) {
            val next = list[index + 1]
            val currentOrder = plan.displayOrder
            val nextOrder = next.displayOrder
            updatePlan(plan.copy(displayOrder = nextOrder, updatedAt = System.currentTimeMillis()))
            updatePlan(next.copy(displayOrder = currentOrder, updatedAt = System.currentTimeMillis()))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WellbeingApplication
                return PromotionPlanViewModel(
                    promotionPlanRepository = application.container.promotionPlanRepository
                ) as T
            }
        }
    }
}
