package com.example.domain

import kotlin.math.max
import kotlin.math.roundToInt

data class ZakatCalculation(
    val netZakatableWealth: Double,
    val goldNisabValue: Double,
    val silverNisabValue: Double,
    val activeNisabValue: Double,
    val isZakatDue: Boolean,
    val zakatPayable: Double
)

data class InheritanceShare(
    val heirLabel: String,
    val fractionLabel: String,
    val percentage: Double,
    val amount: Double
)

object CalculatorsEngine {

    fun calculateZakat(
        cashAndSavings: Double,
        goldGrams24k: Double,
        goldPricePerGram: Double,
        silverGrams: Double,
        silverPricePerGram: Double,
        tradeInventoryAndStocks: Double,
        shortTermDebtsOwed: Double,
        useSilverNisab: Boolean
    ): ZakatCalculation {
        val goldValue = goldGrams24k * goldPricePerGram
        val silverValue = silverGrams * silverPricePerGram
        val gross = max(0.0, cashAndSavings + goldValue + silverValue + tradeInventoryAndStocks)
        val net = max(0.0, gross - max(0.0, shortTermDebtsOwed))
        val goldNisab = 85.0 * goldPricePerGram
        val silverNisab = 595.0 * silverPricePerGram
        val nisab = if (useSilverNisab) silverNisab else goldNisab
        val due = net >= nisab && nisab > 0.0
        return ZakatCalculation(
            netZakatableWealth = net,
            goldNisabValue = goldNisab,
            silverNisabValue = silverNisab,
            activeNisabValue = nisab,
            isZakatDue = due,
            zakatPayable = if (due) net * 0.025 else 0.0
        )
    }

    /**
     * Classical Sunni Faraid calculator for primary heirs:
     * Spouse (None / Husband / Wife), Father, Mother, Sons, Daughters.
     * Supports fixed shares, Awl proportional scaling when shares exceed 1.0,
     * and Asabah (2:1 male:female) residual distribution.
     */
    fun calculateInheritance(
        netEstate: Double,
        spouse: String, // "NONE", "HUSBAND", "WIFE"
        hasFather: Boolean,
        hasMother: Boolean,
        sons: Int,
        daughters: Int
    ): List<InheritanceShare> {
        if (netEstate <= 0.0) return emptyList()
        val hasChildren = sons > 0 || daughters > 0
        val fixedFractions = mutableMapOf<String, Pair<String, Double>>()

        when (spouse) {
            "HUSBAND" -> fixedFractions["Husband"] = if (hasChildren) "1/4" to 0.25 else "1/2" to 0.50
            "WIFE" -> fixedFractions["Wife"] = if (hasChildren) "1/8" to 0.125 else "1/4" to 0.25
        }

        if (hasMother) {
            fixedFractions["Mother"] = if (hasChildren) "1/6" to (1.0 / 6.0) else "1/3" to (1.0 / 3.0)
        }
        if (hasFather && hasChildren) {
            fixedFractions["Father"] = "1/6" to (1.0 / 6.0)
        }
        if (daughters > 0 && sons == 0) {
            fixedFractions["Daughters ($daughters)"] = if (daughters == 1) "1/2" to 0.5 else "2/3" to (2.0 / 3.0)
        }

        var totalFixed = fixedFractions.values.sumOf { it.second }

        // Handle Awl (if fixed shares exceed 100%)
        val results = mutableListOf<InheritanceShare>()
        if (totalFixed >= 1.0) {
            for ((heir, pair) in fixedFractions) {
                val scaled = pair.second / totalFixed
                results.add(
                    InheritanceShare(
                        heirLabel = heir,
                        fractionLabel = "${pair.first} (Awl)",
                        percentage = scaled * 100.0,
                        amount = netEstate * scaled
                    )
                )
            }
            return results
        }

        for ((heir, pair) in fixedFractions) {
            results.add(
                InheritanceShare(
                    heirLabel = heir,
                    fractionLabel = pair.first,
                    percentage = pair.second * 100.0,
                    amount = netEstate * pair.second
                )
            )
        }

        val remainder = max(0.0, 1.0 - totalFixed)
        if (remainder > 0.0001) {
            when {
                sons > 0 -> {
                    val units = sons * 2 + daughters
                    val sonShare = remainder * (2.0 * sons / units)
                    results.add(
                        InheritanceShare(
                            heirLabel = if (sons == 1) "Son" else "Sons ($sons)",
                            fractionLabel = "Asabah (Residue)",
                            percentage = sonShare * 100.0,
                            amount = netEstate * sonShare
                        )
                    )
                    if (daughters > 0) {
                        val daughterShare = remainder * (1.0 * daughters / units)
                        results.add(
                            InheritanceShare(
                                heirLabel = if (daughters == 1) "Daughter" else "Daughters ($daughters)",
                                fractionLabel = "Asabah (2:1 w/ Son)",
                                percentage = daughterShare * 100.0,
                                amount = netEstate * daughterShare
                            )
                        )
                    }
                }
                hasFather -> {
                    val existingFather = results.indexOfFirst { it.heirLabel == "Father" }
                    if (existingFather >= 0) {
                        val cur = results[existingFather]
                        val combined = (cur.percentage / 100.0) + remainder
                        results[existingFather] = InheritanceShare(
                            heirLabel = "Father",
                            fractionLabel = "1/6 + Residue",
                            percentage = combined * 100.0,
                            amount = netEstate * combined
                        )
                    } else {
                        results.add(
                            InheritanceShare(
                                heirLabel = "Father",
                                fractionLabel = "Asabah (Residue)",
                                percentage = remainder * 100.0,
                                amount = netEstate * remainder
                            )
                        )
                    }
                }
                else -> {
                    // Radd back to blood fixed heirs (excluding spouse in classical majority)
                    val bloodIndices = results.indices.filter { results[it].heirLabel !in listOf("Husband", "Wife") }
                    val bloodTotal = bloodIndices.sumOf { results[it].percentage / 100.0 }
                    if (bloodTotal > 0.0) {
                        for (idx in bloodIndices) {
                            val item = results[idx]
                            val base = item.percentage / 100.0
                            val updated = base + remainder * (base / bloodTotal)
                            results[idx] = item.copy(
                                fractionLabel = "${item.fractionLabel} + Radd",
                                percentage = updated * 100.0,
                                amount = netEstate * updated
                            )
                        }
                    }
                }
            }
        }
        return results
    }

    data class WaterUsageResult(
        val estimatedLiters: Double,
        val propheticLiters: Double,
        val ratio: Int,
        val feedback: String
    )

    fun estimateAblutionWater(isGhusl: Boolean, tapSeconds: Int): WaterUsageResult {
        // Standard household faucet ~ 0.11 liters per second (~6.5 L/min)
        val used = max(0.2, tapSeconds * 0.11)
        val prophetic = if (isGhusl) 3.0 else 0.75 // 1 Sa' (~3L) for Ghusl, 1 Mudd (~0.75L) for Wudu
        val ratio = max(1, (used / prophetic).roundToInt())
        val note = if (used <= prophetic * 1.5) {
            "Ma sha Allah! Close to the Prophetic Sunnah of conserving water."
        } else {
            "Try turning the tap off between washings — the Prophet ﷺ performed Wudu with ~0.75 L (1 Mudd) and Ghusl with ~3 L (1 Sa')."
        }
        return WaterUsageResult(used, prophetic, ratio, note)
    }
}
