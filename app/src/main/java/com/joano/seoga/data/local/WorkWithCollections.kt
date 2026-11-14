package com.joano.seoga.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class WorkWithCollections(
    @Embedded val work: WorkEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkCollectionCrossRef::class,
            parentColumn = "workId",
            entityColumn = "collectionId"
        )
    )
    val collections: List<CollectionEntity>
)

data class CollectionWithWorks(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = WorkCollectionCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "workId"
        )
    )
    val works: List<WorkEntity>
)
