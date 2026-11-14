package com.joano.seoga.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "work_collection_cross_ref",
    primaryKeys = ["workId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["workId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["workId"]),
        Index(value = ["collectionId"])
    ]
)
data class WorkCollectionCrossRef(
    val workId: Long,
    val collectionId: Long
)
