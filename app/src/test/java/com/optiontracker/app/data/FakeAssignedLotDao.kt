package com.optiontracker.app.data

import com.optiontracker.app.data.local.AssignedLotDao
import com.optiontracker.app.data.local.AssignedLotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAssignedLotDao : AssignedLotDao {
    private val rows = MutableStateFlow<List<AssignedLotEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<AssignedLotEntity>> = rows

    override suspend fun getById(id: Long): AssignedLotEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun insert(entity: AssignedLotEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        rows.value = rows.value + entity.copy(id = id)
        return id
    }

    override suspend fun update(entity: AssignedLotEntity) {
        rows.value = rows.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun deleteById(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}
