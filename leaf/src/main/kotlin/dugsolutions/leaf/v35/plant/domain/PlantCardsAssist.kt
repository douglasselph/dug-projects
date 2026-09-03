package dugsolutions.leaf.v35.plant.domain

fun List<PlantCard>.getByType(type: PlantType): List<PlantCard> {
    return this.filter { it.type == type }
}

fun List<PlantCard>.getFlourishTypes(): List<PlantType> {
    return this.map { it.type }.distinct()
}
