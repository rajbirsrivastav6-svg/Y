package com.example.engine

import com.example.data.LumaRepository
import com.example.data.MemoryEntity
import com.example.data.ResponseCardType
import com.example.data.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantReply(
    val text: String,
    val cardType: ResponseCardType = ResponseCardType.NONE,
    val cardTitle: String = "",
    val cardSubtitle: String = "",
    val requestedThemeMode: ThemeMode? = null,
    val startTimerSeconds: Int? = null,
    val timerLabel: String? = null
)

class OfflineAssistantEngine(
    private val repository: LumaRepository
) {
    private val offlineTips = listOf(
        "Tip: Use Dark Mode at night to reduce glare and save battery on OLED screens.",
        "Tip: You can teach me personal notes like 'Remember that wifi pin is 4580' and ask me later offline.",
        "Tip: Try quick conversions like '12 km to miles' or '37 c to f' anytime without internet.",
        "Tip: Break big study topics into 25-minute offline focus timers for sharper retention."
    )

    private val motivationNotes = listOf(
        "Small daily progress beats occasional perfection. Pick one task and finish it now.",
        "Chhote kadam roz lo — lagatar mehnat hamesha bade results laati hai.",
        "Stay steady, keep distractions muted, and focus on the next single step.",
        "Har nayi shuruat thodi mushkil lagti hai, par aap kar sakte ho!"
    )

    private val scienceNotes = listOf(
        "Light travels roughly 299,792 kilometers per second in a vacuum.",
        "OLED displays turn black pixels completely off, which is why dark themes consume less power.",
        "Human ears can typically hear sound frequencies between 20 Hz and 20,000 Hz.",
        "Water reaches its maximum density at around 4 degrees Celsius."
    )

    suspend fun processInput(rawInput: String): AssistantReply {
        val input = rawInput.trim()
        val lower = input.lowercase(Locale.getDefault())

        // 1. Theme / Dark Mode commands
        parseThemeCommand(lower)?.let { mode ->
            val modeDesc = when (mode) {
                ThemeMode.DARK -> "Dark Mode is now active — comfortable on your eyes and OLED-friendly."
                ThemeMode.LIGHT -> "Switched to Light Mode for bright daytime readability."
                ThemeMode.SYSTEM -> "Theme is now synced with your device's System setting."
            }
            return AssistantReply(
                text = modeDesc,
                cardType = ResponseCardType.THEME_CHANGED,
                cardTitle = "Theme Updated",
                cardSubtitle = "${mode.label} Mode enabled offline",
                requestedThemeMode = mode
            )
        }

        // 2. Timer / Focus countdown commands
        parseTimerCommand(lower)?.let { (seconds, label) ->
            val formatted = formatDuration(seconds)
            return AssistantReply(
                text = "Starting your $formatted offline timer for $label.",
                cardType = ResponseCardType.TIMER_STARTED,
                cardTitle = "$formatted Timer Started",
                cardSubtitle = label,
                startTimerSeconds = seconds,
                timerLabel = label
            )
        }

        // 3. Teach / Remember custom knowledge in Room
        parseTeachMemory(input)?.let { (key, answer) ->
            repository.addMemory(key, answer)
            return AssistantReply(
                text = "Saved to your offline memory vault. Ask me about \"$key\" anytime.",
                cardType = ResponseCardType.MEMORY_SAVED,
                cardTitle = key,
                cardSubtitle = answer
            )
        }

        // 4. Add Task / Reminder
        parseTaskCommand(input)?.let { title ->
            val isHigh = lower.contains("urgent") || lower.contains("important") || lower.contains("jaroori")
            repository.addTask(title = title, isHighPriority = isHigh)
            return AssistantReply(
                text = "Added \"$title\" to your offline Tasks in the Vault tab.",
                cardType = ResponseCardType.TASK_SAVED,
                cardTitle = title,
                cardSubtitle = if (isHigh) "High Priority • Saved locally" else "Saved to Offline Vault"
            )
        }

        // 5. Save Note
        parseNoteCommand(input)?.let { (title, content) ->
            repository.addNote(title = title, content = content)
            return AssistantReply(
                text = "Saved your offline note under \"$title\".",
                cardType = ResponseCardType.NOTE_SAVED,
                cardTitle = title,
                cardSubtitle = content.take(80)
            )
        }

        // 6. Query saved Tasks or Notes
        if (lower.contains("my task") || lower.contains("show task") || lower.contains("todo list") ||
            lower.contains("meri task") || lower.contains("tasks dikhao")
        ) {
            val tasks = repository.getPendingTasks()
            return if (tasks.isEmpty()) {
                AssistantReply("Your pending task list is clear. Say \"Add task: Drink water\" to create one offline.")
            } else {
                val list = tasks.take(6).mapIndexed { i, t -> "${i + 1}. ${t.title}" }.joinToString("\n")
                AssistantReply("You have ${tasks.size} pending offline task(s):\n$list")
            }
        }

        if (lower.contains("my note") || lower.contains("show note") || lower.contains("notes dikhao")) {
            val notes = repository.getAllNotes()
            return if (notes.isEmpty()) {
                AssistantReply("No offline notes yet. Try saying \"Note: Study plan — revise math formulas\".")
            } else {
                val list = notes.take(5).joinToString("\n") { "• ${it.title}: ${it.content.take(50)}" }
                AssistantReply("Here are your latest offline notes (${notes.size} total):\n$list")
            }
        }

        // 7. Unit Converter
        OfflineUnitConverter.tryConvert(input)?.let { conv ->
            return AssistantReply(
                text = "${conv.sourceSummary} equals ${conv.targetSummary}.",
                cardType = ResponseCardType.CONVERSION,
                cardTitle = conv.targetSummary,
                cardSubtitle = "Converted from ${conv.sourceSummary}"
            )
        }

        // 8. Math Evaluator
        OfflineMathEvaluator.tryEvaluate(input)?.let { math ->
            return AssistantReply(
                text = "The answer is ${math.formattedResult}.",
                cardType = ResponseCardType.MATH,
                cardTitle = "= ${math.formattedResult}",
                cardSubtitle = math.expression
            )
        }

        // 9. User-taught offline memories lookup
        val memories = repository.getAllMemories()
        matchMemory(lower, memories)?.let { matched ->
            return AssistantReply(
                text = "From your offline memory (${matched.triggerKey}): ${matched.answer}",
                cardType = ResponseCardType.MEMORY_SAVED,
                cardTitle = matched.triggerKey,
                cardSubtitle = matched.answer
            )
        }

        // 10. Time / Date / Day
        if (lower.contains("time") || lower.contains("baja") || lower.contains("samay") || lower.contains("waqt")) {
            val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return AssistantReply("It is currently $now on your device clock.")
        }

        if (lower.contains("date") || lower.contains("today") || lower.contains("tareekh") || lower.contains("aaj kya din")) {
            val today = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            return AssistantReply("Today is $today.")
        }

        // 11. Breathing / Calm Focus Guide
        if (lower.contains("breath") || lower.contains("relax") || lower.contains("stress") || lower.contains("calm")) {
            return AssistantReply(
                text = "Try this 4-7-8 calm breathing cycle: inhale slowly for 4 seconds, hold gently for 7 seconds, and exhale for 8 seconds.",
                cardType = ResponseCardType.BREATHING_GUIDE,
                cardTitle = "4-7-8 Calm Breathing",
                cardSubtitle = "Inhale 4s • Hold 7s • Exhale 8s"
            )
        }

        // 12. Study / Productivity offline tips
        if (lower.contains("study") || lower.contains("padhai") || lower.contains("focus") || lower.contains("pomodoro")) {
            return AssistantReply(
                "Offline Focus Plan:\n1. Choose one clear task for 25 minutes.\n2. Say \"Start 25 minute timer\".\n3. Write key takeaways into your Luma Notes."
            )
        }

        // 13. Greetings, Tips, Motivation, Science Facts, Help
        if (lower.matches(Regex("""^(hi|hello|hey|hii+|namaste|salam|good\s+(morning|afternoon|evening)|kaise\s+ho|kya\s+haal).*"""))) {
            return AssistantReply(
                "Hello! I'm Luma, running 100% offline on your device. Ask me to solve math, convert units, save tasks or notes, start a timer, or toggle Dark and Light mode!"
            )
        }

        if (lower.contains("tip") || lower.contains("idea")) {
            return AssistantReply(offlineTips.random())
        }

        if (lower.contains("motivat") || lower.contains("quote") || lower.contains("inspire")) {
            return AssistantReply(motivationNotes.random())
        }

        if (lower.contains("fact") || lower.contains("gyaan") || lower.contains("science")) {
            return AssistantReply("Offline Science Note: ${scienceNotes.random()}")
        }

        if (lower.contains("offline") || lower.contains("internet") || lower.contains("wifi")) {
            return AssistantReply(
                "Yes, Luma runs completely offline. Chats, tasks, notes, memories, math, conversions, timers, and theme settings all stay locally on your device."
            )
        }

        if (lower.contains("help") || lower.contains("kya kar sakte") || lower.contains("what can you do")) {
            return AssistantReply(
                "Offline capabilities you can try right now:\n" +
                    "• Switch themes: \"Turn on dark mode\" / \"Light mode\"\n" +
                    "• Math & %: \"25% of 480\" or \"sqrt(144) + 18\"\n" +
                    "• Unit converter: \"10 km to miles\" or \"37 c to f\"\n" +
                    "• Tasks & Notes: \"Add task: Buy milk\" / \"Note: Ideas\"\n" +
                    "• Teach me facts: \"Remember that locker code is 4820\"\n" +
                    "• Timers: \"Start 5 minute timer\""
            )
        }

        // 14. Clear, honest offline fallback
        return AssistantReply(
            "I'm working in 100% Offline Mode without cloud access, so I couldn't match \"$input\" to a local tool.\n\n" +
                "Try commands like:\n" +
                "• 18% of 2500\n" +
                "• 5 kg to pounds\n" +
                "• Add task: Revise chapter 3\n" +
                "• Remember that bike slot is B12\n" +
                "• Turn on dark mode / Start 5 minute timer"
        )
    }

    private fun parseThemeCommand(lower: String): ThemeMode? {
        val mentionsTheme = lower.contains("mode") || lower.contains("theme")
        if (!mentionsTheme) return null
        return when {
            lower.contains("dark") || lower.contains("night") -> ThemeMode.DARK
            lower.contains("light") || lower.contains("day") || lower.contains("white") -> ThemeMode.LIGHT
            lower.contains("system") || lower.contains("auto") -> ThemeMode.SYSTEM
            else -> null
        }
    }

    private fun parseTimerCommand(lower: String): Pair<Int, String>? {
        if (!lower.contains("timer") && !lower.contains("countdown") && !lower.contains("alarm")) return null
        val match = Regex("""(\d+)\s*(sec|second|seconds|min|minute|minutes|hr|hour|hours)""").find(lower)
            ?: return if (lower.contains("focus")) (25 * 60) to "Focus Session" else null
        val amount = match.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: return null
        val unit = match.groupValues[2]
        val seconds = when {
            unit.startsWith("hr") || unit.startsWith("hour") -> amount * 3600
            unit.startsWith("min") -> amount * 60
            else -> amount
        }.coerceAtMost(12 * 3600)
        val label = if (lower.contains("focus") || lower.contains("study")) "Focus Session" else "Offline Timer"
        return seconds to label
    }

    private fun parseTaskCommand(input: String): String? {
        val regex = Regex(
            """^(?:add\s+task|new\s+task|task|todo|remind\s+me\s+to|task\s+add\s+karo)[:\s]+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        regex.find(input)?.let { m ->
            val title = m.groupValues[1].trim()
            if (title.isNotBlank() && !title.lowercase().startsWith("show")) return title
        }
        return null
    }

    private fun parseNoteCommand(input: String): Pair<String, String>? {
        val regex = Regex("""^(?:save\s+note|add\s+note|note|note\s+likho)[:\s]+(.+)$""", RegexOption.IGNORE_CASE)
        val match = regex.find(input) ?: return null
        val body = match.groupValues[1].trim()
        if (body.isBlank()) return null
        val parts = body.split("—", "-", limit = 2)
        return if (parts.size == 2 && parts[0].trim().length in 2..36) {
            parts[0].trim() to parts[1].trim()
        } else {
            val title = body.split(" ").take(4).joinToString(" ")
            title to body
        }
    }

    private fun parseTeachMemory(input: String): Pair<String, String>? {
        val rememberThat = Regex(
            """^(?:remember\s+that|yaad\s+rakho\s+ki|yaad\s+rakho)\s+(.+?)\s+(?:is|are|=|hai)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        rememberThat.find(input)?.let { m ->
            val key = m.groupValues[1].trim().removePrefix("my ").trim()
            val value = m.groupValues[2].trim()
            if (key.isNotBlank() && value.isNotBlank()) return key to value
        }

        val teachPattern = Regex("""^teach[:\s]+(.+?)\s*=\s*(.+)$""", RegexOption.IGNORE_CASE)
        teachPattern.find(input)?.let { m ->
            val key = m.groupValues[1].trim()
            val value = m.groupValues[2].trim()
            if (key.isNotBlank() && value.isNotBlank()) return key to value
        }

        val rememberSimple = Regex("""^(?:remember|save\s+memory)[:\s]+(.+)$""", RegexOption.IGNORE_CASE)
        rememberSimple.find(input)?.let { m ->
            val raw = m.groupValues[1].trim()
            if (raw.length > 3) {
                val title = raw.split(" ").take(3).joinToString(" ")
                return title to raw
            }
        }
        return null
    }

    private fun matchMemory(lowerQuery: String, memories: List<MemoryEntity>): MemoryEntity? {
        if (memories.isEmpty()) return null
        val stopWords = setOf("what", "is", "my", "the", "a", "an", "tell", "me", "about", "kya", "hai", "mera", "meri")
        val queryTokens = lowerQuery.split(Regex("""\W+""")).filter { it.length > 1 && it !in stopWords }.toSet()
        if (queryTokens.isEmpty()) return null

        var best: MemoryEntity? = null
        var bestScore = 0
        for (memory in memories) {
            val keyLower = memory.triggerKey.lowercase()
            if (lowerQuery.contains(keyLower)) return memory
            val keyTokens = keyLower.split(Regex("""\W+""")).filter { it.length > 1 && it !in stopWords }.toSet()
            val overlap = queryTokens.intersect(keyTokens).size
            if (overlap > bestScore && overlap >= 1) {
                bestScore = overlap
                best = memory
            }
        }
        return best
    }

    private fun formatDuration(totalSeconds: Int): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 && secs > 0 -> "${mins}m ${secs}s"
            mins > 0 -> "$mins min"
            else -> "$secs sec"
        }
    }
}
