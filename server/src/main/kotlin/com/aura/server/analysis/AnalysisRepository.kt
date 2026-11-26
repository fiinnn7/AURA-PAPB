package com.aura.server.analysis

internal interface AnalysisRepository {
    fun loadStoreDataset(idToko: String, sinceMs: Long): StoreAnalysisDataset
}
