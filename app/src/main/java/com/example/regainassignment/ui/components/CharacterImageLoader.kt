package com.example.regainassignment.ui.components

import com.example.regainassignment.R

object CharacterImageLoader {
    fun getCharacterImage(characterId: Int): Int? {
        // Return null for all characters to display emojis instead of images
        return when (characterId) {
            // 1 -> R.drawable.app_icon_raccoon // Togo the Raccoon
            // 2 -> R.drawable.character_cat_new // Cat
            // 3 -> R.drawable.character_dog_3d 
            // 4 -> R.drawable.character_bear_3d
            // 5 -> R.drawable.character_fox_3d
            // 6 -> R.drawable.character_panda_3d
            else -> null
        }
    }
}
