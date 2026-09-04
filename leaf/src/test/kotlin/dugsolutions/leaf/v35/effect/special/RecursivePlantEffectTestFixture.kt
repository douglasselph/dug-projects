package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard

internal object RecursivePlantEffectTestFixture {

    fun plant(
        name: String,
        type: PlantType,
        effect: GameEffect
    ): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = name,
            type = type,
            cost = 1,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = effect
        )

    fun graft(
        player: Player,
        card: PlantCard
    ): CreatureCard =
        player.creature.graft(
            card = card,
            placement =
                player.creature
                    .legalPlacements(card)
                    .first()
        )

    fun faceUp(
        player: Player,
        card: CreatureCard
    ): CreatureCard {
        check(
            player.creature.faceUp(
                card.id
            )
        )
        return player.creature.get(
            card.id
        )!!
    }
}
