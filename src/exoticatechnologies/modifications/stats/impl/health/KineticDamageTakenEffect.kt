package exoticatechnologies.modifications.stats.impl.health

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.MutableStat
import exoticatechnologies.modifications.stats.UpgradeMutableStatEffect

class KineticDamageTakenEffect : UpgradeMutableStatEffect() {
    override var negativeIsBuff: Boolean = true

    override val key: String
        get() = "kineticDamageTaken"

    override fun getStat(stats: MutableShipStatsAPI): MutableStat {
        return stats.kineticDamageTakenMult
    }
}