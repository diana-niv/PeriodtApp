package com.example.periodtapp.db

import com.example.periodtapp.R

enum class FlowType (val iconRes: Int) {
    LIGHT (R.drawable.light_flow),
    MEDIUM(R.drawable.medium_flow),
    HEAVY(R.drawable.heavy_flow),
    NO_PERIOD(R.drawable.other)
}

enum class SpottingType(val iconRes: Int) {
    RED(R.drawable.light_flow),
    BROWN(R.drawable.light_flow)
}

enum class DischargeType(val iconRes: Int?) {
    NONE(null),
    STICKY(R.drawable.sticky),
    WET(R.drawable.wet),
    CREAMY(R.drawable.creamy),
    EGGWHITE(R.drawable.eggwhite),
    ATYPICAL(R.drawable.atypical)
}

enum class CollectionMethod(val iconRes: Int) {
    TAMPON(R.drawable.tampon),
    PAD(R.drawable.pads),
    PANTY_LINER(R.drawable.pads),
    CUP(R.drawable.cup),
    PERIOD_UNDERWEAR(R.drawable.underwear)
}

enum class MoodType(val iconRes: Int) {
    HAPPY(R.drawable.happy),
    EXCITED(R.drawable.excited),
    TIRED(R.drawable.tired),
    ANNOYED(R.drawable.annoyed),
    MOOD_SWINGS(R.drawable.mood_swings),
    SAD(R.drawable.sad),
    CALM(R.drawable.calm)
}

enum class SymptomType(val iconRes: Int) {
    EVERYTHING_OKAY(R.drawable.everything_ok),
    CRAMPS(R.drawable.cramps),
    HEADACHE(R.drawable.headache),
    ACNE(R.drawable.acne),
    BACKACHE(R.drawable.backache),
    FATIGUE(R.drawable.fatigue),
    INSOMNIA(R.drawable.insomnia)
}

enum class ActivityType(val iconRes: Int) {
    NO_EXERCISE(R.drawable.no_exercise),
    GYM(R.drawable.gym),
    DANCE(R.drawable.dance),
    RUN(R.drawable.run),
    WALK(R.drawable.walk),
    SWIMMING(R.drawable.swim),
    YOGA(R.drawable.yoga),
    OTHER(R.drawable.other)
}


enum class CravingType(val iconRes: Int) {
    SWEET(R.drawable.sweet),
    SALTY(R.drawable.salty),
    SPICY(R.drawable.spicy),
    CARBS(R.drawable.bread),
    GREASY(R.drawable.greasy)
}

enum class SexType(val iconRes: Int) {
    DID_NOT_HAVE_SEX(R.drawable.didnt_have_sex),
    PROTECTED(R.drawable.resource_protected),
    UNPROTECTED(R.drawable.not_protected),
    HIGH_SEX_DRIVE(R.drawable.high_sex_drive),
    LOW_SEX_DRIVE(R.drawable.low_sex_drive)
}

enum class Energy(val iconRes: Int) {
    ENERGETIC(R.drawable.high),
    OK(R.drawable.ok),
    LOW(R.drawable.low),
    EXHAUSTED(R.drawable.exhausted)

}

enum class CalendarDayType {
    NONE, ACTUAL_PERIOD, PREDICTED_PERIOD
}

fun Enum<*>.prettyName(): String {
    return this.name
        .replace("_", " ")  // 1. Turn "MOOD_SWINGS" -> "MOOD SWINGS"
        .lowercase()        // 2. Turn "MOOD SWINGS" -> "mood swings"
        .replaceFirstChar { it.titlecase() } // 3. Turn "mood swings" -> "Mood swings"
}

