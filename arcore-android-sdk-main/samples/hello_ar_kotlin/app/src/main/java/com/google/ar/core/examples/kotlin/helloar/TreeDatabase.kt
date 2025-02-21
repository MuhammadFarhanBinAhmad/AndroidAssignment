package com.google.ar.core.examples.kotlin.helloar
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Tree::class], version = 1)
abstract class TreeDatabase : RoomDatabase() {
    abstract fun treeDao(): TreeDao

    companion object {
        @Volatile private var INSTANCE: TreeDatabase? = null

        fun getDatabase(context: Context): TreeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TreeDatabase::class.java,
                    "tree_database"
                )
                    // Pre-populate the database on creation:
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Thread {
                                getDatabase(context).treeDao().insertAll(
                                    Tree(
                                        name = "Ansana tree",
                                        imageRes = R.drawable.ansanatree,  // Ensure you have this drawable resource
                                        info = "The Ansana tree is known for its unique shape and vibrant foliage.",
                                        division = "nil",
                                        growthform = "nil",
                                        LifeSpan = "nil",
                                        ModeOfNutrition = "nil",
                                        PlantShape = "nil",
                                        MaxHeight = 40,
                                        TrunkDiameter = 2
                                    ),
                                    Tree(
                                        name = "Rain tree",
                                        imageRes = R.drawable.raintree,  // Replace with your rain tree drawable
                                        info = "The Rain tree is famous for its expansive canopy and abundant shade.",
                                        division = "nil",
                                        growthform = "nil",
                                        LifeSpan = "nil",
                                        ModeOfNutrition = "nil",
                                        PlantShape = "nil",
                                        MaxHeight = 40,
                                        TrunkDiameter = 2
                                    ),
                                    Tree(
                                        name = "Seal Almond tree",
                                        imageRes = R.drawable.seaalmond,  // Replace with your seal almond tree drawable
                                        info = "The Seal Almond tree has a distinctive appearance and rich history.",
                                        division = "nil",
                                        growthform = "nil",
                                        LifeSpan = "nil",
                                        ModeOfNutrition = "nil",
                                        PlantShape = "nil",
                                        MaxHeight = 40,
                                        TrunkDiameter = 2
                                    )
                                )
                            }.start()
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}