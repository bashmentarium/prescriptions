package com.example.whitelabel.data.database.converters

import androidx.room.TypeConverter
import com.example.whitelabel.data.FoodTiming
import com.example.whitelabel.data.database.entities.ParsedMedicationEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MedicationListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromMedicationList(medications: List<ParsedMedicationEntity>): String {
        return gson.toJson(medications)
    }

    @TypeConverter
    fun toMedicationList(medicationsJson: String): List<ParsedMedicationEntity> {
        val listType = object : TypeToken<List<ParsedMedicationEntity>>() {}.type
        return gson.fromJson(medicationsJson, listType) ?: emptyList()
    }
}

class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(listJson: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(listJson, listType) ?: emptyList()
    }
}

class FoodTimingConverter {
    @TypeConverter
    fun fromFoodTiming(foodTiming: FoodTiming): String {
        return foodTiming.name
    }

    @TypeConverter
    fun toFoodTiming(foodTimingString: String): FoodTiming {
        return try {
            FoodTiming.valueOf(foodTimingString)
        } catch (e: IllegalArgumentException) {
            FoodTiming.NEUTRAL // Default fallback
        }
    }
}
