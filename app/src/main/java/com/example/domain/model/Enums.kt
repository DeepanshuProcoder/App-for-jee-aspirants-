package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ChemistryColor
import com.example.ui.theme.ChemistryColorDark
import com.example.ui.theme.MathsColor
import com.example.ui.theme.MathsColorDark
import com.example.ui.theme.PhysicsColor
import com.example.ui.theme.PhysicsColorDark

enum class Subject(val label: String, val shortLabel: String) {
    PHYSICS("Physics", "PHY"),
    CHEMISTRY("Chemistry", "CHEM"),
    MATHEMATICS("Mathematics", "MATH");

    fun accentColor(isDark: Boolean): Color = when (this) {
        PHYSICS -> if (isDark) PhysicsColor else PhysicsColorDark
        CHEMISTRY -> if (isDark) ChemistryColor else ChemistryColorDark
        MATHEMATICS -> if (isDark) MathsColor else MathsColorDark
    }

    companion object {
        fun fromString(value: String): Subject =
            entries.firstOrNull { it.name == value } ?: PHYSICS
    }
}

enum class ChemistrySection(val label: String) {
    NONE("General"),
    PHYSICAL("Physical Chemistry"),
    INORGANIC("Inorganic Chemistry"),
    ORGANIC("Organic Chemistry");

    val firestoreValue: String
        get() = if (this == NONE) "" else name

    companion object {
        fun fromString(value: String): ChemistrySection =
            if (value.isBlank()) NONE else entries.firstOrNull { it.name == value } ?: NONE
    }
}

enum class ClassLevel(val label: String) {
    CLASS_11("Class 11"),
    CLASS_12("Class 12");

    companion object {
        fun fromString(value: String): ClassLevel =
            entries.firstOrNull { it.name == value } ?: CLASS_11
    }
}

enum class ChapterStatus(val label: String) {
    NOT_STARTED("Not Started"),
    CURRENTLY_STUDYING("Currently Studying"),
    THEORY_COMPLETED("Theory Completed"),
    PRACTICE_IN_PROGRESS("Practice in Progress"),
    REVISION_IN_PROGRESS("Revision in Progress"),
    STRONG("Strong"),
    NEEDS_ATTENTION("Needs Attention");

    companion object {
        fun fromString(value: String): ChapterStatus =
            entries.firstOrNull { it.name == value } ?: NOT_STARTED
    }
}

enum class RevisionOutcome(val label: String, val description: String) {
    EASY("Easy", "Recalled concepts and solved questions comfortably"),
    GOOD("Good", "Remembered most concepts with minor difficulty"),
    DIFFICULT("Difficult", "Required hints or struggled with applications"),
    FORGOT("Forgot", "Could not recall important concepts");

    companion object {
        fun fromString(value: String): RevisionOutcome =
            entries.firstOrNull { it.name == value } ?: GOOD
    }
}

enum class ErrorCategory(val label: String) {
    CONCEPTUAL("Conceptual Error"),
    APPLICATION("Application Error"),
    CALCULATION("Calculation Error"),
    QUESTION_READING("Question-Reading Error"),
    TIME_MANAGEMENT("Time-Management Error");

    companion object {
        fun fromString(value: String): ErrorCategory =
            entries.firstOrNull { it.name == value } ?: CONCEPTUAL
    }
}

enum class ReattemptStatus(val label: String) {
    PENDING("Pending"),
    SCHEDULED("Scheduled"),
    SOLVED_CORRECTLY("Solved Correctly"),
    NEEDS_REVIEW("Needs Review");

    companion object {
        fun fromString(value: String): ReattemptStatus =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

enum class NoteType(val label: String) {
    FORMULA("Formula"),
    DERIVATION("Derivation"),
    REACTION("Important Reaction"),
    EXCEPTION("Exception"),
    CONCEPT("Short Concept"),
    TRICK("Problem Trick"),
    EXAMPLE("Difficult Example");

    companion object {
        fun fromString(value: String): NoteType =
            entries.firstOrNull { it.name == value } ?: FORMULA
    }
}

enum class TestType(val label: String) {
    ALLEN_MINOR("ALLEN Minor"),
    ALLEN_MAJOR("ALLEN Major"),
    JEE_MAIN_MOCK("JEE Main Mock"),
    JEE_ADV_MOCK("JEE Adv Mock"),
    CHAPTER_TEST("Chapter Test"),
    CUSTOM("Custom Test");

    companion object {
        fun fromString(value: String): TestType =
            entries.firstOrNull { it.name == value } ?: ALLEN_MINOR
    }
}

enum class CalendarEventType(val label: String) {
    ALLEN_TEST("ALLEN Test"),
    STUDY_TARGET("Study Target"),
    CUSTOM_REVISION("Custom Revision");

    companion object {
        fun fromString(value: String): CalendarEventType =
            entries.firstOrNull { it.name == value } ?: STUDY_TARGET
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromString(value: String): ThemeMode =
            entries.firstOrNull { it.name == value } ?: DARK
    }
}
