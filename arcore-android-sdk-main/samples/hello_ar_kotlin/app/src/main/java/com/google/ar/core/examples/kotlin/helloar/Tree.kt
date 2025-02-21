package com.google.ar.core.examples.kotlin.helloar

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trees")
data class Tree(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageRes: Int,  // Resource ID for the tree's image
    val info: String,
    val division:String,
    val growthform: String,
    val LifeSpan : String,
    val ModeOfNutrition : String,
    val PlantShape : String,
    val MaxHeight : Int,
    val TrunkDiameter : Int
)