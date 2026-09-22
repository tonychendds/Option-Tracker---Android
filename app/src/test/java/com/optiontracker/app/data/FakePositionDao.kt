package com.optiontracker.app.data

import com.optiontracker.app.data.local.PositionDao
import com.optiontracker.app.data.local.PositionEntity
import com.optiontracker.app.domain.model.PositionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakePositionDao : PositionDao {
    private val rows = MutableStateFlow<List<PositionEntity>>(emptyList())
    private var nextId = 1L

    override fun observeOpen(): Flow<List<PositionEntity>> =
        rows.map { list -> list.filter { it.status == PositionStatus.OPEN.name } }

    override fun observeClosed(): Flow<List<PositionEntity>> =
        rows.map { list -> list.filter { it.status == PositionStatus.CLOSED.name } }

    override fun observeById(id: Long): Flow<PositionEntity?> =
        rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): PositionEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun insert(entity: PositionEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id
    }

    override suspend fun update(entity: PositionEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
