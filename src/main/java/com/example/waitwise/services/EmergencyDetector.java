package com.example.waitwise.services;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Basic emergency detection logic
public class EmergencyDetector {

    // Negation words
    private static final Set<String> NEGATION_WORDS = new HashSet<>(Arrays.asList(
        "no", "not", "never", "none", "nothing", "nowhere", "neither", "nor",
        "don't", "dont", "doesn't", "doesnt", "didn't", "didnt",
        "isn't", "isnt", "aren't", "arent", "wasn't", "wasnt", "weren't", "werent",
        "won't", "wont", "wouldn't", "wouldnt", "shouldn't", "shouldnt",
        "couldn't", "couldnt", "can't", "cant", "cannot",
        "hardly", "barely", "scarcely", "without", "lack", "lacking",
        "deny", "denied", "denying", "refuse", "refused",
        "false", "fake", "fabricated", "lying", "pretend", "pretending"
    ));

    // Emergency keywords organized by category with weights
    private static final Map<String, Integer> KEYWORD_WEIGHTS = new LinkedHashMap<>();

    static {
        // Medical emergencies (high weight)
        for (String kw : new String[]{
            "medical", "hospital", "ambulance", "bleeding", "unconscious",
            "seizure", "heart attack", "stroke", "fracture", "surgery",
            "icu", "critical condition", "dying", "collapsed", "fainted",
            "choking", "anaphylaxis", "cardiac", "resuscitation", "coma",
            "overdose", "poisoning", "burn", "severe pain", "concussion",
            "hemorrhage", "haemorrhage", "miscarriage", "labor", "labour",
            "contractions", "delivery", "pregnant emergency",
            "breathing difficulty", "breathless", "cannot breathe",
            "chest pain", "blood pressure", "diabetic emergency",
            "asthma attack", "allergic reaction", "swelling",
            "broken bone", "dislocation", "wound", "laceration",
            "head injury", "spinal injury", "paralysis", "convulsions",
            "accident", "crashed", "collision"
        }) {
            KEYWORD_WEIGHTS.put(kw, 3);
        }

        // Strong urgency words (high weight)
        for (String kw : new String[]{
            "emergency", "life threatening", "life-threatening",
            "dire", "critical", "desperate", "dying", "death",
            "kill", "killed", "fatal", "lethal", "danger",
            "save my life", "save his life", "save her life",
            "help me", "please help", "need help immediately",
            "very serious", "extremely urgent"
        }) {
            KEYWORD_WEIGHTS.put(kw, 3);
        }

        // Moderate urgency (medium weight)
        for (String kw : new String[]{
            "urgent", "hurry", "rush", "immediately", "asap",
            "time sensitive", "time-sensitive", "deadline",
            "right away", "as soon as possible", "cannot wait",
            "can't wait", "running out of time", "last chance",
            "very important", "extremely important"
        }) {
            KEYWORD_WEIGHTS.put(kw, 2);
        }

        // Travel emergencies (medium weight)
        for (String kw : new String[]{
            "flight", "airport", "boarding", "departure",
            "train leaving", "bus leaving", "traveling",
            "visa expiry", "visa expiring", "deportation",
            "stranded", "missed connection", "layover",
            "plane", "gate closing"
        }) {
            KEYWORD_WEIGHTS.put(kw, 2);
        }

        // Legal/Family emergencies (high weight)
        for (String kw : new String[]{
            "funeral", "court hearing", "court date", "police",
            "arrest", "arrested", "custody", "kidnapping", "kidnapped",
            "missing person", "missing child", "domestic violence",
            "abuse", "threatened", "stalking", "restraining order",
            "bail", "incarcerated", "prison", "jail",
            "child protective", "legal emergency"
        }) {
            KEYWORD_WEIGHTS.put(kw, 3);
        }

        // Disaster emergencies (high weight)
        for (String kw : new String[]{
            "fire", "flood", "earthquake", "evacuate", "evacuation",
            "trapped", "explosion", "gas leak", "building collapse",
            "tornado", "hurricane", "tsunami", "landslide",
            "power outage", "blackout"
        }) {
            KEYWORD_WEIGHTS.put(kw, 3);
        }

        // Emotional distress indicators (low weight - supporting evidence)
        for (String kw : new String[]{
            "crying", "panic", "panicking", "scared", "terrified",
            "frightened", "anxious", "worried sick", "distressed",
            "shaking", "trembling", "hysterical"
        }) {
            KEYWORD_WEIGHTS.put(kw, 1);
        }

        // Injury-related verbs and descriptors (medium weight)
        for (String kw : new String[]{
            "injured", "hurt", "fell", "fallen", "hit",
            "ran over", "attacked", "assaulted", "stabbed", "shot",
            "bitten", "stung", "electrocuted"
        }) {
            KEYWORD_WEIGHTS.put(kw, 2);
        }
    }

    // Minimum score for emergency
    private static final int EMERGENCY_THRESHOLD = 3;

    // Main detection method
    public static boolean isEmergency(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String normalizedText = text.toLowerCase().trim();
        // Remove extra whitespace
        normalizedText = normalizedText.replaceAll("\\s+", " ");

        int totalScore = 0;
        String[] words = normalizedText.split("\\s+");

        // Check each keyword/phrase
        for (Map.Entry<String, Integer> entry : KEYWORD_WEIGHTS.entrySet()) {
            String keyword = entry.getKey();
            int weight = entry.getValue();

            // 1. Exact or substring match (standard)
            if (normalizedText.contains(keyword)) {
                int index = normalizedText.indexOf(keyword);
                while (index >= 0) {
                    if (!isNegatedAtPosition(normalizedText, words, index, keyword)) {
                        totalScore += weight;
                    } else {
                        totalScore -= weight;
                    }
                    index = normalizedText.indexOf(keyword, index + keyword.length());
                }
            } 
            // 2. Fuzzy match for single-word keywords (typo tolerance)
            else if (!keyword.contains(" ")) {
                for (String word : words) {
                    // Clean word of punctuation for fuzzy check
                    String cleanWord = word.replaceAll("[^a-z]", "");
                    if (cleanWord.length() < 3) continue;

                    int distance = calculateLevenshteinDistance(cleanWord, keyword);
                    // Tolerance: 1 error for words > 4 chars, 2 errors for words > 7 chars
                    int tolerance = keyword.length() > 7 ? 2 : 1;

                    if (distance > 0 && distance <= tolerance) {
                        // Fuzzy match found!
                        totalScore += weight;
                        break; // Only count once per keyword
                    }
                }
            }
        }

        return totalScore >= EMERGENCY_THRESHOLD;
    }

    // Typo check logic
    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
    }

    // Context check for negations
    private static boolean isNegatedAtPosition(String fullText, String[] words, int charIndex, String keyword) {
        // Get the text before the keyword
        String precedingText = fullText.substring(Math.max(0, charIndex - 30), charIndex).trim();

        if (precedingText.isEmpty()) {
            return false;
        }

        // Split preceding text into words and check last N words for negation
        String[] precedingWords = precedingText.split("\\s+");
        int windowSize = Math.min(4, precedingWords.length);

        for (int i = precedingWords.length - windowSize; i < precedingWords.length; i++) {
            String word = precedingWords[i].replaceAll("[^a-z']", "");
            if (NEGATION_WORDS.contains(word)) {
                return true;
            }
        }

        // Also check for common negation patterns
        String nearContext = fullText.substring(Math.max(0, charIndex - 40), 
            Math.min(fullText.length(), charIndex + keyword.length() + 20));
        
        if (nearContext.matches(".*\\b(no|not|without|lack of|absence of|don't have|do not have|isn't|is not|there is no|there are no|there's no)\\b.*" + Pattern.quote(keyword) + ".*")) {
            return true;
        }

        return false;
    }
}
